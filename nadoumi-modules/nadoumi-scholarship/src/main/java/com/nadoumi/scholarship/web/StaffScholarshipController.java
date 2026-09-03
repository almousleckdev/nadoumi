package com.nadoumi.scholarship.web;

import com.nadoumi.common.web.PageResponse;
import com.nadoumi.scholarship.domain.enums.FundingModel;
import com.nadoumi.scholarship.domain.enums.PublishStatus;
import com.nadoumi.scholarship.domain.enums.ScholarshipStatus;
import com.nadoumi.scholarship.mapper.ScholarshipSearch;
import com.nadoumi.scholarship.service.ScholarshipAdminService;
import com.nadoumi.scholarship.web.request.ScholarshipInternalRequest;
import com.nadoumi.scholarship.web.request.ScholarshipRequest;
import com.nadoumi.scholarship.web.response.ScholarshipInternalResponse;
import com.nadoumi.scholarship.web.response.ScholarshipResponse;
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

/**
 * Staff scholarship admin. Student-safe CRUD under {@code nad:scholarship:*};
 * the confidential linkage sub-resource is gated separately by
 * {@code nad:scholarship:internal:view} / {@code :edit}.
 */
@RestController
@RequestMapping("/api/staff/scholarships")
public class StaffScholarshipController {

    private final ScholarshipAdminService service;

    public StaffScholarshipController(ScholarshipAdminService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("@ss.hasPermi('nad:scholarship:list')")
    public PageResponse<ScholarshipResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) FundingModel funding,
            @RequestParam(required = false) PublishStatus publishStatus,
            @RequestParam(required = false) ScholarshipStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        ScholarshipSearch f = new ScholarshipSearch(q, country, null, null, null, null, funding,
                null, null, List.of(), List.of(), List.of(), null, null, null, null, publishStatus, status);
        return service.list(f, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('nad:scholarship:view')")
    public ScholarshipResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @PreAuthorize("@ss.hasPermi('nad:scholarship:create')")
    @Log(title = "Scholarship", businessType = BusinessType.INSERT)
    @ResponseStatus(HttpStatus.CREATED)
    public ScholarshipResponse create(@Valid @RequestBody ScholarshipRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('nad:scholarship:edit')")
    @Log(title = "Scholarship", businessType = BusinessType.UPDATE)
    public ScholarshipResponse update(@PathVariable Long id, @Valid @RequestBody ScholarshipRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('nad:scholarship:remove')")
    @Log(title = "Scholarship", businessType = BusinessType.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    // ---- confidential sub-resource ----

    @GetMapping("/{id}/internal")
    @PreAuthorize("@ss.hasPermi('nad:scholarship:internal:view')")
    public ScholarshipInternalResponse getInternal(@PathVariable Long id) {
        return service.getInternal(id);
    }

    @PutMapping("/{id}/internal")
    @PreAuthorize("@ss.hasPermi('nad:scholarship:internal:edit')")
    @Log(title = "Scholarship linkage", businessType = BusinessType.UPDATE)
    public ScholarshipInternalResponse putInternal(@PathVariable Long id,
            @Valid @RequestBody ScholarshipInternalRequest req) {
        return service.putInternal(id, req);
    }
}
