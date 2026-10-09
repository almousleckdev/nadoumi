package com.nadoumi.communication.service;

import com.nadoumi.common.erasure.AccountRetirementParticipant;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Removes the chats of a retired student, after their support tickets (which own SUPPORT chats) are gone. */
@Component
@Order(20)
class ConversationRetirement implements AccountRetirementParticipant {

    private final ConversationService conversations;

    ConversationRetirement(ConversationService conversations) {
        this.conversations = conversations;
    }

    @Override
    public void retire(long userId) {
        conversations.deleteAllOf(userId);
    }
}
