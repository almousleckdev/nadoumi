package com.nadoumi.applicant.web;

import com.nadoumi.applicant.domain.enums.ApplicantStatus;
import com.nadoumi.applicant.service.ApplicantMediaKind;
import com.nadoumi.applicant.service.ApplicantMediaService;
import com.nadoumi.applicant.service.ApplicantService;
import com.nadoumi.applicant.service.EducationService;
import com.nadoumi.applicant.service.PassportService;
import com.nadoumi.applicant.web.response.PassportStatusResponse;
import com.nadoumi.applicant.web.response.ApplicantResponse;
import com.nadoumi.applicant.web.response.ContactResponse;
import com.nadoumi.applicant.web.response.EducationResponse;
import com.nadoumi.applicant.web.response.PageResponse;
import com.nadoumi.applicant.web.request.StaffCreateApplicantRequest;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.enums.BusinessType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff/applicants")
public class StaffApplicantController {

    private final ApplicantService service;
    private final ApplicantMediaService media;
    private final PassportService passports;
    private final EducationService education;

    public StaffApplicantController(ApplicantService service, ApplicantMediaService media, PassportService passports,
            EducationService education) {
        this.service = service;
        this.media = media;
        this.passports = passports;
        this.education = education;
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

    // ---- protected files: profile photo and passport scan ----

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

    @GetMapping("/{id}/contacts")
    @PreAuthorize("@ss.hasPermi('nad:applicant:view')")
    public List<ContactResponse> contacts(@PathVariable Long id) {
        return service.contacts(id);
    }

    }
