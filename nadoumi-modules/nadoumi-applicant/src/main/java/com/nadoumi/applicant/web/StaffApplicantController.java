package com.nadoumi.applicant.web;

import com.nadoumi.applicant.domain.enums.ApplicantStatus;
import com.nadoumi.applicant.service.ApplicantService;
import com.nadoumi.applicant.web.response.ApplicantResponse;
import com.nadoumi.applicant.web.request.ContactRequest;
import com.nadoumi.applicant.web.response.ContactResponse;
import com.nadoumi.applicant.web.request.EducationRequest;
import com.nadoumi.applicant.web.response.EducationResponse;
import com.nadoumi.applicant.web.response.PageResponse;
import com.nadoumi.applicant.web.request.StaffCreateApplicantRequest;
import com.nadoumi.applicant.web.request.TestScoreRequest;
import com.nadoumi.applicant.web.response.TestScoreResponse;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.enums.BusinessType;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff/applicants")
public class StaffApplicantController {

    private final ApplicantService service;

    public StaffApplicantController(ApplicantService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("@ss.hasPermi('nad:applicant:list')")
    public PageResponse<ApplicantResponse> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) ApplicantStatus status,
            @RequestParam(required = false) String nationality,
            @RequestParam(required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
            java.time.LocalDateTime createdAfter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listForStaff(name, status, nationality, createdAfter, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('nad:applicant:view')")
    public ApplicantResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@ss.hasPermi('nad:applicant:create')")
    @Log(title = "Applicant", businessType = BusinessType.INSERT)
    public ApplicantResponse create(@Valid @RequestBody StaffCreateApplicantRequest req) {
        return service.createByStaff(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('nad:applicant:edit')")
    @Log(title = "Applicant", businessType = BusinessType.UPDATE)
    public ApplicantResponse update(@PathVariable Long id,
            @Valid @RequestBody com.nadoumi.applicant.web.request.SelfApplicantRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@ss.hasPermi('nad:applicant:archive')")
    @Log(title = "Applicant", businessType = BusinessType.UPDATE)
    public void archive(@PathVariable Long id) {
        service.archive(id);
    }

    @GetMapping("/{id}/education")
    @PreAuthorize("@ss.hasPermi('nad:applicant:view')")
    public List<EducationResponse> education(@PathVariable Long id) {
        return service.education(id);
    }

    @PostMapping("/{id}/education")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@ss.hasPermi('nad:applicant:edit')")
    public EducationResponse addEducation(@PathVariable Long id, @Valid @RequestBody EducationRequest req) {
        return service.addEducation(id, req);
    }

    @DeleteMapping("/{id}/education/{educationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@ss.hasPermi('nad:applicant:edit')")
    public void deleteEducation(@PathVariable Long id, @PathVariable Long educationId) {
        service.deleteEducation(id, educationId);
    }

    @GetMapping("/{id}/test-scores")
    @PreAuthorize("@ss.hasPermi('nad:applicant:view')")
    public List<TestScoreResponse> testScores(@PathVariable Long id) {
        return service.testScores(id);
    }

    @PostMapping("/{id}/test-scores")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@ss.hasPermi('nad:applicant:edit')")
    public TestScoreResponse addTestScore(@PathVariable Long id, @Valid @RequestBody TestScoreRequest req) {
        return service.addTestScore(id, req);
    }

    @GetMapping("/{id}/contacts")
    @PreAuthorize("@ss.hasPermi('nad:applicant:view')")
    public List<ContactResponse> contacts(@PathVariable Long id) {
        return service.contacts(id);
    }

    @PostMapping("/{id}/contacts")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@ss.hasPermi('nad:applicant:edit')")
    public ContactResponse addContact(@PathVariable Long id, @Valid @RequestBody ContactRequest req) {
        return service.addContact(id, req);
    }
}
