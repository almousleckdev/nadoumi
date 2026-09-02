package com.nadoumi.applicant.web.response;

import com.nadoumi.applicant.domain.ApplicantTestScore;
import java.time.LocalDate;

public record TestScoreResponse(
        Long id,
        String testType,
        String score,
        String subScoresJson,
        LocalDate takenOn,
        LocalDate expiresOn) {

    public static TestScoreResponse of(ApplicantTestScore s) {
        return new TestScoreResponse(
                s.getId(), s.getTestType(),
                s.getScore(), s.getSubScoresJson(),
                s.getTakenOn(), s.getExpiresOn());
    }
}
