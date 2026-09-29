package com.nadoumi.identity.service;

import java.util.Locale;

public final class StudentEmails {

    private StudentEmails() {
    }

    public static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public static String localPart(String email) {
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }
}
