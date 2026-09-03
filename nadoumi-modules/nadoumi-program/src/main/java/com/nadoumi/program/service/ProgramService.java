package com.nadoumi.program.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nadoumi.common.text.Slugs;
import com.nadoumi.common.web.PageResponse;
import com.nadoumi.identity.exception.NadBadRequestException;
import com.nadoumi.identity.exception.NadNotFoundException;
import com.nadoumi.program.domain.Program;
import com.nadoumi.program.domain.ProgramIntake;
import com.nadoumi.program.domain.ProgramMajor;
import com.nadoumi.program.domain.enums.ProgramStatus;
import com.nadoumi.program.domain.enums.ProgramTeachingLanguage;
import com.nadoumi.program.domain.enums.ProgramType;
import com.nadoumi.program.domain.enums.PublishStatus;
import com.nadoumi.program.mapper.ProgramMapper;
import com.nadoumi.program.mapper.ProgramSearch;
import com.nadoumi.program.web.request.ProgramRequest;
import com.nadoumi.program.web.response.ProgramResponse;
import com.nadoumi.program.web.response.PublicProgramResponse;
import com.nadoumi.university.service.UniversityService;
import com.ruoyi.common.utils.SecurityUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Programme catalog. A programme belongs to exactly one university;
 * {@code (university_id, name)} is unique. The majors / intakes lists are edited
 * whole: a save replaces them inside the same transaction as the scalar update.
 * The owning university is validated and its name resolved through
 * {@link UniversityService} -- never a cross-module SQL join.
 */
@Service
public class ProgramService {

    private final ProgramMapper mapper;
    private final UniversityService universityService;

    public ProgramService(ProgramMapper mapper, UniversityService universityService) {
        this.mapper = mapper;
        this.universityService = universityService;
    }

    // ---- staff ----

    public PageResponse<ProgramResponse> list(String q, Long universityId, ProgramType type,
            ProgramTeachingLanguage language, String field, ProgramStatus status, int page, int size) {
        PageHelper.startPage(page + 1, size);
        List<Program> rows = mapper.search(ProgramSearch.staff(q, universityId, type, language, field, status));
        long total = new PageInfo<>(rows).getTotal();
        Map<Long, UniRef> refs = new HashMap<>();
        rows.forEach(p -> applyUniversity(p, staffUniversity(p.getUniversityId(), refs)));
        return PageResponse.of(rows.stream().map(ProgramResponse::of).toList(), page, size, total);
    }

    public ProgramResponse get(Long id) {
        Program p = loadWithChildren(id);
        applyUniversity(p, staffUniversity(p.getUniversityId(), new HashMap<>()));
        return ProgramResponse.of(p);
    }

    @Transactional
    public ProgramResponse create(ProgramRequest req) {
        String universityName = requireUniversity(req.universityId());
        Program p = new Program();
        apply(p, req);
        requireUniqueName(p.getUniversityId(), p.getName(), null);
        p.setSlug(uniqueSlug(universityName, p.getName(), null));
        p.setCreateBy(currentUser());
        mapper.insert(p);
        replaceChildren(p.getId(), req);
        return get(p.getId());
    }

    @Transactional
    public ProgramResponse update(Long id, ProgramRequest req) {
        Program p = load(id);
        String universityName = requireUniversity(req.universityId());
        apply(p, req);
        p.setId(id);
        requireUniqueName(p.getUniversityId(), p.getName(), id);
        p.setSlug(uniqueSlug(universityName, p.getName(), id));
        p.setUpdateBy(currentUser());
        mapper.update(p);
        replaceChildren(id, req);
        return get(id);
    }

    @Transactional
    public void delete(Long id) {
        if (mapper.delete(id) == 0) {
            throw new NadNotFoundException("programme not found");
        }
        // children go with the row (ON DELETE CASCADE)
    }

    // ---- public (published + active programme of a published + active university) ----

    public PageResponse<PublicProgramResponse> publicList(String q, Long universityId, ProgramType type,
            ProgramTeachingLanguage language, String field, Boolean featured, Boolean hot, int page, int size) {
        if (universityId != null) {
            universityService.publicGet(String.valueOf(universityId)); // 404 if the university is not public
        }
        PageHelper.startPage(page + 1, size);
        List<Program> rows = mapper.search(
                ProgramSearch.publicCatalog(q, universityId, type, language, field, featured, hot));
        long rawTotal = new PageInfo<>(rows).getTotal();
        // A programme whose university has since been unpublished is a transient
        // state -- drop it here (the university publish check is UniversityService's,
        // never a cross-module join) and correct the count for this page.
        Map<Long, UniRef> publicRefs = new HashMap<>();
        List<PublicProgramResponse> items = rows.stream()
                .filter(p -> {
                    UniRef ref = publicUniversity(p.getUniversityId(), publicRefs);
                    applyUniversity(p, ref);
                    return ref != null;
                })
                .map(PublicProgramResponse::card)
                .toList();
        long hiddenOnThisPage = rows.size() - items.size();
        return PageResponse.of(items, page, size, rawTotal - hiddenOnThisPage);
    }

    public List<PublicProgramResponse> publicListForUniversity(String universityIdOrSlug) {
        var uni = universityService.publicGet(universityIdOrSlug); // 404 if not public
        List<Program> rows = mapper.search(
                ProgramSearch.publicCatalog(null, uni.id(), null, null, null, null, null));
        rows.forEach(p -> applyUniversity(p, new UniRef(uni.name(), uni.slug())));
        return rows.stream().map(PublicProgramResponse::card).toList();
    }

    public PublicProgramResponse publicGet(String idOrSlug) {
        Program p = loadWithChildren(resolveId(idOrSlug));
        if (p.getStatus() != ProgramStatus.ACTIVE || p.getPublishStatus() != PublishStatus.PUBLISHED) {
            throw new NadNotFoundException("programme not found");
        }
        // the university must itself be public
        var uni = universityService.publicGet(String.valueOf(p.getUniversityId()));
        applyUniversity(p, new UniRef(uni.name(), uni.slug()));
        return PublicProgramResponse.detail(p);
    }

    private Long resolveId(String idOrSlug) {
        if (idOrSlug != null && idOrSlug.chars().allMatch(Character::isDigit)) {
            return Long.valueOf(idOrSlug);
        }
        Long id = mapper.findIdBySlug(idOrSlug);
        if (id == null) {
            throw new NadNotFoundException("programme not found");
        }
        return id;
    }

    // ---- internals ----

    private Program load(Long id) {
        Program p = mapper.findById(id);
        if (p == null) {
            throw new NadNotFoundException("programme not found");
        }
        return p;
    }

    private Program loadWithChildren(Long id) {
        Program p = load(id);
        p.setMajors(mapper.findMajors(id));
        p.setIntakes(mapper.findIntakes(id));
        return p;
    }

    private void replaceChildren(Long programId, ProgramRequest req) {
        mapper.deleteMajors(programId);
        if (req.majors() != null) {
            int order = 0;
            for (ProgramRequest.MajorInput in : req.majors()) {
                if (in.name() == null || in.name().isBlank()) {
                    continue;
                }
                ProgramMajor m = new ProgramMajor();
                m.setProgramId(programId);
                m.setName(in.name().trim());
                m.setNameCn(blankToNull(in.nameCn()));
                m.setSortOrder(order++);
                mapper.insertMajor(m);
            }
        }
        mapper.deleteIntakes(programId);
        if (req.intakes() != null) {
            int order = 0;
            for (ProgramRequest.IntakeInput in : req.intakes()) {
                if (in.term() == null || in.term().isBlank()) {
                    continue;
                }
                ProgramIntake i = new ProgramIntake();
                i.setProgramId(programId);
                i.setTerm(in.term().trim());
                i.setApplicationOpen(in.applicationOpen());
                i.setApplicationClose(in.applicationClose());
                i.setSortOrder(order++);
                mapper.insertIntake(i);
            }
        }
    }

    /** Validates the university exists and returns its name (for the slug). */
    private String requireUniversity(Long universityId) {
        try {
            return universityService.get(universityId).name();
        }
        catch (NadNotFoundException e) {
            throw new NadBadRequestException("university not found: " + universityId);
        }
    }

    private String uniqueSlug(String universityName, String name, Long selfId) {
        String base = Slugs.slugify((universityName == null ? "" : universityName + " ") + name);
        return Slugs.unique(base, selfId, mapper::findIdBySlug);
    }

    private void requireUniqueName(Long universityId, String name, Long selfId) {
        Long existing = mapper.findIdByUniversityAndName(universityId, name);
        if (existing != null && !existing.equals(selfId)) {
            throw new NadBadRequestException("a programme with this name already exists for that university");
        }
    }

    /** A university's public-facing name + slug, resolved via UniversityService. */
    private record UniRef(String name, String slug) {
    }

    private static void applyUniversity(Program p, UniRef ref) {
        p.setUniversityName(ref == null ? null : ref.name());
        p.setUniversitySlug(ref == null ? null : ref.slug());
    }

    private UniRef staffUniversity(Long universityId, Map<Long, UniRef> cache) {
        return cache.computeIfAbsent(universityId, id -> {
            try {
                var u = universityService.get(id);
                return new UniRef(u.name(), u.slug());
            }
            catch (NadNotFoundException e) {
                return null;
            }
        });
    }

    private UniRef publicUniversity(Long universityId, Map<Long, UniRef> cache) {
        return cache.computeIfAbsent(universityId, id -> {
            try {
                var u = universityService.publicGet(String.valueOf(id));
                return new UniRef(u.name(), u.slug());
            }
            catch (NadNotFoundException e) {
                return null;
            }
        });
    }

    private static String currentUser() {
        try {
            return SecurityUtils.getUsername();
        }
        catch (RuntimeException e) {
            return "system";
        }
    }

    private static void apply(Program p, ProgramRequest req) {
        p.setUniversityId(req.universityId());
        p.setName(req.name().trim());
        p.setNameCn(blankToNull(req.nameCn()));
        p.setProgramType(req.programType());
        p.setField(blankToNull(req.field()));
        p.setTeachingLanguage(req.teachingLanguage());
        p.setDurationMonths(req.durationMonths());
        p.setTuitionAmount(req.tuitionAmount());
        p.setTuitionCurrency(upperOrNull(req.tuitionCurrency()));
        p.setSummary(blankToNull(req.summary()));
        p.setFeatured(Boolean.TRUE.equals(req.featured()));
        p.setHot(Boolean.TRUE.equals(req.hot()));
        p.setStatus(req.status());
        p.setPublishStatus(req.publishStatus());
        p.setRemark(blankToNull(req.remark()));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String upperOrNull(String s) {
        return s == null || s.isBlank() ? null : s.trim().toUpperCase();
    }
}
