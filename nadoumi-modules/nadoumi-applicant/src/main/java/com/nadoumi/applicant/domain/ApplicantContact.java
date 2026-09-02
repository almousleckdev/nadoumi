package com.nadoumi.applicant.domain;

import com.nadoumi.applicant.domain.enums.ContactRelation;

public class ApplicantContact {

    private Long id;
    private Long applicantId;
    private ContactRelation relation;
    private String name;
    private String email;
    private String phone;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getApplicantId() { return applicantId; }
    public void setApplicantId(Long applicantId) { this.applicantId = applicantId; }

    public ContactRelation getRelation() { return relation; }
    public void setRelation(ContactRelation relation) { this.relation = relation; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
