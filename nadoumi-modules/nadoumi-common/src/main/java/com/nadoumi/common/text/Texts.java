package com.nadoumi.common.text;

public final class Texts {

    private Texts() {
    }

    public static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    public static String stringOrNull(Object value) {
        return value == null ? null : value.toString();
    }
}
