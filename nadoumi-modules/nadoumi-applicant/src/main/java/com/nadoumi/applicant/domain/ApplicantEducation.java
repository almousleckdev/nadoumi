package com.nadoumi.applicant.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ApplicantEducation {

    private Long id;
    private Long applicantId;
    private String institution;
    private String level;
    private String field;
    private BigDecimal gpa;
    private BigDecimal gpaScale;
    private LocalDate startDate;
    private LocalDate endDate;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getApplicantId() { return applicantId; }
    public void setApplicantId(Long applicantId) { this.applicantId = applicantId; }

    public String getInstitution() { return institution; }
    public void setInstitution(String institution) { this.institution = institution; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getField() { return field; }
    public void setField(String field) { this.field = field; }

    public BigDecimal getGpa() { return gpa; }
    public void setGpa(BigDecimal gpa) { this.gpa = gpa; }

    public BigDecimal getGpaScale() { return gpaScale; }
    public void setGpaScale(BigDecimal gpaScale) { this.gpaScale = gpaScale; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
}
