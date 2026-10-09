package com.nadoumi.applicant.web;

import com.nadoumi.applicant.domain.enums.ApplicantStatus;
import com.nadoumi.applicant.service.ApplicantMediaKind;
import com.nadoumi.applicant.service.ApplicantErasureService;
import com.nadoumi.applicant.service.ApplicantMediaService;
import com.nadoumi.applicant.service.ApplicantRecordsService;
import com.nadoumi.applicant.service.ApplicantService;
import com.nadoumi.applicant.service.EducationService;
import com.nadoumi.applicant.service.PassportService;
import com.nadoumi.applicant.web.request.ContactRequest;
import com.nadoumi.applicant.web.request.EducationRequest;
import com.nadoumi.applicant.web.request.SelfApplicantRequest;
import com.nadoumi.applicant.web.request.StaffCreateApplicantRequest;
import com.nadoumi.applicant.web.request.TestScoreRequest;
import com.nadoumi.applicant.web.response.ApplicantResponse;
import com.nadoumi.applicant.web.response.ContactResponse;
import com.nadoumi.applicant.web.response.EducationResponse;
import com.nadoumi.applicant.web.response.PageResponse;
import com.nadoumi.applicant.web.response.PassportStatusResponse;
import com.nadoumi.applicant.web.response.TestScoreResponse;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.enums.BusinessType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/staff/applicants")
public class StaffApplicantController {

    private final ApplicantService service;
    private final ApplicantRecordsService records;
    private final ApplicantMediaService media;
    private final PassportService passports;
    private final EducationService education;
    private final ApplicantErasureService erasure;

    public StaffApplicantController(ApplicantService service, ApplicantRecordsService records,
            ApplicantMediaService media, PassportService passports, EducationService education,
            ApplicantErasureService erasure) {
        this.service = service;
        this.records = records;
        this.media = media;
        this.passports = passports;
        this.education = education;
        this.erasure = erasure;
    }

    @GetMapping
    @PreAuthorize("@ss.hasPermi('nad:applicant:list')")
    public PageResponse<ApplicantResponse> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) ApplicantStatus status,
            @RequestParam(required = false) String nationality,
            @RequestParam(required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime createdAfter,
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
            @Valid @RequestBody SelfApplicantRequest req) {
        return service.update(id, req);
    }

    /** Permanent delete: the applicant, its applications, documents and files, and a student login left without one. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@ss.hasPermi('nad:applicant:delete')")
    @Log(title = "Applicant", businessType = BusinessType.DELETE)
    public void delete(@PathVariable Long id) {
        erasure.erase(id);
    }

    // ---- protected files: profile photo and passport scan ----

    @PostMapping("/{id}/photo")
    @PreAuthorize("@ss.hasPermi('nad:applicant:edit')")
    @Log(title = "Applicant photo", businessType = BusinessType.UPDATE)
    public ProtectedMediaResponses.Uploaded uploadPhoto(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return new ProtectedMediaResponses.Uploaded(media.upload(id, ApplicantMediaKind.PHOTO, file));
    }

    @GetMapping("/{id}/photo")
    @PreAuthorize("@ss.hasPermi('nad:applicant:view')")
    public ResponseEntity<ProtectedMediaResponses.Url> photo(@PathVariable Long id,
            @RequestParam(name = "json", required = false) String json, HttpServletRequest request) {
        return ProtectedMediaResponses.signed(
                media.signedUrl(id, ApplicantMediaKind.PHOTO, ProtectedMediaResponses.accessContext(request)), json, request);
    }

    /** The passport scan is identity-document PII: staff need the PII permission as well as view. */
    @GetMapping("/{id}/passport/scan")
    @PreAuthorize("@ss.hasPermi('nad:applicant:view') and @ss.hasPermi('nad:applicant:pii:view')")
    public ResponseEntity<ProtectedMediaResponses.Url> passportScan(@PathVariable Long id,
            @RequestParam(name = "json", required = false) String json, HttpServletRequest request) {
        return ProtectedMediaResponses.signed(
                media.signedUrl(id, ApplicantMediaKind.PASSPORT, ProtectedMediaResponses.accessContext(request)), json, request);
    }

    @GetMapping("/{id}/passport")
    @PreAuthorize("@ss.hasPermi('nad:applicant:view')")
    public PassportStatusResponse passport(@PathVariable Long id) {
        return passports.status(id);
    }

    @GetMapping("/{id}/education")
    @PreAuthorize("@ss.hasPermi('nad:applicant:view')")
    public List<EducationResponse> education(@PathVariable Long id) {
        return education.list(id);
    }

    @PostMapping("/{id}/education")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@ss.hasPermi('nad:applicant:edit')")
    public EducationResponse addEducation(@PathVariable Long id, @Valid @RequestBody EducationRequest req) {
        return education.add(id, req);
    }

    @PutMapping("/{id}/education/{educationId}")
    @PreAuthorize("@ss.hasPermi('nad:applicant:edit')")
    @Log(title = "Applicant education", businessType = BusinessType.UPDATE)
    public EducationResponse updateEducation(@PathVariable Long id, @PathVariable Long educationId,
            @Valid @RequestBody EducationRequest req) {
        return education.update(id, educationId, req);
    }

    @DeleteMapping("/{id}/education/{educationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@ss.hasPermi('nad:applicant:edit')")
    public void deleteEducation(@PathVariable Long id, @PathVariable Long educationId) {
        education.delete(id, educationId);
    }

    @GetMapping("/{id}/test-scores")
    @PreAuthorize("@ss.hasPermi('nad:applicant:view')")
    public List<TestScoreResponse> testScores(@PathVariable Long id) {
        return records.testScores(id);
    }

    @PostMapping("/{id}/test-scores")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@ss.hasPermi('nad:applicant:edit')")
    public TestScoreResponse addTestScore(@PathVariable Long id, @Valid @RequestBody TestScoreRequest req) {
        return records.addTestScore(id, req);
    }

    @PutMapping("/{id}/test-scores/{scoreId}")
    @PreAuthorize("@ss.hasPermi('nad:applicant:edit')")
    @Log(title = "Applicant test score", businessType = BusinessType.UPDATE)
    public TestScoreResponse updateTestScore(@PathVariable Long id, @PathVariable Long scoreId,
            @Valid @RequestBody TestScoreRequest req) {
        return records.updateTestScore(id, scoreId, req);
    }

    @DeleteMapping("/{id}/test-scores/{scoreId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@ss.hasPermi('nad:applicant:edit')")
    public void deleteTestScore(@PathVariable Long id, @PathVariable Long scoreId) {
        records.deleteTestScore(id, scoreId);
    }

    @GetMapping("/{id}/contacts")
    @PreAuthorize("@ss.hasPermi('nad:applicant:view')")
    public List<ContactResponse> contacts(@PathVariable Long id) {
        return records.contacts(id);
    }

    @PostMapping("/{id}/contacts")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@ss.hasPermi('nad:applicant:edit')")
    public ContactResponse addContact(@PathVariable Long id, @Valid @RequestBody ContactRequest req) {
        return records.addContact(id, req);
    }

    @PutMapping("/{id}/contacts/{contactId}")
    @PreAuthorize("@ss.hasPermi('nad:applicant:edit')")
    @Log(title = "Applicant contact", businessType = BusinessType.UPDATE)
    public ContactResponse updateContact(@PathVariable Long id, @PathVariable Long contactId,
            @Valid @RequestBody ContactRequest req) {
        return records.updateContact(id, contactId, req);
    }

    @DeleteMapping("/{id}/contacts/{contactId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@ss.hasPermi('nad:applicant:edit')")
    public void deleteContact(@PathVariable Long id, @PathVariable Long contactId) {
        records.deleteContact(id, contactId);
    }
    
    
    
}
