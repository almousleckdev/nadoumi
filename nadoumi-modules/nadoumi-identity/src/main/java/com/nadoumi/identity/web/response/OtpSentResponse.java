package com.nadoumi.identity.web.response;

/**
 * Response for {@code POST /api/student/email-otp}.
 *
 * <p>{@code sent} is <em>always</em> {@code true} — the endpoint never reveals
 * whether the address is registered. {@code throttled} is {@code true} when this
 * call fell inside the caller's own 60&nbsp;s resend cooldown, so no mail went out
 * and {@code retryAfter} holds the seconds until another request is allowed. It
 * does not vary with registration state.
 */
public record OtpSentResponse(boolean sent, boolean throttled, int retryAfter) {
}
