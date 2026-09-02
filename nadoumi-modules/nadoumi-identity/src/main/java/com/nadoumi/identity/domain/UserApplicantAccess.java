package com.nadoumi.identity.domain;

import com.nadoumi.common.access.AccessGrantStatus;
import com.nadoumi.common.access.AccessRole;
import java.time.LocalDateTime;

/** Row of {@code nad_user_applicant_access}. One (user, applicant[, application]) grant. */
public class UserApplicantAccess {

    private Long id;
    private Long userId;
    private Long applicantId;
    private Long applicationId;
    private AccessRole accessRole;
    private AccessGrantStatus status;
    private String invitedEmail;
    private String capabilityOverridesJson;
    private boolean interim;
    private Long grantedByUserId;
    private LocalDateTime grantedAt;
    private Long revokedByUserId;
    private LocalDateTime revokedAt;
    private String revokeReason;
    private LocalDateTime expiresAt;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getApplicantId() { return applicantId; }
    public void setApplicantId(Long applicantId) { this.applicantId = applicantId; }

    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }

    public AccessRole getAccessRole() { return accessRole; }
    public void setAccessRole(AccessRole accessRole) { this.accessRole = accessRole; }

    public AccessGrantStatus getStatus() { return status; }
    public void setStatus(AccessGrantStatus status) { this.status = status; }

    public String getInvitedEmail() { return invitedEmail; }
    public void setInvitedEmail(String invitedEmail) { this.invitedEmail = invitedEmail; }

    public String getCapabilityOverridesJson() { return capabilityOverridesJson; }
    public void setCapabilityOverridesJson(String json) { this.capabilityOverridesJson = json; }

    public boolean isInterim() { return interim; }
    public void setInterim(boolean interim) { this.interim = interim; }

    public Long getGrantedByUserId() { return grantedByUserId; }
    public void setGrantedByUserId(Long grantedByUserId) { this.grantedByUserId = grantedByUserId; }

    public LocalDateTime getGrantedAt() { return grantedAt; }
    public void setGrantedAt(LocalDateTime grantedAt) { this.grantedAt = grantedAt; }

    public Long getRevokedByUserId() { return revokedByUserId; }
    public void setRevokedByUserId(Long revokedByUserId) { this.revokedByUserId = revokedByUserId; }

    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }

    public String getRevokeReason() { return revokeReason; }
    public void setRevokeReason(String revokeReason) { this.revokeReason = revokeReason; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
