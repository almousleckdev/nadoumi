package com.nadoumi.application.web.response;

import com.nadoumi.application.domain.ApplicationLetter;

/**
 * An admission letter or JW202 on an application. {@code locked} is true for a student until the Nadoumi service fee
 * is confirmed; a locked letter never reveals its file name.
 */
public record LetterResponse(String kind, String fileName, boolean locked, String uploadedAt) {

    public static LetterResponse of(ApplicationLetter l, boolean locked) {
        return new LetterResponse(l.getKind(), locked ? null : l.getOriginalFilename(), locked,
                l.getCreateTime() == null ? null : l.getCreateTime().toString());
    }
}
