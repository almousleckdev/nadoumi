package com.nadoumi.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.nadoumi.application.service.RequirementApplicability.Facts;
import com.nadoumi.common.document.RequirementCondition;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class RequirementApplicabilityTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 22);

    private static boolean applies(RequirementCondition c, Facts f) {
        return RequirementApplicability.applies(c, f, TODAY);
    }

    @Test
    void shouldAlwaysApply_withoutACondition() {
        assertThat(applies(null, new Facts(null, false, false, "BACHELOR"))).isTrue();
    }

    @Test
    void shouldTreatUnderEighteenAsAMinor_onTheBirthdayExactly() {
        assertThat(applies(RequirementCondition.MINOR, new Facts(LocalDate.of(2008, 10, 23), null, null, null))).isTrue();  // 17 until tomorrow
        assertThat(applies(RequirementCondition.MINOR, new Facts(LocalDate.of(2008, 10, 22), null, null, null))).isFalse(); // turns 18 today
        assertThat(applies(RequirementCondition.MINOR, new Facts(LocalDate.of(1995, 1, 1), null, null, null))).isFalse();
    }

    @Test
    void shouldFollowTheLocationFacts() {
        assertThat(applies(RequirementCondition.IN_CHINA, new Facts(null, true, null, null))).isTrue();
        assertThat(applies(RequirementCondition.IN_CHINA, new Facts(null, false, null, null))).isFalse();
        assertThat(applies(RequirementCondition.STUDIED_IN_CHINA, new Facts(null, null, true, null))).isTrue();
        assertThat(applies(RequirementCondition.STUDIED_IN_CHINA, new Facts(null, null, false, null))).isFalse();
    }

    @Test
    void shouldFollowTheStudyLevel() {
        assertThat(applies(RequirementCondition.MASTER_OR_DOCTORAL, new Facts(null, null, null, "MASTER"))).isTrue();
        assertThat(applies(RequirementCondition.MASTER_OR_DOCTORAL, new Facts(null, null, null, "PHD"))).isTrue();
        assertThat(applies(RequirementCondition.MASTER_OR_DOCTORAL, new Facts(null, null, null, "BACHELOR"))).isFalse();
        assertThat(applies(RequirementCondition.DOCTORAL, new Facts(null, null, null, "MASTER"))).isFalse();
        assertThat(applies(RequirementCondition.DOCTORAL, new Facts(null, null, null, "DOCTORATE"))).isTrue();
    }

    @Test
    void shouldAskForTheDocument_whenTheFactIsNotKnown() {
        Facts unknown = new Facts(null, null, null, null);
        for (RequirementCondition c : RequirementCondition.values()) {
            assertThat(applies(c, unknown)).as(c.name()).isTrue();
        }
    }

    @Test
    void shouldPickTheHighestLevel() {
        assertThat(RequirementApplicability.highestLevel(List.of("BACHELOR", "MASTER"))).isEqualTo("MASTER");
        assertThat(RequirementApplicability.highestLevel(List.of("master", "PHD"))).isEqualTo("PHD");
        assertThat(RequirementApplicability.highestLevel(java.util.Arrays.asList(null, null))).isNull();
    }
}
