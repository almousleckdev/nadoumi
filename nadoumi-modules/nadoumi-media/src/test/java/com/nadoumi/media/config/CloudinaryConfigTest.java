package com.nadoumi.media.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.cloudinary.Cloudinary;
import com.nadoumi.media.mapper.MediaAccessLogMapper;
import com.nadoumi.media.mapper.MediaAssetMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Fail-fast behaviour of {@link MediaAutoConfiguration#cloudinary()}. The test
 * drives the credential through the {@code CLOUDINARY_URL} <em>system property</em>
 * (the bean reads the env var first, then this fallback) because a JUnit test
 * cannot set a real environment variable.
 */
class CloudinaryConfigTest {

    private static final String CLOUDINARY_URL = "CLOUDINARY_URL";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MediaAutoConfiguration.class))
            .withBean(MediaAssetMapper.class, () -> mock(MediaAssetMapper.class))
            .withBean(MediaAccessLogMapper.class, () -> mock(MediaAccessLogMapper.class));

    @BeforeEach
    @AfterEach
    void clearProperty() {
        System.clearProperty(CLOUDINARY_URL);
    }

    @Test
    void contextFailsWhenCloudinaryUrlMissing() {
        runner.run(context -> assertThat(context)
                .hasFailed()
                .getFailure()
                .hasMessageContaining(CLOUDINARY_URL));
    }

    @Test
    void contextFailsWhenCloudinaryUrlMalformed() {
        System.setProperty(CLOUDINARY_URL, "not-a-cloudinary-url");
        runner.run(context -> assertThat(context)
                .hasFailed()
                .getFailure()
                .hasMessageContaining(CLOUDINARY_URL));
    }

    @Test
    void buildsCloudinaryClientFromUrl() {
        System.setProperty(CLOUDINARY_URL, "cloudinary://key:secret@demo");
        runner.run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(Cloudinary.class);
            Cloudinary cloudinary = context.getBean(Cloudinary.class);
            assertThat(cloudinary.config.cloudName).isEqualTo("demo");
        });
    }

    @Test
    void bindsMediaPropertiesWithClampedTtl() {
        System.setProperty(CLOUDINARY_URL, "cloudinary://key:secret@demo");
        runner.withPropertyValues("nadoumi.media.env=staging",
                        "nadoumi.media.signed-url-ttl-seconds=5")
                .run(context -> {
                    MediaProperties props = context.getBean(MediaProperties.class);
                    assertThat(props.getEnv()).isEqualTo("staging");
                    assertThat(props.getSignedUrlTtlSeconds()).isEqualTo(MediaProperties.MIN_TTL);
                });
    }
}
