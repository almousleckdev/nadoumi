package com.nadoumi.applicant.service;

import com.nadoumi.applicant.domain.ApplicantContact;
import com.nadoumi.applicant.domain.ApplicantTestScore;
import com.nadoumi.applicant.mapper.ApplicantMapper;
import com.nadoumi.applicant.web.request.ContactRequest;
import com.nadoumi.applicant.web.request.TestScoreRequest;
import com.nadoumi.applicant.web.response.ContactResponse;
import com.nadoumi.applicant.web.response.TestScoreResponse;
import com.nadoumi.common.access.ApplicantCapability;
import com.nadoumi.common.exception.NadNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Test scores and emergency contacts attached to an applicant; every call re-checks the applicant grant. */
@Service
public class ApplicantRecordsService {

    private final ApplicantMapper mapper;
    private final ApplicantAccessGuard guard;

    public ApplicantRecordsService(ApplicantMapper mapper, ApplicantAccessGuard guard) {
        this.mapper = mapper;
        this.guard = guard;
    }

    public List<TestScoreResponse> testScores(Long applicantId) {
        guard.require(applicantId, ApplicantCapability.VIEW_PROFILE);
        return mapper.findTestScores(applicantId).stream().map(TestScoreResponse::of).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public TestScoreResponse addTestScore(Long applicantId, TestScoreRequest req) {
        guard.require(applicantId, ApplicantCapability.EDIT_PROFILE);
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
        guard.require(applicantId, ApplicantCapability.EDIT_PROFILE);
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
        guard.require(applicantId, ApplicantCapability.EDIT_PROFILE);
        if (mapper.deleteTestScore(scoreId, applicantId) == 0) {
            throw new NadNotFoundException("test score not found");
        }
    }

    public List<ContactResponse> contacts(Long applicantId) {
        guard.require(applicantId, ApplicantCapability.VIEW_PROFILE);
        return mapper.findContacts(applicantId).stream().map(ContactResponse::of).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public ContactResponse addContact(Long applicantId, ContactRequest req) {
        guard.require(applicantId, ApplicantCapability.EDIT_PROFILE);
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
        guard.require(applicantId, ApplicantCapability.EDIT_PROFILE);
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
        guard.require(applicantId, ApplicantCapability.EDIT_PROFILE);
        if (mapper.deleteContact(contactId, applicantId) == 0) {
            throw new NadNotFoundException("contact not found");
        }
    }
}
