package com.nadoumi.communication.web.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * {@code attachmentMediaIds} are media ids already uploaded via the conversation's
 * {@code /attachments} endpoint (upload-then-attach, since {@code nad_message_attachment.message_id}
 * is not-null and no message exists yet at upload time).
 *
 * <p>{@code body} may be blank (a photo or document sent with no caption) but never
 * {@code null}; {@link com.nadoumi.communication.service.ConversationService#post}
 * rejects the combination of a blank body and no attachments -- there is nothing to
 * post that is neither text nor a file.
 */
public record PostMessageRequest(
        @NotNull @Size(max = 4000) String body,
        @Size(max = 5) List<Long> attachmentMediaIds) {

    public List<Long> attachmentMediaIdsOrEmpty() {
        return attachmentMediaIds == null ? List.of() : attachmentMediaIds;
    }
}
