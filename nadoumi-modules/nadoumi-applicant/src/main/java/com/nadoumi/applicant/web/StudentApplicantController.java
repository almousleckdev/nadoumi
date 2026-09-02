package com.nadoumi.applicant.web;

import com.nadoumi.applicant.service.ApplicantService;
import com.nadoumi.applicant.web.response.ApplicantResponse;
import com.nadoumi.applicant.web.request.ContactRequest;
import com.nadoumi.applicant.web.response.ContactResponse;
import com.nadoumi.applicant.web.request.EducationRequest;
import com.nadoumi.applicant.web.response.EducationResponse;
import com.nadoumi.applicant.web.request.SelfApplicantRequest;
import com.nadoumi.applicant.web.request.TestScoreRequest;
import com.nadoumi.applicant.web.response.TestScoreResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/applicants")
public class StudentApplicantController {

    private final ApplicantService service;

    public StudentApplicantController(ApplicantService service) {
        this.service = service;
    }

    @GetMapping
    public List<ApplicantResponse> mine() {
        return service.listMine();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicantResponse createAboutMe(@Valid @RequestBody SelfApplicantRequest req) {
        return service.createAboutMe(req);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@na.canAccessApplicant(#id, 'VIEW_PROFILE')")
    public ApplicantResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@na.canAccessApplicant(#id, 'EDIT_PROFILE')")
    public ApplicantResponse update(@PathVariable Long id, @Valid @RequestBody SelfApplicantRequest req) {
        return service.update(id, req);
    }

    @GetMapping("/{id}/education")
    @PreAuthorize("@na.canAccessApplicant(#id, 'VIEW_PROFILE')")
    public List<EducationResponse> education(@PathVariable Long id) {
        return service.education(id);
    }

    @PostMapping("/{id}/education")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@na.canAccessApplicant(#id, 'EDIT_PROFILE')")
    public EducationResponse addEducation(@PathVariable Long id, @Valid @RequestBody EducationRequest req) {
        return service.addEducation(id, req);
    }

    @PutMapping("/{id}/education/{educationId}")
    @PreAuthorize("@na.canAccessApplicant(#id, 'EDIT_PROFILE')")
    public EducationResponse updateEducation(@PathVariable Long id, @PathVariable Long educationId,
            @Valid @RequestBody EducationRequest req) {
        return service.updateEducation(id, educationId, req);
    }

    @DeleteMapping("/{id}/education/{educationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@na.canAccessApplicant(#id, 'EDIT_PROFILE')")
    public void deleteEducation(@PathVariable Long id, @PathVariable Long educationId) {
        service.deleteEducation(id, educationId);
    }

    @GetMapping("/{id}/test-scores")
    @PreAuthorize("@na.canAccessApplicant(#id, 'VIEW_PROFILE')")
    public List<TestScoreResponse> testScores(@PathVariable Long id) {
        return service.testScores(id);
    }

    @PostMapping("/{id}/test-scores")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@na.canAccessApplicant(#id, 'EDIT_PROFILE')")
    public TestScoreResponse addTestScore(@PathVariable Long id, @Valid @RequestBody TestScoreRequest req) {
        return service.addTestScore(id, req);
    }

    @DeleteMapping("/{id}/test-scores/{scoreId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@na.canAccessApplicant(#id, 'EDIT_PROFILE')")
    public void deleteTestScore(@PathVariable Long id, @PathVariable Long scoreId) {
        service.deleteTestScore(id, scoreId);
    }

    @GetMapping("/{id}/contacts")
    @PreAuthorize("@na.canAccessApplicant(#id, 'VIEW_PROFILE')")
    public List<ContactResponse> contacts(@PathVariable Long id) {
        return service.contacts(id);
    }

    @PostMapping("/{id}/contacts")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@na.canAccessApplicant(#id, 'EDIT_PROFILE')")
    public ContactResponse addContact(@PathVariable Long id, @Valid @RequestBody ContactRequest req) {
        return service.addContact(id, req);
    }

    @DeleteMapping("/{id}/contacts/{contactId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@na.canAccessApplicant(#id, 'EDIT_PROFILE')")
    public void deleteContact(@PathVariable Long id, @PathVariable Long contactId) {
        service.deleteContact(id, contactId);
    }
}
