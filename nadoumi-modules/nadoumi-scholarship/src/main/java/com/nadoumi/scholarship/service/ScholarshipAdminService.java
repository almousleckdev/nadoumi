package com.nadoumi.scholarship.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nadoumi.common.media.MediaCategory;
import com.nadoumi.common.media.MediaGateway;
import com.nadoumi.common.media.MediaOwnerKind;
import com.nadoumi.common.media.MediaOwnerRef;
import com.nadoumi.common.media.MediaUploadResult;
import com.nadoumi.common.text.Slugs;
import com.nadoumi.common.web.PageResponse;
import com.nadoumi.identity.exception.NadBadRequestException;
import com.nadoumi.identity.exception.NadNotFoundException;
import com.nadoumi.scholarship.domain.Scholarship;
import com.nadoumi.scholarship.domain.ScholarshipAccommodation;
import com.nadoumi.scholarship.domain.ScholarshipCoverage;
import com.nadoumi.scholarship.domain.ScholarshipEligibility;
import com.nadoumi.scholarship.domain.ScholarshipFee;
import com.nadoumi.scholarship.domain.ScholarshipInternal;
import com.nadoumi.scholarship.domain.ScholarshipIntake;
import com.nadoumi.scholarship.domain.ScholarshipLevelStipend;
import com.nadoumi.scholarship.domain.enums.FundingModel;
import com.nadoumi.scholarship.domain.enums.PublishStatus;
import com.nadoumi.scholarship.mapper.ScholarshipMapper;
import com.nadoumi.scholarship.mapper.ScholarshipSearch;
import com.nadoumi.scholarship.web.request.ScholarshipInternalRequest;
import com.nadoumi.scholarship.web.request.ScholarshipRequest;
import com.nadoumi.scholarship.web.response.ScholarshipInternalResponse;
import com.nadoumi.scholarship.web.response.ScholarshipResponse;
import com.nadoumi.university.service.UniversityService;
import com.ruoyi.common.utils.SecurityUtils;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Staff scholarship management: student-safe CRUD plus the confidential
 * {@code nad_scholarship_internal} linkage. Callers are permission-checked at the
 * controller ({@code nad:scholarship:*} and {@code nad:scholarship:internal:*}).
 * Children are replaced whole inside the same transaction as the head update.
 */
@Service
public class ScholarshipAdminService {

    private final ScholarshipMapper mapper;
    private final UniversityService universityService;
    private final MediaGateway media;

    public ScholarshipAdminService(ScholarshipMapper mapper, UniversityService universityService,
            MediaGateway media) {
        this.mapper = mapper;
        this.universityService = universityService;
        this.media = media;
    }

    @Transactional(readOnly = true)
    public PageResponse<ScholarshipResponse> list(ScholarshipSearch filter, int page, int size) {
        PageHelper.startPage(page + 1, size);
        List<Scholarship> rows = mapper.searchStaff(filter);
        long total = new PageInfo<>(rows).getTotal();
        return PageResponse.of(rows.stream().map(this::toResponse).toList(), page, size, total);
    }

    @Transactional(readOnly = true)
    public ScholarshipResponse get(Long id) {
        return toResponse(loadWithChildren(id));
    }

    @Transactional
    public ScholarshipResponse create(ScholarshipRequest req) {
        Scholarship s = new Scholarship();
        apply(s, req);
        s.setSlug(uniqueSlug(req.title(), null));
        s.setCreateBy(currentUser());
        if (req.publishStatus() == PublishStatus.PUBLISHED) {
            s.setPublishedAt(java.time.LocalDateTime.now());
            assignReferenceCode(s);
        }
        mapper.insert(s);
        replaceChildren(s.getId(), req);
        return get(s.getId());
    }

    @Transactional
    public ScholarshipResponse update(Long id, ScholarshipRequest req) {
        Scholarship existing = load(id);
        apply(existing, req);
        existing.setId(id);
        existing.setSlug(uniqueSlug(req.title(), id));
        if (req.publishStatus() == PublishStatus.PUBLISHED) {
            assignReferenceCode(existing);
        }
        existing.setUpdateBy(currentUser());
        mapper.update(existing);
        if (req.publishStatus() == PublishStatus.PUBLISHED) {
            mapper.markPublished(id);
        }
        replaceChildren(id, req);
        return get(id);
    }

    @Transactional
    public void delete(Long id) {
        if (mapper.delete(id) == 0) {
            throw new NadNotFoundException("scholarship not found");
        }
    }

    // ---- media uploads (staff, nad:scholarship:edit) ----

    @Transactional
    public MediaUploadResult uploadHero(long id, MultipartFile file) {
        load(id);
        MediaUploadResult result = uploadFor(id, file, MediaCategory.SCHOLARSHIP_HERO);
        mapper.updateHeroMediaId(id, result.mediaId());
        return result;
    }

    @Transactional
    public MediaUploadResult uploadCover(long id, MultipartFile file) {
        load(id);
        MediaUploadResult result = uploadFor(id, file, MediaCategory.SCHOLARSHIP_COVER);
        mapper.updateCoverMediaId(id, result.mediaId());
        return result;
    }

    private MediaUploadResult uploadFor(long scholarshipId, MultipartFile file, MediaCategory category) {
        try {
            return media.upload(file.getInputStream(), file.getOriginalFilename(), file.getContentType(),
                    file.getSize(), category, null,
                    new MediaOwnerRef(MediaOwnerKind.SCHOLARSHIP, scholarshipId), currentUserId());
        }
        catch (IOException e) {
            throw new UncheckedIOException("failed to read upload", e);
        }
    }

    private ScholarshipResponse toResponse(Scholarship s) {
        return ScholarshipResponse.of(s,
                resolveUrl(s.getHeroMediaId(), s.getHeroImageUrl()),
                resolveUrl(s.getCoverMediaId(), s.getCoverImageUrl()));
    }

    private String resolveUrl(Long mediaId, String legacy) {
        if (mediaId == null) {
            return legacy;
        }
        try {
            return media.publicUrl(mediaId);
        }
        catch (RuntimeException e) {
            return legacy;
        }
    }

    // ---- confidential ----

    @Transactional(readOnly = true)
    public ScholarshipInternalResponse getInternal(Long scholarshipId) {
        load(scholarshipId);
        ScholarshipInternal internal = mapper.findInternal(scholarshipId);
        String universityName = internal != null && internal.universityId() != null
                ? universityService.get(internal.universityId()).name() : null;
        return ScholarshipInternalResponse.of(internal, universityName);
    }

    @Transactional
    public ScholarshipInternalResponse putInternal(Long scholarshipId, ScholarshipInternalRequest req) {
        load(scholarshipId);
        if (req.universityId() != null) {
            universityService.get(req.universityId()); // 404 if it does not exist
        }
        mapper.upsertInternal(scholarshipId, new ScholarshipInternal(
                req.universityId(), req.partnershipId(),
                req.internalStatus() == null ? "DRAFT" : req.internalStatus(),
                req.operationalNotes(), req.confidentialTerms(), req.commissionModelJson()), currentUser());
        return getInternal(scholarshipId);
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
        s.setSummary(blankToNull(req.summary()));
        s.setCountry(req.country().toUpperCase(Locale.ROOT));
        s.setProvince(blankToNull(req.province()));
        s.setCity(blankToNull(req.city()));
        s.setField(blankToNull(req.field()));
        s.setTeachingLanguage(req.teachingLanguage());
        s.setFundingModel(req.fundingModel());
        s.setHasStipend(req.levelStipends() != null && !req.levelStipends().isEmpty());
        s.setDeadline(req.deadline());
        s.setBenefits(blankToNull(req.benefits()));
        s.setRequirements(blankToNull(req.requirements()));
        s.setPolicy(blankToNull(req.policy()));
        s.setRenewalConditions(blankToNull(req.renewalConditions()));
        s.setNonDegreeDuration(req.nonDegreeDuration() == null ? null : req.nonDegreeDuration().name());
        s.setStudyDurationMonths(req.studyDurationMonths());
        s.setApplicationChannel(req.applicationChannel() == null ? null : req.applicationChannel().name());
        s.setAgencyNumber(blankToNull(req.agencyNumber()));
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
        s.setRemark(blankToNull(req.remark()));
        s.setHeroImageUrl(blankToNull(req.heroImageUrl()));
        s.setCoverImageUrl(blankToNull(req.coverImageUrl()));
        // Media ids are primarily set through the dedicated upload endpoints; only
        // overwrite from the request when the client actually sent a value.
        if (req.heroMediaId() != null) {
            s.setHeroMediaId(req.heroMediaId());
        }
        if (req.coverMediaId() != null) {
            s.setCoverMediaId(req.coverMediaId());
        }
    }

    private void replaceChildren(Long id, ScholarshipRequest req) {
        mapper.deleteLevels(id);
        if (req.levels() != null) {
            req.levels().stream().distinct().forEach(l -> mapper.insertLevel(id, l.name()));
        }
        mapper.deleteCategoryLinks(id);
        if (req.categoryCodes() != null) {
            req.categoryCodes().stream().map(c -> c.trim().toUpperCase(Locale.ROOT)).distinct()
                    .filter(c -> categoryAllowed(c, req.fundingModel()))
                    .forEach(c -> mapper.insertCategoryLink(id, c));
        }
        mapper.deleteIntakes(id);
        if (req.intakes() != null) {
            int i = 0;
            for (ScholarshipRequest.IntakeInput in : req.intakes()) {
                mapper.insertIntake(id, new ScholarshipIntake(in.term().trim(),
                        in.applicationOpen(), in.applicationClose()), i++);
            }
        }
        mapper.deleteEligibility(id);
        if (req.eligibility() != null) {
            var e = req.eligibility();
            mapper.insertEligibility(id, new ScholarshipEligibility(
                    e.ageMin(), e.ageMax(),
                    e.nationalityScope() == null ? "ANY" : e.nationalityScope(),
                    blankToNull(e.acceptedCountries()), e.inChina(), e.gpaMin(), e.ieltsMin(),
                    e.toeflMin(), e.duolingoMin(), e.hskMin(), e.cscaMin(), blankToNull(e.notes())));
        }
        mapper.deleteFees(id);
        if (req.fees() != null) {
            int i = 0;
            for (ScholarshipRequest.FeeInput f : req.fees()) {
                mapper.insertFee(id, new ScholarshipFee(f.kind().name(), f.amount(),
                        f.currency().toUpperCase(Locale.ROOT), blankToNull(f.note())), i++);
            }
        }
        mapper.deleteLevelStipends(id);
        if (req.levelStipends() != null) {
            for (ScholarshipRequest.LevelStipendInput st : req.levelStipends()) {
                mapper.insertLevelStipend(id, new ScholarshipLevelStipend(st.level().name(), st.amount(),
                        st.currency().toUpperCase(Locale.ROOT), st.frequency().name(),
                        st.durationMonths(), blankToNull(st.conditions())));
            }
        }
        mapper.deleteAccommodations(id);
        if (req.accommodations() != null) {
            int i = 0;
            for (ScholarshipRequest.AccommodationInput a : req.accommodations()) {
                mapper.insertAccommodation(id, new ScholarshipAccommodation(a.roomType().name(), a.amount(),
                        a.currency() == null ? "CNY" : a.currency().toUpperCase(Locale.ROOT),
                        blankToNull(a.note())), i++);
            }
        }
        mapper.deleteCoverage(id);
        if (req.coverage() != null) {
            int i = 0;
            for (ScholarshipRequest.CoverageInput c : req.coverage()) {
                mapper.insertCoverage(id, new ScholarshipCoverage(c.kind().name(), blankToNull(c.detail())), i++);
            }
        }
        mapper.deleteDocumentRequirements(id);
        if (req.documentRequirements() != null) {
            int i = 0;
            for (ScholarshipRequest.DocumentRequirementInput d : req.documentRequirements()) {
                mapper.insertDocumentRequirement(id,
                        new com.nadoumi.scholarship.domain.ScholarshipDocumentRequirement(
                                d.docType().trim().toUpperCase(Locale.ROOT), d.mandatoryOrDefault(),
                                blankToNull(d.note())), i++);
            }
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
        String prefix = "NAC-" + java.time.Year.now().getValue() + "-";
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

    /**
     * Category applicability by funding model: a self-funded scholarship has no
     * categories at all, and a partially-funded one cannot be a CSC / CGS /
     * government "Type" scholarship (those are fully-funded schemes).
     */
    private static final java.util.Set<String> PARTIAL_EXCLUDED =
            java.util.Set.of("CSC", "CGS", "TYPE_A", "TYPE_B", "TYPE_C", "TYPE_D");

    private static boolean categoryAllowed(String code, FundingModel funding) {
        return switch (funding) {
            case SELF -> false;
            case PARTIAL -> !PARTIAL_EXCLUDED.contains(code);
            case FULLY -> true;
        };
    }

    private static String currentUser() {
        try {
            return SecurityUtils.getUsername();
        }
        catch (RuntimeException e) {
            return "system";
        }
    }

    private static long currentUserId() {
        try {
            Long id = SecurityUtils.getUserId();
            return id == null ? 0L : id;
        }
        catch (RuntimeException e) {
            return 0L;
        }
    }

    private static String upper(String s) {
        return s == null || s.isBlank() ? null : s.trim().toUpperCase(Locale.ROOT);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
