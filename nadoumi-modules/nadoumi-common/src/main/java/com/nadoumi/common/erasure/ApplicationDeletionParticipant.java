package com.nadoumi.common.erasure;

/**
 * A module's part in permanently deleting one application. The application module checks every participant first
 * ({@link #assertDeletable}), then runs them in {@code @Order} before it removes the application row, so each module
 * deletes what it owns (payments, generated documents, ...) without the application module knowing those tables.
 */
public interface ApplicationDeletionParticipant {

    /** Throws when this module's data forbids the deletion (for example a confirmed payment). Must not change anything. */
    default void assertDeletable(long applicationId) {
    }

    /** Deletes or detaches everything this module holds for the application, inside the caller's transaction. */
    void deleteFor(long applicationId);
}
