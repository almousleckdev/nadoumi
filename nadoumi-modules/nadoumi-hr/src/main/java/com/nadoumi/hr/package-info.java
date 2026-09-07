/**
 * Workforce / people-ops. Owns {@code nad_employee} (HR records paired 1:1 with a
 * {@code sys_user} staff account) and {@code nad_task} / {@code nad_task_event}
 * (task assignment + an append-only progress log). Task progress emits a
 * {@code TaskProgressChanged} event through {@link com.nadoumi.common.outbox.OutboxWriter}.
 */
package com.nadoumi.hr;
