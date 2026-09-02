package com.nadoumi.common.access;

/** Discrete action an external user may perform for an applicant. Matrix in {@link AccessCapabilityMatrix}. */
public enum ApplicantCapability {
    VIEW_PROFILE,
    EDIT_PROFILE,
    VIEW_APPLICATION,
    CREATE_APPLICATION,
    SUBMIT_APPLICATION,
    UPLOAD_DOCUMENT,
    VIEW_DOCUMENT,
    MESSAGE_STAFF,
    MANAGE_ACCESS
}
