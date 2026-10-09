package com.nadoumi.scholarship.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.scholarship.domain.ScholarshipField;
import com.nadoumi.scholarship.domain.enums.EducationLevel;
import com.nadoumi.scholarship.domain.enums.FundingModel;
import com.nadoumi.scholarship.web.request.ScholarshipRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScholarshipFieldsTest {

    private static ScholarshipRequest request(String legacyField, List<ScholarshipRequest.FieldInput> fields,
            List<EducationLevel> levels) {
        return new ScholarshipRequest("Title", null, "CN", null, null, legacyField, fields, null, FundingModel.FULLY,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                levels, null, null, null, null, null, null, null, null);
    }

    private static ScholarshipRequest.FieldInput field(EducationLevel level, String name) {
        return new ScholarshipRequest.FieldInput(level, name);
    }

    @Test
    void shouldKeepSeveralFieldsAndTheirLevels() {
        ScholarshipRequest req = request(null, List.of(
                field(null, "Engineering"), field(EducationLevel.MASTER, "Medicine"), field(EducationLevel.PHD, "Medicine")),
                List.of(EducationLevel.MASTER, EducationLevel.PHD));

        assertThat(ScholarshipFields.normalise(req)).containsExactly(
                new ScholarshipField(null, "Engineering"),
                new ScholarshipField("MASTER", "Medicine"),
                new ScholarshipField("PHD", "Medicine"));
    }

    @Test
    void shouldTrimDropBlanksAndRemoveDuplicatesIgnoringCase_perLevel() {
        ScholarshipRequest req = request(null, List.of(
                field(null, "  Law "), field(null, "law"), field(null, "  "), field(EducationLevel.MASTER, "Law")),
                List.of(EducationLevel.MASTER));

        assertThat(ScholarshipFields.normalise(req)).containsExactly(
                new ScholarshipField(null, "Law"), new ScholarshipField("MASTER", "Law"));
    }

    @Test
    void shouldRefuseAFieldForALevelTheScholarshipDoesNotOffer() {
        ScholarshipRequest req = request(null, List.of(field(EducationLevel.PHD, "Physics")), List.of(EducationLevel.BACHELOR));

        assertThatThrownBy(() -> ScholarshipFields.normalise(req))
                .isInstanceOf(NadBadRequestException.class)
                .hasMessageContaining("Physics").hasMessageContaining("PHD");
    }

    @Test
    void shouldTreatTheOlderSingleFieldAsOneEveryLevelEntry_whenNoListIsSent() {
        ScholarshipRequest req = request(" Engineering ", null, List.of(EducationLevel.MASTER));

        assertThat(ScholarshipFields.normalise(req)).containsExactly(new ScholarshipField(null, "Engineering"));
    }

    @Test
    void shouldPreferTheListOverTheOlderSingleField() {
        ScholarshipRequest req = request("Old", List.of(field(null, "New")), List.of());

        assertThat(ScholarshipFields.normalise(req)).containsExactly(new ScholarshipField(null, "New"));
    }

    @Test
    void shouldBuildASummaryOfTheDistinctNames() {
        List<ScholarshipField> fields = List.of(new ScholarshipField(null, "Engineering"),
                new ScholarshipField("MASTER", "Medicine"), new ScholarshipField("PHD", "medicine"));

        assertThat(ScholarshipFields.summary(fields)).isEqualTo("Engineering, Medicine");
    }

    @Test
    void shouldCutTheSummaryAtAWholeNameSoItNeverOverflowsTheColumn() {
        List<ScholarshipField> many = java.util.stream.IntStream.range(0, 40)
                .mapToObj(i -> new ScholarshipField(null, "Discipline number " + i)).toList();

        String summary = ScholarshipFields.summary(many);

        assertThat(summary.length()).isLessThanOrEqualTo(ScholarshipFields.SUMMARY_MAX);
        assertThat(summary).endsWith("Discipline number " + (summary.split(", ").length - 1));
    }

    @Test
    void shouldGiveNoSummary_whenThereAreNoFields() {
        assertThat(ScholarshipFields.summary(List.of())).isNull();
    }
}
