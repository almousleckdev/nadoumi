package com.nadoumi.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.common.access.NadoumiAccessService;
import com.nadoumi.common.exception.NadForbiddenException;
import com.nadoumi.common.media.MediaAccessClass;
import com.nadoumi.common.media.MediaAccessLogContext;
import com.nadoumi.common.media.MediaGateway;
import com.nadoumi.common.media.MediaUploadResult;
import com.nadoumi.common.media.ProxyStream;
import com.nadoumi.common.media.SignedUrl;
import com.nadoumi.common.media.StoredAsset;
import com.nadoumi.document.domain.Document;
import com.nadoumi.document.domain.DocumentVersion;
import com.nadoumi.document.domain.enums.DocumentStatus;
import com.nadoumi.document.domain.enums.ScanStatus;
import com.nadoumi.document.domain.enums.VerificationStatus;
import com.nadoumi.document.mapper.DocumentEventMapper;
import com.nadoumi.document.mapper.DocumentMapper;
import com.nadoumi.document.mapper.DocumentVersionMapper;
import com.nadoumi.identity.access.CurrentCaller;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

class DocumentServiceTest {

    private static final long APPLICANT_ID = 42L;
    private static final long DOCUMENT_ID = 100L;

    private final DocumentMapper documents = mock(DocumentMapper.class);
    private final DocumentVersionMapper versions = mock(DocumentVersionMapper.class);
    private final DocumentEventMapper events = mock(DocumentEventMapper.class);
    private final NadoumiAccessService access = mock(NadoumiAccessService.class);
    private final MediaGateway media = mock(MediaGateway.class);
    private final CurrentCaller caller = mock(CurrentCaller.class);
    private final DocumentAccessGuard guard = new DocumentAccessGuard(access, documents);
    private final DocumentService service = new DocumentService(documents, versions, events, guard, media, caller);

    @BeforeEach
    void setUp() {
        when(caller.requireUserId()).thenReturn(1L);
    }

    private static MockMultipartFile file() {
        return new MockMultipartFile("file", "transcript.pdf", "application/pdf", new byte[] { 1, 2, 3 });
    }

    private static Document draftDocument() {
        Document doc = new Document();
        doc.setId(DOCUMENT_ID);
        doc.setApplicantId(APPLICANT_ID);
        doc.setDocType("TRANSCRIPT");
        doc.setStatus(DocumentStatus.DRAFT);
        return doc;
    }

    private static StoredAsset storedAsset(long id, MediaAccessClass accessClass) {
        return new StoredAsset(id, "CLOUDINARY", accessClass, com.nadoumi.common.media.MediaCategory.APPLICANT_DOCUMENT,
                "raw", "authenticated", "pub-" + id, null, null, null, "transcript.pdf", "application/pdf", 1024,
                null, null, "abc123", 1L, null, "ACTIVE", Instant.now());
    }

    // ---- create + upload ----

    @Test
    void shouldCreateDraftThenSubmitFirstVersion_whenCreating() {
        when(access.canAccessApplicant(APPLICANT_ID, "UPLOAD_DOCUMENT")).thenReturn(true);
        when(versions.maxVersionNo(DOCUMENT_ID)).thenReturn(0);
        when(media.upload(any(), any(), any(), anyLong(), any(), any(), any(), anyLong()))
                .thenReturn(new MediaUploadResult(9L, null));
        when(media.find(9L)).thenReturn(java.util.Optional.of(storedAsset(9L, MediaAccessClass.PROTECTED)));

        // insert() is void-return with a MyBatis-style generated-key side effect; simulate it setting the id.
        org.mockito.Mockito.doAnswer(inv -> {
            Document d = inv.getArgument(0);
            d.setId(DOCUMENT_ID);
            return 1;
        }).when(documents).insert(any());
        org.mockito.Mockito.doAnswer(inv -> {
            DocumentVersion v = inv.getArgument(0);
            v.setId(55L);
            return 1;
        }).when(versions).insert(any());

        Document result = service.createAndUploadFirstVersion(APPLICANT_ID, null, "TRANSCRIPT", file());

        assertThat(result.getStatus()).isEqualTo(DocumentStatus.SUBMITTED);
        ArgumentCaptor<DocumentStatus> statusCaptor = ArgumentCaptor.forClass(DocumentStatus.class);
        verify(documents).updateCurrentVersion(eq(DOCUMENT_ID), anyLong(), statusCaptor.capture(), any());
        assertThat(statusCaptor.getValue()).isEqualTo(DocumentStatus.SUBMITTED);
        verify(events, times(2)).insert(any()); // CREATED + SUBMITTED
    }

    @Test
    void shouldRefuseCreate_whenCallerLacksUploadCapability() {
        when(access.canAccessApplicant(APPLICANT_ID, "UPLOAD_DOCUMENT")).thenReturn(false);

        assertThatThrownBy(() -> service.createAndUploadFirstVersion(APPLICANT_ID, null, "TRANSCRIPT", file()))
                .isInstanceOf(AccessDeniedException.class);
        verify(documents, never()).insert(any());
        verify(media, never()).upload(any(), any(), any(), anyLong(), any(), any(), any(), anyLong());
    }

    // ---- replace: prior versions stay immutable, status moves to IN_REVIEW ----

    @Test
    void shouldMoveToInReview_andKeepPriorVersionImmutable_whenReplacing() {
        Document existing = draftDocument();
        existing.setStatus(DocumentStatus.REJECTED);
        when(documents.findById(DOCUMENT_ID)).thenReturn(existing);
        when(access.canAccessApplicant(APPLICANT_ID, "UPLOAD_DOCUMENT")).thenReturn(true);
        when(versions.maxVersionNo(DOCUMENT_ID)).thenReturn(1); // one prior version already exists
        when(media.upload(any(), any(), any(), anyLong(), any(), any(), any(), anyLong()))
                .thenReturn(new MediaUploadResult(10L, null));
        when(media.find(10L)).thenReturn(java.util.Optional.of(storedAsset(10L, MediaAccessClass.PROTECTED)));
        org.mockito.Mockito.doAnswer(inv -> {
            DocumentVersion v = inv.getArgument(0);
            v.setId(56L);
            return 1;
        }).when(versions).insert(any());

        Document result = service.uploadNewVersion(DOCUMENT_ID, file());

        assertThat(result.getStatus()).isEqualTo(DocumentStatus.IN_REVIEW);
        ArgumentCaptor<DocumentVersion> versionCaptor = ArgumentCaptor.forClass(DocumentVersion.class);
        verify(versions).insert(versionCaptor.capture());
        assertThat(versionCaptor.getValue().getVersionNo()).isEqualTo(2);
        // the prior version row is never touched by this flow — only a new insert happens
        verify(versions, never()).updateVerification(eq(1L), any(), anyLong());
    }

    // ---- verify / reject ----

    @Test
    void shouldVerify_whenCallerHoldsReviewPermission() {
        Document doc = draftDocument();
        doc.setCurrentVersionId(5L);
        doc.setStatus(DocumentStatus.SUBMITTED);
        when(documents.findById(DOCUMENT_ID)).thenReturn(doc);
        when(access.canReviewDocument(DOCUMENT_ID)).thenReturn(true);
        DocumentVersion current = new DocumentVersion();
        current.setId(5L);
        when(versions.findById(5L)).thenReturn(current);

        service.verify(DOCUMENT_ID);

        verify(versions).updateVerification(5L, VerificationStatus.VERIFIED, 1L);
        verify(documents).updateStatus(DOCUMENT_ID, DocumentStatus.VERIFIED, 1L, null, "1");
    }

    @Test
    void shouldMarkExpired_notVerified_whenVerifyingAPastExpiryDocument() {
        Document doc = draftDocument();
        doc.setCurrentVersionId(5L);
        doc.setExpiresOn(LocalDate.now().minusDays(1));
        when(documents.findById(DOCUMENT_ID)).thenReturn(doc);
        when(access.canReviewDocument(DOCUMENT_ID)).thenReturn(true);
        DocumentVersion current = new DocumentVersion();
        current.setId(5L);
        when(versions.findById(5L)).thenReturn(current);

        service.verify(DOCUMENT_ID);

        verify(documents).updateStatus(DOCUMENT_ID, DocumentStatus.EXPIRED, 1L, null, "1");
    }

    @Test
    void shouldReject_withReason() {
        Document doc = draftDocument();
        doc.setCurrentVersionId(5L);
        when(documents.findById(DOCUMENT_ID)).thenReturn(doc);
        when(access.canReviewDocument(DOCUMENT_ID)).thenReturn(true);
        DocumentVersion current = new DocumentVersion();
        current.setId(5L);
        when(versions.findById(5L)).thenReturn(current);

        service.reject(DOCUMENT_ID, "blurry scan");

        verify(versions).updateVerification(5L, VerificationStatus.REJECTED, 1L);
        verify(documents).updateStatus(DOCUMENT_ID, DocumentStatus.REJECTED, 1L, "blurry scan", "1");
    }

    @Test
    void shouldForbidReview_whenCallerLacksReviewPermission() {
        when(documents.findById(DOCUMENT_ID)).thenReturn(draftDocument());
        when(access.canReviewDocument(DOCUMENT_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.verify(DOCUMENT_ID)).isInstanceOf(NadForbiddenException.class);
        verify(versions, never()).updateVerification(anyLong(), any(), anyLong());
    }

    // ---- required authorization test: cross-applicant fetch is denied ----

    @Test
    void shouldForbidFetch_whenCallerHasNoGrantOnTheOwningApplicant() {
        when(documents.findById(DOCUMENT_ID)).thenReturn(draftDocument());
        when(access.canAccessApplicant(APPLICANT_ID, "VIEW_DOCUMENT")).thenReturn(false);

        assertThatThrownBy(() -> service.getMine(DOCUMENT_ID)).isInstanceOf(AccessDeniedException.class);
    }

    // ---- delete draft only ----

    @Test
    void shouldRefuseDelete_whenDocumentIsNotADraft() {
        Document submitted = draftDocument();
        submitted.setStatus(DocumentStatus.SUBMITTED);
        when(documents.findById(DOCUMENT_ID)).thenReturn(submitted);
        when(access.canAccessApplicant(APPLICANT_ID, "EDIT_PROFILE")).thenReturn(true);

        assertThatThrownBy(() -> service.deleteDraft(DOCUMENT_ID)).isInstanceOf(NadForbiddenException.class);
        verify(documents, never()).deleteDraft(anyLong());
    }

    @Test
    void shouldDelete_whenDocumentIsStillADraft() {
        when(documents.findById(DOCUMENT_ID)).thenReturn(draftDocument());
        when(access.canAccessApplicant(APPLICANT_ID, "EDIT_PROFILE")).thenReturn(true);

        service.deleteDraft(DOCUMENT_ID);

        verify(documents).deleteDraft(DOCUMENT_ID);
    }

    // ---- content delivery: scan gate + access-class dispatch ----

    @Test
    void shouldBlockDownload_andLogADenial_whenScanIsNotClean() {
        when(documents.findById(DOCUMENT_ID)).thenReturn(draftDocument());
        when(access.canAccessApplicant(APPLICANT_ID, "VIEW_DOCUMENT")).thenReturn(true);
        DocumentVersion pending = new DocumentVersion();
        pending.setVersionNo(1);
        pending.setMediaAssetId(9L);
        pending.setScanStatus(ScanStatus.PENDING);
        when(versions.findByDocument(DOCUMENT_ID)).thenReturn(List.of(pending));
        MediaAccessLogContext ctx = new MediaAccessLogContext(1L, APPLICANT_ID, null, DOCUMENT_ID, "10.0.0.1", "junit");

        assertThatThrownBy(() -> service.studentContent(DOCUMENT_ID, 1, ctx)).isInstanceOf(NadForbiddenException.class);
        verify(media).denyAndLog(9L, ctx, "SCAN_NOT_CLEAN");
        verify(media, never()).issueSignedUrl(anyLong(), any());
    }

    @Test
    void shouldProxyStream_forASensitiveAsset() {
        when(documents.findById(DOCUMENT_ID)).thenReturn(draftDocument());
        when(access.canAccessApplicant(APPLICANT_ID, "VIEW_DOCUMENT")).thenReturn(true);
        DocumentVersion clean = new DocumentVersion();
        clean.setVersionNo(1);
        clean.setMediaAssetId(9L);
        clean.setScanStatus(ScanStatus.CLEAN);
        when(versions.findByDocument(DOCUMENT_ID)).thenReturn(List.of(clean));
        when(media.find(9L)).thenReturn(java.util.Optional.of(storedAsset(9L, MediaAccessClass.SENSITIVE)));
        ProxyStream stream = new ProxyStream(new ByteArrayInputStream(new byte[0]), "application/pdf", 0, "f.pdf");
        MediaAccessLogContext ctx = new MediaAccessLogContext(1L, APPLICANT_ID, null, DOCUMENT_ID, "10.0.0.1", "junit");
        when(media.openProxyStream(9L, ctx)).thenReturn(stream);

        DocumentContent content = service.studentContent(DOCUMENT_ID, 1, ctx);

        assertThat(content).isInstanceOf(DocumentContent.Proxy.class);
        verify(media, never()).issueSignedUrl(anyLong(), any());
    }

    @Test
    void shouldRedirect_forAProtectedAsset() {
        when(documents.findById(DOCUMENT_ID)).thenReturn(draftDocument());
        when(access.canAccessApplicant(APPLICANT_ID, "VIEW_DOCUMENT")).thenReturn(true);
        DocumentVersion clean = new DocumentVersion();
        clean.setVersionNo(1);
        clean.setMediaAssetId(9L);
        clean.setScanStatus(ScanStatus.CLEAN);
        when(versions.findByDocument(DOCUMENT_ID)).thenReturn(List.of(clean));
        when(media.find(9L)).thenReturn(java.util.Optional.of(storedAsset(9L, MediaAccessClass.PROTECTED)));
        MediaAccessLogContext ctx = new MediaAccessLogContext(1L, APPLICANT_ID, null, DOCUMENT_ID, "10.0.0.1", "junit");
        SignedUrl signed = new SignedUrl("https://x/y.pdf", Instant.now().plusSeconds(60));
        when(media.issueSignedUrl(9L, ctx)).thenReturn(signed);

        DocumentContent content = service.studentContent(DOCUMENT_ID, 1, ctx);

        assertThat(content).isInstanceOf(DocumentContent.Redirect.class);
        assertThat(((DocumentContent.Redirect) content).url()).isSameAs(signed);
    }
}
