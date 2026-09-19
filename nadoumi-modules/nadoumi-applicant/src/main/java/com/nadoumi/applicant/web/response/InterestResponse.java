package com.nadoumi.applicant.web.response;

import com.nadoumi.applicant.domain.ApplicantInterest;
import java.util.List;

public record InterestResponse(
        String desiredLevel,
        List<String> fields,
        List<String> cities,
        String scholarshipInterest,
        Integer intakeYear,
        String intakeTerm,
        String teachingLanguage,
        String notes) {

    public static InterestResponse of(ApplicantInterest i) {
        return new InterestResponse(i.getDesiredLevel().name(), i.getFields(), i.getCities(),
                i.getScholarshipInterest() == null ? null : i.getScholarshipInterest().name(),
                i.getIntakeYear(), i.getIntakeTerm() == null ? null : i.getIntakeTerm().name(),
                i.getTeachingLanguage(), i.getNotes());
    }
}
