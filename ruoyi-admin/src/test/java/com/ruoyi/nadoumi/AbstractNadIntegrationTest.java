package com.ruoyi.nadoumi;

import com.jayway.jsonpath.JsonPath;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.ISysUserService;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the full RuoYi + Nadoumi context against throwaway MySQL + Redis containers.
 * Flyway builds the full migration set into the fresh database. Auto-skips without Docker.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles({ "druid", "test" })
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractNadIntegrationTest {

    protected static final String PASSWORD = "nad-test-pass-1";

    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withUrlParam("serverTimezone", "UTC")
            .withUrlParam("useSSL", "false")
            .withUrlParam("allowPublicKeyRetrieval", "true");

    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    static {
        // Started here (not via @Container) so ports are available when
        // @DynamicPropertySource is evaluated. Guarded so a Docker-less run still
        // reaches the @Testcontainers disabled condition and skips cleanly.
        if (DockerClientFactory.instance().isDockerAvailable()) {
            MYSQL.start();
            REDIS.start();
        }
    }

    @DynamicPropertySource
    static void wire(DynamicPropertyRegistry r) {
        r.add("spring.datasource.druid.master.url", MYSQL::getJdbcUrl);
        r.add("spring.datasource.druid.master.username", MYSQL::getUsername);
        r.add("spring.datasource.druid.master.password", MYSQL::getPassword);
        // give Flyway its own plain DataSource so migrations don't go through Druid
        r.add("spring.flyway.url", MYSQL::getJdbcUrl);
        r.add("spring.flyway.user", MYSQL::getUsername);
        r.add("spring.flyway.password", MYSQL::getPassword);
        r.add("spring.data.redis.host", REDIS::getHost);
        r.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        // Keep the ITs hermetic: a developer's ./config/application-local.yml is
        // still picked up via spring.config.import and may point mail at a real
        // Mailpit. Force the no-network adapter (@DynamicPropertySource wins over
        // every file), so LoggingMailSender + DevMailController are the ones wired.
        r.add("nadoumi.mail.transport", () -> "log");
        r.add("spring.mail.host", () -> "localhost");
        r.add("spring.mail.port", () -> "1025");
    }

    @Autowired protected MockMvc mvc;
    @Autowired protected ISysUserService userService;
    @Autowired private ISysConfigService configService;
    @Autowired private DataSource dataSource;

    protected JdbcTemplate jdbc;

    @BeforeEach
    void baseSetup() {
        jdbc = new JdbcTemplate(dataSource);
        jdbc.update("update sys_config set config_value = 'false' where config_key = 'sys.account.captchaEnabled'");
        configService.resetConfigCache(); // pull the captcha flag change through the Redis cache
        jdbc.update("delete from nad_user_applicant_access");
        jdbc.update("delete from nad_applicant_education");
        jdbc.update("delete from nad_applicant_test_score");
        jdbc.update("delete from nad_applicant_contact");
        jdbc.update("delete from nad_applicant");
        jdbc.update("delete from nad_university");
        jdbc.update("delete from nad_contact_inquiry");
        jdbc.update("delete from sys_user_role where user_id > 3");
        jdbc.update("delete from sys_user where user_id > 3");
    }

    protected long createStudent(String username) {
        SysUser u = newUser(username, username + "@example.test");
        userService.registerUser(u);
        jdbc.update("update sys_user set user_type = '10' where user_id = ?", u.getUserId());
        return u.getUserId();
    }

    protected long createStaff(String username, String roleKey) {
        SysUser u = newUser(username, username + "@staff.test");
        userService.registerUser(u);
        jdbc.update("update sys_user set user_type = '00' where user_id = ?", u.getUserId());
        Long roleId = jdbc.queryForObject("select role_id from sys_role where role_key = ?", Long.class, roleKey);
        jdbc.update("insert into sys_user_role (user_id, role_id) values (?, ?)", u.getUserId(), roleId);
        return u.getUserId();
    }

    protected String staffToken(String username) throws Exception {
        return tokenFrom("/login", "{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}");
    }

    /** Student login is email-first (Revision 2); {@link #createStudent} uses {@code <username>@example.test}. */
    protected String studentToken(String username) throws Exception {
        return tokenFrom("/api/student/login",
                "{\"email\":\"" + username + "@example.test\",\"password\":\"" + PASSWORD + "\"}");
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }

    private SysUser newUser(String username, String email) {
        SysUser u = new SysUser();
        u.setUserName(username);
        u.setNickName(username);
        u.setEmail(email);
        u.setStatus("0");
        u.setPassword(SecurityUtils.encryptPassword(PASSWORD));
        return u;
    }

    private String tokenFrom(String path, String jsonBody) throws Exception {
        String res = mvc.perform(post(path).contentType("application/json").content(jsonBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(res, "$.token");
    }
}
