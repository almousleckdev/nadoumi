package com.nadoumi.identity.access;

import static com.nadoumi.common.access.ApplicantCapability.EDIT_PROFILE;
import static com.nadoumi.common.access.ApplicantCapability.MANAGE_ACCESS;
import static com.nadoumi.common.access.ApplicantCapability.SUBMIT_APPLICATION;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CapabilityOverridesTest {

    @Test
    void nullOrBlankJson_isEmpty() {
        assertThat(CapabilityOverrides.parse(null).isEmpty()).isTrue();
        assertThat(CapabilityOverrides.parse("   ").isEmpty()).isTrue();
        assertThat(CapabilityOverrides.parse("{}").isEmpty()).isTrue();
    }

    @Test
    void parsesAddAndRemoveLists() {
        var o = CapabilityOverrides.parse("{\"add\":[\"SUBMIT_APPLICATION\"],\"remove\":[\"EDIT_PROFILE\"]}");
        assertThat(o.add()).containsExactly(SUBMIT_APPLICATION);
        assertThat(o.remove()).containsExactly(EDIT_PROFILE);
        assertThat(o.isEmpty()).isFalse();
    }

    @Test
    void unknownCapabilityNamesAreIgnored() {
        var o = CapabilityOverrides.parse("{\"add\":[\"BOGUS\",\"MANAGE_ACCESS\"]}");
        assertThat(o.add()).containsExactly(MANAGE_ACCESS);
        assertThat(o.remove()).isEmpty();
    }

    @Test
    void roundTripsThroughJson() {
        var o = new CapabilityOverrides(java.util.EnumSet.of(SUBMIT_APPLICATION), java.util.EnumSet.of(EDIT_PROFILE));
        assertThat(CapabilityOverrides.parse(o.toJson())).isEqualTo(o);
    }
}
