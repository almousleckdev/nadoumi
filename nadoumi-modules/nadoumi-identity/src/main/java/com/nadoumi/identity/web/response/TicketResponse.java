package com.nadoumi.identity.web.response;

/** A single-use handle proving an email was OTP-verified for a purpose. */
public record TicketResponse(String ticket) {
}
