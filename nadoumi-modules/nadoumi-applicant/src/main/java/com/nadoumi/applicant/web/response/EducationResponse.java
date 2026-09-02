package com.nadoumi.applicant.web.response;

import com.nadoumi.applicant.domain.ApplicantEducation;
import java.math.BigDecimal;
import java.time.LocalDate;

public record EducationResponse(
        Long id,
        String institution,
        String level,
        String field,
        BigDecimal gpa,
        BigDecimal gpaScale,
        LocalDate startDate,
        LocalDate endDate) {

    public static EducationResponse of(ApplicantEducation e) {
        return new EducationResponse(e.getId(), e.getInstitution(), e.getLevel(), e.getField(),
                e.getGpa(), e.getGpaScale(), e.getStartDate(), e.getEndDate());
    }
}
