package com.nadoumi.communication.web;

import com.nadoumi.common.media.MediaAccessLogContext;
import com.nadoumi.communication.service.ConversationService;
import com.nadoumi.communication.web.request.OpenConversationRequest;
import com.nadoumi.communication.web.request.PostMessageRequest;
import com.nadoumi.communication.web.response.AttachmentAccessResponse;
import com.nadoumi.communication.web.response.ConversationSummaryResponse;
import com.nadoumi.communication.web.response.MessageResponse;
import com.ruoyi.common.utils.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** The caller's own conversations. Authorization is participant-row and MESSAGE_STAFF-grant based (service layer). */
@RestController
@RequestMapping("/api/student/conversations")
public class StudentConversationController {

    private final ConversationService conversations;

    public StudentConversationController(ConversationService conversations) {
        this.conversations = conversations;
    }

    @GetMapping
    public List<ConversationSummaryResponse> mine() {
        return conversations.listForUser();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse open(@Valid @RequestBody OpenConversationRequest req) {
        return conversations.open(req);
    }

    @GetMapping("/{id}/messages")
    public List<MessageResponse> messages(@PathVariable Long id,
            @RequestParam(name = "beforeId", defaultValue = "0") long beforeId) {
        return conversations.listMessages(id, beforeId);
    }

    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse post(@PathVariable Long id, @Valid @RequestBody PostMessageRequest req) {
        return conversations.post(id, req);
    }

    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable Long id) {
        conversations.markRead(id);
    }

    @PostMapping("/{id}/attachments")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> uploadAttachment(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return Map.of("mediaId", conversations.uploadAttachment(id, file));
    }

    /** A short-lived signed URL to view/download an attachment already on one of this conversation's messages. */
    @GetMapping("/{id}/attachments/{attachmentId}")
    public ResponseEntity<AttachmentAccessResponse> attachmentAccess(@PathVariable Long id, @PathVariable Long attachmentId,
            @RequestParam(name = "json", required = false) String json, HttpServletRequest request) {
        AttachmentAccessResponse access = conversations.attachmentAccess(id, attachmentId, accessContext(request));
        String accept = request.getHeader("Accept");
        boolean wantsJson = "1".equals(json) || (accept != null && accept.contains("application/json"));
        if (wantsJson) {
            return ResponseEntity.ok(access);
        }
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(access.url())).build();
    }

    private static MediaAccessLogContext accessContext(HttpServletRequest request) {
        Long userId;
        try {
            userId = SecurityUtils.getUserId();
        }
        catch (RuntimeException e) {
            userId = null;
        }
        return new MediaAccessLogContext(userId == null ? 0L : userId, null, null, null,
                request.getRemoteAddr(), request.getHeader("User-Agent"));
    }
}
