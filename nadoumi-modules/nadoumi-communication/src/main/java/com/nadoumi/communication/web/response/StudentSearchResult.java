package com.nadoumi.communication.web.response;

/**
 * A student found by a staff member. Only what is needed to pick the right person: a reference, the first name
 * and photo, and the application the search matched when it was searched by application id.
 */
public record StudentSearchResult(long userId, String studentRef, String name, String avatarUrl, boolean online,
        Long matchedApplicationId) {
}
