package com.nadoumi.communication.stream;

import com.nadoumi.communication.domain.ConversationParticipant;
import com.nadoumi.communication.mapper.ConversationParticipantMapper;
import com.nadoumi.communication.web.response.MessageResponse;
import org.springframework.stereotype.Component;

/** What the other participants of a conversation are told, and when: after the change is committed. */
@Component
public class ChatEvents {

    private final ConversationParticipantMapper participants;
    private final RealtimePublisher realtime;

    public ChatEvents(ConversationParticipantMapper participants, RealtimePublisher realtime) {
        this.participants = participants;
        this.realtime = realtime;
    }

    /** Pushes the full message to everyone in the conversation except its sender. */
    public void messagePosted(MessageResponse message) {
        for (ConversationParticipant p : participants.listActiveForConversation(message.conversationId())) {
            if (p.getUserId() != message.senderUserId()) {
                realtime.afterCommit(p.getUserId(), StreamMessageListener.MESSAGE_EVENT, message);
            }
        }
    }

    /** Tells the other participants how far {@code readerUserId} has read. */
    public void messagesRead(long conversationId, long readerUserId, long messageId) {
        DeliveryTracker.ReceiptEvent event = new DeliveryTracker.ReceiptEvent(conversationId, readerUserId, messageId);
        for (ConversationParticipant p : participants.listActiveForConversation(conversationId)) {
            if (p.getUserId() != readerUserId) {
                realtime.afterCommit(p.getUserId(), "read", event);
            }
        }
    }
}
