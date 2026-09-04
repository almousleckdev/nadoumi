package com.nadoumi.applicant.domain;

import com.nadoumi.applicant.domain.enums.ApplicantStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Row of {@code nad_applicant}. dob / nationality / passportNo are PII (SECURITY §6). */
public class Applicant {

    private Long id;
    private String givenName;
    private String familyName;
    private LocalDate dob;
    private String nationality;
    private String passportNo;
    private String email;
    private String phone;
    private Long photoMediaId;
    private ApplicantStatus status;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getGivenName() { return givenName; }
    public void setGivenName(String givenName) { this.givenName = givenName; }

    public String getFamilyName() { return familyName; }
    public void setFamilyName(String familyName) { this.familyName = familyName; }

    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }

    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }

    public String getPassportNo() { return passportNo; }
    public void setPassportNo(String passportNo) { this.passportNo = passportNo; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    /** {@code nad_applicant.photo_media_id} — a PROTECTED media asset; never a public URL. */
    public Long getPhotoMediaId() { return photoMediaId; }
    public void setPhotoMediaId(Long photoMediaId) { this.photoMediaId = photoMediaId; }

    public ApplicantStatus getStatus() { return status; }
    public void setStatus(ApplicantStatus status) { this.status = status; }

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
