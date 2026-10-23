package com.nadoumi.payment.domain;

import com.nadoumi.payment.domain.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Row of {@code nad_application_payment}. All timestamps are UTC. */
public class ApplicationPayment {

    private Long id;
    private Long applicationId;
    private Long applicantId;
    private String applicationReference;
    private String applicantName;
    private String kind;
    private BigDecimal amount;
    private String currency;
    private String method;
    private String reference;
    private PaymentStatus status;
    private LocalDateTime expiresAt;
    private String studentTxnNo;
    private Long proofMediaId;
    private LocalDateTime submittedAt;
    private Long verifiedBy;
    private LocalDateTime verifiedAt;
    private String rejectionReason;
    private int version;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }

    public Long getApplicantId() { return applicantId; }
    public void setApplicantId(Long applicantId) { this.applicantId = applicantId; }

    public String getApplicationReference() { return applicationReference; }
    public void setApplicationReference(String applicationReference) { this.applicationReference = applicationReference; }
    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public String getStudentTxnNo() { return studentTxnNo; }
    public void setStudentTxnNo(String studentTxnNo) { this.studentTxnNo = studentTxnNo; }

    public Long getProofMediaId() { return proofMediaId; }
    public void setProofMediaId(Long proofMediaId) { this.proofMediaId = proofMediaId; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public Long getVerifiedBy() { return verifiedBy; }
    public void setVerifiedBy(Long verifiedBy) { this.verifiedBy = verifiedBy; }

    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
