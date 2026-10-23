package com.nadoumi.program.web;

import com.nadoumi.common.web.PageResponse;
import com.nadoumi.program.service.MajorService;
import com.nadoumi.program.web.request.MajorBulkRequest;
import com.nadoumi.program.web.request.MajorMergeRequest;
import com.nadoumi.program.web.request.MajorRequest;
import com.nadoumi.program.web.response.MajorBulkResponse;
import com.nadoumi.program.web.response.MajorResponse;
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

/** Staff management of the shared major catalogue. */
@RestController
@RequestMapping("/api/staff/majors")
public class StaffMajorController {

    private final MajorService service;

    public StaffMajorController(MajorService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("@ss.hasPermi('nad:major:list')")
    public PageResponse<MajorResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String discipline,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.list(q, discipline, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@ss.hasPermi('nad:major:add')")
    @Log(title = "Major", businessType = BusinessType.INSERT)
    public MajorResponse create(@Valid @RequestBody MajorRequest req) {
        return service.create(req);
    }

    @PostMapping("/bulk")
    @PreAuthorize("@ss.hasPermi('nad:major:add')")
    @Log(title = "Major", businessType = BusinessType.INSERT)
    public MajorBulkResponse bulk(@Valid @RequestBody MajorBulkRequest req) {
        return service.bulk(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('nad:major:edit')")
    @Log(title = "Major", businessType = BusinessType.UPDATE)
    public MajorResponse update(@PathVariable Long id, @Valid @RequestBody MajorRequest req) {
        return service.update(id, req);
    }

    @PostMapping("/{id}/merge")
    @PreAuthorize("@ss.hasPermi('nad:major:edit')")
    @Log(title = "Major", businessType = BusinessType.UPDATE)
    public MajorResponse merge(@PathVariable Long id, @Valid @RequestBody MajorMergeRequest req) {
        return service.merge(id, req.intoId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@ss.hasPermi('nad:major:remove')")
    @Log(title = "Major", businessType = BusinessType.DELETE)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
