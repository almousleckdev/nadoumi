package com.nadoumi.media;

import static org.assertj.core.api.Assertions.assertThat;

import com.nadoumi.common.media.MediaAccessClass;
import com.nadoumi.common.media.MediaCategory;
import com.nadoumi.media.policy.MediaCategoryPolicy;
import org.junit.jupiter.api.Test;

class MediaCategoryPolicyTest {

    @Test
    void logoIsPublicImage4mb() {
        var r = MediaCategoryPolicy.of(MediaCategory.UNIVERSITY_LOGO);
        assertThat(r.defaultAccessClass()).isEqualTo(MediaAccessClass.PUBLIC);
        assertThat(r.allowedMime()).containsExactlyInAnyOrder("image/jpeg", "image/png", "image/webp");
        assertThat(r.maxBytes()).isEqualTo(4L * 1024 * 1024);
        assertThat(r.resourceType()).isEqualTo("image");
    }

    @Test
    void jw202IsSensitivePdf20mb() {
        var r = MediaCategoryPolicy.of(MediaCategory.JW202);
        assertThat(r.defaultAccessClass()).isEqualTo(MediaAccessClass.SENSITIVE);
        assertThat(r.allowedMime()).contains("application/pdf");
        assertThat(r.maxBytes()).isEqualTo(20L * 1024 * 1024);
    }

    @Test
    void folderIsEnvNamespaced() {
        assertThat(MediaCategoryPolicy.folder(MediaCategory.SCHOLARSHIP_HERO, "prod"))
                .isEqualTo("nadoumi/prod/scholarship/hero");
    }

    @Test
    void everyCategoryHasARule() {
        for (var c : MediaCategory.values()) {
            assertThat(MediaCategoryPolicy.of(c)).isNotNull();
        }
    }
}
