package com.nadoumi.document.service;

import com.nadoumi.document.domain.DocumentRequirement;
import com.nadoumi.document.domain.enums.RequirementScope;
import com.nadoumi.document.mapper.DocumentRequirementMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only resolution of the document-type checklist for a program, scholarship
 * or workflow stage. Consumed by the (not yet built) Application-domain workflow
 * engine's {@code ALL_REQUIRED_DOCUMENTS_VERIFIED}-style guard — this service only
 * resolves checklist *definitions*; a per-application checklist instance (which
 * items are attached/waived on one specific application) is Application-domain
 * state, not this module's.
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
}
