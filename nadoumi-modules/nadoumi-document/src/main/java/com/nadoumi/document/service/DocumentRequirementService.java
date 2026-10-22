package com.nadoumi.document.service;

import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.document.domain.DocumentRequirement;
import com.nadoumi.document.domain.enums.RequirementScope;
import com.nadoumi.document.mapper.DocumentRequirementMapper;
import com.nadoumi.document.web.request.DocumentRequirementsRequest;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The document-type checklist a programme (or scholarship, or workflow stage) asks for. Resolving it for one
 * application, and matching it against the student's documents, is the application module's job; this service
 * only owns the definitions.
 */
@Service
public class DocumentRequirementService {

    private final DocumentRequirementMapper mapper;

    public DocumentRequirementService(DocumentRequirementMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<DocumentRequirement> checklistFor(RequirementScope scope, long refId) {
        return mapper.findByScope(scope, refId);
    }

    /** Replaces the whole checklist of {@code scope}/{@code refId}; a doc type can appear only once. */
    @Transactional(rollbackFor = Exception.class)
    public List<DocumentRequirement> replace(RequirementScope scope, long refId,
            List<DocumentRequirementsRequest.Item> items) {
        Set<String> seen = new HashSet<>();
        for (DocumentRequirementsRequest.Item item : items) {
            if (!seen.add(item.docType())) {
                throw new NadBadRequestException("document type " + item.docType() + " is listed twice");
            }
        }
        mapper.deleteByScope(scope, refId);
        for (DocumentRequirementsRequest.Item item : items) {
            DocumentRequirement requirement = new DocumentRequirement();
            requirement.setScope(scope);
            requirement.setRefId(refId);
            requirement.setDocType(item.docType());
            requirement.setMandatory(item.mandatory());
            requirement.setAppliesWhen(item.appliesWhen() == null || item.appliesWhen().isBlank() ? null : item.appliesWhen());
            requirement.setNotes(item.notes() == null || item.notes().isBlank() ? null : item.notes().strip());
            requirement.setCreateBy("");
            mapper.insert(requirement);
        }
        return mapper.findByScope(scope, refId);
    }
}
