package com.nadoumi.application.service;

import com.nadoumi.application.domain.Application;
import com.nadoumi.application.mapper.ApplicationMapper;
import com.nadoumi.common.exception.NadNotFoundException;
import com.nadoumi.document.domain.DocumentTemplate;
import com.nadoumi.document.domain.enums.RequirementScope;
import com.nadoumi.document.service.DocumentTemplateService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The templates an application can download: those of its own programme and its own scholarship, nothing else. A
 * template id is never trusted on its own; it must belong to one of the application's two opportunities.
 */
@Service
public class ApplicationTemplateService {

    private final ApplicationMapper applications;
    private final DocumentTemplateService templates;

    public ApplicationTemplateService(ApplicationMapper applications, DocumentTemplateService templates) {
        this.applications = applications;
        this.templates = templates;
    }

    @Transactional(readOnly = true)
    public List<DocumentTemplate> forApplication(long applicationId) {
        Application application = load(applicationId);
        List<DocumentTemplate> result = new ArrayList<>();
        if (application.getProgramId() != null) {
            result.addAll(templates.list(RequirementScope.PROGRAM, application.getProgramId()));
        }
        if (application.getScholarshipId() != null) {
            result.addAll(templates.list(RequirementScope.SCHOLARSHIP, application.getScholarshipId()));
        }
        return List.copyOf(result);
    }

    /** The template, or "not found" when it does not belong to this application's programme or scholarship. */
    @Transactional(readOnly = true)
    public DocumentTemplate find(long applicationId, long templateId) {
        return forApplication(applicationId).stream()
                .filter(t -> t.getId() == templateId)
                .findFirst()
                .orElseThrow(() -> new NadNotFoundException("template not found"));
    }

    private Application load(long applicationId) {
        Application application = applications.findById(applicationId);
        if (application == null) {
            throw new NadNotFoundException("application not found");
        }
        return application;
    }
}
