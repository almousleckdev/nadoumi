package com.nadoumi.applicant.domain;

import com.nadoumi.applicant.domain.enums.MaritalStatus;

/** Row of {@code nad_applicant_personal}. */
public class ApplicantPersonal {

    private Long id;
    private Long applicantId;
    private MaritalStatus maritalStatus;
    private String religion;
    private String placeOfBirth;
    private String officialLanguage;
    private String permanentAddress;
    private String permanentPostcode;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getApplicantId() { return applicantId; }
    public void setApplicantId(Long applicantId) { this.applicantId = applicantId; }
    public MaritalStatus getMaritalStatus() { return maritalStatus; }
    public void setMaritalStatus(MaritalStatus maritalStatus) { this.maritalStatus = maritalStatus; }
    public String getReligion() { return religion; }
    public void setReligion(String religion) { this.religion = religion; }
    public String getPlaceOfBirth() { return placeOfBirth; }
    public void setPlaceOfBirth(String placeOfBirth) { this.placeOfBirth = placeOfBirth; }
    public String getOfficialLanguage() { return officialLanguage; }
    public void setOfficialLanguage(String officialLanguage) { this.officialLanguage = officialLanguage; }
    public String getPermanentAddress() { return permanentAddress; }
    public void setPermanentAddress(String permanentAddress) { this.permanentAddress = permanentAddress; }
    public String getPermanentPostcode() { return permanentPostcode; }
    public void setPermanentPostcode(String permanentPostcode) { this.permanentPostcode = permanentPostcode; }
}
