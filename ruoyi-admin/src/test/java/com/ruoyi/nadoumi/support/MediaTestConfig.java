package com.ruoyi.nadoumi.support;

import com.nadoumi.common.media.MediaStorageService;
import com.nadoumi.media.FakeMediaStorage;
import com.nadoumi.media.mapper.MediaAssetMapper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test wiring for {@code nadoumi-media}: registers the in-memory
 * {@link FakeMediaStorage} as a {@code @Primary} {@link MediaStorageService} so the
 * integration context never builds a real Cloudinary client. The
 * {@code @ConditionalOnMissingBean(MediaStorageService.class)} guards on
 * {@code MediaAutoConfiguration#cloudinary()} then back off and {@code CLOUDINARY_URL}
 * is never read.
 *
 * <p>Only the storage SPI is faked. The real {@code MediaService}
 * ({@code MediaGateway}) still builds on top of it, so ITs exercise the genuine
 * validation and access-log code paths against a fake backend.</p>
 */
@TestConfiguration(proxyBeanMethods = false)
public class MediaTestConfig {

    @Bean
    @Primary
    MediaStorageService fakeMediaStorage(MediaAssetMapper mapper) {
        return new FakeMediaStorage(mapper);
    }
}
