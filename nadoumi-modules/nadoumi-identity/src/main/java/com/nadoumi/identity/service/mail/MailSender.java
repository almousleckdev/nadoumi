package com.nadoumi.identity.service.mail;

/**
 * Outbound transactional email. The concrete adapter is chosen at runtime by the
 * {@code nadoumi.mail.transport} property ({@code smtp} or {@code log}); no provider
 * name appears in the domain layer (spec Revision 2, D-R2-1 / §15.1).
 */
public interface MailSender {

    void send(EmailMessage message);
}
