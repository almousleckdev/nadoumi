package com.nadoumi.payment.domain;

import com.nadoumi.payment.domain.enums.DocumentKind;
import java.time.LocalDateTime;

/** Row of {@code nad_payment_document}: one rendered PDF (the bytes are a media asset). UTC timestamps. */
public class PaymentDocument {

    private Long id;
    private Long paymentId;
    private Long applicationId;
    private DocumentKind kind;
    private int version;
    private String docNumber;
    private String verifyCode;
    private Long mediaId;
    private String sha256;
    private int byteSize;
    private LocalDateTime generatedAt;
    private LocalDateTime deliveredAt;
    private String createBy;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }
    public DocumentKind getKind() { return kind; }
    public void setKind(DocumentKind kind) { this.kind = kind; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public String getDocNumber() { return docNumber; }
    public void setDocNumber(String docNumber) { this.docNumber = docNumber; }
    public String getVerifyCode() { return verifyCode; }
    public void setVerifyCode(String verifyCode) { this.verifyCode = verifyCode; }
    public Long getMediaId() { return mediaId; }
    public void setMediaId(Long mediaId) { this.mediaId = mediaId; }
    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }
    public int getByteSize() { return byteSize; }
    public void setByteSize(int byteSize) { this.byteSize = byteSize; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
