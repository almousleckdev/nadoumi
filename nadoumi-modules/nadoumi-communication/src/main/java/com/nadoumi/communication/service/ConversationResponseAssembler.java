package com.nadoumi.communication.service;

import com.nadoumi.common.media.MediaGateway;
import com.nadoumi.communication.domain.Message;
import com.nadoumi.communication.domain.MessageAttachment;
import com.nadoumi.communication.mapper.InboxRow;
import com.nadoumi.communication.mapper.MessageAttachmentMapper;
import com.nadoumi.communication.stream.PresenceService;
import com.nadoumi.communication.web.response.AttachmentResponse;
import com.nadoumi.communication.web.response.ChatPerson;
import com.nadoumi.communication.web.response.ConversationSummaryResponse;
import com.nadoumi.communication.web.response.MessageResponse;
import com.nadoumi.identity.profile.PublicProfile;
import com.nadoumi.identity.profile.PublicProfileService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Turns rows into API responses with a fixed number of queries however long the list is: one for every
 * message's attachments and one for every distinct person's profile. Nothing here talks to the media provider.
 */
@Component
public class ConversationResponseAssembler {

    private static final String UNKNOWN_PERSON = "Someone";
    private static final String IMAGE_PREFIX = "image/";

    private final MessageAttachmentMapper attachments;
    private final PublicProfileService profiles;
    private final MediaGateway media;
    private final PresenceService presence;

    public ConversationResponseAssembler(MessageAttachmentMapper attachments, PublicProfileService profiles,
            MediaGateway media, PresenceService presence) {
        this.attachments = attachments;
        this.profiles = profiles;
        this.media = media;
        this.presence = presence;
    }

    public MessageResponse message(Message m) {
        return messages(List.of(m)).get(0);
    }

    public List<MessageResponse> messages(List<Message> found) {
        if (found.isEmpty()) {
            return List.of();
        }
        Map<Long, List<MessageAttachment>> byMessage = attachments
                .listByMessageIds(found.stream().map(Message::getId).toList()).stream()
                .collect(Collectors.groupingBy(MessageAttachment::getMessageId));
        Map<Long, PublicProfile> people = profiles.resolve(
                found.stream().map(Message::getSenderUserId).collect(Collectors.toSet()));
        List<MessageResponse> out = new ArrayList<>(found.size());
        for (Message m : found) {
            PublicProfile sender = people.get(m.getSenderUserId());
            out.add(new MessageResponse(m.getId(), m.getConversationId(), m.getSenderUserId(),
                    sender == null ? UNKNOWN_PERSON : sender.displayName(), m.getBody(), m.getCreatedAt(),
                    m.getEditedAt(), byMessage.getOrDefault(m.getId(), List.of()).stream().map(this::attachment).toList()));
        }
        return out;
    }

    public List<ConversationSummaryResponse> inbox(List<InboxRow> rows) {
        Set<Long> peerIds = new HashSet<>();
        rows.stream().map(InboxRow::peerUserId).filter(java.util.Objects::nonNull).forEach(peerIds::add);
        Map<Long, ChatPerson> people = people(peerIds);
        return rows.stream().map(r -> summary(r, people)).toList();
    }

    private ConversationSummaryResponse summary(InboxRow r, Map<Long, ChatPerson> people) {
        ChatPerson peer = r.peerUserId() == null ? null : people.get(r.peerUserId());
        return new ConversationSummaryResponse(r.conversationId(), r.subject(), r.applicationId(),
                r.conversationType(), r.status(), r.lastMessageId(), r.lastMessagePreview(), r.lastMessageAt(),
                r.lastSenderUserId(), r.unreadCount(), peer, r.peerDeliveredMessageId(), r.peerReadMessageId());
    }

    /** People for a set of user ids, e.g. the participants of one conversation. */
    public Map<Long, ChatPerson> people(Collection<Long> userIds) {
        Map<Long, PublicProfile> found = profiles.resolve(userIds);
        Map<Long, PresenceService.Presence> live = presence.presenceOf(userIds);
        Map<Long, ChatPerson> out = new HashMap<>();
        for (Long id : userIds) {
            PublicProfile profile = found.get(id);
            PresenceService.Presence p = live.getOrDefault(id, new PresenceService.Presence(false, null));
            out.put(id, new ChatPerson(id, profile == null ? UNKNOWN_PERSON : profile.displayName(),
                    profile == null ? null : profile.avatarUrl(), p.online(), p.lastSeenAt()));
        }
        return out;
    }

    private AttachmentResponse attachment(MessageAttachment a) {
        String filename = a.getOriginalFilename();
        String contentType = a.getContentType();
        long size = a.getByteSize() == null ? 0 : a.getByteSize();
        if (filename == null || contentType == null) {
            var asset = media.find(a.getMediaAssetId());
            if (asset.isPresent()) {
                filename = asset.get().originalFilename();
                contentType = asset.get().contentType();
                size = asset.get().byteSize();
            }
        }
        return new AttachmentResponse(a.getId(), filename, contentType, size,
                contentType != null && contentType.startsWith(IMAGE_PREFIX));
    }
}
