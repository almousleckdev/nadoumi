package com.nadoumi.applicant.domain;

import com.nadoumi.applicant.domain.enums.ChinaVisaType;
import com.nadoumi.applicant.domain.enums.EducationLevel;
import java.time.LocalDate;

/** Row of {@code nad_applicant_residence}: where the applicant is now. */
public class ApplicantResidence {

    private Long id;
    private Long applicantId;
    private boolean inChina;
    private String country;
    private String city;
    private String address;
    private EducationLevel chinaEducationLevel;
    private String chinaSchool;
    private ChinaVisaType visaType;
    private LocalDate visaExpiryDate;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getApplicantId() { return applicantId; }
    public void setApplicantId(Long applicantId) { this.applicantId = applicantId; }

    public boolean isInChina() { return inChina; }
    public void setInChina(boolean inChina) { this.inChina = inChina; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public EducationLevel getChinaEducationLevel() { return chinaEducationLevel; }
    public void setChinaEducationLevel(EducationLevel chinaEducationLevel) { this.chinaEducationLevel = chinaEducationLevel; }

    public String getChinaSchool() { return chinaSchool; }
    public void setChinaSchool(String chinaSchool) { this.chinaSchool = chinaSchool; }

    public ChinaVisaType getVisaType() { return visaType; }
    public void setVisaType(ChinaVisaType visaType) { this.visaType = visaType; }

    public LocalDate getVisaExpiryDate() { return visaExpiryDate; }
    public void setVisaExpiryDate(LocalDate visaExpiryDate) { this.visaExpiryDate = visaExpiryDate; }

}
