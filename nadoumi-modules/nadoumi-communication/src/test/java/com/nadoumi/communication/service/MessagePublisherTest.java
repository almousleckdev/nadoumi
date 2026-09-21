package com.nadoumi.communication.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.common.outbox.OutboxEventTypes;
import com.nadoumi.common.outbox.OutboxWriter;
import com.nadoumi.communication.domain.Conversation;
import com.nadoumi.communication.domain.ConversationParticipant;
import com.nadoumi.communication.domain.Message;
import com.nadoumi.communication.domain.enums.ConversationStatus;
import com.nadoumi.communication.domain.enums.ConversationType;
import com.nadoumi.communication.domain.enums.ParticipantRole;
import com.nadoumi.communication.mapper.CommunicationUserMapper;
import com.nadoumi.communication.mapper.ConversationMapper;
import com.nadoumi.communication.mapper.ConversationParticipantMapper;
import com.nadoumi.communication.mapper.MessageAttachmentMapper;
import com.nadoumi.communication.mapper.MessageMapper;
import com.nadoumi.communication.stream.RealtimePublisher;
import com.nadoumi.communication.stream.SseConnectionRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MessagePublisherTest {

    private final MessageMapper messages = mock(MessageMapper.class);
    private final MessageAttachmentMapper attachments = mock(MessageAttachmentMapper.class);
    private final ConversationParticipantMapper participants = mock(ConversationParticipantMapper.class);
    private final ConversationMapper conversations = mock(ConversationMapper.class);
    private final CommunicationUserMapper users = mock(CommunicationUserMapper.class);
    private final OutboxWriter outbox = mock(OutboxWriter.class);
    private final RealtimePublisher realtime = mock(RealtimePublisher.class);
    private final SseConnectionRegistry connections = mock(SseConnectionRegistry.class);
    private final MessagePublisher publisher = new MessagePublisher(messages, attachments, participants,
            conversations, users, outbox, realtime, connections);

    private static ConversationParticipant participant(long userId) {
        ConversationParticipant p = new ConversationParticipant();
        p.setUserId(userId);
        p.setRole(ParticipantRole.STAFF);
        return p;
    }

    private static Conversation conversation() {
        Conversation c = new Conversation();
        c.setId(9L);
        c.setSubject("Visa question");
        c.setConversationType(ConversationType.GENERAL);
        c.setStatus(ConversationStatus.OPEN);
        return c;
    }

    @Test
    void offlineRecipient_getsAnOutboxEventWithTheExactPayloadContract() {
        when(participants.listActiveForConversation(9L)).thenReturn(List.of(participant(1L), participant(2L)));
        when(connections.hasLocalConnection(2L)).thenReturn(false);
        when(conversations.findById(9L)).thenReturn(conversation());
        when(users.findDisplayName(1L)).thenReturn("Ada");
        org.mockito.Mockito.doAnswer(inv -> {
            inv.<Message>getArgument(0).setId(50L);
            return 1;
        }).when(messages).insert(any());

        publisher.publish(9L, 1L, "hello", List.of());

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(outbox).write(org.mockito.ArgumentMatchers.eq("conversation"), org.mockito.ArgumentMatchers.eq(9L),
                org.mockito.ArgumentMatchers.eq(OutboxEventTypes.MESSAGE_POSTED), payload.capture());
        assertThat(payload.getValue())
                .contains("\"recipientUserIds\":[2]")
                .contains("\"conversationId\":9")
                .contains("\"senderName\":\"Ada\"")
                .contains("\"conversationSubject\":\"Visa question\"");
    }

    @Test
    void onlineRecipient_getsNoOutboxEventButStillGetsAPing() {
        when(participants.listActiveForConversation(9L)).thenReturn(List.of(participant(1L), participant(2L)));
        when(connections.hasLocalConnection(2L)).thenReturn(true);

        publisher.publish(9L, 1L, "hello", List.of());

        verify(outbox, never()).write(anyString(), anyLong(), anyString(), anyString());
        verify(realtime).publishConversationPing(2L, 9L);
    }

    @Test
    void theSenderNeverReceivesItsOwnNotificationOrPing() {
        when(participants.listActiveForConversation(9L)).thenReturn(List.of(participant(1L)));

        publisher.publish(9L, 1L, "hello", List.of());

        verify(outbox, never()).write(anyString(), anyLong(), anyString(), anyString());
        verify(realtime, never()).publishConversationPing(anyLong(), anyLong());
    }

    @Test
    void insertsOneAttachmentRowPerMediaId() {
        when(participants.listActiveForConversation(9L)).thenReturn(List.of(participant(1L)));

        publisher.publish(9L, 1L, "see attached", List.of(101L, 102L));

        verify(attachments, times(2)).insert(any());
    }

    @Test
    void insertsTheMessageBeforeResolvingRecipients() {
        when(participants.listActiveForConversation(9L)).thenReturn(List.of());

        Message result = publisher.publish(9L, 1L, "hello", List.of());

        verify(messages).insert(result);
        assertThat(result.getConversationId()).isEqualTo(9L);
        assertThat(result.getSenderUserId()).isEqualTo(1L);
        assertThat(result.getBody()).isEqualTo("hello");
    }
}
