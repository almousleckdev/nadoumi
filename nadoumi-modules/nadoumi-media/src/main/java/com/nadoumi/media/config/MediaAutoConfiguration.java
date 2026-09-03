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
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

/**
 * The single wiring point for {@code nadoumi-media}. Registered as a Spring Boot
 * auto-configuration (see
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports})
 * rather than component-scanned, so every bean here is
 * {@link ConditionalOnMissingBean} and a test can substitute its own
 * {@code MediaStorageService} / {@code MediaGateway} without the Cloudinary client
 * ever being built.
 *
 * <p>Fail-fast on a missing {@code CLOUDINARY_URL} lives in this
 * {@link #cloudinary()} bean method (spec §I.10): the context refuses to start.
 * The bean is also suppressed when a {@code MediaStorageService} is already
 * present, so a {@code FakeMediaStorage}-based test never reads the variable.</p>
 */
@AutoConfiguration
@EnableConfigurationProperties(MediaProperties.class)
public class MediaAutoConfiguration {

    private static final String CLOUDINARY_URL_VAR = "CLOUDINARY_URL";

    /**
     * The Cloudinary client, built from {@code CLOUDINARY_URL}
     * ({@code cloudinary://<key>:<secret>@<cloud>}). Read from the environment
     * first, then a JVM system property (so tests can set it without spawning a
     * new process).
     *
     * @throws IllegalStateException when the variable is absent or malformed
     */
    @Bean
    @ConditionalOnMissingBean({ Cloudinary.class, MediaStorageService.class })
    Cloudinary cloudinary() {
        String fromEnv = System.getenv(CLOUDINARY_URL_VAR);
        String url = StringUtils.hasText(fromEnv) ? fromEnv : System.getProperty(CLOUDINARY_URL_VAR);
        if (!StringUtils.hasText(url)) {
            throw new IllegalStateException(CLOUDINARY_URL_VAR + " is not configured");
        }
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
    @ConditionalOnMissingBean(MediaStorageService.class)
    MediaStorageService cloudinaryMediaStorage(Cloudinary cloudinary, MediaAssetMapper assetMapper,
            MediaProperties properties) {
        return new CloudinaryMediaStorage(cloudinary, assetMapper, properties);
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
