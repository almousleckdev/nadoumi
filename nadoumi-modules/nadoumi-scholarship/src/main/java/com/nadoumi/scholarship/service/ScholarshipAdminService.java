package com.nadoumi.scholarship.service;

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
import com.nadoumi.scholarship.domain.Scholarship;
import com.nadoumi.scholarship.domain.enums.PublishStatus;
import com.nadoumi.scholarship.mapper.ScholarshipMapper;
import com.nadoumi.scholarship.mapper.ScholarshipSearch;
import com.nadoumi.scholarship.web.request.ScholarshipRequest;
import com.nadoumi.scholarship.web.response.ScholarshipResponse;
import com.nadoumi.university.service.UniversityService;
import com.ruoyi.common.utils.AuditActor;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Staff scholarship management: student-safe CRUD. The confidential linkage lives in
 * {@link ScholarshipInternalService}. Callers are permission-checked at the controller
 * ({@code nad:scholarship:*}).
 * Children are replaced whole inside the same transaction as the head update.
 */
@Service
public class ScholarshipAdminService {

    private final ScholarshipMapper mapper;
    private final UniversityService universityService;
    private final MediaGateway media;
    private final OutboxWriter outboxWriter;
    private final ScholarshipChildrenWriter children;

    public ScholarshipAdminService(ScholarshipMapper mapper, UniversityService universityService,
            MediaGateway media, OutboxWriter outboxWriter, ScholarshipChildrenWriter children) {
        this.mapper = mapper;
        this.universityService = universityService;
        this.media = media;
        this.outboxWriter = outboxWriter;
        this.children = children;
    }

    @Transactional(readOnly = true)
    public PageResponse<ScholarshipResponse> list(ScholarshipSearch filter, int page, int size) {
        page = PageSupport.clampPage(page);
        size = PageSupport.clampSize(size);
        PageHelper.startPage(page + 1, size);
        List<Scholarship> rows = mapper.searchStaff(filter);
        long total = new PageInfo<>(rows).getTotal();
        return PageResponse.of(rows.stream().map(this::toResponse).toList(), page, size, total);
    }

    @Transactional(readOnly = true)
    public ScholarshipResponse get(Long id) {
        return toResponse(loadWithChildren(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public ScholarshipResponse create(ScholarshipRequest req) {
        Scholarship s = new Scholarship();
        apply(s, req);
        s.setSlug(uniqueSlug(req.title(), null));
        s.setCreateBy(AuditActor.username());
        boolean publishing = req.publishStatus() == PublishStatus.PUBLISHED;
        if (publishing) {
            s.setPublishedAt(LocalDateTime.now());
            assignReferenceCode(s);
        }
        mapper.insert(s);
        children.replace(s.getId(), req);
        if (publishing) {
            emitPublished(s);
        }
        return get(s.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public ScholarshipResponse update(Long id, ScholarshipRequest req) {
        Scholarship existing = load(id);
        boolean wasPublished = existing.getPublishStatus() == PublishStatus.PUBLISHED;
        apply(existing, req);
        existing.setId(id);
        existing.setSlug(uniqueSlug(req.title(), id));
        boolean publishing = req.publishStatus() == PublishStatus.PUBLISHED;
        if (publishing) {
            assignReferenceCode(existing);
        }
        existing.setUpdateBy(AuditActor.username());
        mapper.update(existing);
        if (publishing) {
            mapper.markPublished(id);
        }
        children.replace(id, req);
        if (publishing && !wasPublished) {
            emitPublished(existing);
        }
        return get(id);
    }

    /**
     * Write {@code ScholarshipPublished} to the outbox in this transaction — the
     * notification pipeline picks it up. Payload is safe scalars only; the
     * confidential linkage never leaves the aggregate.
     */
    private void emitPublished(Scholarship s) {
        JSONObject payload = new JSONObject();
        payload.put("scholarshipId", s.getId());
        payload.put("scholarshipTitle", s.getTitle());
        payload.put("scholarshipSlug", s.getSlug() == null ? "" : s.getSlug());
        payload.put("scholarshipReference", s.getReferenceCode() == null ? "" : s.getReferenceCode());
        outboxWriter.write("scholarship", s.getId(),
                OutboxEventTypes.SCHOLARSHIP_PUBLISHED, payload.toJSONString());
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (mapper.delete(id) == 0) {
            throw new NadNotFoundException("scholarship not found");
        }
    }

    // ---- media uploads (staff, nad:scholarship:edit) ----

    @Transactional(rollbackFor = Exception.class)
    public MediaUploadResult uploadHero(long id, MultipartFile file) {
        load(id);
        MediaUploadResult result = uploadFor(id, file, MediaCategory.SCHOLARSHIP_HERO);
        mapper.updateHeroMediaId(id, result.mediaId());
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public MediaUploadResult uploadCover(long id, MultipartFile file) {
        load(id);
        MediaUploadResult result = uploadFor(id, file, MediaCategory.SCHOLARSHIP_COVER);
        mapper.updateCoverMediaId(id, result.mediaId());
        return result;
    }

    private MediaUploadResult uploadFor(long scholarshipId, MultipartFile file, MediaCategory category) {
        return media.upload(file::getInputStream, file.getOriginalFilename(), file.getContentType(),
                    file.getSize(), category, null,
                    new MediaOwnerRef(MediaOwnerKind.SCHOLARSHIP, scholarshipId), AuditActor.userId());
    }

    private ScholarshipResponse toResponse(Scholarship s) {
        return ScholarshipResponse.of(s,
                MediaUrls.resolve(media, s.getHeroMediaId(), s.getHeroImageUrl()),
                MediaUrls.resolve(media, s.getCoverMediaId(), s.getCoverImageUrl()));
    }

    // ---- internals ----

    private Scholarship load(Long id) {
        Scholarship s = mapper.findById(id);
        if (s == null) {
            throw new NadNotFoundException("scholarship not found");
        }
        return s;
    }

    private Scholarship loadWithChildren(Long id) {
        Scholarship s = load(id);
        s.setLevels(mapper.findLevels(id));
        s.setCategories(mapper.findCategories(id));
        s.setIntakes(mapper.findIntakes(id));
        s.setFields(mapper.findFields(id));
        s.setEligibility(mapper.findEligibility(id));
        s.setFees(mapper.findFees(id));
        s.setLevelStipends(mapper.findLevelStipends(id));
        s.setAccommodations(mapper.findAccommodations(id));
        s.setCoverage(mapper.findCoverage(id));
        s.setDocumentRequirements(mapper.findDocumentRequirements(id));
        return s;
    }

    private void apply(Scholarship s, ScholarshipRequest req) {
        s.setTitle(req.title().trim());
        s.setSummary(Texts.blankToNull(req.summary()));
        s.setCountry(req.country().toUpperCase(Locale.ROOT));
        s.setProvince(Texts.blankToNull(req.province()));
        s.setCity(Texts.blankToNull(req.city()));
        s.setField(ScholarshipFields.summary(ScholarshipFields.normalise(req)));
        s.setTeachingLanguage(req.teachingLanguage());
        s.setFundingModel(req.fundingModel());
        s.setHasStipend(req.levelStipends() != null && !req.levelStipends().isEmpty());
        s.setDeadline(req.deadline());
        s.setBenefits(Texts.blankToNull(req.benefits()));
        s.setRequirements(Texts.blankToNull(req.requirements()));
        s.setPolicy(Texts.blankToNull(req.policy()));
        s.setRenewalConditions(Texts.blankToNull(req.renewalConditions()));
        s.setNonDegreeDuration(req.nonDegreeDuration() == null ? null : req.nonDegreeDuration().name());
        s.setStudyDurationMonths(req.studyDurationMonths());
        s.setApplicationChannel(req.applicationChannel() == null ? null : req.applicationChannel().name());
        s.setAgencyNumber(Texts.blankToNull(req.agencyNumber()));
        s.setRequiresFinancialProof(Boolean.TRUE.equals(req.requiresFinancialProof()));
        s.setRequiresFoundationYear(Boolean.TRUE.equals(req.requiresFoundationYear()));
        s.setApplicationFeeAmount(req.applicationFeeAmount());
        s.setApplicationFeeCurrency(upper(req.applicationFeeCurrency()));
        s.setServiceFeeAmount(req.serviceFeeAmount());
        s.setServiceFeeCurrency(upper(req.serviceFeeCurrency()));
        s.setSlots(req.slots());
        s.setFeatured(Boolean.TRUE.equals(req.featured()));
        s.setRecommended(Boolean.TRUE.equals(req.recommended()));
        s.setHot(Boolean.TRUE.equals(req.hot()));
        s.setStatus(req.status());
        s.setPublishStatus(req.publishStatus());
        s.setRemark(Texts.blankToNull(req.remark()));
        s.setHeroImageUrl(Texts.blankToNull(req.heroImageUrl()));
        s.setCoverImageUrl(Texts.blankToNull(req.coverImageUrl()));
        // Media ids are primarily set through the dedicated upload endpoints; only
        // overwrite from the request when the client actually sent a value.
        if (req.heroMediaId() != null) {
            s.setHeroMediaId(req.heroMediaId());
        }
        if (req.coverMediaId() != null) {
            s.setCoverMediaId(req.coverMediaId());
        }
    }

    /**
     * Assign the human-facing reference code {@code NAC-<year>-NNNN} on first
     * publish. Idempotent -- a scholarship keeps its code once it has one.
     */
    private void assignReferenceCode(Scholarship s) {
        if (s.getReferenceCode() != null && !s.getReferenceCode().isBlank()) {
            return;
        }
        String prefix = "NAC-" + Year.now().getValue() + "-";
        Integer max = mapper.maxReferenceSeq(prefix);
        s.setReferenceCode(prefix + String.format(Locale.ROOT, "%04d", (max == null ? 0 : max) + 1));
    }

    /** The slug is always derived from the title -- lower-case kebab, de-duplicated. */
    private String uniqueSlug(String title, Long selfId) {
        try {
            return Slugs.unique(Slugs.slugify(title), selfId, mapper::findIdBySlug);
        }
        catch (IllegalArgumentException e) {
            throw new NadBadRequestException("could not derive a slug from the title");
        }
    }

    private static String upper(String s) {
        return s == null || s.isBlank() ? null : s.trim().toUpperCase(Locale.ROOT);
    }
}
