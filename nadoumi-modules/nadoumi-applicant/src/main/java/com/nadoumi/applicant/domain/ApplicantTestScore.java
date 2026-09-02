package com.nadoumi.applicant.domain;

import java.time.LocalDate;

public class ApplicantTestScore {

    private Long id;
    private Long applicantId;
    private String testType;
    private String score;
    private String subScoresJson;
    private LocalDate takenOn;
    private LocalDate expiresOn;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getApplicantId() { return applicantId; }
    public void setApplicantId(Long applicantId) { this.applicantId = applicantId; }

    public String getTestType() { return testType; }
    public void setTestType(String testType) { this.testType = testType; }

    public String getScore() { return score; }
    public void setScore(String score) { this.score = score; }

    public String getSubScoresJson() { return subScoresJson; }
    public void setSubScoresJson(String subScoresJson) { this.subScoresJson = subScoresJson; }

    public LocalDate getTakenOn() { return takenOn; }
    public void setTakenOn(LocalDate takenOn) { this.takenOn = takenOn; }

    public LocalDate getExpiresOn() { return expiresOn; }
    public void setExpiresOn(LocalDate expiresOn) { this.expiresOn = expiresOn; }
}
