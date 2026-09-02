package com.nadoumi.identity.mail;

import static org.assertj.core.api.Assertions.assertThat;

import com.nadoumi.identity.service.mail.EmailMessage;
import com.nadoumi.identity.service.mail.LoggingMailSender;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LoggingMailSenderTest {

    @Test
    void remembers_the_last_message_per_recipient_and_appends_to_the_outbox(@TempDir Path dir) throws Exception {
        Path outbox = dir.resolve("out.log");
        LoggingMailSender sender = new LoggingMailSender(outbox.toString());

        sender.send(new EmailMessage("A@x.com", "one", "b1"));
        sender.send(new EmailMessage("a@x.com", "two", "b2"));

        assertThat(sender.last("a@x.com")).get().extracting(EmailMessage::subject).isEqualTo("two");
        assertThat(sender.last("MISSING@x.com")).isEmpty();
        assertThat(Files.readAllLines(outbox)).hasSize(2);
    }
}
