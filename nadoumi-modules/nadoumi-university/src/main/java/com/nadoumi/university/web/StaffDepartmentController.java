package com.nadoumi.university.web;

import com.nadoumi.university.service.DepartmentService;
import com.nadoumi.university.web.request.DepartmentRequest;
import com.nadoumi.university.web.response.DepartmentResponse;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Academic departments of a university. Managed inline on the University detail screen. */
@RestController
@RequestMapping("/api/staff/universities/{universityId}/departments")
public class StaffDepartmentController {

    private final DepartmentService service;

    public StaffDepartmentController(DepartmentService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("@ss.hasAnyPermi('nad:department:list,nad:department:add,nad:university:edit,nad:program:create,nad:program:edit')")
    public List<DepartmentResponse> list(@PathVariable long universityId) {
        return service.list(universityId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasAnyPermi('nad:department:list,nad:department:add,nad:university:edit,nad:program:create,nad:program:edit')")
    public DepartmentResponse get(@PathVariable long universityId, @PathVariable long id) {
        return service.get(universityId, id);
    }

    @PostMapping
    @PreAuthorize("@ss.hasAnyPermi('nad:department:add,nad:university:edit')")
    @Log(title = "Department", businessType = BusinessType.INSERT)
    @ResponseStatus(HttpStatus.CREATED)
    public DepartmentResponse create(@PathVariable long universityId, @Valid @RequestBody DepartmentRequest req) {
        return service.create(universityId, req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasAnyPermi('nad:department:edit,nad:university:edit')")
    @Log(title = "Department", businessType = BusinessType.UPDATE)
    public DepartmentResponse update(@PathVariable long universityId, @PathVariable long id,
            @Valid @RequestBody DepartmentRequest req) {
        return service.update(universityId, id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasAnyPermi('nad:department:remove,nad:university:edit')")
    @Log(title = "Department", businessType = BusinessType.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long universityId, @PathVariable long id) {
        service.delete(universityId, id);
    }
}
