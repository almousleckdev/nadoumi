package com.nadoumi.university.web;

import com.nadoumi.common.web.PageResponse;
import com.nadoumi.university.domain.enums.UniversityStatus;
import com.nadoumi.university.domain.enums.UniversityType;
import com.nadoumi.university.service.UniversityService;
import com.nadoumi.university.web.request.UniversityRequest;
import com.nadoumi.university.web.response.UniversityResponse;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.enums.BusinessType;
import jakarta.validation.Valid;
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

/** Staff university catalog admin. Public data only — no partnership linkage here. */
@RestController
@RequestMapping("/api/staff/universities")
public class StaffUniversityController {

    private final UniversityService service;

    public StaffUniversityController(UniversityService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("@ss.hasPermi('nad:university:list')")
    public PageResponse<UniversityResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) UniversityType type,
            @RequestParam(required = false) UniversityStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.list(q, country, province, city, type, status, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('nad:university:view')")
    public UniversityResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@ss.hasPermi('nad:university:create')")
    @Log(title = "University", businessType = BusinessType.INSERT)
    public UniversityResponse create(@Valid @RequestBody UniversityRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('nad:university:edit')")
    @Log(title = "University", businessType = BusinessType.UPDATE)
    public UniversityResponse update(@PathVariable Long id, @Valid @RequestBody UniversityRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@ss.hasPermi('nad:university:remove')")
    @Log(title = "University", businessType = BusinessType.DELETE)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
