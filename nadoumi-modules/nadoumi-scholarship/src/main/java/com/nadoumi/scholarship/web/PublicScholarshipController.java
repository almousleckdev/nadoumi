package com.nadoumi.scholarship.web;

import com.nadoumi.common.web.PageResponse;
import com.nadoumi.scholarship.domain.ScholarshipCategory;
import com.nadoumi.scholarship.domain.enums.FundingModel;
import com.nadoumi.scholarship.domain.enums.TeachingLanguage;
import com.nadoumi.scholarship.mapper.ScholarshipSearch;
import com.nadoumi.scholarship.service.ScholarshipService;
import com.nadoumi.scholarship.web.response.PublicScholarshipResponse;
import com.nadoumi.scholarship.web.response.ScholarshipFacets;
import com.ruoyi.common.annotation.Anonymous;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public scholarship discovery — anonymous. Serves only
 * {@link PublicScholarshipResponse}, sourced from {@code v_scholarship_student}
 * (PUBLISHED + ACTIVE) and the student-safe child tables. No university /
 * partnership / commission field is reachable here on any path.
 */
@RestController
@RequestMapping("/api/public/scholarships")
public class PublicScholarshipController {

    private final ScholarshipService service;

    public PublicScholarshipController(ScholarshipService service) {
        this.service = service;
    }

    @Anonymous
    @GetMapping
    public PageResponse<PublicScholarshipResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String field,
            @RequestParam(required = false) TeachingLanguage language,
            @RequestParam(required = false) FundingModel funding,
            @RequestParam(required = false) Boolean hasStipend,
            @RequestParam(required = false) LocalDate deadlineBefore,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String intake,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) Boolean recommended,
            @RequestParam(required = false) Boolean hot,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return service.list(filter(q, country, province, city, field, language, funding, hasStipend,
                deadlineBefore, level, category, intake, featured, recommended, hot, sort), page, size);
    }

    @Anonymous
    @GetMapping("/facets")
    public ScholarshipFacets facets(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String field,
            @RequestParam(required = false) TeachingLanguage language,
            @RequestParam(required = false) FundingModel funding,
            @RequestParam(required = false) Boolean hasStipend,
            @RequestParam(required = false) LocalDate deadlineBefore,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String intake) {
        return service.facets(filter(q, country, province, city, field, language, funding, hasStipend,
                deadlineBefore, level, category, intake, null, null, null, null));
    }

    /** The extensible category reference — literal path, matched before {slugOrId}. */
    @Anonymous
    @GetMapping("/categories")
    public List<ScholarshipCategory> categories() {
        return service.categories();
    }

    @Anonymous
    @GetMapping("/{slugOrId}")
    public PublicScholarshipResponse get(@PathVariable String slugOrId) {
        return service.getPublic(slugOrId);
    }

    private static ScholarshipSearch filter(String q, String country, String province, String city,
            String field, TeachingLanguage language, FundingModel funding, Boolean hasStipend,
            LocalDate deadlineBefore, String level, String category, String intake,
            Boolean featured, Boolean recommended, Boolean hot, String sort) {
        return new ScholarshipSearch(
                trimToNull(q), upper(country), trimToNull(province), trimToNull(city), trimToNull(field),
                language, funding, hasStipend, deadlineBefore,
                csv(level), csv(category), csv(intake),
                featured, recommended, hot, trimToNull(sort), null, null);
    }

    private static List<String> csv(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private static String trimToNull(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }

    private static String upper(String s) {
        return StringUtils.hasText(s) ? s.trim().toUpperCase() : null;
    }
}
