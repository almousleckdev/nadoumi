package com.nadoumi.identity.contact;

import com.nadoumi.identity.contact.mapper.ContactInquiryMapper;
import com.nadoumi.identity.service.mail.EmailMessage;
import com.nadoumi.identity.service.mail.MailSender;
import com.nadoumi.identity.service.mail.MailTemplates;
import com.nadoumi.identity.web.request.ContactRequest;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Persists a website contact message and notifies the support inbox. The row is
 * the source of truth; a mail failure is logged but does not fail the request or
 * roll back the insert (the message is not lost, and triage can still see it).
 */
@Service
public class ContactService {

    private static final Logger log = LoggerFactory.getLogger(ContactService.class);

    private final ContactInquiryMapper mapper;
    private final MailSender mail;
    private final MailTemplates templates;
    private final String supportInbox;

    public ContactService(ContactInquiryMapper mapper, MailSender mail, MailTemplates templates,
            @Value("${nadoumi.mail.supportInbox:support@nadoumi.local}") String supportInbox) {
        this.mapper = mapper;
        this.mail = mail;
        this.templates = templates;
        this.supportInbox = supportInbox;
    }

    /**
     * @return {@code true} if the message was accepted and stored, {@code false}
     *         if it was dropped as a bot submission (honeypot tripped).
     */
    @Transactional
    public boolean submit(ContactRequest req, String ipAddress, String userAgent) {
        if (StringUtils.hasText(req.website())) {
            log.debug("contact honeypot tripped from ip={}", ipAddress);
            return false;
        }

        ContactInquiry inquiry = new ContactInquiry();
        inquiry.setFirstName(req.firstName().trim());
        inquiry.setLastName(req.lastName().trim());
        inquiry.setName(req.fullName());
        inquiry.setEmail(req.email().trim());
        inquiry.setPhone(blankToNull(req.phone()));
        inquiry.setCategory(blankToNull(req.category()));
        inquiry.setSubject(blankToNull(req.subject()));
        inquiry.setMessage(req.message().trim());
        inquiry.setLocale(blankToNull(req.locale()));
        inquiry.setIpAddress(ipAddress);
        inquiry.setUserAgent(clip(userAgent, 400));
        mapper.insert(inquiry);

        notifySupport(inquiry);
        return true;
    }

    private void notifySupport(ContactInquiry inquiry) {
        String subject = inquiry.getSubject() == null ? "(no subject)" : inquiry.getSubject();
        try {
            mail.send(new EmailMessage(supportInbox, "Website enquiry: " + subject,
                    templates.render("contact-inquiry", Map.of(
                            "name", inquiry.getName() == null ? "" : inquiry.getName(),
                            "email", inquiry.getEmail(),
                            "phone", inquiry.getPhone() == null ? "—" : inquiry.getPhone(),
                            "category", inquiry.getCategory() == null ? "—" : inquiry.getCategory(),
                            "subject", subject,
                            "message", inquiry.getMessage()))));
        }
        catch (RuntimeException e) {
            log.warn("contact inquiry {} stored but the support notification failed: {}",
                    inquiry.getId(), e.toString());
        }
    }

    private static String blankToNull(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }

    private static String clip(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
