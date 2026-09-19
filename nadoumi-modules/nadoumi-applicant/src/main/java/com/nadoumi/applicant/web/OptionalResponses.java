package com.nadoumi.applicant.web;

import java.util.Optional;
import org.springframework.http.ResponseEntity;

/** A single-record section the applicant has not filled in yet answers 204, not an error. */
final class OptionalResponses {

    private OptionalResponses() {
    }

    static <T> ResponseEntity<T> okOrNoContent(Optional<T> body) {
        return body.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }
}
