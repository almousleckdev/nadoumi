package com.nadoumi.applicant.web.response;

import com.nadoumi.applicant.domain.ApplicantContact;

public record ContactResponse(Long id, String relation, String name, String email, String phone) {
    public static ContactResponse of(ApplicantContact c) {
        return new ContactResponse(c.getId(), c.getRelation() == null ? null : c.getRelation().name(),
                c.getName(), c.getEmail(), c.getPhone());
    }
}
