package com.nadoumi.notification.job;

import com.nadoumi.notification.dispatch.NotificationDeliveryDispatcher;

/**
 * Quartz entry point for channel delivery. Invoked by bean name from a
 * {@code sys_job} row ({@code invoke_target = 'notificationDispatchJob.run()'},
 * seeded active by V34); the bean is wired in {@code NotificationAutoConfiguration}.
 * Delegates straight to {@link NotificationDeliveryDispatcher} — the same query
 * serves fresh PENDING rows and backed-off retries.
 */
public class NotificationDispatchJob {

    private final NotificationDeliveryDispatcher dispatcher;

    public NotificationDispatchJob(NotificationDeliveryDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public void run() {
        dispatcher.run();
    }
}
