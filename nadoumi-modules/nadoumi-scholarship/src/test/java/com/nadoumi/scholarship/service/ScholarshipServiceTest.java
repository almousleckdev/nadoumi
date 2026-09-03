package com.nadoumi.scholarship.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.common.media.MediaGateway;
import com.nadoumi.identity.exception.NadNotFoundException;
import com.nadoumi.scholarship.domain.Scholarship;
import com.nadoumi.scholarship.domain.ScholarshipEligibility;
import com.nadoumi.scholarship.domain.ScholarshipFee;
import com.nadoumi.scholarship.domain.enums.FundingModel;
import com.nadoumi.scholarship.domain.enums.PublishStatus;
import com.nadoumi.scholarship.domain.enums.ScholarshipStatus;
import com.nadoumi.scholarship.mapper.ScholarshipFacetRow;
import com.nadoumi.scholarship.mapper.ScholarshipMapper;
import com.nadoumi.scholarship.mapper.ScholarshipSearch;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScholarshipServiceTest {

    private final ScholarshipMapper mapper = mock(ScholarshipMapper.class);
    private final MediaGateway media = mock(MediaGateway.class);
    private final ScholarshipService service = new ScholarshipService(mapper, media);

    private static ScholarshipSearch anyFilter() {
        return new ScholarshipSearch(null, null, null, null, null, null, null, null, null,
                List.of(), List.of(), List.of(), null, null, null, null, null, null);
    }

    private static Scholarship row(long id) {
        Scholarship s = new Scholarship();
        s.setId(id);
        s.setSlug("csc-" + id);
        s.setTitle("CSC Scholarship " + id);
        s.setCountry("CN");
        s.setFundingModel(FundingModel.FULLY);
        s.setPublishStatus(PublishStatus.PUBLISHED);
        s.setStatus(ScholarshipStatus.ACTIVE);
        return s;
    }

    @Test
    void list_maps_rows_and_loads_the_card_children_for_each() {
        when(mapper.searchPublic(any())).thenReturn(List.of(row(1), row(2)));
        when(mapper.findLevels(anyLong())).thenReturn(List.of("MASTER"));
        when(mapper.findCategories(anyLong())).thenReturn(List.of());
        when(mapper.findIntakes(anyLong())).thenReturn(List.of());

        var page = service.list(anyFilter(), 0, 12);

        assertThat(page.content()).hasSize(2);
        assertThat(page.content().get(0).levels()).containsExactly("MASTER");
        verify(mapper).findLevels(1L);
        verify(mapper).findLevels(2L);
    }

    @Test
    void getPublic_throws_not_found_when_the_slug_is_not_in_the_student_view() {
        when(mapper.findPublicBySlug("ghost")).thenReturn(null);

        assertThatThrownBy(() -> service.getPublic("ghost"))
                .isInstanceOf(NadNotFoundException.class);
    }

    @Test
    void getPublic_assembles_every_student_safe_child_for_the_detail_view() {
        when(mapper.findPublicById(5L)).thenReturn(row(5));
        when(mapper.findLevels(5L)).thenReturn(List.of("PHD"));
        when(mapper.findCategories(5L)).thenReturn(List.of());
        when(mapper.findIntakes(5L)).thenReturn(List.of());
        when(mapper.findEligibility(5L)).thenReturn(new ScholarshipEligibility(
                18, 35, "ANY", null, null, new BigDecimal("3.0"), null, null, null, null, null, null));
        when(mapper.findFees(5L)).thenReturn(List.of(
                new ScholarshipFee("APPLICATION", new BigDecimal("710.00"), "CNY", null)));
        when(mapper.findLevelStipends(5L)).thenReturn(List.of());
        when(mapper.findAccommodations(5L)).thenReturn(List.of());
        when(mapper.findCoverage(5L)).thenReturn(List.of(
                new com.nadoumi.scholarship.domain.ScholarshipCoverage("TUITION", "Full waiver"),
                new com.nadoumi.scholarship.domain.ScholarshipCoverage("MEDICAL_INSURANCE", null)));
        when(mapper.findDocumentRequirements(5L)).thenReturn(List.of());

        var detail = service.getPublic("5");

        assertThat(detail.eligibility().ageMin()).isEqualTo(18);
        assertThat(detail.fees()).hasSize(1);
        // 710 CNY -> 710 RMB, 100 USD at the fixed display rate
        assertThat(detail.fees().get(0).amountRmb()).isEqualByComparingTo("710");
        assertThat(detail.fees().get(0).amountUsd()).isEqualByComparingTo("100");
        assertThat(detail.stipends()).isEmpty();
        assertThat(detail.accommodation()).isEmpty();
        assertThat(detail.coverage()).extracting(c -> c.kind()).containsExactly("TUITION", "MEDICAL_INSURANCE");
    }

    @Test
    void getPublic_resolves_heroUrl_from_media_id_without_touching_a_confidential_field() {
        Scholarship s = row(9);
        s.setHeroMediaId(9L);
        s.setHeroImageUrl("/profile/legacy-hero.png");
        when(mapper.findPublicById(9L)).thenReturn(s);
        when(mapper.findLevels(9L)).thenReturn(List.of());
        when(mapper.findCategories(9L)).thenReturn(List.of());
        when(mapper.findIntakes(9L)).thenReturn(List.of());
        when(media.publicUrl(9L)).thenReturn("https://res.cloudinary.com/x/hero.png");

        var detail = service.getPublic("9");

        assertThat(detail.heroMediaId()).isEqualTo(9L);
        assertThat(detail.heroUrl()).isEqualTo("https://res.cloudinary.com/x/hero.png");
        // no confidential leak: the public shape carries no university / partnership / commission field
        assertThat(java.util.Arrays.stream(detail.getClass().getRecordComponents()).map(c -> c.getName()))
                .noneMatch(n -> n.toLowerCase().contains("university")
                        || n.toLowerCase().contains("partnership")
                        || n.toLowerCase().contains("commission"));
    }

    @Test
    void getPublic_falls_back_to_the_legacy_hero_image_url_when_no_media_id() {
        Scholarship s = row(11);
        s.setHeroImageUrl("/profile/legacy-hero.png");
        when(mapper.findPublicById(11L)).thenReturn(s);
        when(mapper.findLevels(11L)).thenReturn(List.of());
        when(mapper.findCategories(11L)).thenReturn(List.of());
        when(mapper.findIntakes(11L)).thenReturn(List.of());

        var detail = service.getPublic("11");

        assertThat(detail.heroUrl()).isEqualTo("/profile/legacy-hero.png");
    }

    @Test
    void facets_composes_the_four_dimensions() {
        when(mapper.facetLevels(any())).thenReturn(List.of(new ScholarshipFacetRow("MASTER", 3)));
        when(mapper.facetCategories(any())).thenReturn(List.of(new ScholarshipFacetRow("CSC", 2)));
        when(mapper.facetFundingModels(any())).thenReturn(List.of(new ScholarshipFacetRow("FULLY", 4)));
        when(mapper.facetTeachingLanguages(any())).thenReturn(List.of(new ScholarshipFacetRow("ENGLISH", 1)));

        var facets = service.facets(anyFilter());

        assertThat(facets.levels()).singleElement().extracting(b -> b.value()).isEqualTo("MASTER");
        assertThat(facets.categories().get(0).count()).isEqualTo(2);
        assertThat(facets.fundingModels()).hasSize(1);
        assertThat(facets.teachingLanguages().get(0).value()).isEqualTo("ENGLISH");
    }
}
