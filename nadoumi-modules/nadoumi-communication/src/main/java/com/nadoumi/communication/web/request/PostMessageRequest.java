package com.nadoumi.communication.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * {@code attachmentMediaIds} are media ids already uploaded via the conversation's
 * {@code /attachments} endpoint (upload-then-attach, since {@code nad_message_attachment.message_id}
 * is not-null and no message exists yet at upload time). Never blank, never null.
 */
public record PostMessageRequest(
        @NotBlank @Size(max = 4000) String body,
        List<Long> attachmentMediaIds) {

    public List<Long> attachmentMediaIdsOrEmpty() {
        return attachmentMediaIds == null ? List.of() : attachmentMediaIds;
    }
}
