package com.nadoumi.payment.mapper;

import com.nadoumi.payment.domain.ApplicationPayment;
import com.nadoumi.payment.domain.PaymentDocument;
import com.nadoumi.payment.domain.enums.DocumentKind;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis mapper for {@code nad_payment_document} and its delivery claims. The two {@code sys_user} reads are
 * read-only lookups of an email address, the same way the notification module resolves its audiences.
 */
public interface PaymentDocumentMapper {

    int insert(PaymentDocument document);

    /** Every version issued under a document number (the number is shared by the versions of one document). */
    List<PaymentDocument> findByNumber(@Param("docNumber") String docNumber);

    PaymentDocument findLatest(@Param("paymentId") long paymentId, @Param("kind") DocumentKind kind);

    List<PaymentDocument> findLatestByApplication(@Param("applicationId") long applicationId);

    /** Confirmed payments that have no receipt yet: the work queue of the documents job. */
    List<ApplicationPayment> findConfirmedWithoutReceipt(@Param("limit") int limit);

    /** Payments whose version 1 form exists but whose submitted (version 2) form does not. */
    List<ApplicationPayment> findAwaitingFinalForm(@Param("limit") int limit);

    /** Documents of this kind and version that are not fully delivered yet. */
    List<PaymentDocument> findUndelivered(@Param("kind") DocumentKind kind, @Param("version") int version,
            @Param("limit") int limit);

    /** Takes the right to send {@code documentId} to {@code userId}; a duplicate key means someone already did. */
    int claimDelivery(@Param("documentId") long documentId, @Param("userId") long userId,
            @Param("now") LocalDateTime now);

    int markSent(@Param("documentId") long documentId, @Param("userId") long userId, @Param("now") LocalDateTime now);

    int releaseClaim(@Param("documentId") long documentId, @Param("userId") long userId);

    /** When the document was last emailed to anyone, or null if never. */
    LocalDateTime lastSentAt(@Param("documentId") long documentId);

    /** Forgets every delivery of the document so it is emailed again by the next worker pass. */
    int clearDeliveries(@Param("documentId") long documentId);

    int markUndelivered(@Param("documentId") long documentId);

    /** 1 once the document was really sent to this user. */
    int countSent(@Param("documentId") long documentId, @Param("userId") long userId);

    int markDelivered(@Param("documentId") long documentId, @Param("now") LocalDateTime now);

    /** Frees a claim whose sender crashed before sending, so the delivery can be retried. */
    int deleteStaleClaim(@Param("documentId") long documentId, @Param("userId") long userId,
            @Param("cutoff") LocalDateTime cutoff);

    /** Active, non-deleted staff accounts that hold one of the roles and have an email address. */
    List<Recipient> findActiveByRoleKeys(@Param("roleKeys") Collection<String> roleKeys);

    Recipient findRecipient(@Param("userId") long userId);

    /** Media of every document generated for an applicant's payments, for erasure. */
    List<Long> findMediaIdsByApplicant(@Param("applicantId") long applicantId);

    int deleteByApplicantId(@Param("applicantId") long applicantId);

    /** A person an email can go to. */
    record Recipient(Long userId, String email, String name) {
    }
}
