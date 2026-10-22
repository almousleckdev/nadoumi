package com.nadoumi.applicant.domain;

import com.nadoumi.applicant.domain.enums.PersonRole;

/** Row of {@code nad_applicant_person}: the financial supporter or the emergency contact in China. */
public class ApplicantPerson {

    private Long id;
    private Long applicantId;
    private PersonRole role;
    private String fullName;
    private String relationship;
    private String gender;
    private String email;
    private String phone;
    private String position;
    private String company;
    private String companyAddress;
    private String homeAddress;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getApplicantId() { return applicantId; }
    public void setApplicantId(Long applicantId) { this.applicantId = applicantId; }
    public PersonRole getRole() { return role; }
    public void setRole(PersonRole role) { this.role = role; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getRelationship() { return relationship; }
    public void setRelationship(String relationship) { this.relationship = relationship; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getCompanyAddress() { return companyAddress; }
    public void setCompanyAddress(String companyAddress) { this.companyAddress = companyAddress; }
    public String getHomeAddress() { return homeAddress; }
    public void setHomeAddress(String homeAddress) { this.homeAddress = homeAddress; }
}
