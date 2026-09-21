package com.nadoumi.communication.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.nadoumi.common.outbox.OutboxEventTypes;
import com.nadoumi.common.outbox.OutboxWriter;
import com.nadoumi.communication.domain.Conversation;
import com.nadoumi.communication.domain.ConversationParticipant;
import com.nadoumi.communication.domain.Message;
import com.nadoumi.communication.domain.MessageAttachment;
import com.nadoumi.communication.mapper.CommunicationUserMapper;
import com.nadoumi.communication.mapper.ConversationMapper;
import com.nadoumi.communication.mapper.ConversationParticipantMapper;
import com.nadoumi.communication.mapper.MessageAttachmentMapper;
import com.nadoumi.communication.mapper.MessageMapper;
import com.nadoumi.communication.stream.RealtimePublisher;
import com.nadoumi.communication.stream.SseConnectionRegistry;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes a message and, in the same transaction, an outbox event for every
 * recipient this instance cannot reach live -- everyone else still gets a
 * best-effort realtime ping. "Online" is a local-instance connection check only
 * (docs/superpowers/specs/2026-09-20-messaging-domain-design.md §3): a recipient
 * connected to a different instance is treated as offline here and gets both an
 * IN_APP notification and (from their own instance) a ping -- a harmless
 * over-notify, never a lost message. No presence-tracking infrastructure beyond
 * that is in scope.
 */
@Component
public class MessagePublisher {

    private final MessageMapper messages;
    private final MessageAttachmentMapper attachments;
    private final ConversationParticipantMapper participants;
    private final ConversationMapper conversations;
    private final CommunicationUserMapper users;
    private final OutboxWriter outbox;
    private final RealtimePublisher realtime;
    private final SseConnectionRegistry connections;

    public MessagePublisher(MessageMapper messages, MessageAttachmentMapper attachments,
            ConversationParticipantMapper participants, ConversationMapper conversations,
            CommunicationUserMapper users, OutboxWriter outbox, RealtimePublisher realtime,
            SseConnectionRegistry connections) {
        this.messages = messages;
        this.attachments = attachments;
        this.participants = participants;
        this.conversations = conversations;
        this.users = users;
        this.outbox = outbox;
        this.realtime = realtime;
        this.connections = connections;
    }

    @Transactional(rollbackFor = Exception.class)
    public Message publish(long conversationId, long senderUserId, String body, List<Long> attachmentMediaIds) {
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setSenderUserId(senderUserId);
        message.setBody(body);
        message.setCreatedAt(LocalDateTime.now());
        messages.insert(message);

        for (Long mediaId : attachmentMediaIds) {
            MessageAttachment attachment = new MessageAttachment();
            attachment.setMessageId(message.getId());
            attachment.setMediaAssetId(mediaId);
            attachments.insert(attachment);
        }

        List<Long> otherParticipantIds = new ArrayList<>();
        List<Long> offlineRecipientIds = new ArrayList<>();
        for (ConversationParticipant p : participants.listActiveForConversation(conversationId)) {
            if (p.getUserId() == senderUserId) {
                continue;
            }
            otherParticipantIds.add(p.getUserId());
            if (!connections.hasLocalConnection(p.getUserId())) {
                offlineRecipientIds.add(p.getUserId());
            }
        }

        if (!offlineRecipientIds.isEmpty()) {
            writeNotificationEvent(conversationId, senderUserId, message.getId(), offlineRecipientIds);
        }
        for (Long userId : otherParticipantIds) {
            realtime.publishConversationPing(userId, conversationId);
        }

        return message;
    }

    private void writeNotificationEvent(long conversationId, long senderUserId, long messageId,
            List<Long> recipientUserIds) {
        Conversation conversation = conversations.findById(conversationId);
        String senderName = users.findDisplayName(senderUserId);
        JSONObject payload = new JSONObject();
        payload.put("recipientUserIds", new JSONArray(recipientUserIds.toArray()));
        payload.put("conversationId", conversationId);
        payload.put("messageId", messageId);
        if (conversation != null && conversation.getApplicationId() != null) {
            payload.put("applicationId", conversation.getApplicationId());
        }
        payload.put("senderName", senderName == null ? "Someone" : senderName);
        payload.put("conversationSubject", conversation != null && conversation.getSubject() != null
                ? conversation.getSubject() : "your conversation");
        outbox.write("conversation", conversationId, OutboxEventTypes.MESSAGE_POSTED, payload.toJSONString());
    }
}
