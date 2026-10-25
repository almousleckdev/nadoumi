package com.nadoumi.document.service;

import com.nadoumi.common.erasure.ApplicationDeletionParticipant;
import com.nadoumi.document.mapper.DocumentMapper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Documents belong to the applicant: deleting an application only drops their link to it. */
@Component
@Order(15)
class DocumentApplicationDeletion implements ApplicationDeletionParticipant {

    private final DocumentMapper documents;

    DocumentApplicationDeletion(DocumentMapper documents) {
        this.documents = documents;
    }

    @Override
    public void deleteFor(long applicationId) {
        documents.detachFromApplication(applicationId);
    }
}
