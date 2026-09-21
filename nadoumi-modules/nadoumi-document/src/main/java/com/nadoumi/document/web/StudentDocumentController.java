package com.nadoumi.document.web;

import com.nadoumi.document.domain.Document;
import com.nadoumi.document.service.DocumentContent;
import com.nadoumi.document.service.DocumentService;
import com.nadoumi.document.web.response.DocumentVersionResponse;
import com.nadoumi.document.web.response.StudentDocumentResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

/**
 * A student's own documents: own applicant only, enforced by {@link DocumentService}
 * re-checking the applicant-access grant on every call (never trust the path id
 * alone) — the {@code applicantId} query/form parameter names whose documents,
 * ownership of the resulting document row is what is actually authorized.
 */
@RestController
@RequestMapping("/api/student/documents")
public class StudentDocumentController {

    private final DocumentService service;

    public StudentDocumentController(DocumentService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("@na.canAccessApplicant(#applicantId, 'VIEW_DOCUMENT')")
    public List<StudentDocumentResponse> mine(@RequestParam long applicantId,
            @RequestParam(required = false) Long applicationId) {
        return service.listMine(applicantId, applicationId).stream().map(this::toResponse).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@na.canAccessApplicant(#applicantId, 'UPLOAD_DOCUMENT')")
    public StudentDocumentResponse create(@RequestParam long applicantId, @RequestParam String docType,
            @RequestParam(required = false) Long applicationId, @RequestParam("file") MultipartFile file) {
        return toResponse(service.createAndUploadFirstVersion(applicantId, applicationId, docType, file));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@documentAccessGuard.ownsDocument(#id, 'VIEW_DOCUMENT')")
    public StudentDocumentResponse get(@PathVariable long id) {
        return toResponse(service.getMine(id));
    }

    @PostMapping("/{id}/versions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@documentAccessGuard.ownsDocument(#id, 'UPLOAD_DOCUMENT')")
    public StudentDocumentResponse replace(@PathVariable long id, @RequestParam("file") MultipartFile file) {
        return toResponse(service.uploadNewVersion(id, file));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@documentAccessGuard.ownsDocument(#id, 'EDIT_PROFILE')")
    public void delete(@PathVariable long id) {
        service.deleteDraft(id);
    }

    @GetMapping("/{id}/versions/{versionNo}/content")
    @PreAuthorize("@documentAccessGuard.ownsDocument(#id, 'VIEW_DOCUMENT')")
    public ResponseEntity<?> content(@PathVariable long id, @PathVariable long versionNo,
            @RequestParam(required = false) String json, HttpServletRequest request) {
        DocumentContent content = service.studentContent(id, versionNo, DocumentContentResponses.accessContext(request, id));
        return DocumentContentResponses.respond(content, json, request);
    }

    private StudentDocumentResponse toResponse(Document doc) {
        DocumentVersionResponse current = doc.getCurrentVersionId() == null ? null
                : service.versionsOf(doc.getId()).stream()
                        .filter(v -> v.getId().equals(doc.getCurrentVersionId()))
                        .findFirst()
                        .map(DocumentVersionResponse::from)
                        .orElse(null);
        return StudentDocumentResponse.from(doc, current);
    }
}
