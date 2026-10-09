package com.nadoumi.common.erasure;

/**
 * A module's part in closing a student sign-in account that has no applicant left. Each module removes what it
 * keeps under that person (chats, tickets, comments, notifications) so no "Deleted student" ghost remains in a
 * staff screen. Runs inside the caller's transaction, before the account itself is anonymised.
 */
public interface AccountRetirementParticipant {

    void retire(long userId);
}
