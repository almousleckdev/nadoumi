package com.nadoumi.support.service;

import com.nadoumi.common.erasure.StudentRetirementParticipant;
import com.nadoumi.communication.service.ConversationService;
import com.nadoumi.support.domain.SupportTicket;
import com.nadoumi.support.mapper.SupportMeetingMapper;
import com.nadoumi.support.mapper.SupportTicketMapper;
import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Removes the support tickets a retired student opened, with their history, meetings and chat. */
@Component
@Order(10)
class TicketRetirement implements StudentRetirementParticipant {

    private final SupportTicketMapper tickets;
    private final SupportMeetingMapper meetings;
    private final ConversationService conversations;

    TicketRetirement(SupportTicketMapper tickets, SupportMeetingMapper meetings, ConversationService conversations) {
        this.tickets = tickets;
        this.meetings = meetings;
        this.conversations = conversations;
    }

    @Override
    public void retire(long userId) {
        List<SupportTicket> opened = tickets.findOpenedBy(userId);
        for (SupportTicket ticket : opened) {
            meetings.deleteByTicketId(ticket.getId());
            tickets.deleteById(ticket.getId());
            conversations.deleteSupportConversation(ticket.getConversationId());
        }
    }
}
