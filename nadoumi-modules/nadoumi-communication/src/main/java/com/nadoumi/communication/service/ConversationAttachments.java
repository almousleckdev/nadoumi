package com.nadoumi.communication.service;

import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.common.exception.NadForbiddenException;
import com.nadoumi.common.exception.NadNotFoundException;
import com.nadoumi.common.media.MediaAccessLogContext;
import com.nadoumi.common.media.MediaCategory;
import com.nadoumi.common.media.MediaGateway;
import com.nadoumi.common.media.MediaOwnerKind;
import com.nadoumi.common.media.MediaOwnerRef;
import com.nadoumi.common.media.StoredAsset;
import com.nadoumi.communication.domain.Message;
import com.nadoumi.communication.domain.MessageAttachment;
import com.nadoumi.communication.mapper.MessageAttachmentMapper;
import com.nadoumi.communication.mapper.MessageMapper;
import com.nadoumi.communication.web.response.AttachmentAccessResponse;
import com.nadoumi.identity.access.CurrentCaller;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ConversationAttachments {

    private final ConversationGuard guard;
    private final MessageAttachmentMapper attachments;
    private final MessageMapper messages;
    private final MediaGateway media;
    private final CurrentCaller caller;

    public ConversationAttachments(ConversationGuard guard, MessageAttachmentMapper attachments,
            MessageMapper messages, MediaGateway media, CurrentCaller caller) {
        this.guard = guard;
        this.attachments = attachments;
        this.messages = messages;
        this.media = media;
        this.caller = caller;
    }

    /** Stores the file and returns its raw media id -- upload-then-attach, see {@code PostMessageRequest}. */
    public long upload(long conversationId, MultipartFile file) {
        long userId = caller.requireUserId();
        guard.requireActiveParticipant(conversationId, userId);
        try (InputStream in = file.getInputStream()) {
            var result = media.upload(in, file.getOriginalFilename(), file.getContentType(), file.getSize(),
                    MediaCategory.MESSAGE_ATTACHMENT, null,
                    new MediaOwnerRef(MediaOwnerKind.MESSAGE, conversationId), userId);
            return result.mediaId();
        }
        catch (IOException e) {
            throw new UncheckedIOException("failed to read upload", e);
        }
    }

    /**
     * A short-lived signed URL to view a PROTECTED attachment inline. The attachment
     * must belong to a message actually posted in {@code conversationId} -- knowing an
     * attachment id from one conversation must never unlock a read in another.
     */
    public AttachmentAccessResponse access(long conversationId, long attachmentId, boolean download, MediaAccessLogContext ctx) {
        guard.requireActiveParticipant(conversationId, caller.requireUserId());
        MessageAttachment attachment = attachments.findById(attachmentId);
        if (attachment == null) {
            throw new NadNotFoundException("attachment not found");
        }
        Message owner = messages.findById(attachment.getMessageId());
        if (owner == null || owner.getConversationId() != conversationId) {
            throw new NadNotFoundException("attachment not found");
        }
        var signed = download
                ? media.issueSignedUrl(attachment.getMediaAssetId(), ctx)
                : media.issueInlineSignedUrl(attachment.getMediaAssetId(), ctx);
        var asset = media.find(attachment.getMediaAssetId()).orElse(null);
        return new AttachmentAccessResponse(
                signed.url(), signed.expiresAt().toString(),
                asset == null ? null : asset.originalFilename(),
                asset == null ? null : asset.contentType());
    }

    /** Every attachment on a new message must be a message attachment this caller uploaded to this conversation. */
    public void requireOwned(List<Long> mediaIds, long conversationId, long userId) {
        for (Long mediaId : mediaIds) {
            StoredAsset asset = media.find(mediaId)
                    .orElseThrow(() -> new NadNotFoundException("media asset " + mediaId + " not found"));
            if (asset.category() != MediaCategory.MESSAGE_ATTACHMENT) {
                throw new NadBadRequestException("media asset " + mediaId + " is not a message attachment");
            }
            if (asset.owner() == null
                    || asset.owner().kind() != MediaOwnerKind.MESSAGE
                    || !Objects.equals(asset.owner().id(), conversationId)
                    || asset.uploadedBy() != userId) {
                throw new NadForbiddenException("media asset " + mediaId + " does not belong to this conversation");
            }
        }
    }
}
