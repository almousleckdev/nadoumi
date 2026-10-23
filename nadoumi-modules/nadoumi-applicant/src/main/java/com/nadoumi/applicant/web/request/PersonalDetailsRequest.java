package com.nadoumi.applicant.web.request;

import com.nadoumi.applicant.domain.enums.MailingChoice;
import com.nadoumi.applicant.domain.enums.MaritalStatus;
import jakarta.validation.constraints.Size;

/** The extra personal details the application form asks for. Everything is optional so it can be saved in parts. */
public record PersonalDetailsRequest(
        MaritalStatus maritalStatus,
        @Size(max = 60)
        String religion,
        @Size(max = 120)
        String placeOfBirth,
        @Size(max = 60)
        String officialLanguage,
        @Size(max = 300)
        String permanentAddress,
        @Size(max = 20)
        String permanentPostcode,
        @Size(max = 20)
        String currentPostcode,
        MailingChoice mailingChoice,
        @Size(max = 300)
        String mailingAddress,
        @Size(max = 20)
        String mailingPostcode) {
}
