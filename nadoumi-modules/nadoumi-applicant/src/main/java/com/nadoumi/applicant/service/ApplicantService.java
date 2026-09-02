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
import com.nadoumi.identity.access.CurrentCaller;
import com.nadoumi.identity.access.NadoumiAccessServiceImpl;
import com.nadoumi.identity.exception.NadNotFoundException;
import com.nadoumi.identity.service.UserApplicantAccessService;
import com.ruoyi.framework.web.service.PermissionService;

import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public ApplicantService(ApplicantMapper mapper, NadoumiAccessServiceImpl access,
            UserApplicantAccessService grants, CurrentCaller caller, PermissionService rbac) {
        this.mapper = mapper;
        this.access = access;
        this.grants = grants;
        this.caller = caller;
        this.rbac = rbac;
    }

    @Transactional
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

    @Transactional
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

    @Transactional
    public ApplicantResponse update(Long id, SelfApplicantRequest req) {
        requireCapability(id, ApplicantCapability.EDIT_PROFILE);
        Applicant a = load(id);
        apply(a, req.givenName(), req.familyName(), req.dob(), req.nationality(),
                req.passportNo(), req.email(), req.phone());
        a.setUpdateBy(String.valueOf(caller.requireUserId()));
        mapper.update(a);
        return ApplicantResponse.of(a, includePii());
    }

    @Transactional
    public void archive(Long id) {
        if (!caller.isStaff()) {
            throw new AccessDeniedException("archive is a staff action");
        }
        load(id);
        mapper.updateStatus(id, ApplicantStatus.ARCHIVED, String.valueOf(caller.requireUserId()));
    }

    public PageResponse<ApplicantResponse> listForStaff(String name, ApplicantStatus status,
            String nationality, java.time.LocalDateTime createdAfter, int page, int size) {
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

    // ---- education ----

    public List<EducationResponse> education(Long applicantId) {
        requireCapability(applicantId, ApplicantCapability.VIEW_PROFILE);
        return mapper.findEducation(applicantId).stream().map(EducationResponse::of).toList();
    }

    @Transactional
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

    @Transactional
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

    @Transactional
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

    @Transactional
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

    @Transactional
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

    @Transactional
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

    @Transactional
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
