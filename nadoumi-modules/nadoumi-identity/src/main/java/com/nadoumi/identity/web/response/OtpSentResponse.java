package com.nadoumi.identity.web.response;

/** Always {@code {"sent": true}} - the endpoint never reveals whether the email exists. */
public record OtpSentResponse(boolean sent) {
}
