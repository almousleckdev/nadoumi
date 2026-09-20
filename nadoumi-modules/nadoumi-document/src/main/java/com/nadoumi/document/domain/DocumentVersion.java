package com.nadoumi.document.domain;

import com.nadoumi.document.domain.enums.ScanStatus;
import com.nadoumi.document.domain.enums.VerificationStatus;
import java.time.LocalDateTime;

/** Row of {@code nad_document_version}. Immutable once inserted — a replace always creates a new row. */
public class DocumentVersion {

    private Long id;
    private Long documentId;
    private int versionNo;
    private Long mediaAssetId;
    private String contentType;
    private long sizeBytes;
    private String checksumSha256;
    private Long uploadedBy;
    private LocalDateTime uploadedAt;
    private VerificationStatus verificationStatus;
    private Long verifiedBy;
    private LocalDateTime verifiedAt;
    private ScanStatus scanStatus;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public int getVersionNo() { return versionNo; }
    public void setVersionNo(int versionNo) { this.versionNo = versionNo; }

    public Long getMediaAssetId() { return mediaAssetId; }
    public void setMediaAssetId(Long mediaAssetId) { this.mediaAssetId = mediaAssetId; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }

    public String getChecksumSha256() { return checksumSha256; }
    public void setChecksumSha256(String checksumSha256) { this.checksumSha256 = checksumSha256; }

    public Long getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(Long uploadedBy) { this.uploadedBy = uploadedBy; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(VerificationStatus verificationStatus) { this.verificationStatus = verificationStatus; }

    public Long getVerifiedBy() { return verifiedBy; }
    public void setVerifiedBy(Long verifiedBy) { this.verifiedBy = verifiedBy; }

    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }

    public ScanStatus getScanStatus() { return scanStatus; }
    public void setScanStatus(ScanStatus scanStatus) { this.scanStatus = scanStatus; }
}
