package com.nadoumi.notification.domain;

import com.nadoumi.common.notification.NotificationChannelKind;
import java.util.Set;

/**
 * A kind of notification. Each type declares whether it is <b>transactional</b>
 * (a preference cannot switch it off — security, decisions, document rejections)
 * and which channels <i>besides</i> {@code IN_APP} it fans out to by default.
 * {@code IN_APP} is always produced.
 *
 * <p>The name matches the {@code type} column of {@code nad_notification} /
 * {@code nad_notification_template} and the {@code docs/DOMAIN_EVENTS.md} event
 * type where one maps 1:1.</p>
 */
public enum NotificationType {

    /** A public contact-form inquiry was accepted — support staff must see it. */
    CONTACT_INQUIRY_RECEIVED(true, "New contact inquiry", Set.of(NotificationChannelKind.EMAIL)),

    /** A scholarship was published — informational, preference-controllable. */
    SCHOLARSHIP_PUBLISHED(false, "Scholarship published", Set.of(NotificationChannelKind.EMAIL)),

    /** A scholarship deadline is approaching — informational, preference-controllable. */
    SCHOLARSHIP_DEADLINE_REMINDER(false, "Scholarship deadline", Set.of(NotificationChannelKind.EMAIL)),

    /** A university was published to the public catalog — informational. */
    UNIVERSITY_PUBLISHED(false, "University published", Set.of(NotificationChannelKind.EMAIL)),

    /** A programme was published to the public catalog — informational. */
    PROGRAM_PUBLISHED(false, "Programme published", Set.of(NotificationChannelKind.EMAIL)),

    /** A task changed status or assignment — the creator, assignee and admins are told. */
    TASK_PROGRESS(true, "Task update", Set.of(NotificationChannelKind.EMAIL)),

    /**
     * A student finished registration. Declares <b>no</b> secondary channel on
     * purpose: {@code IN_APP} is produced by {@code NotificationService}, and the
     * personalised welcome email (with its programme / scholarship sections) is
     * composed and sent by {@code WelcomeContentComposer}, not the generic
     * flat-string dispatch path.
     */
    WELCOME(true, "Welcome to Nadoumi", Set.of()),

    /** An application was submitted — the applicant's linked users are told. */
    APPLICATION_SUBMITTED(true, "Application submitted", Set.of(NotificationChannelKind.EMAIL)),

    /** An application's stage/status changed — the applicant's linked users are told. */
    APPLICATION_STATUS_CHANGED(true, "Application status updated", Set.of(NotificationChannelKind.EMAIL));

    private final boolean transactional;
    private final String defaultTitle;
    private final Set<NotificationChannelKind> secondaryChannels;

    NotificationType(boolean transactional, String defaultTitle,
            Set<NotificationChannelKind> secondaryChannels) {
        this.transactional = transactional;
        this.defaultTitle = defaultTitle;
        this.secondaryChannels = secondaryChannels;
    }

    public boolean isTransactional() {
        return transactional;
    }

    /** Short heading for the in-app row; the body comes from the rendered template. */
    public String defaultTitle() {
        return defaultTitle;
    }

    /** Channels other than {@code IN_APP} this type delivers to when the preference allows. */
    public Set<NotificationChannelKind> secondaryChannels() {
        return secondaryChannels;
    }
}
