package com.nadoumi.scholarship.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nadoumi.common.web.PageResponse;
import com.nadoumi.identity.exception.NadNotFoundException;
import com.nadoumi.scholarship.domain.Scholarship;
import com.nadoumi.scholarship.mapper.ScholarshipMapper;
import com.nadoumi.scholarship.mapper.ScholarshipSearch;
import com.nadoumi.scholarship.web.response.PublicScholarshipResponse;
import com.nadoumi.scholarship.web.response.ScholarshipFacets;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Public scholarship discovery. Every read here goes through
 * {@code v_scholarship_student} and the student-safe child tables — the
 * confidential {@code nad_scholarship_internal} is never touched on this path.
 */
@Service
@Transactional(readOnly = true)
public class ScholarshipService {

    private final ScholarshipMapper mapper;

    public ScholarshipService(ScholarshipMapper mapper) {
        this.mapper = mapper;
    }

    public PageResponse<PublicScholarshipResponse> list(ScholarshipSearch filter, int page, int size) {
        PageHelper.startPage(page + 1, size);
        List<Scholarship> rows = mapper.searchPublic(filter);
        long total = new PageInfo<>(rows).getTotal();
        rows.forEach(this::loadCardChildren);
        return PageResponse.of(rows.stream().map(PublicScholarshipResponse::card).toList(), page, size, total);
    }

    public PublicScholarshipResponse getPublic(String slugOrId) {
        Scholarship s = resolvePublic(slugOrId);
        if (s == null) {
            throw new NadNotFoundException("scholarship not found");
        }
        loadCardChildren(s);
        s.setEligibility(mapper.findEligibility(s.getId()));
        s.setFees(mapper.findFees(s.getId()));
        s.setLevelStipends(mapper.findLevelStipends(s.getId()));
        s.setAccommodations(mapper.findAccommodations(s.getId()));
        s.setCoverage(mapper.findCoverage(s.getId()));
        s.setDocumentRequirements(mapper.findDocumentRequirements(s.getId()));
        return PublicScholarshipResponse.detail(s);
    }

    public java.util.List<com.nadoumi.scholarship.domain.ScholarshipCategory> categories() {
        return mapper.allCategories();
    }

    public ScholarshipFacets facets(ScholarshipSearch filter) {
        return ScholarshipFacets.of(
                mapper.facetLevels(filter),
                mapper.facetCategories(filter),
                mapper.facetFundingModels(filter),
                mapper.facetTeachingLanguages(filter));
    }

    private Scholarship resolvePublic(String slugOrId) {
        Long id = parseId(slugOrId);
        return id != null ? mapper.findPublicById(id) : mapper.findPublicBySlug(slugOrId);
    }

    private void loadCardChildren(Scholarship s) {
        s.setLevels(mapper.findLevels(s.getId()));
        s.setCategories(mapper.findCategories(s.getId()));
        s.setIntakes(mapper.findIntakes(s.getId()));
    }

    private static Long parseId(String value) {
        try {
            return Long.valueOf(value);
        }
        catch (NumberFormatException e) {
            return null;
        }
    }
}
