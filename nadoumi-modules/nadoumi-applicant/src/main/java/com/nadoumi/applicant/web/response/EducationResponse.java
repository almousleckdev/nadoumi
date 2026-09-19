package com.nadoumi.applicant.web.response;

import com.nadoumi.applicant.domain.ApplicantEducation;
import java.math.BigDecimal;
import java.time.LocalDate;

public record EducationResponse(
        Long id,
        String institution,
        String country,
        String city,
        String level,
        String qualification,
        String field,
        BigDecimal gpa,
        BigDecimal gpaScale,
        LocalDate startDate,
        LocalDate endDate,
        boolean current) {

    public static EducationResponse of(ApplicantEducation e) {
        return new EducationResponse(e.getId(), e.getInstitution(), e.getCountry(), e.getCity(),
                e.getLevel() == null ? null : e.getLevel().name(), e.getQualification(), e.getField(),
                e.getGpa(), e.getGpaScale(), e.getStartDate(), e.getEndDate(), e.isCurrent());
    }
}
