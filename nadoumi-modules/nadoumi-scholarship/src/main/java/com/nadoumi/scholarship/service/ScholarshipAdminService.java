package com.nadoumi.scholarship.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nadoumi.common.web.PageResponse;
import com.nadoumi.identity.exception.NadBadRequestException;
import com.nadoumi.identity.exception.NadNotFoundException;
import com.nadoumi.scholarship.domain.Scholarship;
import com.nadoumi.scholarship.domain.ScholarshipEligibility;
import com.nadoumi.scholarship.domain.ScholarshipFee;
import com.nadoumi.scholarship.domain.ScholarshipInternal;
import com.nadoumi.scholarship.domain.ScholarshipIntake;
import com.nadoumi.scholarship.domain.ScholarshipStipend;
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
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public ScholarshipAdminService(ScholarshipMapper mapper, UniversityService universityService) {
        this.mapper = mapper;
        this.universityService = universityService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ScholarshipResponse> list(ScholarshipSearch filter, int page, int size) {
        PageHelper.startPage(page + 1, size);
        List<Scholarship> rows = mapper.searchStaff(filter);
        long total = new PageInfo<>(rows).getTotal();
        return PageResponse.of(rows.stream().map(ScholarshipResponse::of).toList(), page, size, total);
    }

    @Transactional(readOnly = true)
    public ScholarshipResponse get(Long id) {
        return ScholarshipResponse.of(loadWithChildren(id));
    }

    @Transactional
    public ScholarshipResponse create(ScholarshipRequest req) {
        Scholarship s = new Scholarship();
        apply(s, req);
        s.setSlug(uniqueSlug(req.title(), null));
        s.setCreateBy(currentUser());
        if (req.publishStatus() == PublishStatus.PUBLISHED) {
            s.setPublishedAt(java.time.LocalDateTime.now());
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
        s.setStipend(mapper.findStipend(id));
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
        s.setHasStipend(Boolean.TRUE.equals(req.hasStipend()) || req.stipend() != null);
        s.setDeadline(req.deadline());
        s.setBenefits(blankToNull(req.benefits()));
        s.setRequirements(blankToNull(req.requirements()));
        s.setPolicy(blankToNull(req.policy()));
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
        mapper.deleteStipend(id);
        if (req.stipend() != null) {
            var st = req.stipend();
            mapper.insertStipend(id, new ScholarshipStipend(st.amount(),
                    st.currency().toUpperCase(Locale.ROOT), st.frequency().name(),
                    st.durationMonths(), blankToNull(st.conditions())));
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

    /** The slug is always derived from the title -- lower-case kebab, de-duplicated. */
    private String uniqueSlug(String title, Long selfId) {
        String base = slugify(title);
        String candidate = base;
        for (int n = 2; ; n++) {
            Long owner = mapper.findIdBySlug(candidate);
            if (owner == null || owner.equals(selfId)) {
                return candidate;
            }
            candidate = base + "-" + n;
        }
    }

    private static String slugify(String value) {
        String s = Normalizer.normalize(value, Normalizer.Form.NFKD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (s.isEmpty()) {
            throw new NadBadRequestException("could not derive a slug from the title");
        }
        return s.length() > 150 ? s.substring(0, 150) : s;
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

    private static String upper(String s) {
        return s == null || s.isBlank() ? null : s.trim().toUpperCase(Locale.ROOT);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
