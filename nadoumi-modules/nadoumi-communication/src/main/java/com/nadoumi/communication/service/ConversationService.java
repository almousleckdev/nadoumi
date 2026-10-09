package com.nadoumi.communication.service;

import com.nadoumi.common.access.ApplicantCapability;
import com.nadoumi.common.access.NadoumiAccessService;
import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.common.exception.NadForbiddenException;
import com.nadoumi.common.exception.NadNotFoundException;
import com.nadoumi.common.media.MediaAccessLogContext;
import com.nadoumi.communication.domain.Conversation;
import com.nadoumi.communication.domain.ConversationParticipant;
import com.nadoumi.communication.domain.Message;
import com.nadoumi.communication.domain.enums.ConversationStatus;
import com.nadoumi.communication.domain.enums.ConversationType;
import com.nadoumi.communication.domain.enums.ParticipantRole;
import com.nadoumi.communication.mapper.CommunicationUserMapper;
import com.nadoumi.communication.mapper.ConversationMapper;
import com.nadoumi.communication.mapper.ConversationParticipantMapper;
import com.nadoumi.communication.mapper.MessageAttachmentMapper;
import com.nadoumi.communication.mapper.MessageMapper;
import com.nadoumi.communication.web.request.OpenConversationRequest;
import com.nadoumi.communication.web.request.PostMessageRequest;
import com.nadoumi.communication.web.request.StaffCreateConversationRequest;
import com.nadoumi.communication.web.response.AdminContactResponse;
import com.nadoumi.communication.web.response.AttachmentAccessResponse;
import com.nadoumi.communication.web.response.ConversationSummaryResponse;
import com.nadoumi.communication.web.response.MessageResponse;
import com.nadoumi.communication.web.response.ParticipantResponse;
import com.nadoumi.communication.stream.ChatEvents;
import com.nadoumi.identity.access.CurrentCaller;
import com.ruoyi.common.utils.AuditActor;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Public service other modules call to open/read/post to a conversation (e.g. a
 * future Support/Ticketing module creating a {@code SUPPORT}-typed one -- see
 * {@link ConversationType}). Every mutating method re-checks authorization itself
 * (defense in depth on top of the controllers' {@code @PreAuthorize}), per
 * {@code security.md}.
 */
@Service
public class ConversationService {

    private static final int PAGE_SIZE = 30;
    private static final int INBOX_PAGE_SIZE = 30;

    private final ConversationMapper conversations;
    private final ConversationParticipantMapper participants;
    private final MessageMapper messages;
    private final CommunicationUserMapper users;
    private final NadoumiAccessService access;
    private final CurrentCaller caller;
    private final MessagePublisher publisher;
    private final ConversationGuard guard;
    private final ConversationResponseAssembler assembler;
    private final ConversationAttachments attachmentService;
    private final ChatEvents events;
    private final MessageAttachmentMapper attachmentRows;

    public ConversationService(ConversationMapper conversations, ConversationParticipantMapper participants,
            MessageMapper messages, CommunicationUserMapper users, NadoumiAccessService access,
            CurrentCaller caller, MessagePublisher publisher, ConversationGuard guard,
            ConversationResponseAssembler assembler, ConversationAttachments attachmentService,
            ChatEvents events, MessageAttachmentMapper attachmentRows) {
        this.conversations = conversations;
        this.participants = participants;
        this.messages = messages;
        this.users = users;
        this.access = access;
        this.caller = caller;
        this.publisher = publisher;
        this.guard = guard;
        this.assembler = assembler;
        this.attachmentService = attachmentService;
        this.events = events;
        this.attachmentRows = attachmentRows;
    }

    /**
     * A student starts (or resumes) their private chat with one chosen staff member. The staff side must be an
     * active staff account; the pair shares exactly one DIRECT conversation, so repeating this call posts into it.
     */
    @Transactional(rollbackFor = Exception.class)
    public MessageResponse open(OpenConversationRequest req) {
        if (caller.isStaff()) {
            throw new AccessDeniedException("students only");
        }
        Long applicantId = req.applicantId();
        if (applicantId == null) {
            List<Long> ids = access.accessibleApplicantIds();
            if (!ids.isEmpty()) {
                applicantId = ids.get(0);
            }
        }
        if (applicantId != null && !access.canAccessApplicant(applicantId, ApplicantCapability.MESSAGE_STAFF.name())) {
            throw new NadForbiddenException("missing MESSAGE_STAFF on applicant " + applicantId);
        }
        long userId = caller.requireUserId();
        long staffUserId = resolveStaff(req.adminUserId());
        ParticipantRole role = applicantId == null ? ParticipantRole.APPLICANT : guard.participantRoleFor(applicantId);

        Conversation conversation = directConversation(staffUserId, userId, role, req.subject(), req.applicationId());
        return publishAndAnnounce(conversation.getId(), userId, req.body(), List.of());
    }

    /** Staff start (or resume) their private chat with one student. The recipient must be an active student. */
    @Transactional(rollbackFor = Exception.class)
    public MessageResponse createByStaff(StaffCreateConversationRequest req) {
        guard.requireStaff();
        long staffUserId = caller.requireUserId();
        Long studentUserId = req.studentUserId();
        if (studentUserId == null || studentUserId <= 0 || !users.isActiveStudent(studentUserId)) {
            throw new NadBadRequestException("the recipient must be an active student");
        }

        Conversation conversation = directConversation(staffUserId, studentUserId, ParticipantRole.APPLICANT,
                req.subject(), req.applicationId());
        return publishAndAnnounce(conversation.getId(), staffUserId, req.body(), List.of());
    }

    /**
     * Opens a {@code SUPPORT}-typed conversation for the calling student, with no applicant
     * profile required (a student may need help before onboarding is finished). Called by the
     * Support/Ticketing module, which owns the ticket; {@link #open} stays the applicant-scoped
     * path and still demands {@code MESSAGE_STAFF}.
     */
    @Transactional(rollbackFor = Exception.class)
    public MessageResponse openSupport(String subject, String body) {
        if (caller.isStaff()) {
            throw new AccessDeniedException("students only");
        }
        long userId = caller.requireUserId();

        Conversation conversation = newConversation(subject, null, ConversationType.SUPPORT);

        addParticipantRow(conversation.getId(), userId, ParticipantRole.APPLICANT);

        Message message = publisher.publish(conversation.getId(), userId, body, List.of());
        return assembler.message(message);
    }

    /**
     * Staff-only read of a {@code SUPPORT} thread without joining it, so a staff member with
     * ticket-view permission can inspect a pool ticket before claiming it. Refuses any other
     * conversation type (application chats stay participant-only).
     */
    @Transactional(readOnly = true)
    public List<MessageResponse> listSupportMessagesForStaff(long conversationId, long beforeId) {
        guard.requireStaff();
        Conversation conversation = conversations.findById(conversationId);
        if (conversation == null || conversation.getConversationType() != ConversationType.SUPPORT) {
            throw new NadNotFoundException("support conversation not found");
        }
        return assembler.messages(messages.listByConversation(conversationId, beforeId, PAGE_SIZE));
    }

    /** Posts a message to a conversation the caller is an active participant of. */
    @Transactional(rollbackFor = Exception.class)
    public MessageResponse post(long conversationId, PostMessageRequest req) {
        long userId = caller.requireUserId();
        Conversation conversation = guard.requireActiveParticipant(conversationId, userId);
        if (conversation.getStatus() == ConversationStatus.CLOSED) {
            throw new NadBadRequestException("this conversation is closed");
        }
        List<Long> attachmentMediaIds = req.attachmentMediaIdsOrEmpty();
        if (req.body().isBlank() && attachmentMediaIds.isEmpty()) {
            throw new NadBadRequestException("a message needs text, an attachment, or both");
        }
        attachmentService.requireOwned(attachmentMediaIds, conversationId, userId);
        return publishAndAnnounce(conversationId, userId, req.body(), attachmentMediaIds);
    }

    /** Newest-first message page (cursor by id, exclusive), for a caller who is an active participant. */
    @Transactional(readOnly = true)
    public List<MessageResponse> listMessages(long conversationId, long beforeId) {
        guard.requireActiveParticipant(conversationId, caller.requireUserId());
        return assembler.messages(messages.listByConversation(conversationId, beforeId, PAGE_SIZE));
    }

    @Transactional(rollbackFor = Exception.class)
    public void markRead(long conversationId) {
        long userId = caller.requireUserId();
        guard.requireActiveParticipant(conversationId, userId);
        Message latest = messages.findLatest(conversationId);
        if (latest != null) {
            participants.updateLastDelivered(conversationId, userId, latest.getId());
            participants.updateLastRead(conversationId, userId, latest.getId());
            events.messagesRead(conversationId, userId, latest.getId());
        }
    }

    /** Staff-only: closes a conversation. Controller-level {@code nad:conversation:participate} gates this. */
    @Transactional(rollbackFor = Exception.class)
    public void close(long conversationId) {
        guard.requireStaff();
        guard.requireActiveParticipant(conversationId, caller.requireUserId());
        conversations.updateStatus(conversationId, ConversationStatus.CLOSED.name(), AuditActor.username());
    }

    /**
     * Permanently deletes a private chat (messages, attachments and all) for both sides. Only an active staff
     * participant may do it; ticket chats belong to the support module and are refused. Attachment files are
     * released after the rows are gone, and the student's screen is told to drop the conversation.
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(long conversationId) {
        guard.requireStaff();
        long userId = caller.requireUserId();
        Conversation conversation = guard.requireActiveParticipant(conversationId, userId);
        if (conversation.getConversationType() == ConversationType.SUPPORT) {
            throw new NadBadRequestException("a support ticket's chat cannot be deleted here");
        }
        List<Long> participantIds = participants.listActiveForConversation(conversationId).stream()
                .map(ConversationParticipant::getUserId).toList();
        List<Long> mediaIds = attachmentRows.listMediaIdsByConversation(conversationId);
        conversations.deleteById(conversationId);
        mediaIds.forEach(id -> attachmentService.release(id, userId));
        events.conversationRemoved(conversationId, userId, participantIds);
    }

    /** Staff-only, gated by {@code nad:conversation:participant:manage} at the controller. */
    @Transactional(rollbackFor = Exception.class)
    public void addParticipant(long conversationId, long userId, ParticipantRole role) {
        requireManageable(conversationId);
        if (!users.isActiveStaff(userId) && !isSupport(conversationId)) {
            throw new NadBadRequestException("only staff can be added to this conversation");
        }
        if (participants.findActive(conversationId, userId) != null) {
            return;
        }
        addParticipantRow(conversationId, userId, role);
    }

    /** Staff-only, gated by {@code nad:conversation:participant:manage} at the controller. */
    @Transactional(rollbackFor = Exception.class)
    public void removeParticipant(long conversationId, long userId) {
        requireManageable(conversationId);
        participants.remove(conversationId, userId, LocalDateTime.now());
    }

    /**
     * The caller's own conversations, newest activity first, in one query. Staff see only chats they take part
     * in: another staff member's private conversation never appears here.
     */
    @Transactional(readOnly = true)
    public List<ConversationSummaryResponse> inbox(String query, Long applicationId, int page) {
        long userId = caller.requireUserId();
        String trimmed = query == null ? null : query.trim();
        int offset = Math.max(page, 0) * INBOX_PAGE_SIZE;
        return assembler.inbox(conversations.listInbox(userId, trimmed, applicationId, null, INBOX_PAGE_SIZE, offset));
    }

    /** Total unread messages for the caller, in one query: what the navigation badge shows. */
    @Transactional(readOnly = true)
    public long unreadCount() {
        return conversations.countUnread(caller.requireUserId());
    }

    /** Staff inbox: {@link #inbox} behind a staff check (the controller's permission is not the only gate). */
    @Transactional(readOnly = true)
    public List<ConversationSummaryResponse> listForStaff(String query, Long applicationId, int page) {
        guard.requireStaff();
        return inbox(query, applicationId, page);
    }

    /** Staff-only: the caller's own conversations attached to one application. */
    @Transactional(readOnly = true)
    public List<ConversationSummaryResponse> listForApplication(long applicationId) {
        guard.requireStaff();
        return inbox(null, applicationId, 0);
    }

    @Transactional(readOnly = true)
    public List<ParticipantResponse> listParticipants(long conversationId) {
        guard.requireActiveParticipant(conversationId, caller.requireUserId());
        return participants.listActiveForConversation(conversationId).stream()
                .map(p -> new ParticipantResponse(p.getUserId(), p.getRole().name(), p.getAddedAt()))
                .toList();
    }

    /** Stores the file and returns its raw media id -- upload-then-attach, see {@code PostMessageRequest}. */
    public long uploadAttachment(long conversationId, MultipartFile file) {
        return attachmentService.upload(conversationId, file);
    }

    /** A short-lived signed URL to view (or, with {@code download}, save) a PROTECTED attachment (see {@link ConversationAttachments#access}). */
    public AttachmentAccessResponse attachmentAccess(long conversationId, long attachmentId, boolean download,
            MediaAccessLogContext ctx) {
        return attachmentService.access(conversationId, attachmentId, download, ctx);
    }

    /**
     * Gets or creates the caller's private chat with {@code otherUserId} without posting anything. A staff caller
     * can only reach an active student, and a student caller only a chat-eligible staff member.
     */
    @Transactional(rollbackFor = Exception.class)
    public ConversationSummaryResponse openDirect(long otherUserId) {
        long userId = caller.requireUserId();
        if (userId == otherUserId) {
            throw new NadBadRequestException("you cannot chat with yourself");
        }
        Conversation conversation;
        if (caller.isStaff()) {
            if (!users.isActiveStudent(otherUserId)) {
                throw new NadBadRequestException("the recipient must be an active student");
            }
            conversation = directConversation(userId, otherUserId, ParticipantRole.APPLICANT, null, null);
        }
        else {
            if (!users.isActiveStaff(otherUserId)) {
                throw new NadBadRequestException("the chosen advisor is not an active staff member");
            }
            conversation = directConversation(otherUserId, userId, ParticipantRole.APPLICANT, null, null);
        }
        return assembler.inbox(conversations.listInbox(userId, null, null, conversation.getId(), 1, 0)).get(0);
    }

    // ---- internals ----

    /** Stores the message, then tells the other participants once the transaction commits. */
    private MessageResponse publishAndAnnounce(long conversationId, long senderUserId, String body, List<Long> mediaIds) {
        MessageResponse response = assembler.message(publisher.publish(conversationId, senderUserId, body, mediaIds));
        events.messagePosted(response);
        return response;
    }

    /** Participant changes on a private chat are made by someone already in it; SUPPORT pools stay staff-managed. */
    private void requireManageable(long conversationId) {
        guard.requireStaff();
        Conversation conversation = conversations.findById(conversationId);
        if (conversation == null) {
            throw new NadNotFoundException("conversation not found");
        }
        if (conversation.getConversationType() != ConversationType.SUPPORT) {
            guard.requireActiveParticipant(conversationId, caller.requireUserId());
        }
    }

    private boolean isSupport(long conversationId) {
        Conversation conversation = conversations.findById(conversationId);
        return conversation != null && conversation.getConversationType() == ConversationType.SUPPORT;
    }

    private long resolveStaff(Long requestedStaffUserId) {
        if (requestedStaffUserId != null && requestedStaffUserId > 0) {
            if (!users.isActiveStaff(requestedStaffUserId)) {
                throw new NadBadRequestException("the chosen advisor is not an active staff member");
            }
            return requestedStaffUserId;
        }
        List<AdminContactResponse> admins = users.listStaffAdmins();
        if (admins.isEmpty()) {
            throw new NadBadRequestException("no advisor is available right now");
        }
        return admins.get(0).userId();
    }

    /** The single DIRECT conversation of a staff/student pair: created on first contact, reopened if it was closed. */
    private Conversation directConversation(long staffUserId, long studentUserId, ParticipantRole studentRole,
            String subject, Long applicationId) {
        String key = staffUserId + ":" + studentUserId;
        Conversation existing = conversations.findByDirectKey(key);
        if (existing != null) {
            if (existing.getStatus() == ConversationStatus.CLOSED) {
                conversations.updateStatus(existing.getId(), ConversationStatus.OPEN.name(), AuditActor.username());
            }
            return existing;
        }
        Conversation created = new Conversation();
        created.setSubject(subject);
        created.setApplicationId(applicationId);
        created.setConversationType(ConversationType.DIRECT);
        created.setStatus(ConversationStatus.OPEN);
        created.setDirectKey(key);
        created.setCreateBy(AuditActor.username());
        try {
            conversations.insert(created);
        }
        catch (DuplicateKeyException raced) {
            return conversations.findByDirectKey(key);
        }
        addParticipantRow(created.getId(), staffUserId, ParticipantRole.STAFF);
        addParticipantRow(created.getId(), studentUserId, studentRole);
        return created;
    }

    private Conversation newConversation(String subject, Long applicationId, ConversationType type) {
        Conversation conversation = new Conversation();
        conversation.setSubject(subject);
        conversation.setApplicationId(applicationId);
        conversation.setConversationType(type);
        conversation.setStatus(ConversationStatus.OPEN);
        conversation.setCreateBy(AuditActor.username());
        conversations.insert(conversation);
        return conversation;
    }

    private void addParticipantRow(long conversationId, long userId, ParticipantRole role) {
        ConversationParticipant p = new ConversationParticipant();
        p.setConversationId(conversationId);
        p.setUserId(userId);
        p.setRole(role);
        p.setAddedAt(LocalDateTime.now());
        p.setMuted(false);
        participants.insert(p);
    }
}
