package com.nadoumi.identity.profile;

/** How a person appears to the public next to what they wrote or reacted to. {@code avatarUrl} is null when there is none. */
public record PublicProfile(String displayName, String avatarUrl) {
}
