package com.nadoumi.applicant.domain;

import com.nadoumi.applicant.domain.enums.IntakeTerm;
import com.nadoumi.applicant.domain.enums.ScholarshipInterest;
import com.nadoumi.applicant.domain.enums.StudyLevel;
import java.util.List;

/** Row of {@code nad_applicant_interest} plus its multi-select choices. */
public class ApplicantInterest {

    private Long id;
    private Long applicantId;
    private StudyLevel desiredLevel;
    private ScholarshipInterest scholarshipInterest;
    private Integer intakeYear;
    private IntakeTerm intakeTerm;
    private String teachingLanguage;
    private String notes;
    private List<String> fields;
    private List<String> cities;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getApplicantId() { return applicantId; }
    public void setApplicantId(Long applicantId) { this.applicantId = applicantId; }

    public StudyLevel getDesiredLevel() { return desiredLevel; }
    public void setDesiredLevel(StudyLevel desiredLevel) { this.desiredLevel = desiredLevel; }

    public ScholarshipInterest getScholarshipInterest() { return scholarshipInterest; }
    public void setScholarshipInterest(ScholarshipInterest scholarshipInterest) { this.scholarshipInterest = scholarshipInterest; }

    public Integer getIntakeYear() { return intakeYear; }
    public void setIntakeYear(Integer intakeYear) { this.intakeYear = intakeYear; }

    public IntakeTerm getIntakeTerm() { return intakeTerm; }
    public void setIntakeTerm(IntakeTerm intakeTerm) { this.intakeTerm = intakeTerm; }

    public String getTeachingLanguage() { return teachingLanguage; }
    public void setTeachingLanguage(String teachingLanguage) { this.teachingLanguage = teachingLanguage; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<String> getFields() { return fields; }
    public void setFields(List<String> fields) { this.fields = fields; }

    public List<String> getCities() { return cities; }
    public void setCities(List<String> cities) { this.cities = cities; }

}
