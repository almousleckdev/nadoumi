package com.nadoumi.media.config;

import com.cloudinary.Cloudinary;
import com.nadoumi.common.media.MediaGateway;
import com.nadoumi.common.media.MediaStorageService;
import com.nadoumi.media.job.MediaReconciliationJob;
import com.nadoumi.media.mapper.MediaAccessLogMapper;
import com.nadoumi.media.mapper.MediaAssetMapper;
import com.nadoumi.media.service.MediaAccessLogWriter;
import com.nadoumi.media.service.MediaService;
import com.nadoumi.media.spi.CloudinaryMediaStorage;
import com.nadoumi.media.spi.LocalFilesystemMediaStorage;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * The single wiring point for {@code nadoumi-media}. Registered as a Spring Boot
 * auto-configuration (see
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports})
 * rather than component-scanned, so every bean is {@link ConditionalOnMissingBean}
 * and a test can substitute its own {@code MediaStorageService} / {@code MediaGateway}
 * without any provider client being built.
 *
 * <p>Storage provider selection:</p>
 * <ul>
 *   <li><b>{@code CLOUDINARY_URL} set</b> — the production path:
 *       {@link CloudinaryMediaStorage} over the Cloudinary client.</li>
 *   <li><b>{@code CLOUDINARY_URL} absent</b> — a local-development fallback:
 *       {@link LocalFilesystemMediaStorage}, writing under {@code ruoyi.profile}
 *       and serving from {@code /profile/media/**}. Not for any real environment.</li>
 * </ul>
 */
@AutoConfiguration
@EnableConfigurationProperties(MediaProperties.class)
public class MediaAutoConfiguration {

    static final String CLOUDINARY_URL_VAR = "CLOUDINARY_URL";
    static final String CLOUDINARY_URL_PROPERTY = "nadoumi.media.cloudinary-url";

    /**
     * Resolve the Cloudinary connection string from (in order): the
     * {@code CLOUDINARY_URL} env var, the same as a JVM system property, the
     * {@code CLOUDINARY_URL} Spring property, or {@code nadoumi.media.cloudinary-url}
     * (so it can also sit in {@code config/application-local.yml}).
     */
    static String resolveCloudinaryUrl(org.springframework.core.env.Environment env) {
        String fromEnv = System.getenv(CLOUDINARY_URL_VAR);
        if (StringUtils.hasText(fromEnv)) {
            return fromEnv;
        }
        String fromSys = System.getProperty(CLOUDINARY_URL_VAR);
        if (StringUtils.hasText(fromSys)) {
            return fromSys;
        }
        if (env != null) {
            String fromProp = env.getProperty(CLOUDINARY_URL_VAR);
            if (StringUtils.hasText(fromProp)) {
                return fromProp;
            }
            return env.getProperty(CLOUDINARY_URL_PROPERTY);
        }
        return null;
    }

    /** True when a Cloudinary URL can be resolved from any of the supported sources. */
    static final class CloudinaryConfigured implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return StringUtils.hasText(resolveCloudinaryUrl(context.getEnvironment()));
        }
    }

    /**
     * The Cloudinary client, built from the resolved Cloudinary URL
     * ({@code cloudinary://<key>:<secret>@<cloud>}). Only created when one is
     * present ({@link CloudinaryConfigured}); a malformed value still fails fast.
     */
    @Bean
    @Conditional(CloudinaryConfigured.class)
    @ConditionalOnMissingBean({ Cloudinary.class, MediaStorageService.class })
    Cloudinary cloudinary(org.springframework.core.env.Environment env) {
        String url = resolveCloudinaryUrl(env);
        Cloudinary cloudinary;
        try {
            cloudinary = new Cloudinary(url);
        } catch (RuntimeException e) {
            throw new IllegalStateException(CLOUDINARY_URL_VAR + " is malformed", e);
        }
        if (!StringUtils.hasText(cloudinary.config.cloudName)) {
            throw new IllegalStateException(CLOUDINARY_URL_VAR + " is malformed: missing cloud name");
        }
        return cloudinary;
    }

    @Bean
    @ConditionalOnBean(Cloudinary.class)
    @ConditionalOnMissingBean(MediaStorageService.class)
    MediaStorageService cloudinaryMediaStorage(Cloudinary cloudinary, MediaAssetMapper assetMapper,
            MediaProperties properties) {
        org.slf4j.LoggerFactory.getLogger(MediaAutoConfiguration.class)
                .info("media storage: Cloudinary (cloud={})", cloudinary.config.cloudName);
        return new CloudinaryMediaStorage(cloudinary, assetMapper, properties);
    }

    /**
     * Local-development fallback when no Cloudinary client is present. Files go
     * under {@code <ruoyi.profile>/media}; URLs point at
     * {@code <nadoumi.media.local.base-url>/profile/media/...} (default
     * {@code http://localhost:8080}).
     */
    @Bean
    @ConditionalOnMissingBean(MediaStorageService.class)
    MediaStorageService localFilesystemMediaStorage(MediaAssetMapper assetMapper, MediaProperties properties,
            Environment env) {
        String profileDir = env.getProperty("ruoyi.profile", System.getProperty("user.home") + "/nadoumi/upload");
        String baseUrl = env.getProperty("nadoumi.media.local.base-url", "http://localhost:8080");
        Path baseDir = Paths.get(profileDir, "media");
        return new LocalFilesystemMediaStorage(assetMapper, properties, baseDir, baseUrl);
    }

    @Bean
    @ConditionalOnMissingBean
    MediaAccessLogWriter mediaAccessLogWriter(MediaAccessLogMapper accessLogMapper) {
        return new MediaAccessLogWriter(accessLogMapper);
    }

    @Bean
    @ConditionalOnMissingBean(MediaGateway.class)
    MediaGateway mediaService(MediaStorageService mediaStorageService, MediaAccessLogWriter accessLogWriter,
            MediaProperties properties) {
        return new MediaService(mediaStorageService, accessLogWriter, properties);
    }

    /**
     * The media reconciliation Quartz job. Registered under the bean name
     * {@code mediaReconciliationJob} so a {@code sys_job} row can invoke it as
     * {@code mediaReconciliationJob.run()} (seeded, paused, by V29).
     */
    @Bean
    @ConditionalOnMissingBean
    MediaReconciliationJob mediaReconciliationJob(MediaStorageService mediaStorageService,
            MediaAssetMapper assetMapper, MediaAccessLogMapper accessLogMapper, MediaProperties properties) {
        return new MediaReconciliationJob(assetMapper, accessLogMapper, mediaStorageService, properties);
    }
}
