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
    private String gender;
    private String countryOfOrigin;
    private String countryOfResidence;
    private String nativeLanguage;
    private String wechatId;
    private String whatsapp;
    private LocalDateTime emailVerifiedAt;
    private LocalDateTime onboardedAt;
    private LocalDateTime welcomedAt;
    private Long passportMediaId;
    private String passportGivenName;
    private String passportFamilyName;
    private LocalDate passportDob;
    private LocalDate passportIssueDate;
    private LocalDate passportExpiryDate;
    private String passportReadMethod;
    private boolean passportDataEdited;
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

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getCountryOfOrigin() { return countryOfOrigin; }
    public void setCountryOfOrigin(String countryOfOrigin) { this.countryOfOrigin = countryOfOrigin; }

    public String getCountryOfResidence() { return countryOfResidence; }
    public void setCountryOfResidence(String countryOfResidence) { this.countryOfResidence = countryOfResidence; }

    public String getNativeLanguage() { return nativeLanguage; }
    public void setNativeLanguage(String nativeLanguage) { this.nativeLanguage = nativeLanguage; }

    public String getWechatId() { return wechatId; }
    public void setWechatId(String wechatId) { this.wechatId = wechatId; }

    public String getWhatsapp() { return whatsapp; }
    public void setWhatsapp(String whatsapp) { this.whatsapp = whatsapp; }

    /** When the contact email was proven by a one-time code; null = unverified. */
    public LocalDateTime getEmailVerifiedAt() { return emailVerifiedAt; }
    public void setEmailVerifiedAt(LocalDateTime emailVerifiedAt) { this.emailVerifiedAt = emailVerifiedAt; }

    /** When the student saw the welcome celebration; null = not yet shown. */
    public LocalDateTime getWelcomedAt() { return welcomedAt; }
    public void setWelcomedAt(LocalDateTime welcomedAt) { this.welcomedAt = welcomedAt; }

    /** Set only by the onboarding-complete endpoint; null = onboarding not finished. */
    public LocalDateTime getOnboardedAt() { return onboardedAt; }
    public void setOnboardedAt(LocalDateTime onboardedAt) { this.onboardedAt = onboardedAt; }

    /** The scanned passport: a PROTECTED media asset, never a public URL. */
    public Long getPassportMediaId() { return passportMediaId; }
    public void setPassportMediaId(Long passportMediaId) { this.passportMediaId = passportMediaId; }

    /** Holder data as read from, or typed off, the passport (compared with the profile). */
    public String getPassportGivenName() { return passportGivenName; }
    public void setPassportGivenName(String passportGivenName) { this.passportGivenName = passportGivenName; }

    public String getPassportFamilyName() { return passportFamilyName; }
    public void setPassportFamilyName(String passportFamilyName) { this.passportFamilyName = passportFamilyName; }

    public LocalDate getPassportDob() { return passportDob; }
    public void setPassportDob(LocalDate passportDob) { this.passportDob = passportDob; }

    public LocalDate getPassportIssueDate() { return passportIssueDate; }
    public void setPassportIssueDate(LocalDate passportIssueDate) { this.passportIssueDate = passportIssueDate; }

    public LocalDate getPassportExpiryDate() { return passportExpiryDate; }
    public void setPassportExpiryDate(LocalDate passportExpiryDate) { this.passportExpiryDate = passportExpiryDate; }

    /** {@code MRZ} (read in the browser) or {@code MANUAL}. */
    public String getPassportReadMethod() { return passportReadMethod; }
    public void setPassportReadMethod(String passportReadMethod) { this.passportReadMethod = passportReadMethod; }

    /** True when the student changed what the reader produced. */
    public boolean isPassportDataEdited() { return passportDataEdited; }
    public void setPassportDataEdited(boolean passportDataEdited) { this.passportDataEdited = passportDataEdited; }

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
