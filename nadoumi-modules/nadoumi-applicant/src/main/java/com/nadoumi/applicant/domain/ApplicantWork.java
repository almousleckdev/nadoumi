package com.nadoumi.applicant.domain;

import com.nadoumi.applicant.domain.enums.ChinaVisaType;
import com.nadoumi.applicant.domain.enums.EmploymentType;
import java.time.LocalDate;

/** Row of {@code nad_applicant_work}. */
public class ApplicantWork {

    private Long id;
    private Long applicantId;
    private String employer;
    private String jobTitle;
    private EmploymentType employmentType;
    private String country;
    private String city;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean current;
    private String description;
    private ChinaVisaType workVisaType;
    private LocalDate workVisaExpiry;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getApplicantId() { return applicantId; }
    public void setApplicantId(Long applicantId) { this.applicantId = applicantId; }

    public String getEmployer() { return employer; }
    public void setEmployer(String employer) { this.employer = employer; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public EmploymentType getEmploymentType() { return employmentType; }
    public void setEmploymentType(EmploymentType employmentType) { this.employmentType = employmentType; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public boolean isCurrent() { return current; }
    public void setCurrent(boolean current) { this.current = current; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ChinaVisaType getWorkVisaType() { return workVisaType; }
    public void setWorkVisaType(ChinaVisaType workVisaType) { this.workVisaType = workVisaType; }

    public LocalDate getWorkVisaExpiry() { return workVisaExpiry; }
    public void setWorkVisaExpiry(LocalDate workVisaExpiry) { this.workVisaExpiry = workVisaExpiry; }

}
