package com.nadoumi.application.web.response;

/**
 * One status change a staff member can make from the current stage. {@code decision} is the formal decision the
 * step records (for example "Eligible"), {@code blockedReason} is why it cannot be taken right now, or null.
 */
public record NextStepResponse(String code, String label, String status, String decision, String blockedReason) {
}
