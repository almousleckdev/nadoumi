package com.nadoumi.communication.service;

import com.nadoumi.common.access.AccessRole;
import com.nadoumi.common.access.ApplicantCapability;
import com.nadoumi.common.access.NadoumiAccessService;
import com.nadoumi.common.exception.NadForbiddenException;
import com.nadoumi.common.exception.NadNotFoundException;
import com.nadoumi.common.media.MediaGateway;
import com.nadoumi.common.media.MediaOwnerKind;
import com.nadoumi.common.media.MediaOwnerRef;
import com.nadoumi.communication.domain.Conversation;
import com.nadoumi.communication.domain.ConversationParticipant;
import com.nadoumi.communication.domain.Message;
import com.nadoumi.communication.domain.MessageAttachment;
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
import com.nadoumi.communication.web.response.AttachmentResponse;
import com.nadoumi.communication.web.response.ConversationSummaryResponse;
import com.nadoumi.communication.web.response.MessageResponse;
import com.nadoumi.communication.web.response.ParticipantResponse;
import com.nadoumi.identity.access.CurrentCaller;
import com.nadoumi.identity.domain.UserApplicantAccess;
import com.nadoumi.identity.mapper.UserApplicantAccessMapper;
import com.ruoyi.common.utils.AuditActor;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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

    private static final int PREVIEW_LENGTH = 140;
    private static final int PAGE_SIZE = 50;

    private final ConversationMapper conversations;
    private final ConversationParticipantMapper participants;
    private final MessageMapper messages;
    private final MessageAttachmentMapper attachments;
    private final CommunicationUserMapper users;
    private final NadoumiAccessService access;
    private final CurrentCaller caller;
    private final UserApplicantAccessMapper grants;
    private final MessagePublisher publisher;
    private final MediaGateway media;

    public ConversationService(ConversationMapper conversations, ConversationParticipantMapper participants,
            MessageMapper messages, MessageAttachmentMapper attachments, CommunicationUserMapper users,
            NadoumiAccessService access, CurrentCaller caller, UserApplicantAccessMapper grants,
            MessagePublisher publisher, MediaGateway media) {
        this.conversations = conversations;
        this.participants = participants;
        this.messages = messages;
        this.attachments = attachments;
        this.users = users;
        this.access = access;
        this.caller = caller;
        this.grants = grants;
        this.publisher = publisher;
        this.media = media;
    }

    /** Opens a conversation for the caller's applicant, adds them as the first participant, posts the first message. */
    @Transactional(rollbackFor = Exception.class)
    public MessageResponse open(OpenConversationRequest req) {
        if (!access.canAccessApplicant(req.applicantId(), ApplicantCapability.MESSAGE_STAFF.name())) {
            throw new NadForbiddenException("missing MESSAGE_STAFF on applicant " + req.applicantId());
        }
        long userId = caller.requireUserId();

        Conversation conversation = new Conversation();
        conversation.setSubject(req.subject());
        conversation.setApplicationId(req.applicationId());
        conversation.setConversationType(ConversationType.GENERAL);
        conversation.setStatus(ConversationStatus.OPEN);
        conversation.setCreateBy(AuditActor.username());
        conversations.insert(conversation);

        addParticipantRow(conversation.getId(), userId, participantRoleFor(req.applicantId()));

        Message message = publisher.publish(conversation.getId(), userId, req.body(), List.of());
        return toMessageResponse(message);
    }

    /** Posts a message to a conversation the caller is an active participant of. */
    @Transactional(rollbackFor = Exception.class)
    public MessageResponse post(long conversationId, PostMessageRequest req) {
        requireActiveParticipant(conversationId, caller.requireUserId());
        Message message = publisher.publish(conversationId, caller.requireUserId(), req.body(),
                req.attachmentMediaIdsOrEmpty());
        return toMessageResponse(message);
    }

    /** Newest-first message page (cursor by id, exclusive), for a caller who is an active participant. */
    @Transactional(readOnly = true)
    public List<MessageResponse> listMessages(long conversationId, long beforeId) {
        requireActiveParticipant(conversationId, caller.requireUserId());
        return messages.listByConversation(conversationId, beforeId, PAGE_SIZE).stream()
                .map(this::toMessageResponse)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void markRead(long conversationId) {
        long userId = caller.requireUserId();
        requireActiveParticipant(conversationId, userId);
        Message latest = messages.findLatest(conversationId);
        if (latest != null) {
            participants.updateLastRead(conversationId, userId, latest.getId());
        }
    }

    /** Staff-only: closes a conversation. Controller-level {@code nad:conversation:participate} gates this. */
    @Transactional(rollbackFor = Exception.class)
    public void close(long conversationId) {
        requireStaff();
        requireActiveParticipant(conversationId, caller.requireUserId());
        conversations.updateStatus(conversationId, ConversationStatus.CLOSED.name(), AuditActor.username());
    }

    /** Staff-only, gated by {@code nad:conversation:participant:manage} at the controller. */
    @Transactional(rollbackFor = Exception.class)
    public void addParticipant(long conversationId, long userId, ParticipantRole role) {
        requireStaff();
        if (participants.findActive(conversationId, userId) != null) {
            return;
        }
        addParticipantRow(conversationId, userId, role);
    }

    /** Staff-only, gated by {@code nad:conversation:participant:manage} at the controller. */
    @Transactional(rollbackFor = Exception.class)
    public void removeParticipant(long conversationId, long userId) {
        requireStaff();
        participants.remove(conversationId, userId, LocalDateTime.now());
    }

    /** Every conversation the current caller is an active participant of, newest activity first. */
    @Transactional(readOnly = true)
    public List<ConversationSummaryResponse> listForUser() {
        long userId = caller.requireUserId();
        List<ConversationParticipant> mine = participants.listActiveForUser(userId);
        if (mine.isEmpty()) {
            return List.of();
        }
        List<Conversation> found = conversations.findByIds(mine.stream().map(ConversationParticipant::getConversationId).toList());
        List<ConversationSummaryResponse> out = new ArrayList<>(found.size());
        for (Conversation c : found) {
            ConversationParticipant p = mine.stream().filter(m -> m.getConversationId().equals(c.getId())).findFirst().orElseThrow();
            out.add(toSummary(c, p));
        }
        return out.stream()
                .sorted(Comparator.comparing(ConversationSummaryResponse::lastMessageAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    /** Staff-only: every conversation attached to one application (public API for other modules -- see class doc). */
    @Transactional(readOnly = true)
    public List<ConversationSummaryResponse> listForApplication(long applicationId) {
        requireStaff();
        return conversations.findByApplicationId(applicationId).stream()
                .map(c -> toSummary(c, null))
                .toList();
    }

    /** Unclaimed (no active STAFF participant) OPEN conversations plus the caller's own -- the staff inbox. */
    @Transactional(readOnly = true)
    public List<ConversationSummaryResponse> listForStaff() {
        requireStaff();
        long userId = caller.requireUserId();
        List<ConversationParticipant> mine = participants.listActiveForUser(userId);
        List<Conversation> unclaimed = conversations.findUnclaimed();

        List<ConversationSummaryResponse> out = new ArrayList<>();
        if (!mine.isEmpty()) {
            List<Conversation> found = conversations.findByIds(mine.stream().map(ConversationParticipant::getConversationId).toList());
            for (Conversation c : found) {
                ConversationParticipant p = mine.stream().filter(m -> m.getConversationId().equals(c.getId())).findFirst().orElseThrow();
                out.add(toSummary(c, p));
            }
        }
        List<Long> alreadyIncluded = out.stream().map(ConversationSummaryResponse::id).toList();
        for (Conversation c : unclaimed) {
            if (!alreadyIncluded.contains(c.getId())) {
                out.add(toSummary(c, null));
            }
        }
        return out.stream()
                .sorted(Comparator.comparing(ConversationSummaryResponse::lastMessageAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public List<ParticipantResponse> listParticipants(long conversationId) {
        requireActiveParticipant(conversationId, caller.requireUserId());
        return participants.listActiveForConversation(conversationId).stream()
                .map(p -> new ParticipantResponse(p.getUserId(), p.getRole().name(), p.getAddedAt()))
                .toList();
    }

    /** Stores the file and returns its raw media id -- upload-then-attach, see {@code PostMessageRequest}. */
    public long uploadAttachment(long conversationId, MultipartFile file) {
        requireActiveParticipant(conversationId, caller.requireUserId());
        try (InputStream in = file.getInputStream()) {
            var result = media.upload(in, file.getOriginalFilename(), file.getContentType(), file.getSize(),
                    com.nadoumi.common.media.MediaCategory.MESSAGE_ATTACHMENT, null,
                    new MediaOwnerRef(MediaOwnerKind.MESSAGE, conversationId), caller.requireUserId());
            return result.mediaId();
        }
        catch (IOException e) {
            throw new UncheckedIOException("failed to read upload", e);
        }
    }

    // ---- internals ----

    private void addParticipantRow(long conversationId, long userId, ParticipantRole role) {
        ConversationParticipant p = new ConversationParticipant();
        p.setConversationId(conversationId);
        p.setUserId(userId);
        p.setRole(role);
        p.setAddedAt(LocalDateTime.now());
        p.setMuted(false);
        participants.insert(p);
    }

    private void requireActiveParticipant(long conversationId, long userId) {
        if (conversations.findById(conversationId) == null) {
            throw new NadNotFoundException("conversation not found");
        }
        if (participants.findActive(conversationId, userId) == null) {
            throw new AccessDeniedException("not a participant of conversation " + conversationId);
        }
    }

    private void requireStaff() {
        if (!caller.isStaff()) {
            throw new AccessDeniedException("staff only");
        }
    }

    /** OWNER maps to APPLICANT (the applicant themselves); AGENT/GUARDIAN pass through. Staff never reach here. */
    private ParticipantRole participantRoleFor(long applicantId) {
        if (caller.isStaff()) {
            return ParticipantRole.STAFF;
        }
        UserApplicantAccess grant = grants.findActiveApplicantGrant(caller.requireUserId(), applicantId);
        AccessRole role = grant == null ? AccessRole.OWNER : grant.getAccessRole();
        return switch (role) {
            case AGENT -> ParticipantRole.AGENT;
            case GUARDIAN -> ParticipantRole.GUARDIAN;
            case OWNER, VIEWER -> ParticipantRole.APPLICANT;
        };
    }

    private ConversationSummaryResponse toSummary(Conversation c, ConversationParticipant myParticipant) {
        Message latest = messages.findLatest(c.getId());
        long afterId = myParticipant == null || myParticipant.getLastReadMessageId() == null
                ? 0 : myParticipant.getLastReadMessageId();
        long unread = messages.countAfter(c.getId(), afterId);
        String preview = latest == null ? null : truncate(latest.getBody());
        return new ConversationSummaryResponse(c.getId(), c.getSubject(), c.getApplicationId(),
                c.getConversationType().name(), c.getStatus().name(), preview,
                latest == null ? null : latest.getCreatedAt(), unread);
    }

    private MessageResponse toMessageResponse(Message m) {
        List<AttachmentResponse> attached = attachments.listByMessage(m.getId()).stream()
                .map(this::toAttachmentResponse)
                .toList();
        String senderName = users.findDisplayName(m.getSenderUserId());
        return new MessageResponse(m.getId(), m.getConversationId(), m.getSenderUserId(), senderName,
                m.getBody(), m.getCreatedAt(), m.getEditedAt(), attached);
    }

    private AttachmentResponse toAttachmentResponse(MessageAttachment a) {
        return media.find(a.getMediaAssetId())
                .map(asset -> new AttachmentResponse(a.getId(), a.getMediaAssetId(), asset.originalFilename(),
                        asset.contentType(), asset.byteSize()))
                .orElseGet(() -> new AttachmentResponse(a.getId(), a.getMediaAssetId(), null, null, 0));
    }

    private static String truncate(String body) {
        return body.length() <= PREVIEW_LENGTH ? body : body.substring(0, PREVIEW_LENGTH) + "…";
    }
}
