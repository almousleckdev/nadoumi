package com.nadoumi.application.web.response;

import com.nadoumi.application.service.ApplicationChecklistService.Item;

/** One row of an application's document checklist. {@code status} is {@code MISSING} until a document exists. */
public record ChecklistItemResponse(String docType, boolean mandatory, String note, String status, Long documentId,
        String rejectionReason) {

    public static ChecklistItemResponse of(Item i) {
        return new ChecklistItemResponse(i.docType(), i.mandatory(), i.note(), i.status().name(), i.documentId(),
                i.rejectionReason());
    }
}
