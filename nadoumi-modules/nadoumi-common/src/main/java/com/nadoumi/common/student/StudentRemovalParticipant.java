package com.nadoumi.common.student;

import java.util.List;

/**
 * A module's say in deleting a student account. The identity module owns the account and calls every
 * participant: {@link #beforeRemove} may veto by throwing, {@link #afterRemove} cleans up the module's own
 * data in the same transaction. This keeps identity from knowing about applications or applicants.
 */
public interface StudentRemovalParticipant {

    /**
     * @param userId       the student account being deleted
     * @param applicantIds every applicant the account can currently reach
     * @param ownedIds     the subset the account owns
     */
    record Subject(long userId, List<Long> applicantIds, List<Long> ownedIds) {
    }

    /** Throw a {@code NadBadRequestException} to refuse the deletion; must not change any data. */
    default void beforeRemove(Subject subject) {
    }

    default void afterRemove(Subject subject) {
    }
}
