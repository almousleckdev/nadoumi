package com.nadoumi.program.service;

import com.alibaba.fastjson2.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.common.exception.NadNotFoundException;
import com.nadoumi.common.media.MediaCategory;
import com.nadoumi.common.media.MediaGateway;
import com.nadoumi.common.media.MediaOwnerKind;
import com.nadoumi.common.media.MediaOwnerRef;
import com.nadoumi.common.media.MediaUploadResult;
import com.nadoumi.common.media.MediaUrls;
import com.nadoumi.common.outbox.OutboxEventTypes;
import com.nadoumi.common.outbox.OutboxWriter;
import com.nadoumi.common.text.Slugs;
import com.nadoumi.common.text.Texts;
import com.nadoumi.common.web.PageResponse;
import com.nadoumi.common.web.PageSupport;
import com.nadoumi.identity.money.FxRates;
import com.nadoumi.program.domain.Program;
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
import com.ruoyi.common.utils.AuditActor;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    private final ProgramChildrenWriter children;
    private final MediaGateway media;
    private final FxRates fx;
    private final OutboxWriter outbox;

    public ProgramService(ProgramMapper mapper, UniversityService universityService,
            ProgramChildrenWriter children, MediaGateway media, FxRates fx, OutboxWriter outbox) {
        this.mapper = mapper;
        this.universityService = universityService;
        this.children = children;
        this.media = media;
        this.fx = fx;
        this.outbox = outbox;
    }

    // ---- staff ----

    public PageResponse<ProgramResponse> list(String q, Long universityId, ProgramType type,
            ProgramTeachingLanguage language, String field, ProgramStatus status, int page, int size) {
        page = PageSupport.clampPage(page);
        size = PageSupport.clampSize(size);
        PageHelper.startPage(page + 1, size);
        List<Program> rows = mapper.search(ProgramSearch.staff(q, universityId, type, language, field, status));
        long total = new PageInfo<>(rows).getTotal();
        Map<Long, UniRef> refs = new HashMap<>();
        rows.forEach(p -> applyUniversity(p, staffUniversity(p.getUniversityId(), refs)));
        return PageResponse.of(rows.stream().map(this::toResponse).toList(), page, size, total);
    }

    public ProgramResponse get(Long id) {
        Program p = loadWithChildren(id);
        applyUniversity(p, staffUniversity(p.getUniversityId(), new HashMap<>()));
        return toResponse(p);
    }

    @Transactional(rollbackFor = Exception.class)
    public ProgramResponse create(ProgramRequest req) {
        String universityName = requireUniversity(req.universityId());
        Program p = new Program();
        apply(p, req);
        requireUniqueName(p.getUniversityId(), p.getName(), null);
        p.setSlug(uniqueSlug(universityName, p.getName(), null));
        p.setCreateBy(AuditActor.username());
        mapper.insert(p);
        children.replace(p, req);
        if (isLive(p)) {
            emitPublished(p, universityName);
        }
        return get(p.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public ProgramResponse update(Long id, ProgramRequest req) {
        Program p = load(id);
        boolean wasLive = isLive(p);
        String universityName = requireUniversity(req.universityId());
        apply(p, req);
        p.setId(id);
        requireUniqueName(p.getUniversityId(), p.getName(), id);
        p.setSlug(uniqueSlug(universityName, p.getName(), id));
        p.setUpdateBy(AuditActor.username());
        mapper.update(p);
        children.replace(p, req);
        if (isLive(p) && !wasLive) {
            emitPublished(p, universityName);
        }
        return get(id);
    }

    private static boolean isLive(Program p) {
        return p.getStatus() == ProgramStatus.ACTIVE && p.getPublishStatus() == PublishStatus.PUBLISHED;
    }

    /** Announce a newly public programme to staff + registered students (safe scalars only). */
    private void emitPublished(Program p, String universityName) {
        JSONObject payload = new JSONObject();
        payload.put("programId", p.getId());
        payload.put("programName", p.getName());
        payload.put("programSlug", p.getSlug() == null ? "" : p.getSlug());
        payload.put("universityName", universityName == null ? "" : universityName);
        outbox.write("program", p.getId(), OutboxEventTypes.PROGRAM_PUBLISHED, payload.toJSONString());
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (mapper.delete(id) == 0) {
            throw new NadNotFoundException("programme not found");
        }
        // children go with the row (ON DELETE CASCADE)
    }

    // ---- media upload (staff, nad:program:edit) ----

    @Transactional(rollbackFor = Exception.class)
    public MediaUploadResult uploadImage(long id, MultipartFile file) {
        load(id);
        MediaUploadResult result = media.upload(file::getInputStream, file.getOriginalFilename(), file.getContentType(),
                    file.getSize(), MediaCategory.PROGRAM_IMAGE, null,
                    new MediaOwnerRef(MediaOwnerKind.PROGRAM, id), AuditActor.userId());
        mapper.updateImageMediaId(id, result.mediaId());
        return result;
    }

    // ---- public (published + active programme of a published + active university) ----

    public PageResponse<PublicProgramResponse> publicList(String q, Long universityId, ProgramType type,
            ProgramTeachingLanguage language, String field, Boolean featured, Boolean hot, int page, int size) {
        if (universityId != null) {
            universityService.publicGet(String.valueOf(universityId)); // 404 if the university is not public
        }
        page = PageSupport.clampPage(page);
        size = PageSupport.clampSize(size);
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
                .map(p -> PublicProgramResponse.card(p, imageUrl(p), usd(p)))
                .toList();
        long hiddenOnThisPage = rows.size() - items.size();
        return PageResponse.of(items, page, size, rawTotal - hiddenOnThisPage);
    }

    public List<PublicProgramResponse> publicListForUniversity(String universityIdOrSlug) {
        var uni = universityService.publicGet(universityIdOrSlug); // 404 if not public
        List<Program> rows = mapper.search(
                ProgramSearch.publicCatalog(null, uni.id(), null, null, null, null, null));
        rows.forEach(p -> applyUniversity(p, new UniRef(uni.name(), uni.slug())));
        return rows.stream().map(p -> PublicProgramResponse.card(p, imageUrl(p), usd(p))).toList();
    }

    public PublicProgramResponse publicGet(String idOrSlug) {
        Program p = loadWithChildren(resolveId(idOrSlug));
        if (p.getStatus() != ProgramStatus.ACTIVE || p.getPublishStatus() != PublishStatus.PUBLISHED) {
            throw new NadNotFoundException("programme not found");
        }
        // the university must itself be public
        var uni = universityService.publicGet(String.valueOf(p.getUniversityId()));
        applyUniversity(p, new UniRef(uni.name(), uni.slug()));
        return PublicProgramResponse.detail(p, imageUrl(p), usd(p));
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
        p.setLevels(mapper.findLevels(id));
        p.setMajors(mapper.findMajors(id));
        p.setIntakes(mapper.findIntakes(id));
        return p;
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

    private ProgramResponse toResponse(Program p) {
        return ProgramResponse.of(p, imageUrl(p), usd(p));
    }

    /** USD display value for the tuition, from the editable CNY&rarr;USD rate. */
    private BigDecimal usd(Program p) {
        return fx.toUsd(p.getTuitionAmount(), p.getTuitionCurrency());
    }

    /** Resolve {@code image_media_id} to a public delivery URL, or null when unset. */
    private String imageUrl(Program p) {
        return MediaUrls.resolve(media, p.getImageMediaId(), null);
    }

    private static void apply(Program p, ProgramRequest req) {
        p.setUniversityId(req.universityId());
        p.setName(req.name().trim());
        p.setNameCn(Texts.blankToNull(req.nameCn()));
        p.setProgramType(req.programType());
        p.setField(Texts.blankToNull(req.field()));
        // term_length is a LANGUAGE / NON_DEGREE concept only; degree types use majors.
        p.setTermLength(req.programType() != null && !req.programType().isDegree()
                ? normalizeTermLength(req.termLength()) : null);
        p.setTeachingLanguage(req.teachingLanguage());
        p.setDurationMonths(req.durationMonths());
        p.setTuitionAmount(req.tuitionAmount());
        p.setTuitionCurrency(upperOrNull(req.tuitionCurrency()));
        p.setSummary(Texts.blankToNull(req.summary()));
        // Primarily set through the dedicated upload endpoint; only overwrite from
        // the request when the client actually sent a value.
        if (req.imageMediaId() != null) {
            p.setImageMediaId(req.imageMediaId());
        }
        p.setFeatured(Boolean.TRUE.equals(req.featured()));
        p.setHot(Boolean.TRUE.equals(req.hot()));
        p.setStatus(req.status());
        p.setPublishStatus(req.publishStatus());
        p.setRemark(Texts.blankToNull(req.remark()));
    }

    private static final Set<String> TERM_LENGTHS = Set.of("ONE_SEMESTER", "ONE_YEAR");

    private static String normalizeTermLength(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String v = raw.trim().toUpperCase();
        if (!TERM_LENGTHS.contains(v)) {
            throw new NadBadRequestException("term length must be ONE_SEMESTER or ONE_YEAR");
        }
        return v;
    }

    private static String upperOrNull(String s) {
        return s == null || s.isBlank() ? null : s.trim().toUpperCase();
    }
}
