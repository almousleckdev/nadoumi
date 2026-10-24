package com.nadoumi.application.mapper;

/** A {@code sys_user} row reduced to what the assignment picker and the audit trail show. */
public record StaffUserRow(Long userId, String nickName, String userName) {
}
