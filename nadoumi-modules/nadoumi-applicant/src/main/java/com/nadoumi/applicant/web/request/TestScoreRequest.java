package com.nadoumi.applicant.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record TestScoreRequest(
        @NotBlank @Size(max = 24) String testType,
        @NotBlank @Size(max = 32) String score,
        String subScoresJson,
        LocalDate takenOn,
        LocalDate expiresOn) {
}
