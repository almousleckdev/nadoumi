package com.nadoumi.identity.contact;

import com.alibaba.fastjson2.JSONObject;
import com.nadoumi.common.outbox.OutboxEventTypes;
import com.nadoumi.common.outbox.OutboxWriter;
import com.nadoumi.identity.contact.mapper.ContactInquiryMapper;
import com.nadoumi.identity.web.request.ContactRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Persists a website contact message. The row is the source of truth; a
 * {@code ContactInquiryReceived} event is written to the outbox in the same
 * transaction, and the notification pipeline fans it out to support staff
 * (in-app + email). This module keeps ownership of {@code nad_contact_inquiry}
 * until {@code nadoumi-content} exists.
 */
@Service
public class ContactService {

    private static final Logger log = LoggerFactory.getLogger(ContactService.class);

    private static final String AGGREGATE_TYPE = "contact_inquiry";
    private static final String CATEGORY_FALLBACK = "GENERAL";

    private final ContactInquiryMapper mapper;
    private final OutboxWriter outboxWriter;

    public ContactService(ContactInquiryMapper mapper, OutboxWriter outboxWriter) {
        this.mapper = mapper;
        this.outboxWriter = outboxWriter;
    }

    /**
     * @return {@code true} if the message was accepted and stored, {@code false}
     *         if it was dropped as a bot submission (honeypot tripped).
     */
    @Transactional(rollbackFor = Exception.class)
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

        outboxWriter.write(AGGREGATE_TYPE, inquiry.getId(),
                OutboxEventTypes.CONTACT_INQUIRY_RECEIVED, eventPayload(inquiry));
        return true;
    }

    private static String eventPayload(ContactInquiry inquiry) {
        JSONObject payload = new JSONObject();
        payload.put("inquiryId", inquiry.getId());
        payload.put("inquiryName", inquiry.getName() == null ? inquiry.getEmail() : inquiry.getName());
        payload.put("inquiryCategory", inquiry.getCategory() == null ? CATEGORY_FALLBACK : inquiry.getCategory());
        return payload.toJSONString();
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
