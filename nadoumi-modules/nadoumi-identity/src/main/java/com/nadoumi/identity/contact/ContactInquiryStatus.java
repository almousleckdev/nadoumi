package com.nadoumi.identity.contact;

/** Triage state of a {@link ContactInquiry}. Only {@code NEW} is set by the public write path. */
public enum ContactInquiryStatus {
    NEW, READ, RESPONDED, ARCHIVED
}
