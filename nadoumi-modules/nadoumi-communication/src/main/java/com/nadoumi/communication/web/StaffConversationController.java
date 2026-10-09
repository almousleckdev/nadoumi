package com.nadoumi.communication.web;

import com.nadoumi.common.media.MediaAccessLogContext;
import com.nadoumi.communication.domain.enums.ParticipantRole;
import com.nadoumi.communication.service.ChatDirectoryService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.enums.BusinessType;
import com.nadoumi.communication.service.ConversationService;
import com.nadoumi.communication.web.request.OpenDirectRequest;
import com.nadoumi.communication.web.response.StudentSearchResult;
import com.nadoumi.communication.web.request.AddParticipantRequest;
import com.nadoumi.communication.web.request.PostMessageRequest;
import com.nadoumi.communication.web.request.StaffCreateConversationRequest;
import com.nadoumi.communication.web.response.ConversationSummaryResponse;
import com.nadoumi.communication.web.response.MessageResponse;
import com.nadoumi.communication.web.response.ParticipantResponse;
import com.ruoyi.common.utils.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import com.nadoumi.communication.web.response.AttachmentAccessResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff")
public class StaffConversationController {

    private final ConversationService conversations;
    private final ChatDirectoryService directory;

    public StaffConversationController(ConversationService conversations, ChatDirectoryService directory) {
        this.conversations = conversations;
        this.directory = directory;
    }

    @GetMapping("/conversations")
    @PreAuthorize("@ss.hasPermi('nad:conversation:participate')")
    public List<ConversationSummaryResponse> inbox(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long applicationId,
            @RequestParam(name = "page", defaultValue = "0") int page) {
        return conversations.listForStaff(q, applicationId, page);
    }

    @GetMapping("/conversations/unread-count")
    @PreAuthorize("@ss.hasPermi('nad:conversation:participate')")
    public Map<String, Long> unreadCount() {
        return Map.of("count", conversations.unreadCount());
    }

    /** Finds students to chat with by student id, application id or name; a few results at a time. */
    @GetMapping("/chat/students")
    @PreAuthorize("@ss.hasPermi('nad:conversation:participate')")
    public List<StudentSearchResult> searchStudents(@RequestParam(name = "q", defaultValue = "") String q) {
        return directory.searchStudents(q);
    }

    /** Gets or creates the staff member's private chat with one student; nothing is posted. */
    @PostMapping("/conversations/direct")
    @PreAuthorize("@ss.hasPermi('nad:conversation:participate')")
    public ConversationSummaryResponse openDirect(@Valid @RequestBody OpenDirectRequest req) {
        return conversations.openDirect(req.userId());
    }

    @PostMapping("/conversations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@ss.hasPermi('nad:conversation:participate')")
    public MessageResponse create(@Valid @RequestBody StaffCreateConversationRequest req) {
        return conversations.createByStaff(req);
    }

    @GetMapping("/conversations/{id}/messages")
    @PreAuthorize("@ss.hasPermi('nad:conversation:participate')")
    public List<MessageResponse> messages(@PathVariable Long id,
            @RequestParam(name = "beforeId", defaultValue = "0") long beforeId) {
        return conversations.listMessages(id, beforeId);
    }

    @PostMapping("/conversations/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@ss.hasPermi('nad:conversation:participate')")
    public MessageResponse post(@PathVariable Long id, @Valid @RequestBody PostMessageRequest req) {
        return conversations.post(id, req);
    }

    @PostMapping("/conversations/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@ss.hasPermi('nad:conversation:participate')")
    public void markRead(@PathVariable Long id) {
        conversations.markRead(id);
    }

    @PostMapping("/conversations/{id}/attachments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@ss.hasPermi('nad:conversation:participate')")
    public Map<String, Long> uploadAttachment(@PathVariable Long id,
            @org.springframework.web.bind.annotation.RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        return Map.of("mediaId", conversations.uploadAttachment(id, file));
    }

    @GetMapping("/conversations/{id}/attachments/{attachmentId}")
    @PreAuthorize("@ss.hasPermi('nad:conversation:participate')")
    public ResponseEntity<AttachmentAccessResponse> attachmentAccess(@PathVariable Long id, @PathVariable Long attachmentId,
            @RequestParam(name = "json", required = false) String json,
            @RequestParam(name = "download", required = false) String download, HttpServletRequest request) {
        AttachmentAccessResponse access = conversations.attachmentAccess(id, attachmentId, "1".equals(download),
                accessContext(request));
        return AttachmentRedirects.respond(access, json, request);
    }

    @GetMapping("/conversations/{id}/participants")
    @PreAuthorize("@ss.hasPermi('nad:conversation:participate')")
    public List<ParticipantResponse> participants(@PathVariable Long id) {
        return conversations.listParticipants(id);
    }

    @PostMapping("/conversations/{id}/participants")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@ss.hasPermi('nad:conversation:participant:manage')")
    public void addParticipant(@PathVariable Long id, @Valid @RequestBody AddParticipantRequest req) {
        conversations.addParticipant(id, req.userId(), ParticipantRole.valueOf(req.role()));
    }

    @DeleteMapping("/conversations/{id}/participants/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@ss.hasPermi('nad:conversation:participant:manage')")
    public void removeParticipant(@PathVariable Long id, @PathVariable Long userId) {
        conversations.removeParticipant(id, userId);
    }

    @DeleteMapping("/conversations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@ss.hasPermi('nad:conversation:participate')")
    @Log(title = "Delete conversation", businessType = BusinessType.DELETE)
    public void delete(@PathVariable Long id) {
        conversations.delete(id);
    }

    @PostMapping("/conversations/{id}/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@ss.hasPermi('nad:conversation:participate')")
    public void close(@PathVariable Long id) {
        conversations.close(id);
    }

    /**
     * Deferred until the Document domain (Step 7) exists. A real 501, not a fake
     * success, so nadoumi-web can build the button against a stable contract now
     * (docs/superpowers/specs/2026-09-20-messaging-domain-design.md §6).
     */
    @PostMapping("/messages/{id}/promote-document")
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    @PreAuthorize("@ss.hasPermi('nad:conversation:participate')")
    public ProblemDetail promoteToDocument(@PathVariable Long id) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_IMPLEMENTED);
        problem.setDetail("promote-to-document ships with the Document domain (Step 7)");
        return problem;
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
