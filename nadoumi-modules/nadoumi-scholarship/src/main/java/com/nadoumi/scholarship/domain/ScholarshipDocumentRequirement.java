package com.nadoumi.scholarship.domain;

/** A document the application must supply. The frontend renders this list; it never hard-codes it. */
public record ScholarshipDocumentRequirement(String docType, boolean mandatory, String note) {
}
