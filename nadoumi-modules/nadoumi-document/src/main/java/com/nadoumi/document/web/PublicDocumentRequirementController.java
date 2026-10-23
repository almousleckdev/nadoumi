package com.nadoumi.document.web;

import com.nadoumi.document.domain.enums.RequirementScope;
import com.nadoumi.document.service.DocumentRequirementService;
import com.nadoumi.document.web.response.DocumentRequirementResponse;
import com.ruoyi.common.annotation.Anonymous;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The documents a programme asks applicants to upload, for the public programme page. These are the same rows that
 * drive the applicant's checklist, so what visitors read cannot differ from what students are asked for. Nothing
 * here is confidential: a type, whether it is mandatory, an optional note and the condition it applies under.
 */
@RestController
@RequestMapping("/api/public/document-requirements")
public class PublicDocumentRequirementController {

    private final DocumentRequirementService service;

    public PublicDocumentRequirementController(DocumentRequirementService service) {
        this.service = service;
    }

    @Anonymous
    @GetMapping("/programs/{programId}")
    public List<DocumentRequirementResponse> program(@PathVariable long programId) {
        return service.checklistFor(RequirementScope.PROGRAM, programId).stream()
                .map(DocumentRequirementResponse::of).toList();
    }
}
