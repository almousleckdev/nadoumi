package com.ruoyi.nadoumi;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * The Nadoumi admin baseline (spec Revision 2 §17): {@code almousleck} is the
 * super-admin, the inherited RuoYi {@code admin} is a disabled break-glass account,
 * and the "initial password" nag is switched off.
 */
class AdminBootstrapTest extends AbstractNadIntegrationTest {

    @Test
    void inherited_admin_account_is_disabled() {
        String status = jdbc.queryForObject(
                "select status from sys_user where user_name = 'admin'", String.class);
        assertThat(status).isEqualTo("1");
    }

    @Test
    void almousleck_is_the_active_super_admin() {
        var row = jdbc.queryForMap(
                "select u.user_type, u.status, r.role_key "
                        + "from sys_user u join sys_user_role ur on ur.user_id = u.user_id "
                        + "join sys_role r on r.role_id = ur.role_id "
                        + "where u.user_name = 'almousleck'");
        assertThat(row.get("user_type")).isEqualTo("00");
        assertThat(row.get("status")).isEqualTo("0");
        assertThat(row.get("role_key")).isEqualTo("nadoumi_super_admin");
    }

    @Test
    void the_initial_password_modify_flag_is_off() {
        String value = jdbc.queryForObject(
                "select config_value from sys_config where config_key = 'sys.account.initPasswordModify'",
                String.class);
        assertThat(value).isEqualTo("0");
    }
}
