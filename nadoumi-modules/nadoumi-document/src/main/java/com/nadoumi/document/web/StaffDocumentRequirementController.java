package com.nadoumi.document.web;

import com.nadoumi.document.domain.enums.RequirementScope;
import com.nadoumi.document.service.DocumentRequirementService;
import com.nadoumi.document.web.request.DocumentRequirementsRequest;
import com.nadoumi.document.web.response.DocumentRequirementResponse;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.enums.BusinessType;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The documents a programme asks for, edited from the programme screen. Scholarships keep their own list. */
@RestController
@RequestMapping("/api/staff/document-requirements/programs/{programId}")
public class StaffDocumentRequirementController {

    private final DocumentRequirementService service;

    public StaffDocumentRequirementController(DocumentRequirementService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("@ss.hasPermi('nad:program:view')")
    public List<DocumentRequirementResponse> get(@PathVariable long programId) {
        return service.checklistFor(RequirementScope.PROGRAM, programId).stream()
                .map(DocumentRequirementResponse::of).toList();
    }

    @PutMapping
    @PreAuthorize("@ss.hasPermi('nad:program:edit')")
    @Log(title = "Programme document requirements", businessType = BusinessType.UPDATE)
    public List<DocumentRequirementResponse> replace(@PathVariable long programId,
            @Valid @RequestBody DocumentRequirementsRequest req) {
        return service.replace(RequirementScope.PROGRAM, programId, req.requirements()).stream()
                .map(DocumentRequirementResponse::of).toList();
    }
}
