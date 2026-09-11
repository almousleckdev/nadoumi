package com.nadoumi.common.outbox;

/**
 * Canonical domain event {@code type} strings shared by producers and the outbox
 * dispatcher. The authoritative catalogue with emit conditions and consumers is
 * {@code docs/DOMAIN_EVENTS.md}; this class is the compile-time constant so a
 * producer and its consumer cannot drift on the spelling.
 */
public final class OutboxEventTypes {

    private OutboxEventTypes() {
    }

    /** A scholarship transitioned to {@code PUBLISHED}. Aggregate: {@code scholarship}. */
    public static final String SCHOLARSHIP_PUBLISHED = "ScholarshipPublished";

    /** A published scholarship's deadline is approaching (within 10 days). Aggregate: {@code scholarship}. */
    public static final String SCHOLARSHIP_DEADLINE_REMINDER = "ScholarshipDeadlineReminder";

    /** A university transitioned to {@code PUBLISHED}. Aggregate: {@code university}. */
    public static final String UNIVERSITY_PUBLISHED = "UniversityPublished";

    /** A programme transitioned to {@code PUBLISHED}. Aggregate: {@code program}. */
    public static final String PROGRAM_PUBLISHED = "ProgramPublished";

    /** A public contact-form inquiry was accepted. Aggregate: {@code contact_inquiry}. */
    public static final String CONTACT_INQUIRY_RECEIVED = "ContactInquiryReceived";

    /** A task's status or assignment changed. Aggregate: {@code task}. */
    public static final String TASK_PROGRESS_CHANGED = "TaskProgressChanged";

    /** A student finished registration (email verified + account created). Aggregate: {@code user}. */
    public static final String STUDENT_REGISTERED = "StudentRegistered";

    /**
     * An application was submitted. Aggregate: {@code application}.
     * <p>PLANNED — no producer yet; the Application module (platform Step 6) emits this.</p>
     */
    public static final String APPLICATION_SUBMITTED = "ApplicationSubmitted";

    /**
     * An application's stage/status changed. Aggregate: {@code application}.
     * <p>PLANNED — no producer yet; the Application module (platform Step 6) emits this.</p>
     */
    public static final String APPLICATION_STATUS_CHANGED = "ApplicationStatusChanged";
}
