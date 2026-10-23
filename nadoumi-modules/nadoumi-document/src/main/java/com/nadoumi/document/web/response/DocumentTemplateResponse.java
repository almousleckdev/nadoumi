package com.nadoumi.document.web.response;

import com.nadoumi.document.domain.DocumentTemplate;

/** A template as staff and applicants see it: no media id, no storage location. */
public record DocumentTemplateResponse(long id, String title, String fileName) {

    public static DocumentTemplateResponse of(DocumentTemplate t) {
        return new DocumentTemplateResponse(t.getId(), t.getTitle(), t.getFileName());
    }
}
