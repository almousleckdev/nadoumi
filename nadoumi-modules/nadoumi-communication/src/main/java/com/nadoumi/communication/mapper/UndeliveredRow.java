package com.nadoumi.communication.mapper;

/** The newest message in a conversation that a user has not yet been marked as having received. */
public record UndeliveredRow(long conversationId, long messageId, long senderUserId) {
}
