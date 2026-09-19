package com.nadoumi.applicant.web.response;

import com.nadoumi.applicant.domain.ApplicantWork;
import java.time.LocalDate;

public record WorkResponse(
        Long id,
        String employer,
        String jobTitle,
        String employmentType,
        String country,
        String city,
        LocalDate startDate,
        LocalDate endDate,
        boolean current,
        String description,
        String workVisaType,
        LocalDate workVisaExpiry) {

    public static WorkResponse of(ApplicantWork w) {
        return new WorkResponse(w.getId(), w.getEmployer(), w.getJobTitle(),
                w.getEmploymentType() == null ? null : w.getEmploymentType().name(), w.getCountry(), w.getCity(),
                w.getStartDate(), w.getEndDate(), w.isCurrent(), w.getDescription(),
                w.getWorkVisaType() == null ? null : w.getWorkVisaType().name(), w.getWorkVisaExpiry());
    }
}
