package com.nadoumi.document.web.response;

import com.nadoumi.document.domain.DocumentRequirement;

public record DocumentRequirementResponse(String docType, boolean mandatory, String notes) {

    public static DocumentRequirementResponse of(DocumentRequirement r) {
        return new DocumentRequirementResponse(r.getDocType(), r.isMandatory(), r.getNotes());
    }
}
