package com.nadoumi.university.domain;

/** Row of {@code nad_university_link}: an official or social channel. {@code value} is a URL, or an account id for WECHAT. */
public record UniversityLink(Long id, String kind, String value) {
}
