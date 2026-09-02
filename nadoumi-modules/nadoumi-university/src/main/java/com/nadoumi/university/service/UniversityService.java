package com.nadoumi.university.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nadoumi.common.web.PageResponse;
import com.nadoumi.identity.exception.NadBadRequestException;
import com.nadoumi.identity.exception.NadNotFoundException;
import com.nadoumi.university.domain.University;
import com.nadoumi.university.domain.enums.UniversityStatus;
import com.nadoumi.university.mapper.UniversityMapper;
import com.nadoumi.university.web.request.UniversityRequest;
import com.nadoumi.university.web.response.UniversityResponse;
import com.ruoyi.common.utils.SecurityUtils;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * University catalog. {@code (name, country)} is unique; the service checks it
 * before insert/update so callers get a clean {@code 400} instead of a raw
 * constraint violation.
 */
@Service
public class UniversityService {

    private final UniversityMapper mapper;

    public UniversityService(UniversityMapper mapper) {
        this.mapper = mapper;
    }

    public PageResponse<UniversityResponse> list(String q, String country, UniversityStatus status,
            int page, int size) {
        PageHelper.startPage(page + 1, size);
        List<University> rows = mapper.search(q, country, status);
        long total = new PageInfo<>(rows).getTotal();
        return PageResponse.of(rows.stream().map(UniversityResponse::of).toList(), page, size, total);
    }

    public UniversityResponse get(Long id) {
        return UniversityResponse.of(load(id));
    }

    @Transactional
    public UniversityResponse create(UniversityRequest req) {
        University u = new University();
        apply(u, req);
        requireUniqueName(u.getName(), u.getCountry(), null);
        u.setCreateBy(currentUser());
        mapper.insert(u);
        return UniversityResponse.of(u);
    }

    @Transactional
    public UniversityResponse update(Long id, UniversityRequest req) {
        University u = load(id);
        apply(u, req);
        requireUniqueName(u.getName(), u.getCountry(), id);
        u.setUpdateBy(currentUser());
        mapper.update(u);
        return UniversityResponse.of(u);
    }

    @Transactional
    public void delete(Long id) {
        if (mapper.delete(id) == 0) {
            throw new NadNotFoundException("university not found");
        }
    }

    // ---- internals ----

    private University load(Long id) {
        University u = mapper.findById(id);
        if (u == null) {
            throw new NadNotFoundException("university not found");
        }
        return u;
    }

    private void requireUniqueName(String name, String country, Long selfId) {
        Long existing = mapper.findIdByNameAndCountry(name, country);
        if (existing != null && !existing.equals(selfId)) {
            throw new NadBadRequestException("a university with this name already exists in " + country);
        }
    }

    private static String normalizeCountry(String c) {
        return c == null ? null : c.toUpperCase();
    }

    /** Authenticated username, or {@code "system"} when there is no security context. */
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
        u.setCountry(normalizeCountry(req.country()));
        u.setCity(blankToNull(req.city()));
        u.setWebsite(blankToNull(req.website()));
        u.setRankingTier(blankToNull(req.rankingTier()));
        u.setStatus(req.status());
        u.setRemark(blankToNull(req.remark()));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
