package com.nadoumi.common.access;

import static com.nadoumi.common.access.ApplicantCapability.CREATE_APPLICATION;
import static com.nadoumi.common.access.ApplicantCapability.EDIT_PROFILE;
import static com.nadoumi.common.access.ApplicantCapability.MANAGE_ACCESS;
import static com.nadoumi.common.access.ApplicantCapability.MESSAGE_STAFF;
import static com.nadoumi.common.access.ApplicantCapability.SUBMIT_APPLICATION;
import static com.nadoumi.common.access.ApplicantCapability.UPLOAD_DOCUMENT;
import static com.nadoumi.common.access.ApplicantCapability.VIEW_APPLICATION;
import static com.nadoumi.common.access.ApplicantCapability.VIEW_DOCUMENT;
import static com.nadoumi.common.access.ApplicantCapability.VIEW_PROFILE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * The default role -&gt; capability matrix is frozen in {@code docs/DOMAIN_MODEL.md} §4.2.
 * These assertions are the executable copy of that table; changing them means the
 * documented baseline changed.
 */
class AccessCapabilityMatrixTest {

    @Test
    void owner_holds_every_capability() {
        assertThat(AccessCapabilityMatrix.defaultCapabilities(AccessRole.OWNER))
                .containsExactlyInAnyOrder(ApplicantCapability.values());
    }

    @Test
    void agent_holds_everything_except_manage_access() {
        assertThat(AccessCapabilityMatrix.defaultCapabilities(AccessRole.AGENT))
                .containsExactlyInAnyOrder(VIEW_PROFILE, EDIT_PROFILE, VIEW_APPLICATION,
                        CREATE_APPLICATION, SUBMIT_APPLICATION, UPLOAD_DOCUMENT, VIEW_DOCUMENT,
                        MESSAGE_STAFF)
                .doesNotContain(MANAGE_ACCESS);
    }

    @Test
    void guardian_default_is_view_edit_upload_message_only() {
        assertThat(AccessCapabilityMatrix.defaultCapabilities(AccessRole.GUARDIAN))
                .containsExactlyInAnyOrder(VIEW_PROFILE, EDIT_PROFILE, VIEW_APPLICATION,
                        VIEW_DOCUMENT, UPLOAD_DOCUMENT, MESSAGE_STAFF)
                .doesNotContain(CREATE_APPLICATION, SUBMIT_APPLICATION, MANAGE_ACCESS);
    }

    @Test
    void viewer_is_read_only() {
        assertThat(AccessCapabilityMatrix.defaultCapabilities(AccessRole.VIEWER))
                .containsExactlyInAnyOrder(VIEW_PROFILE, VIEW_APPLICATION, VIEW_DOCUMENT);
    }

    @Test
    void default_capability_set_is_immutable() {
        Set<ApplicantCapability> caps = AccessCapabilityMatrix.defaultCapabilities(AccessRole.VIEWER);
        assertThatThrownBy(() -> caps.add(MANAGE_ACCESS))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void override_can_add_a_privileged_capability_to_a_guardian() {
        Set<ApplicantCapability> effective = AccessCapabilityMatrix.effectiveCapabilities(
                AccessRole.GUARDIAN, EnumSet.of(SUBMIT_APPLICATION), EnumSet.noneOf(ApplicantCapability.class));

        assertThat(effective).contains(SUBMIT_APPLICATION, EDIT_PROFILE);
    }

    @Test
    void override_can_remove_a_default_capability() {
        Set<ApplicantCapability> effective = AccessCapabilityMatrix.effectiveCapabilities(
                AccessRole.VIEWER, EnumSet.noneOf(ApplicantCapability.class), EnumSet.of(VIEW_DOCUMENT));

        assertThat(effective).containsExactlyInAnyOrder(VIEW_PROFILE, VIEW_APPLICATION);
    }

    @Test
    void remove_wins_over_add_for_the_same_capability() {
        Set<ApplicantCapability> effective = AccessCapabilityMatrix.effectiveCapabilities(
                AccessRole.VIEWER, EnumSet.of(MANAGE_ACCESS), EnumSet.of(MANAGE_ACCESS));

        assertThat(effective).doesNotContain(MANAGE_ACCESS);
    }

    @Test
    void hasCapability_reflects_defaults_and_overrides() {
        assertThat(AccessCapabilityMatrix.hasCapability(AccessRole.GUARDIAN, SUBMIT_APPLICATION, null, null))
                .isFalse();
        assertThat(AccessCapabilityMatrix.hasCapability(AccessRole.GUARDIAN, SUBMIT_APPLICATION,
                EnumSet.of(SUBMIT_APPLICATION), null)).isTrue();
        assertThat(AccessCapabilityMatrix.hasCapability(AccessRole.OWNER, MANAGE_ACCESS, null, null))
                .isTrue();
    }
}
