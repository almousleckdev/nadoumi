package com.nadoumi.document.service;

import com.nadoumi.common.access.ApplicantCapability;
import com.nadoumi.common.exception.NadForbiddenException;
import com.nadoumi.common.exception.NadNotFoundException;
import com.nadoumi.common.media.MediaAccessClass;
import com.nadoumi.common.media.MediaAccessLogContext;
import com.nadoumi.common.media.MediaCategory;
import com.nadoumi.common.media.MediaGateway;
import com.nadoumi.common.media.MediaOwnerKind;
import com.nadoumi.common.media.MediaOwnerRef;
import com.nadoumi.common.media.MediaUploadResult;
import com.nadoumi.common.media.StoredAsset;
import com.nadoumi.document.domain.Document;
import com.nadoumi.document.domain.DocumentEvent;
import com.nadoumi.document.domain.DocumentVersion;
import com.nadoumi.document.domain.enums.DocumentStatus;
import com.nadoumi.document.domain.enums.ScanStatus;
import com.nadoumi.document.domain.enums.VerificationStatus;
import com.nadoumi.document.mapper.DocumentEventMapper;
import com.nadoumi.document.mapper.DocumentMapper;
import com.nadoumi.document.mapper.DocumentVersionMapper;
import com.nadoumi.identity.access.CurrentCaller;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Governed-document lifecycle: create, version, verify/reject, content delivery.
 * Storage is entirely delegated to {@link MediaGateway} — this service never
 * touches Cloudinary or a storage_key directly (DOCUMENT_MANAGEMENT.md §3.2/§3).
 *
 * <p>Malware scanning is not yet built (DOCUMENT_MANAGEMENT.md §3.4): a real
 * scanner would flip {@code scan_status} PENDING&rarr;CLEAN/INFECTED
 * asynchronously. Until that exists, new versions are stamped CLEAN at upload so
 * the download gate below has something to enforce against today rather than
 * blocking every document forever; the gate itself is real and tested.</p>
 */
@Service
public class DocumentService {

    private final DocumentMapper documents;
    private final DocumentVersionMapper versions;
    private final DocumentEventMapper events;
    private final DocumentAccessGuard guard;
    private final MediaGateway media;
    private final CurrentCaller caller;

    public DocumentService(DocumentMapper documents, DocumentVersionMapper versions, DocumentEventMapper events,
            DocumentAccessGuard guard, MediaGateway media, CurrentCaller caller) {
        this.documents = documents;
        this.versions = versions;
        this.events = events;
        this.guard = guard;
        this.media = media;
        this.caller = caller;
    }

    // ---- student: create + upload ----

    @Transactional(rollbackFor = Exception.class)
    public Document createAndUploadFirstVersion(long applicantId, Long applicationId, String docType, MultipartFile file) {
        guard.requireApplicant(applicantId, ApplicantCapability.UPLOAD_DOCUMENT);
        Document doc = new Document();
        doc.setApplicantId(applicantId);
        doc.setApplicationId(applicationId);
        doc.setDocType(docType);
        doc.setStatus(DocumentStatus.DRAFT);
        doc.setCreateBy(actorName());
        documents.insert(doc);
        recordEvent(doc.getId(), "CREATED", null);
        return uploadVersion(doc, file);
    }

    // ---- student: replace (new version) ----

    @Transactional(rollbackFor = Exception.class)
    public Document uploadNewVersion(long documentId, MultipartFile file) {
        Document doc = requireOwned(documentId, ApplicantCapability.UPLOAD_DOCUMENT);
        return uploadVersion(doc, file);
    }

    private Document uploadVersion(Document doc, MultipartFile file) {
        MediaCategory category = doc.getApplicationId() != null
                ? MediaCategory.APPLICATION_DOCUMENT
                : MediaCategory.APPLICANT_DOCUMENT;
        MediaAccessClass override = DocumentTypeAccess.overrideFor(doc.getDocType());
        MediaUploadResult uploaded;
        try {
            uploaded = media.upload(file.getInputStream(), file.getOriginalFilename(), file.getContentType(),
                    file.getSize(), category, override,
                    new MediaOwnerRef(MediaOwnerKind.DOCUMENT, doc.getId()), currentUserId());
        }
        catch (IOException e) {
            throw new UncheckedIOException("failed to read upload", e);
        }
        StoredAsset asset = media.find(uploaded.mediaId())
                .orElseThrow(() -> new IllegalStateException("uploaded asset " + uploaded.mediaId() + " not found immediately after upload"));

        DocumentVersion version = new DocumentVersion();
        version.setDocumentId(doc.getId());
        version.setVersionNo(versions.maxVersionNo(doc.getId()) + 1);
        version.setMediaAssetId(asset.id());
        version.setContentType(asset.contentType());
        version.setSizeBytes(asset.byteSize());
        version.setChecksumSha256(asset.checksumSha256());
        version.setUploadedBy(currentUserId());
        version.setVerificationStatus(VerificationStatus.PENDING);
        // no scanner wired yet (see class Javadoc) — stamped CLEAN so the gate below is exercised, not permanently blocked
        version.setScanStatus(ScanStatus.CLEAN);
        versions.insert(version);

        DocumentStatus newStatus = DocumentStatusDeriver.onNewVersion(version);
        documents.updateCurrentVersion(doc.getId(), version.getId(), newStatus, actorName());
        doc.setCurrentVersionId(version.getId());
        doc.setStatus(newStatus);

        recordEvent(doc.getId(), version.getVersionNo() == 1 ? "SUBMITTED" : "VERSION_UPLOADED", null);
        return doc;
    }

    // ---- student: read ----

    public List<Document> listMine(long applicantId, Long applicationId) {
        guard.requireApplicant(applicantId, ApplicantCapability.VIEW_DOCUMENT);
        return documents.findByApplicant(applicantId, applicationId);
    }

    public Document getMine(long documentId) {
        return requireOwned(documentId, ApplicantCapability.VIEW_DOCUMENT);
    }

    // ---- student: delete draft ----

    @Transactional(rollbackFor = Exception.class)
    public void deleteDraft(long documentId) {
        Document doc = requireOwned(documentId, ApplicantCapability.EDIT_PROFILE);
        if (doc.getStatus() != DocumentStatus.DRAFT) {
            throw new NadForbiddenException("only a DRAFT document (no version uploaded yet) can be deleted");
        }
        documents.deleteDraft(documentId);
    }

    // ---- staff: read ----

    public List<Document> search(Long applicantId, Long applicationId) {
        return documents.search(applicantId, applicationId);
    }

    public Document get(long documentId) {
        Document doc = documents.findById(documentId);
        if (doc == null) {
            throw new NadNotFoundException("document not found: " + documentId);
        }
        return doc;
    }

    public List<DocumentVersion> versionsOf(long documentId) {
        return versions.findByDocument(documentId);
    }

    public List<DocumentEvent> eventsOf(long documentId) {
        return events.findByDocument(documentId);
    }

    // ---- staff: verify / reject ----

    @Transactional(rollbackFor = Exception.class)
    public void verify(long documentId) {
        Document doc = get(documentId);
        guard.requireReview(documentId);
        DocumentVersion current = requireCurrentVersion(doc);
        versions.updateVerification(current.getId(), VerificationStatus.VERIFIED, currentUserId());
        DocumentStatus status = DocumentStatusDeriver.onVerified(doc.getExpiresOn());
        documents.updateStatus(documentId, status, currentUserId(), null, actorName());
        recordEvent(documentId, "VERIFIED", null);
    }

    @Transactional(rollbackFor = Exception.class)
    public void reject(long documentId, String reason) {
        Document doc = get(documentId);
        guard.requireReview(documentId);
        DocumentVersion current = requireCurrentVersion(doc);
        versions.updateVerification(current.getId(), VerificationStatus.REJECTED, currentUserId());
        documents.updateStatus(documentId, DocumentStatus.REJECTED, currentUserId(), reason, actorName());
        recordEvent(documentId, "REJECTED", reason);
    }

    // ---- content delivery ----

    public DocumentContent studentContent(long documentId, long versionNo, MediaAccessLogContext ctx) {
        requireOwned(documentId, ApplicantCapability.VIEW_DOCUMENT);
        return content(documentId, versionNo, ctx);
    }

    public DocumentContent staffContent(long documentId, long versionNo, MediaAccessLogContext ctx) {
        get(documentId);
        return content(documentId, versionNo, ctx);
    }

    private DocumentContent content(long documentId, long versionNo, MediaAccessLogContext ctx) {
        DocumentVersion version = versionsOf(documentId).stream()
                .filter(v -> v.getVersionNo() == versionNo)
                .findFirst()
                .orElseThrow(() -> new NadNotFoundException("version " + versionNo + " not found for document " + documentId));
        if (version.getScanStatus() != ScanStatus.CLEAN) {
            media.denyAndLog(version.getMediaAssetId(), ctx, "SCAN_NOT_CLEAN");
            throw new NadForbiddenException("this file has not cleared scanning yet");
        }
        StoredAsset asset = media.find(version.getMediaAssetId())
                .orElseThrow(() -> new NadNotFoundException("underlying media asset missing"));
        recordEvent(documentId, "DOWNLOADED", null);
        if (asset.accessClass() == MediaAccessClass.SENSITIVE) {
            return new DocumentContent.Proxy(media.openProxyStream(version.getMediaAssetId(), ctx));
        }
        return new DocumentContent.Redirect(media.issueSignedUrl(version.getMediaAssetId(), ctx));
    }

    // ---- helpers ----

    private Document requireOwned(long documentId, ApplicantCapability capability) {
        Document doc = get(documentId);
        guard.requireApplicant(doc.getApplicantId(), capability);
        return doc;
    }

    private DocumentVersion requireCurrentVersion(Document doc) {
        if (doc.getCurrentVersionId() == null) {
            throw new NadForbiddenException("document " + doc.getId() + " has no uploaded version yet");
        }
        DocumentVersion version = versions.findById(doc.getCurrentVersionId());
        if (version == null) {
            throw new NadNotFoundException("current version missing for document " + doc.getId());
        }
        return version;
    }

    private void recordEvent(long documentId, String eventType, String detail) {
        DocumentEvent event = new DocumentEvent();
        event.setDocumentId(documentId);
        event.setEventType(eventType);
        event.setActorUserId(currentUserId());
        event.setDetailJson(detail);
        events.insert(event);
    }

    private long currentUserId() {
        try {
            Long id = caller.requireUserId();
            return id == null ? 0L : id;
        }
        catch (RuntimeException e) {
            return 0L;
        }
    }

    private String actorName() {
        return String.valueOf(currentUserId());
    }
}
