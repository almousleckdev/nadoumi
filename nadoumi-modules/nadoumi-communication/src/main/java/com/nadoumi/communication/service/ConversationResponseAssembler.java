package com.nadoumi.communication.service;

import com.nadoumi.common.media.MediaAccessLogContext;
import com.nadoumi.common.media.MediaGateway;
import com.nadoumi.communication.domain.Conversation;
import com.nadoumi.communication.domain.ConversationParticipant;
import com.nadoumi.communication.domain.Message;
import com.nadoumi.communication.domain.MessageAttachment;
import com.nadoumi.communication.domain.enums.ParticipantRole;
import com.nadoumi.communication.mapper.CommunicationUserMapper;
import com.nadoumi.communication.mapper.ConversationParticipantMapper;
import com.nadoumi.communication.mapper.MessageAttachmentMapper;
import com.nadoumi.communication.mapper.MessageMapper;
import com.nadoumi.communication.web.response.AttachmentResponse;
import com.nadoumi.communication.web.response.ConversationSummaryResponse;
import com.nadoumi.communication.web.response.MessageResponse;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ConversationResponseAssembler {

    private static final Logger log = LoggerFactory.getLogger(ConversationResponseAssembler.class);
    private static final int PREVIEW_LENGTH = 140;

    private final ConversationParticipantMapper participants;
    private final MessageMapper messages;
    private final MessageAttachmentMapper attachments;
    private final CommunicationUserMapper users;
    private final MediaGateway media;

    public ConversationResponseAssembler(ConversationParticipantMapper participants, MessageMapper messages,
            MessageAttachmentMapper attachments, CommunicationUserMapper users, MediaGateway media) {
        this.participants = participants;
        this.messages = messages;
        this.attachments = attachments;
        this.users = users;
        this.media = media;
    }

    public ConversationSummaryResponse summary(Conversation c, ConversationParticipant myParticipant) {
        Message latest = messages.findLatest(c.getId());
        long afterId = myParticipant == null || myParticipant.getLastReadMessageId() == null
                ? 0 : myParticipant.getLastReadMessageId();
        long unread = myParticipant == null ? 0 : messages.countAfter(c.getId(), afterId, myParticipant.getUserId());
        String preview = latest == null ? null : previewFor(latest);

        Long studentUserId = null;
        String studentName = null;
        Long adminUserId = null;
        String adminName = null;

        for (ConversationParticipant p : participants.listActiveForConversation(c.getId())) {
            if (p.getRole() == ParticipantRole.STAFF) {
                if (adminUserId == null) {
                    adminUserId = p.getUserId();
                    adminName = users.findDisplayName(p.getUserId());
                }
            } else if (studentUserId == null) {
                studentUserId = p.getUserId();
                studentName = users.findDisplayName(p.getUserId());
            }
        }

        return new ConversationSummaryResponse(c.getId(), c.getSubject(), c.getApplicationId(),
                c.getConversationType().name(), c.getStatus().name(), preview,
                latest == null ? null : latest.getCreatedAt(), unread,
                studentUserId, studentName, adminUserId, adminName);
    }

    public MessageResponse message(Message m) {
        return message(m, null);
    }

    public MessageResponse message(Message m, MediaAccessLogContext ctx) {
        List<AttachmentResponse> attached = attachments.listByMessage(m.getId()).stream()
                .map(a -> attachment(a, ctx))
                .toList();
        String senderName = users.findDisplayName(m.getSenderUserId());
        return new MessageResponse(m.getId(), m.getConversationId(), m.getSenderUserId(), senderName,
                m.getBody(), m.getCreatedAt(), m.getEditedAt(), attached);
    }

    private AttachmentResponse attachment(MessageAttachment a, MediaAccessLogContext ctx) {
        return media.find(a.getMediaAssetId())
                .map(asset -> new AttachmentResponse(a.getId(), a.getMediaAssetId(), asset.originalFilename(),
                        asset.contentType(), asset.byteSize(), signedUrl(a, ctx)))
                .orElseGet(() -> new AttachmentResponse(a.getId(), a.getMediaAssetId(), null, null, 0, null));
    }

    private String signedUrl(MessageAttachment a, MediaAccessLogContext ctx) {
        if (ctx == null) {
            return null;
        }
        try {
            return media.issueInlineSignedUrl(a.getMediaAssetId(), ctx).url();
        }
        catch (RuntimeException e) {
            log.warn("could not pre-sign attachment {} (media {}); the client will fetch it on demand",
                    a.getId(), a.getMediaAssetId(), e);
            return null;
        }
    }

    private static String truncate(String body) {
        return body.length() <= PREVIEW_LENGTH ? body : body.substring(0, PREVIEW_LENGTH) + "…";
    }

    /** An attachment-only message (no caption) still needs an honest, non-empty preview. */
    private String previewFor(Message latest) {
        if (!latest.getBody().isBlank()) {
            return truncate(latest.getBody());
        }
        int count = attachments.listByMessage(latest.getId()).size();
        return count == 1 ? "📎 Attachment" : "📎 " + count + " attachments";
    }
}
