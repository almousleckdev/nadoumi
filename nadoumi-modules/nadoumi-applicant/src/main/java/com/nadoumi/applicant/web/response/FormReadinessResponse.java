package com.nadoumi.applicant.web.response;

import java.util.List;

/**
 * What the Nadoumi application form still lacks for this applicant. Codes: {@code PERSONAL_DETAILS},
 * {@code FAMILY}, {@code SUPPORTER}, {@code CHINA_EMERGENCY}, {@code SIGNATURE}. It is guidance, not a gate on submitting.
 */
public record FormReadinessResponse(List<String> missing) {

    public boolean complete() {
        return missing.isEmpty();
    }
}
