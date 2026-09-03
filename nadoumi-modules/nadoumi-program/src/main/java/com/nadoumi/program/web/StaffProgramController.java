package com.nadoumi.program.web;

import com.nadoumi.common.web.PageResponse;
import com.nadoumi.program.domain.enums.ProgramStatus;
import com.nadoumi.program.domain.enums.ProgramTeachingLanguage;
import com.nadoumi.program.domain.enums.ProgramType;
import com.nadoumi.program.service.ProgramService;
import com.nadoumi.program.web.request.ProgramRequest;
import com.nadoumi.program.web.response.ProgramResponse;
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

/** Staff programme catalog admin. A programme belongs to one university. */
@RestController
@RequestMapping("/api/staff/programs")
public class StaffProgramController {

    private final ProgramService service;

    public StaffProgramController(ProgramService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("@ss.hasPermi('nad:program:list')")
    public PageResponse<ProgramResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long universityId,
            @RequestParam(required = false) ProgramType type,
            @RequestParam(required = false) ProgramTeachingLanguage language,
            @RequestParam(required = false) String field,
            @RequestParam(required = false) ProgramStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.list(q, universityId, type, language, field, status, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('nad:program:view')")
    public ProgramResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@ss.hasPermi('nad:program:create')")
    @Log(title = "Programme", businessType = BusinessType.INSERT)
    public ProgramResponse create(@Valid @RequestBody ProgramRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('nad:program:edit')")
    @Log(title = "Programme", businessType = BusinessType.UPDATE)
    public ProgramResponse update(@PathVariable Long id, @Valid @RequestBody ProgramRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@ss.hasPermi('nad:program:remove')")
    @Log(title = "Programme", businessType = BusinessType.DELETE)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
