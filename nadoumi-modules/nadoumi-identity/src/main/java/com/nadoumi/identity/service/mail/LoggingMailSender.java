package com.nadoumi.identity.service.mail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Development / CI / test adapter: no network. Appends each message as one JSON line
 * to {@code nadoumi.mail.logFile} and keeps the last message per recipient in memory
 * so {@code GET /api/dev/mail/latest} (and the Playwright {@code readOtp} helper) can
 * retrieve it. Active when {@code nadoumi.mail.transport} is {@code log} or unset.
 */
@Component
@ConditionalOnProperty(name = "nadoumi.mail.transport", havingValue = "log", matchIfMissing = true)
public class LoggingMailSender implements MailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingMailSender.class);

    private final ConcurrentMap<String, EmailMessage> lastByRecipient = new ConcurrentHashMap<>();
    private final Path outbox;

    public LoggingMailSender(@Value("${nadoumi.mail.logFile:./mail-outbox.log}") String logFile) {
        this.outbox = Path.of(logFile);
    }

    @Override
    public void send(EmailMessage message) {
        lastByRecipient.put(message.to().toLowerCase(), message);
        try {
            Files.writeString(outbox, toJsonLine(message) + System.lineSeparator(),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
        catch (IOException e) {
            log.warn("could not append to the mail outbox {}", outbox, e);
        }
        log.info("[LoggingMailSender] to={} subject={}", message.to(), message.subject());
    }

    public Optional<EmailMessage> last(String to) {
        return Optional.ofNullable(lastByRecipient.get(to.toLowerCase()));
    }

    private static String toJsonLine(EmailMessage m) {
        return "{\"to\":" + quote(m.to())
                + ",\"subject\":" + quote(m.subject())
                + ",\"body\":" + quote(m.body()) + "}";
    }

    private static String quote(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 2).append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    }
                    else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }
}
