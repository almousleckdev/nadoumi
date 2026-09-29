package com.nadoumi.scholarship.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nadoumi.scholarship.domain.ScholarshipEligibility;
import com.nadoumi.scholarship.domain.ScholarshipFee;
import com.nadoumi.scholarship.domain.ScholarshipIntake;
import com.nadoumi.scholarship.domain.enums.EducationLevel;
import com.nadoumi.scholarship.domain.enums.FeeKind;
import com.nadoumi.scholarship.domain.enums.FundingModel;
import com.nadoumi.scholarship.mapper.ScholarshipMapper;
import com.nadoumi.scholarship.web.request.ScholarshipRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ScholarshipChildrenWriterTest {

    private static final long ID = 7L;

    private final ScholarshipMapper mapper = mock(ScholarshipMapper.class);
    private final ScholarshipChildrenWriter writer = new ScholarshipChildrenWriter(mapper);

    private static ScholarshipRequest request(FundingModel funding, List<EducationLevel> levels, List<String> categories,
            List<ScholarshipRequest.IntakeInput> intakes, ScholarshipRequest.EligibilityInput eligibility,
            List<ScholarshipRequest.FeeInput> fees, List<ScholarshipRequest.DocumentRequirementInput> documents) {
        return new ScholarshipRequest("Title", null, "CN", null, null, null, null, funding, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, levels, categories, intakes, eligibility, fees, null,
                null, null, documents);
    }

    private static ScholarshipRequest categoriesOnly(FundingModel funding, List<String> categories) {
        return request(funding, null, categories, null, null, null, null);
    }

    private List<String> linkedCategories() {
        ArgumentCaptor<String> codes = ArgumentCaptor.forClass(String.class);
        verify(mapper, org.mockito.Mockito.atLeast(0)).insertCategoryLink(eq(ID), codes.capture());
        return codes.getAllValues();
    }

    @Test
    void shouldClearEveryChildCollection_evenWhenTheRequestOmitsThem() {
        writer.replace(ID, categoriesOnly(FundingModel.FULLY, null));

        verify(mapper).deleteLevels(ID);
        verify(mapper).deleteCategoryLinks(ID);
        verify(mapper).deleteIntakes(ID);
        verify(mapper).deleteEligibility(ID);
        verify(mapper).deleteFees(ID);
        verify(mapper).deleteLevelStipends(ID);
        verify(mapper).deleteAccommodations(ID);
        verify(mapper).deleteCoverage(ID);
        verify(mapper).deleteDocumentRequirements(ID);
        verify(mapper, never()).insertLevel(anyLong(), anyString());
        verify(mapper, never()).insertCategoryLink(anyLong(), anyString());
    }

    @Test
    void shouldLinkNoCategoriesToASelfFundedScholarship() {
        writer.replace(ID, categoriesOnly(FundingModel.SELF, List.of("CSC", "OTHER")));

        assertThat(linkedCategories()).isEmpty();
    }

    @Test
    void shouldExcludeFullyFundedSchemesFromAPartiallyFundedScholarship() {
        writer.replace(ID, categoriesOnly(FundingModel.PARTIAL, List.of("CSC", "cgs", "TYPE_A", "PROVINCIAL")));

        assertThat(linkedCategories()).containsExactly("PROVINCIAL");
    }

    @Test
    void shouldKeepEveryCategoryForAFullyFundedScholarship_normalisedAndDistinct() {
        writer.replace(ID, categoriesOnly(FundingModel.FULLY, List.of(" csc ", "CSC", "type_a")));

        assertThat(linkedCategories()).containsExactly("CSC", "TYPE_A");
    }

    @Test
    void shouldInsertEachLevelOnce() {
        writer.replace(ID, request(FundingModel.FULLY, List.of(EducationLevel.MASTER, EducationLevel.MASTER, EducationLevel.PHD),
                null, null, null, null, null));

        verify(mapper).insertLevel(ID, "MASTER");
        verify(mapper).insertLevel(ID, "PHD");
        verify(mapper, org.mockito.Mockito.times(2)).insertLevel(eq(ID), anyString());
    }

    @Test
    void shouldTrimIntakeTermsAndKeepTheirOrder() {
        var first = new ScholarshipRequest.IntakeInput(" AUTUMN_SEPTEMBER ", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 4, 1));
        var second = new ScholarshipRequest.IntakeInput("SPRING_MARCH", null, null);
        writer.replace(ID, request(FundingModel.FULLY, null, null, List.of(first, second), null, null, null));

        ArgumentCaptor<ScholarshipIntake> intake = ArgumentCaptor.forClass(ScholarshipIntake.class);
        ArgumentCaptor<Integer> order = ArgumentCaptor.forClass(Integer.class);
        verify(mapper, org.mockito.Mockito.times(2)).insertIntake(eq(ID), intake.capture(), order.capture());
        assertThat(intake.getAllValues().get(0).term()).isEqualTo("AUTUMN_SEPTEMBER");
        assertThat(order.getAllValues()).containsExactly(0, 1);
    }

    @Test
    void shouldDefaultTheNationalityScopeToAnyAndBlankOutEmptyText() {
        var eligibility = new ScholarshipRequest.EligibilityInput(18, 35, null, "   ", null, null, null, null, null, null, null, "  ");
        writer.replace(ID, request(FundingModel.FULLY, null, null, null, eligibility, null, null));

        ArgumentCaptor<ScholarshipEligibility> saved = ArgumentCaptor.forClass(ScholarshipEligibility.class);
        verify(mapper).insertEligibility(eq(ID), saved.capture());
        assertThat(saved.getValue().nationalityScope()).isEqualTo("ANY");
        assertThat(saved.getValue().acceptedCountries()).isNull();
        assertThat(saved.getValue().notes()).isNull();
        assertThat(saved.getValue().ageMax()).isEqualTo(35);
    }

    @Test
    void shouldUppercaseFeeCurrencyAndBlankOutEmptyNotes() {
        var fee = new ScholarshipRequest.FeeInput(FeeKind.REGISTRATION, new BigDecimal("400"), "cny", " ");
        writer.replace(ID, request(FundingModel.FULLY, null, null, null, null, List.of(fee), null));

        ArgumentCaptor<ScholarshipFee> saved = ArgumentCaptor.forClass(ScholarshipFee.class);
        verify(mapper).insertFee(eq(ID), saved.capture(), eq(0));
        assertThat(saved.getValue().currency()).isEqualTo("CNY");
        assertThat(saved.getValue().note()).isNull();
    }

    @Test
    void shouldUppercaseDocumentTypesAndDefaultToMandatory() {
        var doc = new ScholarshipRequest.DocumentRequirementInput(" passport ", null, null);
        writer.replace(ID, request(FundingModel.FULLY, null, null, null, null, null, List.of(doc)));

        ArgumentCaptor<com.nadoumi.scholarship.domain.ScholarshipDocumentRequirement> saved =
                ArgumentCaptor.forClass(com.nadoumi.scholarship.domain.ScholarshipDocumentRequirement.class);
        verify(mapper).insertDocumentRequirement(eq(ID), saved.capture(), anyInt());
        assertThat(saved.getValue().docType()).isEqualTo("PASSPORT");
        assertThat(saved.getValue().mandatory()).isTrue();
    }
}
