package com.nadoumi.document.web;

import com.nadoumi.common.exception.NadNotFoundException;
import com.nadoumi.document.domain.DocumentTemplate;
import com.nadoumi.document.domain.enums.RequirementScope;
import com.nadoumi.document.service.DocumentTemplateService;
import com.nadoumi.document.web.response.DocumentTemplateResponse;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.SecurityUtils;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Form templates attached to a programme or a scholarship, managed from their admin drawers. */
@RestController
@RequestMapping("/api/staff/document-templates")
public class StaffDocumentTemplateController {

    private final DocumentTemplateService service;

    public StaffDocumentTemplateController(DocumentTemplateService service) {
        this.service = service;
    }

    @GetMapping("/programs/{programId}")
    @PreAuthorize("@ss.hasPermi('nad:program:view')")
    public List<DocumentTemplateResponse> programs(@PathVariable long programId) {
        return list(RequirementScope.PROGRAM, programId);
    }

    @GetMapping("/scholarships/{scholarshipId}")
    @PreAuthorize("@ss.hasPermi('nad:scholarship:view')")
    public List<DocumentTemplateResponse> scholarships(@PathVariable long scholarshipId) {
        return list(RequirementScope.SCHOLARSHIP, scholarshipId);
    }

    @PostMapping(value = "/programs/{programId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@ss.hasPermi('nad:program:edit')")
    @Log(title = "Programme template", businessType = BusinessType.INSERT)
    public DocumentTemplateResponse addToProgram(@PathVariable long programId, @RequestParam String title,
            @RequestParam("file") MultipartFile file) {
        return add(RequirementScope.PROGRAM, programId, title, file);
    }

    @PostMapping(value = "/scholarships/{scholarshipId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@ss.hasPermi('nad:scholarship:edit')")
    @Log(title = "Scholarship template", businessType = BusinessType.INSERT)
    public DocumentTemplateResponse addToScholarship(@PathVariable long scholarshipId, @RequestParam String title,
            @RequestParam("file") MultipartFile file) {
        return add(RequirementScope.SCHOLARSHIP, scholarshipId, title, file);
    }

    /** Removes a template; the caller needs the edit permission of the opportunity the template belongs to. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@ss.hasAnyPermi('nad:program:edit,nad:scholarship:edit')")
    @Log(title = "Document template", businessType = BusinessType.DELETE)
    public void remove(@PathVariable long id) {
        DocumentTemplate template = service.require(id);
        String permission = template.getScope() == RequirementScope.PROGRAM ? "nad:program:edit" : "nad:scholarship:edit";
        if (!SecurityUtils.hasPermi(permission)) {
            throw new NadNotFoundException("template not found"); // do not reveal what another team's drawer holds
        }
        service.remove(id, SecurityUtils.getUserId());
    }

    private List<DocumentTemplateResponse> list(RequirementScope scope, long refId) {
        return service.list(scope, refId).stream().map(DocumentTemplateResponse::of).toList();
    }

    private DocumentTemplateResponse add(RequirementScope scope, long refId, String title, MultipartFile file) {
        return DocumentTemplateResponse.of(service.add(scope, refId, title, file, SecurityUtils.getUserId()));
    }
}
