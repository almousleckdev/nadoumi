package com.nadoumi.document.web;

import com.nadoumi.document.domain.Document;
import com.nadoumi.document.service.DocumentContent;
import com.nadoumi.document.service.DocumentService;
import com.nadoumi.document.web.request.RejectDocumentRequest;
import com.nadoumi.document.web.response.DocumentEventResponse;
import com.nadoumi.document.web.response.DocumentVersionResponse;
import com.nadoumi.document.web.response.StaffDocumentResponse;
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
@RequestMapping("/api/staff/documents")
public class StaffDocumentController {

    private final DocumentService service;

    public StaffDocumentController(DocumentService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("@ss.hasPermi('nad:document:view')")
    public List<StaffDocumentResponse> list(@RequestParam(required = false) Long applicantId,
            @RequestParam(required = false) Long applicationId) {
        return service.search(applicantId, applicationId).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('nad:document:view')")
    public StaffDocumentResponse get(@PathVariable long id) {
        return toResponse(service.get(id));
    }

    @GetMapping("/{id}/versions/{versionNo}/content")
    @PreAuthorize("@ss.hasPermi('nad:document:download')")
    public ResponseEntity<?> content(@PathVariable long id, @PathVariable long versionNo,
            @RequestParam(required = false) String json, HttpServletRequest request) {
        DocumentContent content = service.staffContent(id, versionNo, DocumentContentResponses.accessContext(request, id));
        return DocumentContentResponses.respond(content, json, request);
    }

    @PostMapping("/{id}/verify")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@ss.hasPermi('nad:document:verify')")
    public void verify(@PathVariable long id) {
        service.verify(id);
    }

    @PostMapping("/{id}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@ss.hasPermi('nad:document:reject')")
    public void reject(@PathVariable long id, @Valid @RequestBody RejectDocumentRequest req) {
        service.reject(id, req.reason());
    }

    private StaffDocumentResponse toResponse(Document doc) {
        List<DocumentVersionResponse> versions = service.versionsOf(doc.getId()).stream()
                .map(DocumentVersionResponse::from).toList();
        List<DocumentEventResponse> events = service.eventsOf(doc.getId()).stream()
                .map(DocumentEventResponse::from).toList();
        return StaffDocumentResponse.from(doc, versions, events);
    }
}
