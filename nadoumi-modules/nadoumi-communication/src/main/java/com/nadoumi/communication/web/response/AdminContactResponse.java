package com.nadoumi.communication.web.response;

public record AdminContactResponse(
        Long userId,
        String name,
        String email,
        String avatar) {
}
