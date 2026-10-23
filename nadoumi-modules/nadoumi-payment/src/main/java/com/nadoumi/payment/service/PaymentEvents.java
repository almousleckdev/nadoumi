package com.nadoumi.payment.service;

import com.alibaba.fastjson2.JSONObject;
import com.nadoumi.common.outbox.OutboxEventTypes;
import com.nadoumi.common.outbox.OutboxWriter;
import com.nadoumi.payment.domain.ApplicationPayment;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Writes the payment domain events to the outbox, inside the caller's transaction. Payloads carry ids and
 * safe scalars only; renderers fill the notification text from them.
 */
@Component
public class PaymentEvents {

    private static final String STAFF_VERIFY_PERMISSION = "nad:payment:verify";
    private static final String AGGREGATE = "application";

    private final OutboxWriter outbox;
    private final ApplicantRecipients recipients;

    public PaymentEvents(OutboxWriter outbox, ApplicantRecipients recipients) {
        this.outbox = outbox;
        this.recipients = recipients;
    }

    /** Tells the staff who verify payments that a proof is waiting. The student who submitted it is not told. */
    public void proofSubmitted(ApplicationPayment payment, long actorUserId) {
        JSONObject payload = base(payment);
        payload.put("audiencePermission", STAFF_VERIFY_PERMISSION);
        payload.put("actorUserId", actorUserId);
        outbox.write(AGGREGATE, payment.getApplicationId(), OutboxEventTypes.PAYMENT_PROOF_SUBMITTED,
                payload.toJSONString());
    }

    public void confirmed(ApplicationPayment payment) {
        toApplicant(payment, OutboxEventTypes.PAYMENT_CONFIRMED, null);
    }

    public void rejected(ApplicationPayment payment, String reason) {
        toApplicant(payment, OutboxEventTypes.PAYMENT_REJECTED, reason);
    }

    private void toApplicant(ApplicationPayment payment, String type, String reason) {
        List<Long> users = recipients.userIdsOf(payment.getApplicantId());
        if (users.isEmpty()) {
            return;
        }
        JSONObject payload = base(payment);
        payload.put("recipientUserIds", users);
        if (reason != null) {
            payload.put("reason", reason);
        }
        outbox.write(AGGREGATE, payment.getApplicationId(), type, payload.toJSONString());
    }

    private static JSONObject base(ApplicationPayment payment) {
        JSONObject payload = new JSONObject();
        payload.put("applicationId", payment.getApplicationId());
        payload.put("applicationRef", payment.getApplicationReference());
        payload.put("paymentId", payment.getId());
        payload.put("reference", payment.getReference());
        payload.put("amount", payment.getAmount().toPlainString() + " " + payment.getCurrency());
        return payload;
    }
}
