package com.nadoumi.communication.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.common.access.ApplicantCapability;
import com.nadoumi.common.access.NadoumiAccessService;
import com.nadoumi.common.exception.NadForbiddenException;
import com.nadoumi.common.exception.NadNotFoundException;
import com.nadoumi.common.media.MediaAccessClass;
import com.nadoumi.common.media.MediaAccessLogContext;
import com.nadoumi.common.media.MediaCategory;
import com.nadoumi.common.media.MediaGateway;
import com.nadoumi.common.media.MediaOwnerKind;
import com.nadoumi.common.media.MediaOwnerRef;
import com.nadoumi.common.media.SignedUrl;
import com.nadoumi.common.media.StoredAsset;
import com.nadoumi.communication.domain.Conversation;
import com.nadoumi.communication.domain.ConversationParticipant;
import com.nadoumi.communication.domain.Message;
import com.nadoumi.communication.domain.MessageAttachment;
import com.nadoumi.communication.domain.enums.ConversationStatus;
import com.nadoumi.communication.domain.enums.ConversationType;
import com.nadoumi.communication.domain.enums.ParticipantRole;
import com.nadoumi.communication.mapper.CommunicationUserMapper;
import com.nadoumi.communication.mapper.ConversationMapper;
import com.nadoumi.communication.mapper.ConversationParticipantMapper;
import com.nadoumi.communication.mapper.MessageAttachmentMapper;
import com.nadoumi.communication.mapper.MessageMapper;
import com.nadoumi.communication.web.request.OpenConversationRequest;
import com.nadoumi.communication.web.request.PostMessageRequest;
import com.nadoumi.identity.access.CurrentCaller;
import com.nadoumi.identity.mapper.UserApplicantAccessMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class ConversationServiceTest {

    private final ConversationMapper conversations = mock(ConversationMapper.class);
    private final ConversationParticipantMapper participants = mock(ConversationParticipantMapper.class);
    private final MessageMapper messages = mock(MessageMapper.class);
    private final MessageAttachmentMapper attachments = mock(MessageAttachmentMapper.class);
    private final CommunicationUserMapper users = mock(CommunicationUserMapper.class);
    private final NadoumiAccessService access = mock(NadoumiAccessService.class);
    private final CurrentCaller caller = mock(CurrentCaller.class);
    private final UserApplicantAccessMapper grants = mock(UserApplicantAccessMapper.class);
    private final MessagePublisher publisher = mock(MessagePublisher.class);
    private final MediaGateway media = mock(MediaGateway.class);
    private final ConversationService service = new ConversationService(conversations, participants, messages,
            attachments, users, access, caller, grants, publisher, media);

    private static Conversation conversation() {
        Conversation c = new Conversation();
        c.setId(9L);
        c.setConversationType(ConversationType.GENERAL);
        c.setStatus(ConversationStatus.OPEN);
        return c;
    }

    // ---- required security test: a caller without MESSAGE_STAFF cannot open a conversation ----

    @Test
    void open_rejectsACallerWithoutMessageStaffOnTheApplicant() {
        when(access.canAccessApplicant(5L, ApplicantCapability.MESSAGE_STAFF.name())).thenReturn(false);
        var req = new OpenConversationRequest(5L, null, "Question", "Hi there");

        assertThatThrownBy(() -> service.open(req)).isInstanceOf(NadForbiddenException.class);
    }

    @Test
    void open_succeedsAndPublishesTheFirstMessageWhenAuthorized() {
        when(access.canAccessApplicant(5L, ApplicantCapability.MESSAGE_STAFF.name())).thenReturn(true);
        when(caller.requireUserId()).thenReturn(1L);
        when(caller.isStaff()).thenReturn(false);
        when(grants.findActiveApplicantGrant(1L, 5L)).thenReturn(null);
        org.mockito.Mockito.doAnswer(inv -> {
            inv.<Conversation>getArgument(0).setId(9L);
            return 1;
        }).when(conversations).insert(any());
        Message posted = new Message();
        posted.setId(100L);
        posted.setConversationId(9L);
        posted.setSenderUserId(1L);
        posted.setBody("Hi there");
        when(publisher.publish(anyLong(), org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq("Hi there"), any())).thenReturn(posted);

        var req = new OpenConversationRequest(5L, null, "Question", "Hi there");
        service.open(req);

        verify(participants).insert(any());
        verify(publisher).publish(anyLong(), org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq("Hi there"), any());
    }

    @Test
    void openSupport_createsASupportConversationWithoutAnApplicantAndRejectsStaff() {
        when(caller.isStaff()).thenReturn(false);
        when(caller.requireUserId()).thenReturn(1L);
        org.mockito.Mockito.doAnswer(inv -> {
            inv.<Conversation>getArgument(0).setId(9L);
            return 1;
        }).when(conversations).insert(any());
        Message posted = new Message();
        posted.setId(100L);
        posted.setConversationId(9L);
        posted.setSenderUserId(1L);
        posted.setBody("Help");
        when(publisher.publish(anyLong(), org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq("Help"), any())).thenReturn(posted);

        var response = service.openSupport("Visa", "Help");

        org.mockito.ArgumentCaptor<Conversation> saved = org.mockito.ArgumentCaptor.forClass(Conversation.class);
        verify(conversations).insert(saved.capture());
        assertThat(saved.getValue().getConversationType()).isEqualTo(ConversationType.SUPPORT);
        assertThat(response.conversationId()).isEqualTo(9L);

        when(caller.isStaff()).thenReturn(true);
        assertThatThrownBy(() -> service.openSupport("Visa", "Help")).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void listSupportMessagesForStaff_refusesAGeneralConversationAndANonStaffCaller() {
        when(caller.isStaff()).thenReturn(true);
        when(conversations.findById(9L)).thenReturn(conversation()); // GENERAL

        assertThatThrownBy(() -> service.listSupportMessagesForStaff(9L, 0))
                .isInstanceOf(com.nadoumi.common.exception.NadNotFoundException.class);

        when(caller.isStaff()).thenReturn(false);
        assertThatThrownBy(() -> service.listSupportMessagesForStaff(9L, 0)).isInstanceOf(AccessDeniedException.class);
    }

    // ---- required security test: a non-participant cannot read or post to a conversation ----

    @Test
    void post_rejectsACallerWhoIsNotAnActiveParticipant() {
        when(caller.requireUserId()).thenReturn(2L);
        when(conversations.findById(9L)).thenReturn(conversation());
        when(participants.findActive(9L, 2L)).thenReturn(null);

        assertThatThrownBy(() -> service.post(9L, new PostMessageRequest("hi", null)))
                .isInstanceOf(AccessDeniedException.class);
        verify(publisher, never()).publish(anyLong(), anyLong(), anyString(), any());
    }

    @Test
    void listMessages_rejectsACallerWhoIsNotAnActiveParticipant() {
        when(caller.requireUserId()).thenReturn(2L);
        when(conversations.findById(9L)).thenReturn(conversation());
        when(participants.findActive(9L, 2L)).thenReturn(null);

        assertThatThrownBy(() -> service.listMessages(9L, 0)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void post_rejectsABlankBodyWithNoAttachments() {
        when(caller.requireUserId()).thenReturn(1L);
        when(conversations.findById(9L)).thenReturn(conversation());
        when(participants.findActive(9L, 1L)).thenReturn(new ConversationParticipant());

        assertThatThrownBy(() -> service.post(9L, new PostMessageRequest("   ", null)))
                .isInstanceOf(com.nadoumi.common.exception.NadBadRequestException.class)
                .hasMessageContaining("needs text, an attachment, or both");
        verify(publisher, never()).publish(anyLong(), anyLong(), anyString(), any());
    }

    @Test
    void post_acceptsABlankBodyWhenThereIsAtLeastOneAttachment_photoWithNoCaption() {
        when(caller.requireUserId()).thenReturn(1L);
        when(conversations.findById(9L)).thenReturn(conversation());
        when(participants.findActive(9L, 1L)).thenReturn(new ConversationParticipant());
        StoredAsset asset = new StoredAsset(7L, "LOCAL", MediaAccessClass.PROTECTED, MediaCategory.MESSAGE_ATTACHMENT,
                "raw", "upload", "pub-id", "asset-id", null, null,
                "photo.jpg", "image/jpeg", 100L, null, null, null, 1L,
                new MediaOwnerRef(MediaOwnerKind.MESSAGE, 9L), "ACTIVE", java.time.Instant.now());
        when(media.find(7L)).thenReturn(java.util.Optional.of(asset));
        Message posted = new Message();
        posted.setId(100L);
        posted.setConversationId(9L);
        posted.setSenderUserId(1L);
        posted.setBody("");
        when(publisher.publish(9L, 1L, "", java.util.List.of(7L))).thenReturn(posted);

        var response = service.post(9L, new PostMessageRequest("", java.util.List.of(7L)));

        assertThat(response.id()).isEqualTo(100L);
        verify(publisher).publish(9L, 1L, "", java.util.List.of(7L));
    }

    @Test
    void post_rejectsAttachmentBelongingToAnotherUser() {
        when(caller.requireUserId()).thenReturn(1L);
        when(conversations.findById(9L)).thenReturn(conversation());
        when(participants.findActive(9L, 1L)).thenReturn(new ConversationParticipant());
        StoredAsset asset = new StoredAsset(7L, "LOCAL", MediaAccessClass.PROTECTED, MediaCategory.MESSAGE_ATTACHMENT,
                "raw", "upload", "pub-id", "asset-id", null, null,
                "photo.jpg", "image/jpeg", 100L, null, null, null, 999L, // uploaded by another user
                new MediaOwnerRef(MediaOwnerKind.MESSAGE, 9L), "ACTIVE", java.time.Instant.now());
        when(media.find(7L)).thenReturn(java.util.Optional.of(asset));

        assertThatThrownBy(() -> service.post(9L, new PostMessageRequest("", java.util.List.of(7L))))
                .isInstanceOf(NadForbiddenException.class)
                .hasMessageContaining("does not belong to this conversation");
        verify(publisher, never()).publish(anyLong(), anyLong(), anyString(), any());
    }

    @Test
    void post_rejectsAttachmentBelongingToAnotherConversation() {
        when(caller.requireUserId()).thenReturn(1L);
        when(conversations.findById(9L)).thenReturn(conversation());
        when(participants.findActive(9L, 1L)).thenReturn(new ConversationParticipant());
        StoredAsset asset = new StoredAsset(7L, "LOCAL", MediaAccessClass.PROTECTED, MediaCategory.MESSAGE_ATTACHMENT,
                "raw", "upload", "pub-id", "asset-id", null, null,
                "photo.jpg", "image/jpeg", 100L, null, null, null, 1L,
                new MediaOwnerRef(MediaOwnerKind.MESSAGE, 42L), // different conversation
                "ACTIVE", java.time.Instant.now());
        when(media.find(7L)).thenReturn(java.util.Optional.of(asset));

        assertThatThrownBy(() -> service.post(9L, new PostMessageRequest("", java.util.List.of(7L))))
                .isInstanceOf(NadForbiddenException.class)
                .hasMessageContaining("does not belong to this conversation");
        verify(publisher, never()).publish(anyLong(), anyLong(), anyString(), any());
    }

    @Test
    void post_rejectsUnknownMediaAsset() {
        when(caller.requireUserId()).thenReturn(1L);
        when(conversations.findById(9L)).thenReturn(conversation());
        when(participants.findActive(9L, 1L)).thenReturn(new ConversationParticipant());
        when(media.find(7L)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.post(9L, new PostMessageRequest("", java.util.List.of(7L))))
                .isInstanceOf(NadNotFoundException.class)
                .hasMessageContaining("media asset 7 not found");
        verify(publisher, never()).publish(anyLong(), anyLong(), anyString(), any());
    }

    @Test
    void post_rejectsWhenTheConversationDoesNotExist() {
        when(caller.requireUserId()).thenReturn(2L);
        when(conversations.findById(9L)).thenReturn(null);

        assertThatThrownBy(() -> service.post(9L, new PostMessageRequest("hi", null)))
                .isInstanceOf(com.nadoumi.common.exception.NadNotFoundException.class);
    }

    // ---- required security test: staff-only transitions reject non-staff callers ----

    @Test
    void close_rejectsANonStaffCaller() {
        when(caller.isStaff()).thenReturn(false);

        assertThatThrownBy(() -> service.close(9L)).isInstanceOf(AccessDeniedException.class);
        verify(conversations, never()).updateStatus(anyLong(), anyString(), anyString());
    }

    @Test
    void addParticipant_rejectsANonStaffCaller() {
        when(caller.isStaff()).thenReturn(false);

        assertThatThrownBy(() -> service.addParticipant(9L, 3L, ParticipantRole.STAFF))
                .isInstanceOf(AccessDeniedException.class);
        verify(participants, never()).insert(any());
    }

    @Test
    void removeParticipant_rejectsANonStaffCaller() {
        when(caller.isStaff()).thenReturn(false);

        assertThatThrownBy(() -> service.removeParticipant(9L, 3L)).isInstanceOf(AccessDeniedException.class);
        verify(participants, never()).remove(anyLong(), anyLong(), any());
    }

    @Test
    void listForStaff_rejectsANonStaffCaller() {
        when(caller.isStaff()).thenReturn(false);

        assertThatThrownBy(() -> service.listForStaff()).isInstanceOf(AccessDeniedException.class);
    }

    // ---- read-state math ----

    @Test
    void markRead_advancesToTheLatestMessageId() {
        when(caller.requireUserId()).thenReturn(1L);
        when(conversations.findById(9L)).thenReturn(conversation());
        when(participants.findActive(9L, 1L)).thenReturn(new ConversationParticipant());
        Message latest = new Message();
        latest.setId(55L);
        when(messages.findLatest(9L)).thenReturn(latest);

        service.markRead(9L);

        verify(participants).updateLastRead(9L, 1L, 55L);
    }

    @Test
    void markRead_isANoOpWhenTheConversationHasNoMessagesYet() {
        when(caller.requireUserId()).thenReturn(1L);
        when(conversations.findById(9L)).thenReturn(conversation());
        when(participants.findActive(9L, 1L)).thenReturn(new ConversationParticipant());
        when(messages.findLatest(9L)).thenReturn(null);

        service.markRead(9L);

        verify(participants, never()).updateLastRead(anyLong(), anyLong(), anyLong());
    }

    // ---- attachmentAccess ----

    private static MessageAttachment attachment(long id, long messageId, long mediaAssetId) {
        MessageAttachment a = new MessageAttachment();
        a.setId(id);
        a.setMessageId(messageId);
        a.setMediaAssetId(mediaAssetId);
        return a;
    }

    private static Message message(long id, long conversationId) {
        Message m = new Message();
        m.setId(id);
        m.setConversationId(conversationId);
        return m;
    }

    private static MediaAccessLogContext ctx() {
        return new MediaAccessLogContext(1L, null, null, null, "127.0.0.1", "test-agent");
    }

    @Test
    void attachmentAccess_rejectsACallerWhoIsNotAnActiveParticipant() {
        when(caller.requireUserId()).thenReturn(2L);
        when(conversations.findById(9L)).thenReturn(conversation());
        when(participants.findActive(9L, 2L)).thenReturn(null);

        assertThatThrownBy(() -> service.attachmentAccess(9L, 500L, ctx())).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void attachmentAccess_rejectsAnUnknownAttachment() {
        when(caller.requireUserId()).thenReturn(1L);
        when(conversations.findById(9L)).thenReturn(conversation());
        when(participants.findActive(9L, 1L)).thenReturn(new ConversationParticipant());
        when(attachments.findById(500L)).thenReturn(null);

        assertThatThrownBy(() -> service.attachmentAccess(9L, 500L, ctx())).isInstanceOf(NadNotFoundException.class);
    }

    @Test
    void attachmentAccess_rejectsAnAttachmentBelongingToADifferentConversation() {
        when(caller.requireUserId()).thenReturn(1L);
        when(conversations.findById(9L)).thenReturn(conversation());
        when(participants.findActive(9L, 1L)).thenReturn(new ConversationParticipant());
        when(attachments.findById(500L)).thenReturn(attachment(500L, 100L, 7L));
        when(messages.findById(100L)).thenReturn(message(100L, 42L)); // a different conversation

        assertThatThrownBy(() -> service.attachmentAccess(9L, 500L, ctx())).isInstanceOf(NadNotFoundException.class);
    }

    @Test
    void attachmentAccess_returnsASignedUrlAndDisplayMetadataForAValidAttachment() {
        when(caller.requireUserId()).thenReturn(1L);
        when(conversations.findById(9L)).thenReturn(conversation());
        when(participants.findActive(9L, 1L)).thenReturn(new ConversationParticipant());
        when(attachments.findById(500L)).thenReturn(attachment(500L, 100L, 7L));
        when(messages.findById(100L)).thenReturn(message(100L, 9L));
        var expiry = java.time.Instant.parse("2026-02-03T09:15:00Z");
        when(media.issueInlineSignedUrl(org.mockito.ArgumentMatchers.eq(7L), any()))
                .thenReturn(new SignedUrl("https://cdn.example.com/scan.pdf", expiry));
        StoredAsset asset = new StoredAsset(7L, "CLOUDINARY", MediaAccessClass.PROTECTED, MediaCategory.MESSAGE_ATTACHMENT,
                "raw", "upload", "pub-id", "asset-id", null, null,
                "transcript.pdf", "application/pdf", 1234L, null, null, null, 1L, null, "ACTIVE", expiry);
        when(media.find(7L)).thenReturn(java.util.Optional.of(asset));

        var access = service.attachmentAccess(9L, 500L, ctx());

        assertThat(access.url()).isEqualTo("https://cdn.example.com/scan.pdf");
        assertThat(access.filename()).isEqualTo("transcript.pdf");
        assertThat(access.contentType()).isEqualTo("application/pdf");
    }

    @Test
    void uploadAttachment_rejectsWhenNotActiveParticipant() {
        when(caller.requireUserId()).thenReturn(2L);
        when(conversations.findById(9L)).thenReturn(conversation());
        when(participants.findActive(9L, 2L)).thenReturn(null);
        org.springframework.web.multipart.MultipartFile file = mock(org.springframework.web.multipart.MultipartFile.class);

        assertThatThrownBy(() -> service.uploadAttachment(9L, file)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void uploadAttachment_storesFileAndReturnsMediaIdWhenActiveParticipant() throws Exception {
        when(caller.requireUserId()).thenReturn(1L);
        when(conversations.findById(9L)).thenReturn(conversation());
        when(participants.findActive(9L, 1L)).thenReturn(new ConversationParticipant());

        org.springframework.web.multipart.MultipartFile file = mock(org.springframework.web.multipart.MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("doc.pdf");
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getSize()).thenReturn(1024L);
        when(file.getInputStream()).thenReturn(new java.io.ByteArrayInputStream("fake".getBytes()));

        com.nadoumi.common.media.MediaUploadResult uploaded = new com.nadoumi.common.media.MediaUploadResult(88L, null);
        when(media.upload(any(), org.mockito.ArgumentMatchers.eq("doc.pdf"), org.mockito.ArgumentMatchers.eq("application/pdf"),
                org.mockito.ArgumentMatchers.eq(1024L), org.mockito.ArgumentMatchers.eq(MediaCategory.MESSAGE_ATTACHMENT),
                org.mockito.ArgumentMatchers.isNull(), any(), org.mockito.ArgumentMatchers.eq(1L)))
                .thenReturn(uploaded);

        long mediaId = service.uploadAttachment(9L, file);
        assertThat(mediaId).isEqualTo(88L);
    }
}
