package com.nadoumi.applicant.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nadoumi.applicant.domain.Applicant;
import com.nadoumi.applicant.domain.ApplicantContact;
import com.nadoumi.applicant.domain.ApplicantEducation;
import com.nadoumi.applicant.domain.ApplicantTestScore;
import com.nadoumi.applicant.domain.enums.ApplicantStatus;
import com.nadoumi.applicant.mapper.ApplicantMapper;
import com.nadoumi.applicant.web.request.ContactRequest;
import com.nadoumi.applicant.web.request.EducationRequest;
import com.nadoumi.applicant.web.request.SelfApplicantRequest;
import com.nadoumi.applicant.web.request.StaffCreateApplicantRequest;
import com.nadoumi.applicant.web.request.TestScoreRequest;
import com.nadoumi.applicant.web.response.ApplicantResponse;
import com.nadoumi.applicant.web.response.ContactResponse;
import com.nadoumi.applicant.web.response.EducationResponse;
import com.nadoumi.applicant.web.response.PageResponse;
import com.nadoumi.applicant.web.response.TestScoreResponse;
import com.nadoumi.common.access.ApplicantCapability;
import com.nadoumi.common.web.PageSupport;
import com.nadoumi.common.media.MediaAccessLogContext;
import com.nadoumi.common.media.MediaCategory;
import com.nadoumi.common.media.MediaGateway;
import com.nadoumi.common.media.MediaOwnerKind;
import com.nadoumi.common.media.MediaOwnerRef;
import com.nadoumi.common.media.MediaUploadResult;
import com.nadoumi.common.media.SignedUrl;
import com.nadoumi.identity.access.CurrentCaller;
import com.nadoumi.identity.access.NadoumiAccessServiceImpl;
import com.nadoumi.identity.exception.NadForbiddenException;
import com.nadoumi.identity.exception.NadNotFoundException;
import com.nadoumi.identity.service.UserApplicantAccessService;
import com.ruoyi.framework.web.service.PermissionService;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Applicant profile + education / test scores / contacts. Every applicant-scoped
 * method re-checks authorization (defense in depth on top of the controller
 * {@code @PreAuthorize}); every student finder is restricted to the caller's
 * accessible applicant ids.
 */
@Service
public class ApplicantService {

    private final ApplicantMapper mapper;
    private final NadoumiAccessServiceImpl access;
    private final UserApplicantAccessService grants;
    private final CurrentCaller caller;
    private final PermissionService rbac;
    private final MediaGateway media;

    public ApplicantService(ApplicantMapper mapper, NadoumiAccessServiceImpl access,
            UserApplicantAccessService grants, CurrentCaller caller, PermissionService rbac,
            MediaGateway media) {
        this.mapper = mapper;
        this.access = access;
        this.grants = grants;
        this.caller = caller;
        this.rbac = rbac;
        this.media = media;
    }

    @Transactional(rollbackFor = Exception.class)
    public ApplicantResponse createAboutMe(SelfApplicantRequest req) {
        if (!caller.isExternal()) {
            throw new AccessDeniedException("staff create applicants via /api/staff/applicants");
        }
        Applicant a = new Applicant();
        apply(a, req.givenName(), req.familyName(), req.dob(), req.nationality(),
                req.passportNo(), req.email(), req.phone());
        a.setStatus(ApplicantStatus.ACTIVE);
        a.setCreateBy(String.valueOf(caller.requireUserId()));
        mapper.insert(a);
        grants.grantOwnerOnSelfRegistration(caller.requireUserId(), a.getId());
        return ApplicantResponse.of(a, true);
    }

    @Transactional(rollbackFor = Exception.class)
    public ApplicantResponse createByStaff(StaffCreateApplicantRequest req) {
        Applicant a = new Applicant();
        apply(a, req.givenName(), req.familyName(), req.dob(), req.nationality(),
                req.passportNo(), req.email(), req.phone());
        a.setStatus(ApplicantStatus.DRAFT);
        a.setCreateBy(String.valueOf(caller.requireUserId()));
        mapper.insert(a);
        grants.createStaffApplicantAccess(a.getId(), req.invitedEmail(), caller.requireUserId());
        return ApplicantResponse.of(a, includePii());
    }

    public ApplicantResponse get(Long id) {
        requireCapability(id, ApplicantCapability.VIEW_PROFILE);
        return ApplicantResponse.of(load(id), includePii());
    }

    @Transactional(rollbackFor = Exception.class)
    public ApplicantResponse update(Long id, SelfApplicantRequest req) {
        requireCapability(id, ApplicantCapability.EDIT_PROFILE);
        Applicant a = load(id);
        apply(a, req.givenName(), req.familyName(), req.dob(), req.nationality(),
                req.passportNo(), req.email(), req.phone());
        a.setUpdateBy(String.valueOf(caller.requireUserId()));
        mapper.update(a);
        return ApplicantResponse.of(a, includePii());
    }

    @Transactional(rollbackFor = Exception.class)
    public void archive(Long id) {
        if (!caller.isStaff()) {
            throw new AccessDeniedException("archive is a staff action");
        }
        load(id);
        mapper.updateStatus(id, ApplicantStatus.ARCHIVED, String.valueOf(caller.requireUserId()));
    }

    public PageResponse<ApplicantResponse> listForStaff(String name, ApplicantStatus status,
            String nationality, java.time.LocalDateTime createdAfter, int page, int size) {
        page = PageSupport.clampPage(page);
        size = PageSupport.clampSize(size);
        PageHelper.startPage(page + 1, size);
        List<Applicant> rows = mapper.search(name, status, nationality, createdAfter);
        long total = new PageInfo<>(rows).getTotal();
        boolean pii = includePii();
        List<ApplicantResponse> content = rows.stream().map(a -> ApplicantResponse.of(a, pii)).toList();
        return PageResponse.of(content, page, size, total);
    }

    public List<ApplicantResponse> listMine() {
        List<Long> ids = access.accessibleApplicantIds();
        if (ids.isEmpty()) {
            return List.of();
        }
        return mapper.findByIds(ids).stream().map(a -> ApplicantResponse.of(a, true)).toList();
    }

    // ---- profile photo (PROTECTED — signed URL, never a public URL) ----

    /**
     * Store the applicant's profile photo as a PROTECTED media asset and point
     * {@code photo_media_id} at it. Returns the new media id (no URL — PROTECTED
     * bytes are served only through {@link #photoUrl}).
     */
    @Transactional(rollbackFor = Exception.class)
    public long uploadPhoto(long applicantId, MultipartFile file) {
        requireCapability(applicantId, ApplicantCapability.EDIT_PROFILE);
        MediaUploadResult result;
        try {
            result = media.upload(file.getInputStream(), file.getOriginalFilename(), file.getContentType(),
                    file.getSize(), MediaCategory.APPLICANT_PHOTO, null,
                    new MediaOwnerRef(MediaOwnerKind.APPLICANT, applicantId), currentUserId());
        }
        catch (IOException e) {
            throw new UncheckedIOException("failed to read upload", e);
        }
        mapper.updatePhotoMediaId(applicantId, result.mediaId());
        return result.mediaId();
    }

    /**
     * Issue a short-TTL signed URL for the applicant's photo, writing a
     * {@code nad_media_access_log} entry. A caller without {@code VIEW_PROFILE}
     * gets a logged denial and a 403.
     */
    public SignedUrl photoUrl(long applicantId, MediaAccessLogContext ctx) {
        Applicant a = load(applicantId);
        Long photoMediaId = a.getPhotoMediaId();
        try {
            requireCapability(applicantId, ApplicantCapability.VIEW_PROFILE);
        }
        catch (AccessDeniedException e) {
            if (photoMediaId != null) {
                media.denyAndLog(photoMediaId, ctx, "NO_APPLICANT_GRANT");
            }
            throw new NadForbiddenException("missing VIEW_PROFILE on applicant " + applicantId);
        }
        if (photoMediaId == null) {
            throw new NadNotFoundException("applicant has no photo");
        }
        return media.issueSignedUrl(photoMediaId, ctx);
    }

    // ---- education ----

    public List<EducationResponse> education(Long applicantId) {
        requireCapability(applicantId, ApplicantCapability.VIEW_PROFILE);
        return mapper.findEducation(applicantId).stream().map(EducationResponse::of).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public EducationResponse addEducation(Long applicantId, EducationRequest req) {
        requireCapability(applicantId, ApplicantCapability.EDIT_PROFILE);
        ApplicantEducation e = new ApplicantEducation();
        e.setApplicantId(applicantId);
        e.setInstitution(req.institution());
        e.setLevel(req.level());
        e.setField(req.field());
        e.setGpa(req.gpa());
        e.setGpaScale(req.gpaScale());
        e.setStartDate(req.startDate());
        e.setEndDate(req.endDate());
        mapper.insertEducation(e);
        return EducationResponse.of(e);
    }

    @Transactional(rollbackFor = Exception.class)
    public EducationResponse updateEducation(Long applicantId, Long educationId, EducationRequest req) {
        requireCapability(applicantId, ApplicantCapability.EDIT_PROFILE);
        ApplicantEducation e = mapper.findEducationById(educationId);
        if (e == null || !e.getApplicantId().equals(applicantId)) {
            throw new NadNotFoundException("education not found");
        }
        e.setInstitution(req.institution());
        e.setLevel(req.level());
        e.setField(req.field());
        e.setGpa(req.gpa());
        e.setGpaScale(req.gpaScale());
        e.setStartDate(req.startDate());
        e.setEndDate(req.endDate());
        mapper.updateEducation(e);
        return EducationResponse.of(e);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteEducation(Long applicantId, Long educationId) {
        requireCapability(applicantId, ApplicantCapability.EDIT_PROFILE);
        if (mapper.deleteEducation(educationId, applicantId) == 0) {
            throw new NadNotFoundException("education not found");
        }
    }

    // ---- test scores ----

    public List<TestScoreResponse> testScores(Long applicantId) {
        requireCapability(applicantId, ApplicantCapability.VIEW_PROFILE);
        return mapper.findTestScores(applicantId).stream().map(TestScoreResponse::of).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public TestScoreResponse addTestScore(Long applicantId, TestScoreRequest req) {
        requireCapability(applicantId, ApplicantCapability.EDIT_PROFILE);
        ApplicantTestScore s = new ApplicantTestScore();
        s.setApplicantId(applicantId);
        s.setTestType(req.testType());
        s.setScore(req.score());
        s.setSubScoresJson(req.subScoresJson());
        s.setTakenOn(req.takenOn());
        s.setExpiresOn(req.expiresOn());
        mapper.insertTestScore(s);
        return TestScoreResponse.of(s);
    }

    @Transactional(rollbackFor = Exception.class)
    public TestScoreResponse updateTestScore(Long applicantId, Long scoreId, TestScoreRequest req) {
        requireCapability(applicantId, ApplicantCapability.EDIT_PROFILE);
        ApplicantTestScore s = mapper.findTestScoreById(scoreId);
        if (s == null || !s.getApplicantId().equals(applicantId)) {
            throw new NadNotFoundException("test score not found");
        }
        s.setTestType(req.testType());
        s.setScore(req.score());
        s.setSubScoresJson(req.subScoresJson());
        s.setTakenOn(req.takenOn());
        s.setExpiresOn(req.expiresOn());
        mapper.updateTestScore(s);
        return TestScoreResponse.of(s);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteTestScore(Long applicantId, Long scoreId) {
        requireCapability(applicantId, ApplicantCapability.EDIT_PROFILE);
        if (mapper.deleteTestScore(scoreId, applicantId) == 0) {
            throw new NadNotFoundException("test score not found");
        }
    }

    // ---- contacts ----

    public List<ContactResponse> contacts(Long applicantId) {
        requireCapability(applicantId, ApplicantCapability.VIEW_PROFILE);
        return mapper.findContacts(applicantId).stream().map(ContactResponse::of).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public ContactResponse addContact(Long applicantId, ContactRequest req) {
        requireCapability(applicantId, ApplicantCapability.EDIT_PROFILE);
        ApplicantContact c = new ApplicantContact();
        c.setApplicantId(applicantId);
        c.setRelation(req.relation());
        c.setName(req.name());
        c.setEmail(req.email());
        c.setPhone(req.phone());
        mapper.insertContact(c);
        return ContactResponse.of(c);
    }

    @Transactional(rollbackFor = Exception.class)
    public ContactResponse updateContact(Long applicantId, Long contactId, ContactRequest req) {
        requireCapability(applicantId, ApplicantCapability.EDIT_PROFILE);
        ApplicantContact c = mapper.findContactById(contactId);
        if (c == null || !c.getApplicantId().equals(applicantId)) {
            throw new NadNotFoundException("contact not found");
        }
        c.setRelation(req.relation());
        c.setName(req.name());
        c.setEmail(req.email());
        c.setPhone(req.phone());
        mapper.updateContact(c);
        return ContactResponse.of(c);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteContact(Long applicantId, Long contactId) {
        requireCapability(applicantId, ApplicantCapability.EDIT_PROFILE);
        if (mapper.deleteContact(contactId, applicantId) == 0) {
            throw new NadNotFoundException("contact not found");
        }
    }

    // ---- internals ----

    private Applicant load(Long id) {
        Applicant a = mapper.findById(id);
        if (a == null) {
            throw new NadNotFoundException("applicant not found");
        }
        return a;
    }

    private void requireCapability(Long applicantId, ApplicantCapability capability) {
        if (!access.canAccessApplicant(applicantId, capability.name())) {
            throw new AccessDeniedException("missing " + capability + " on applicant " + applicantId);
        }
    }

    private boolean includePii() {
        return caller.isExternal() || rbac.hasPermi("nad:applicant:pii:view");
    }

    private long currentUserId() {
        try {
            Long id = caller.requireUserId();
            return id == null ? 0L : id;
        }
        catch (RuntimeException e) {
            return 0L;
        }
    }

    private static void apply(
            Applicant a, String given,
            String family, LocalDate dob,
            String nationality, String passportNo,
            String email, String phone) {
        a.setGivenName(given);
        a.setFamilyName(family);
        a.setDob(dob);
        a.setNationality(nationality);
        a.setPassportNo(passportNo);
        a.setEmail(email);
        a.setPhone(phone);
    }
}
