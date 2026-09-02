package com.nadoumi.university.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nadoumi.common.web.PageResponse;
import com.nadoumi.identity.exception.NadBadRequestException;
import com.nadoumi.identity.exception.NadNotFoundException;
import com.nadoumi.university.domain.University;
import com.nadoumi.university.domain.UniversityHighlight;
import com.nadoumi.university.domain.UniversityRanking;
import com.nadoumi.university.domain.enums.PublishStatus;
import com.nadoumi.university.domain.enums.UniversityStatus;
import com.nadoumi.university.mapper.UniversityMapper;
import com.nadoumi.university.web.request.UniversityRequest;
import com.nadoumi.university.web.response.PublicUniversityResponse;
import com.nadoumi.university.web.response.UniversityResponse;
import com.ruoyi.common.utils.SecurityUtils;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * University catalog. {@code (name, country)} is unique. The rankings and
 * highlights lists are edited whole: a save replaces them for that university
 * inside the same transaction as the scalar update.
 */
@Service
public class UniversityService {

    private final UniversityMapper mapper;

    public UniversityService(UniversityMapper mapper) {
        this.mapper = mapper;
    }

    // ---- staff ----

    public PageResponse<UniversityResponse> list(String q, String country, UniversityStatus status,
            int page, int size) {
        PageHelper.startPage(page + 1, size);
        List<University> rows = mapper.search(q, country, status, null);
        long total = new PageInfo<>(rows).getTotal();
        return PageResponse.of(rows.stream().map(UniversityResponse::of).toList(), page, size, total);
    }

    public UniversityResponse get(Long id) {
        return UniversityResponse.of(loadWithChildren(id));
    }

    @Transactional
    public UniversityResponse create(UniversityRequest req) {
        University u = new University();
        apply(u, req);
        requireUniqueName(u.getName(), u.getCountry(), null);
        u.setCreateBy(currentUser());
        mapper.insert(u);
        replaceChildren(u.getId(), req);
        return get(u.getId());
    }

    @Transactional
    public UniversityResponse update(Long id, UniversityRequest req) {
        University u = load(id);
        apply(u, req);
        requireUniqueName(u.getName(), u.getCountry(), id);
        u.setUpdateBy(currentUser());
        mapper.update(u);
        replaceChildren(id, req);
        return get(id);
    }

    @Transactional
    public void delete(Long id) {
        if (mapper.delete(id) == 0) {
            throw new NadNotFoundException("university not found");
        }
        // children go with the row (ON DELETE CASCADE)
    }

    // ---- public (published + active only) ----

    public PageResponse<PublicUniversityResponse> publicList(String q, String country, int page, int size) {
        PageHelper.startPage(page + 1, size);
        List<University> rows = mapper.search(q, country, UniversityStatus.ACTIVE, PublishStatus.PUBLISHED);
        long total = new PageInfo<>(rows).getTotal();
        return PageResponse.of(rows.stream().map(PublicUniversityResponse::of).toList(), page, size, total);
    }

    public PublicUniversityResponse publicGet(Long id) {
        University u = loadWithChildren(id);
        if (u.getStatus() != UniversityStatus.ACTIVE || u.getPublishStatus() != PublishStatus.PUBLISHED) {
            throw new NadNotFoundException("university not found");
        }
        return PublicUniversityResponse.of(u);
    }

    // ---- internals ----

    private University load(Long id) {
        University u = mapper.findById(id);
        if (u == null) {
            throw new NadNotFoundException("university not found");
        }
        return u;
    }

    private University loadWithChildren(Long id) {
        University u = load(id);
        u.setRankings(mapper.findRankings(id));
        u.setHighlights(mapper.findHighlights(id));
        return u;
    }

    private void replaceChildren(Long universityId, UniversityRequest req) {
        mapper.deleteRankings(universityId);
        if (req.rankings() != null) {
            for (UniversityRequest.RankingInput in : req.rankings()) {
                UniversityRanking r = new UniversityRanking();
                r.setUniversityId(universityId);
                r.setSource(in.source().trim());
                r.setRankPosition(in.rankPosition());
                r.setRankYear(in.rankYear() == null ? null : in.rankYear().intValue());
                r.setNote(blankToNull(in.note()));
                mapper.insertRanking(r);
            }
        }
        mapper.deleteHighlights(universityId);
        if (req.highlights() != null) {
            int order = 0;
            for (UniversityRequest.HighlightInput in : req.highlights()) {
                UniversityHighlight h = new UniversityHighlight();
                h.setUniversityId(universityId);
                h.setKind(in.kind());
                h.setSortOrder(order++);
                h.setText(in.text().trim());
                mapper.insertHighlight(h);
            }
        }
    }

    private void requireUniqueName(String name, String country, Long selfId) {
        Long existing = mapper.findIdByNameAndCountry(name, country);
        if (existing != null && !existing.equals(selfId)) {
            throw new NadBadRequestException("a university with this name already exists in " + country);
        }
    }

    private static String currentUser() {
        try {
            return SecurityUtils.getUsername();
        }
        catch (RuntimeException e) {
            return "system";
        }
    }

    private static void apply(University u, UniversityRequest req) {
        u.setName(req.name().trim());
        u.setNameCn(blankToNull(req.nameCn()));
        u.setCountry(req.country().toUpperCase());
        u.setType(req.type());
        u.setCity(blankToNull(req.city()));
        u.setProvince(blankToNull(req.province()));
        u.setFoundedYear(req.foundedYear());
        u.setTotalStudents(req.totalStudents());
        u.setInternationalStudents(req.internationalStudents());
        u.setFacultyCount(req.facultyCount());
        u.setWebsite(blankToNull(req.website()));
        u.setRankingTier(blankToNull(req.rankingTier()));
        u.setIntroduction(blankToNull(req.introduction()));
        u.setHistory(blankToNull(req.history()));
        u.setCampusInfo(blankToNull(req.campusInfo()));
        u.setAccommodationInfo(blankToNull(req.accommodationInfo()));
        u.setNearbyInfo(blankToNull(req.nearbyInfo()));
        u.setAdmissionsEmail(blankToNull(req.admissionsEmail()));
        u.setOfficePhone(blankToNull(req.officePhone()));
        u.setRecommended(Boolean.TRUE.equals(req.recommended()));
        u.setFeatured(Boolean.TRUE.equals(req.featured()));
        u.setStatus(req.status());
        u.setPublishStatus(req.publishStatus());
        u.setRemark(blankToNull(req.remark()));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
