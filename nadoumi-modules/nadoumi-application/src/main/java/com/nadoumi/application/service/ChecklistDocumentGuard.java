package com.nadoumi.application.service;

import com.nadoumi.application.service.ApplicationChecklistService.Item;
import com.nadoumi.application.service.ApplicationChecklistService.ItemStatus;
import org.springframework.stereotype.Component;

/** Backs the document predicates from the application's own checklist: the same list the student sees. */
@Component
public class ChecklistDocumentGuard implements DocumentGuardProvider {

    private final ApplicationChecklistService checklist;

    public ChecklistDocumentGuard(ApplicationChecklistService checklist) {
        this.checklist = checklist;
    }

    @Override
    public boolean allRequiredDocumentsAttached(long applicationId) {
        return checklist.missingMandatory(applicationId).isEmpty();
    }

    @Override
    public boolean allRequiredDocumentsVerified(long applicationId) {
        return checklist.checklist(applicationId).stream()
                .filter(Item::mandatory)
                .allMatch(i -> i.status() == ItemStatus.VERIFIED);
    }
}
