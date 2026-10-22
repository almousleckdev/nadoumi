package com.nadoumi.scholarship.domain;

/**
 * A document the application must supply. {@code appliesWhen} is a RequirementCondition code, or null when it always
 * applies. The frontend renders this list; it never hard-codes it.
 */
public record ScholarshipDocumentRequirement(String docType, boolean mandatory, String note, String appliesWhen) {
}
