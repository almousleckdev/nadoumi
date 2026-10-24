package com.nadoumi.application.web.response;

/** A Nadoumi staff account an application can be assigned to. */
public record StaffUserResponse(Long userId, String name, String userName) {
}
