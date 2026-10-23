package com.nadoumi.applicant.domain;

import com.nadoumi.applicant.domain.enums.FamilyRelation;
import java.time.LocalDate;

/** Row of {@code nad_applicant_family_member}. */
public class ApplicantFamilyMember {

    private Long id;
    private Long applicantId;
    private FamilyRelation relation;
    private String fullName;
    private LocalDate dateOfBirth;
    private String nationality;
    private boolean deceased;
    private String note;
    private String company;
    private String position;
    private String companyAddress;
    private String phone;
    private String email;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getApplicantId() { return applicantId; }
    public void setApplicantId(Long applicantId) { this.applicantId = applicantId; }
    public FamilyRelation getRelation() { return relation; }
    public void setRelation(FamilyRelation relation) { this.relation = relation; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }
    public boolean isDeceased() { return deceased; }
    public void setDeceased(boolean deceased) { this.deceased = deceased; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public String getCompanyAddress() { return companyAddress; }
    public void setCompanyAddress(String companyAddress) { this.companyAddress = companyAddress; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
