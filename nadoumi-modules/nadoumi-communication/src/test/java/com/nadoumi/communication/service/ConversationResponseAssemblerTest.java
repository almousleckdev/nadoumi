package com.nadoumi.communication.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nadoumi.common.media.MediaAccessClass;
import com.nadoumi.common.media.MediaAccessLogContext;
import com.nadoumi.common.media.MediaCategory;
import com.nadoumi.common.media.MediaGateway;
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
import com.nadoumi.communication.mapper.ConversationParticipantMapper;
import com.nadoumi.communication.mapper.MessageAttachmentMapper;
import com.nadoumi.communication.mapper.MessageMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ConversationResponseAssemblerTest {

    private final ConversationParticipantMapper participants = mock(ConversationParticipantMapper.class);
    private final MessageMapper messages = mock(MessageMapper.class);
    private final MessageAttachmentMapper attachments = mock(MessageAttachmentMapper.class);
    private final CommunicationUserMapper users = mock(CommunicationUserMapper.class);
    private final MediaGateway media = mock(MediaGateway.class);
    private final ConversationResponseAssembler assembler =
            new ConversationResponseAssembler(participants, messages, attachments, users, media);

    private static Message message(long id, String body) {
        Message m = new Message();
        m.setId(id);
        m.setConversationId(9L);
        m.setSenderUserId(1L);
        m.setBody(body);
        m.setCreatedAt(LocalDateTime.parse("2026-02-03T09:15:00"));
        return m;
    }

    private static MessageAttachment attachment(long id, long mediaAssetId) {
        MessageAttachment a = new MessageAttachment();
        a.setId(id);
        a.setMediaAssetId(mediaAssetId);
        return a;
    }

    private static StoredAsset asset(long id) {
        return new StoredAsset(id, "CLOUDINARY", MediaAccessClass.PROTECTED, MediaCategory.MESSAGE_ATTACHMENT,
                "raw", "upload", "pub", "asset", null, null, "scan.pdf", "application/pdf", 2048L,
                null, null, null, 1L, null, "ACTIVE", Instant.parse("2026-02-03T09:15:00Z"));
    }

    private static MediaAccessLogContext ctx() {
        return new MediaAccessLogContext(1L, null, null, null, "127.0.0.1", "test-agent");
    }

    private static Conversation conversation() {
        Conversation c = new Conversation();
        c.setId(9L);
        c.setSubject("Visa question");
        c.setConversationType(ConversationType.GENERAL);
        c.setStatus(ConversationStatus.OPEN);
        return c;
    }

    private static ConversationParticipant participant(long userId, ParticipantRole role, Long lastRead) {
        ConversationParticipant p = new ConversationParticipant();
        p.setConversationId(9L);
        p.setUserId(userId);
        p.setRole(role);
        p.setLastReadMessageId(lastRead);
        return p;
    }

    @Test
    void shouldIncludeSenderNameAndAttachmentMetadata_whenMappingMessage() {
        when(users.findDisplayName(1L)).thenReturn("Amina");
        when(attachments.listByMessage(5L)).thenReturn(List.of(attachment(50L, 7L)));
        when(media.find(7L)).thenReturn(Optional.of(asset(7L)));

        var response = assembler.message(message(5L, "hello"));

        assertThat(response.senderName()).isEqualTo("Amina");
        assertThat(response.attachments()).hasSize(1);
        assertThat(response.attachments().get(0).filename()).isEqualTo("scan.pdf");
        assertThat(response.attachments().get(0).url()).isNull();
    }

    @Test
    void shouldPreSignAttachmentUrls_whenAccessContextIsProvided() {
        when(attachments.listByMessage(5L)).thenReturn(List.of(attachment(50L, 7L)));
        when(media.find(7L)).thenReturn(Optional.of(asset(7L)));
        when(media.issueInlineSignedUrl(anyLong(), any()))
                .thenReturn(new SignedUrl("https://cdn.example.com/x", Instant.parse("2026-02-03T09:20:00Z")));

        var response = assembler.message(message(5L, "hello"), ctx());

        assertThat(response.attachments().get(0).url()).isEqualTo("https://cdn.example.com/x");
    }

    @Test
    void shouldLeaveUrlEmptyAndKeepMessage_whenSigningFails() {
        when(attachments.listByMessage(5L)).thenReturn(List.of(attachment(50L, 7L)));
        when(media.find(7L)).thenReturn(Optional.of(asset(7L)));
        when(media.issueInlineSignedUrl(anyLong(), any())).thenThrow(new IllegalStateException("cloudinary down"));

        var response = assembler.message(message(5L, "hello"), ctx());

        assertThat(response.attachments()).hasSize(1);
        assertThat(response.attachments().get(0).url()).isNull();
        assertThat(response.attachments().get(0).filename()).isEqualTo("scan.pdf");
    }

    @Test
    void shouldReturnBareAttachment_whenMediaAssetIsMissing() {
        when(attachments.listByMessage(5L)).thenReturn(List.of(attachment(50L, 7L)));
        when(media.find(7L)).thenReturn(Optional.empty());

        var response = assembler.message(message(5L, "hello"));

        assertThat(response.attachments().get(0).filename()).isNull();
        assertThat(response.attachments().get(0).byteSize()).isZero();
    }

    @Test
    void shouldTruncateLongPreview_whenSummarisingConversation() {
        when(messages.findLatest(9L)).thenReturn(message(5L, "x".repeat(300)));
        when(participants.listActiveForConversation(9L)).thenReturn(List.of());

        var summary = assembler.summary(conversation(), null);

        assertThat(summary.lastMessagePreview()).hasSize(141).endsWith("…");
    }

    @Test
    void shouldDescribeAttachmentOnlyMessages_whenBodyIsBlank() {
        when(messages.findLatest(9L)).thenReturn(message(5L, "  "));
        when(participants.listActiveForConversation(9L)).thenReturn(List.of());
        when(attachments.listByMessage(5L)).thenReturn(List.of(attachment(1L, 7L)));
        assertThat(assembler.summary(conversation(), null).lastMessagePreview()).isEqualTo("📎 Attachment");

        when(attachments.listByMessage(5L)).thenReturn(List.of(attachment(1L, 7L), attachment(2L, 8L)));
        assertThat(assembler.summary(conversation(), null).lastMessagePreview()).isEqualTo("📎 2 attachments");
    }

    @Test
    void shouldReportNoUnreadAndNoPreview_whenThereIsNoCallerParticipantAndNoMessages() {
        when(messages.findLatest(9L)).thenReturn(null);
        when(participants.listActiveForConversation(9L)).thenReturn(List.of());

        var summary = assembler.summary(conversation(), null);

        assertThat(summary.unreadCount()).isZero();
        assertThat(summary.lastMessagePreview()).isNull();
        assertThat(summary.lastMessageAt()).isNull();
    }

    @Test
    void shouldCountUnreadAfterTheCallersLastReadMessage() {
        ConversationParticipant me = participant(3L, ParticipantRole.APPLICANT, 40L);
        when(messages.findLatest(9L)).thenReturn(message(45L, "hi"));
        when(messages.countAfter(9L, 40L, 3L)).thenReturn(4L);
        when(participants.listActiveForConversation(9L)).thenReturn(List.of(me));

        assertThat(assembler.summary(conversation(), me).unreadCount()).isEqualTo(4L);
    }

    @Test
    void shouldPickTheFirstStaffAsAdminAndTheFirstOtherAsStudent() {
        when(messages.findLatest(9L)).thenReturn(null);
        when(participants.listActiveForConversation(9L)).thenReturn(List.of(
                participant(20L, ParticipantRole.STAFF, null),
                participant(21L, ParticipantRole.STAFF, null),
                participant(30L, ParticipantRole.APPLICANT, null),
                participant(31L, ParticipantRole.GUARDIAN, null)));
        when(users.findDisplayName(20L)).thenReturn("Karim");
        when(users.findDisplayName(30L)).thenReturn("Amina");

        var summary = assembler.summary(conversation(), null);

        assertThat(summary.adminUserId()).isEqualTo(20L);
        assertThat(summary.adminName()).isEqualTo("Karim");
        assertThat(summary.studentUserId()).isEqualTo(30L);
        assertThat(summary.studentName()).isEqualTo("Amina");
    }
}
