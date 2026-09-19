package com.nadoumi.applicant.domain;

import com.nadoumi.applicant.domain.enums.EducationLevel;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Row of {@code nad_applicant_education}. */
public class ApplicantEducation {

    private Long id;
    private Long applicantId;
    private String institution;
    private String country;
    private String city;
    private EducationLevel level;
    private String qualification;
    private String field;
    private BigDecimal gpa;
    private BigDecimal gpaScale;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean current;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getApplicantId() { return applicantId; }
    public void setApplicantId(Long applicantId) { this.applicantId = applicantId; }

    public String getInstitution() { return institution; }
    public void setInstitution(String institution) { this.institution = institution; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public EducationLevel getLevel() { return level; }
    public void setLevel(EducationLevel level) { this.level = level; }

    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }

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

    public boolean isCurrent() { return current; }
    public void setCurrent(boolean current) { this.current = current; }

}
