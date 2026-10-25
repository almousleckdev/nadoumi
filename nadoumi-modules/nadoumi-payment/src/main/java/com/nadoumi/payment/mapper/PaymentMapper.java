package com.nadoumi.payment.mapper;

import com.nadoumi.payment.domain.ApplicationPayment;
import com.nadoumi.payment.domain.enums.PaymentStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis mapper for {@code nad_application_payment}. Every state change is one conditional update
 * ({@code where status = ? and version = ?}); a result of 0 rows means someone else changed the row first.
 */
public interface PaymentMapper {

    int insert(ApplicationPayment payment);

    ApplicationPayment findById(@Param("id") long id);

    ApplicationPayment findOpen(@Param("applicationId") long applicationId, @Param("kind") String kind);

    ApplicationPayment findLatest(@Param("applicationId") long applicationId, @Param("kind") String kind);

    /** The raw {@code sys_config} value for a key (read-only), or null when there is no such row. */
    String findConfigValue(@Param("key") String key);

    /** Payment sessions opened for the application since {@code since}: caps start-cancel loops. */
    int countSessionsSince(@Param("applicationId") long applicationId, @Param("kind") String kind,
            @Param("since") LocalDateTime since);

    /** Rows (any status) that carry this transaction number on a different application. */
    int countTxnOnOtherApplication(@Param("txnNo") String txnNo, @Param("applicationId") long applicationId);

    int countPaidOrReported(@Param("applicationId") long applicationId, @Param("kind") String kind);

    int extendExpiry(@Param("id") long id, @Param("expiresAt") java.time.LocalDateTime expiresAt,
            @Param("now") java.time.LocalDateTime now, @Param("by") String by);

    int countConfirmed(@Param("applicationId") long applicationId, @Param("kind") String kind);

    List<ApplicationPayment> search(@Param("status") PaymentStatus status, @Param("q") String q);

    int markProofSubmitted(@Param("id") long id, @Param("version") int version, @Param("txnNo") String txnNo,
            @Param("proofMediaId") long proofMediaId, @Param("now") LocalDateTime now, @Param("by") String by);

    int markConfirmed(@Param("id") long id, @Param("version") int version, @Param("verifiedBy") long verifiedBy,
            @Param("now") LocalDateTime now);

    int markRejected(@Param("id") long id, @Param("version") int version, @Param("verifiedBy") long verifiedBy,
            @Param("reason") String reason, @Param("now") LocalDateTime now);

    int markCancelled(@Param("id") long id, @Param("version") int version, @Param("now") LocalDateTime now,
            @Param("by") String by);

    /** Moves one overdue {@code PENDING} payment to {@code EXPIRED}; 0 when it is no longer overdue and pending. */
    int markExpired(@Param("id") long id, @Param("now") LocalDateTime now);

    List<Long> findOverdueIds(@Param("now") LocalDateTime now);

    List<Long> findProofMediaIdsByApplicant(@Param("applicantId") long applicantId);

    int deleteByApplicantId(@Param("applicantId") long applicantId);

    List<Long> findProofMediaIdsByApplication(@Param("applicationId") long applicationId);

    int deleteByApplicationId(@Param("applicationId") long applicationId);

    int countConfirmedAny(@Param("applicationId") long applicationId);

    int deleteById(@Param("id") long id);

    int insertEvent(@Param("paymentId") long paymentId, @Param("from") PaymentStatus from,
            @Param("to") PaymentStatus to, @Param("actorKind") String actorKind, @Param("actorId") Long actorId,
            @Param("note") String note, @Param("at") LocalDateTime at);
}
