package com.nadoumi.communication.stream;

import com.nadoumi.communication.mapper.ConversationParticipantMapper;
import com.nadoumi.communication.mapper.UndeliveredRow;
import org.springframework.stereotype.Component;

/**
 * Records that a message reached a user's device and tells its sender. "Reached" means this instance wrote the
 * event to a live connection of the recipient, or the recipient's stream just opened and caught up, so no
 * acknowledgement request is needed from the client.
 */
@Component
public class DeliveryTracker {

    /** Wire body of {@code delivered} and {@code read} events. */
    public record ReceiptEvent(long conversationId, long userId, long messageId) {
    }

    private final ConversationParticipantMapper participants;
    private final RealtimePublisher realtime;

    public DeliveryTracker(ConversationParticipantMapper participants, RealtimePublisher realtime) {
        this.participants = participants;
        this.realtime = realtime;
    }

    public void delivered(long conversationId, long recipientUserId, long messageId, long senderUserId) {
        participants.updateLastDelivered(conversationId, recipientUserId, messageId);
        realtime.publish(senderUserId, "delivered", new ReceiptEvent(conversationId, recipientUserId, messageId));
    }

    /** A user just connected: everything sent to them while they were away has now reached them. */
    public void caughtUp(long userId) {
        for (UndeliveredRow row : participants.listUndelivered(userId)) {
            delivered(row.conversationId(), userId, row.messageId(), row.senderUserId());
        }
    }
}
