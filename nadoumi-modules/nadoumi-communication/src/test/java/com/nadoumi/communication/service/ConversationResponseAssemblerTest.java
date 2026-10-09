package com.nadoumi.communication.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.nadoumi.common.media.MediaGateway;
import com.nadoumi.communication.domain.Message;
import com.nadoumi.communication.domain.MessageAttachment;
import com.nadoumi.communication.mapper.InboxRow;
import com.nadoumi.communication.mapper.MessageAttachmentMapper;
import com.nadoumi.communication.web.response.MessageResponse;
import com.nadoumi.identity.profile.PublicProfile;
import com.nadoumi.identity.profile.PublicProfileService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConversationResponseAssemblerTest {

    private final MessageAttachmentMapper attachments = mock(MessageAttachmentMapper.class);
    private final PublicProfileService profiles = mock(PublicProfileService.class);
    private final MediaGateway media = mock(MediaGateway.class);
    private final com.nadoumi.communication.stream.PresenceService presence =
            mock(com.nadoumi.communication.stream.PresenceService.class);
    private final ConversationResponseAssembler assembler = new ConversationResponseAssembler(attachments, profiles, media, presence);

    private static Message message(long id, long sender) {
        Message m = new Message();
        m.setId(id);
        m.setConversationId(9L);
        m.setSenderUserId(sender);
        m.setBody("body " + id);
        m.setCreatedAt(LocalDateTime.of(2026, 10, 9, 8, 0));
        return m;
    }

    private static MessageAttachment attachment(long id, long messageId, String name, String type, long size) {
        MessageAttachment a = new MessageAttachment();
        a.setId(id);
        a.setMessageId(messageId);
        a.setMediaAssetId(500 + id);
        a.setOriginalFilename(name);
        a.setContentType(type);
        a.setByteSize(size);
        return a;
    }

    @Test
    void shouldUseOneAttachmentQueryAndOneProfileQuery_whenAssemblingAWholePage() {
        List<Message> page = java.util.stream.LongStream.rangeClosed(1, 30)
                .mapToObj(i -> message(i, i % 2 == 0 ? 1L : 100L)).toList();
        when(attachments.listByMessageIds(anyCollection())).thenReturn(List.of());
        when(profiles.resolve(anyCollection())).thenReturn(Map.of(1L, new PublicProfile("Jane", null),
                100L, new PublicProfile("Amina", null)));

        List<MessageResponse> out = assembler.messages(page);

        assertThat(out).hasSize(30);
        verify(attachments, times(1)).listByMessageIds(anyCollection());
        verify(profiles, times(1)).resolve(anyCollection());
        verify(attachments, never()).listByMessage(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void shouldNameSendersByTheirPublicProfile_neverTheirAccountName() {
        when(attachments.listByMessageIds(anyCollection())).thenReturn(List.of());
        when(profiles.resolve(anyCollection())).thenReturn(Map.of(100L, new PublicProfile("Amina", null)));

        MessageResponse out = assembler.message(message(1, 100L));

        assertThat(out.senderName()).isEqualTo("Amina");
    }

    @Test
    void shouldDescribeAttachmentsFromTheRowWithoutSigningOrTouchingMedia() {
        when(attachments.listByMessageIds(anyCollection())).thenReturn(List.of(
                attachment(1, 1, "photo.png", "image/png", 2048), attachment(2, 1, "cv.pdf", "application/pdf", 4096)));
        when(profiles.resolve(anyCollection())).thenReturn(Map.of());

        MessageResponse out = assembler.message(message(1, 100L));

        assertThat(out.attachments()).extracting("filename", "image").containsExactly(
                org.assertj.core.groups.Tuple.tuple("photo.png", true), org.assertj.core.groups.Tuple.tuple("cv.pdf", false));
        assertThat(out.senderName()).isEqualTo("Someone");
        verifyNoInteractions(media);
    }

    @Test
    void shouldFallBackToTheMediaRecord_whenAnOldAttachmentRowHasNoMetadata() {
        MessageAttachment legacy = attachment(1, 1, null, null, 0);
        when(attachments.listByMessageIds(anyCollection())).thenReturn(List.of(legacy));
        when(profiles.resolve(anyCollection())).thenReturn(Map.of());
        when(media.find(501L)).thenReturn(java.util.Optional.empty());

        MessageResponse out = assembler.message(message(1, 100L));

        assertThat(out.attachments()).hasSize(1);
        assertThat(out.attachments().get(0).image()).isFalse();
        verify(media).find(501L);
    }

    @Test
    void shouldShowOnlyFirstNameAndAvatarForThePeer_inTheInbox() {
        InboxRow row = new InboxRow(9L, "Hi", null, "DIRECT", "OPEN", 12L, "last words",
                LocalDateTime.of(2026, 10, 9, 8, 0), 100L, 3L, 100L, 11L, 10L);
        when(profiles.resolve(anyCollection())).thenReturn(Map.of(100L, new PublicProfile("Amina", null)));

        var out = assembler.inbox(List.of(row));

        assertThat(out).hasSize(1);
        assertThat(out.get(0).peer().name()).isEqualTo("Amina");
        assertThat(out.get(0).peer().avatarUrl()).isNull();
        assertThat(out.get(0).unreadCount()).isEqualTo(3L);
        assertThat(out.get(0).peerReadMessageId()).isEqualTo(10L);
        verify(profiles, times(1)).resolve(anyCollection());
    }
}
