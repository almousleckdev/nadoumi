package com.nadoumi.document.service;

import com.nadoumi.common.erasure.ApplicantErasureParticipant;
import com.nadoumi.common.media.MediaGateway;
import com.nadoumi.document.mapper.DocumentErasureMapper;
import com.nadoumi.identity.access.CurrentCaller;
import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Removes an applicant's documents, their version and event history, and releases the stored files. */
@Component
@Order(10)
class DocumentErasure implements ApplicantErasureParticipant {

    private final DocumentErasureMapper mapper;
    private final MediaGateway media;
    private final CurrentCaller caller;

    DocumentErasure(DocumentErasureMapper mapper, MediaGateway media, CurrentCaller caller) {
        this.mapper = mapper;
        this.media = media;
        this.caller = caller;
    }

    @Override
    public void erase(long applicantId) {
        List<Long> assets = mapper.mediaAssetIds(applicantId);
        mapper.removeEvents(applicantId);
        mapper.removeVersions(applicantId);
        mapper.removeDocuments(applicantId);
        long actor = caller.requireUserId();
        assets.forEach(id -> media.softDelete(id, actor));
    }
}
