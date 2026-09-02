package com.nadoumi.identity.contact.mapper;

import com.nadoumi.identity.contact.ContactInquiry;

/** Write-only for now; the admin triage screen adds reads with the Content slice. */
public interface ContactInquiryMapper {

    int insert(ContactInquiry inquiry);
}
