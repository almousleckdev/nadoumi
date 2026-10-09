package com.nadoumi.communication.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.nadoumi.common.outbox.OutboxEventTypes;
import com.nadoumi.common.outbox.OutboxWriter;
import com.nadoumi.common.media.MediaGateway;
import com.nadoumi.communication.domain.Conversation;
import com.nadoumi.communication.domain.ConversationParticipant;
import com.nadoumi.communication.domain.Message;
import com.nadoumi.communication.domain.MessageAttachment;
import com.nadoumi.communication.mapper.CommunicationUserMapper;
import com.nadoumi.communication.mapper.ConversationMapper;
import com.nadoumi.communication.mapper.ConversationParticipantMapper;
import com.nadoumi.communication.mapper.MessageAttachmentMapper;
import com.nadoumi.communication.mapper.MessageMapper;
import com.nadoumi.communication.stream.PresenceService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes a message and its attachments, keeps the conversation's last-message summary current, and, in the same
 * transaction, writes an outbox event for every recipient who is not connected anywhere (presence is cluster-wide),
 * so they hear about it as an IN_APP notification. Live recipients are reached by {@code ChatEvents} once the
 * transaction commits.
 */
@Component
public class MessagePublisher {

    private static final int PREVIEW_LENGTH = 160;

    private final MessageMapper messages;
    private final MessageAttachmentMapper attachments;
    private final ConversationParticipantMapper participants;
    private final ConversationMapper conversations;
    private final CommunicationUserMapper users;
    private final OutboxWriter outbox;
    private final PresenceService presence;
    private final MediaGateway media;

    public MessagePublisher(MessageMapper messages, MessageAttachmentMapper attachments,
            ConversationParticipantMapper participants, ConversationMapper conversations,
            CommunicationUserMapper users, OutboxWriter outbox,
            PresenceService presence, MediaGateway media) {
        this.messages = messages;
        this.attachments = attachments;
        this.participants = participants;
        this.conversations = conversations;
        this.users = users;
        this.outbox = outbox;
        this.presence = presence;
        this.media = media;
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
            media.find(mediaId).ifPresent(asset -> {
                attachment.setOriginalFilename(asset.originalFilename());
                attachment.setContentType(asset.contentType());
                attachment.setByteSize(asset.byteSize());
            });
            attachments.insert(attachment);
        }

        conversations.updateLastMessage(conversationId, message.getId(), message.getCreatedAt(),
                previewOf(body, attachmentMediaIds.size()), senderUserId);
        participants.updateLastRead(conversationId, senderUserId, message.getId());
        participants.updateLastDelivered(conversationId, senderUserId, message.getId());

        List<Long> offlineRecipientIds = new ArrayList<>();
        for (ConversationParticipant p : participants.listActiveForConversation(conversationId)) {
            if (p.getUserId() == senderUserId) {
                continue;
            }
            if (!presence.isOnline(p.getUserId())) {
                offlineRecipientIds.add(p.getUserId());
            }
        }

        if (!offlineRecipientIds.isEmpty()) {
            writeNotificationEvent(conversationId, senderUserId, message.getId(), offlineRecipientIds);
        }

        return message;
    }

    /** An attachment-only message (no caption) still gets an honest, non-empty preview. */
    private static String previewOf(String body, int attachmentCount) {
        if (!body.isBlank()) {
            return body.length() <= PREVIEW_LENGTH ? body : body.substring(0, PREVIEW_LENGTH - 1) + "…";
        }
        return attachmentCount == 1 ? "📎 Attachment" : "📎 " + attachmentCount + " attachments";
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
