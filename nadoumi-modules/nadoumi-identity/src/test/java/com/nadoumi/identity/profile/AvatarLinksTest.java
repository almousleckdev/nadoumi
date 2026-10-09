package com.nadoumi.identity.profile;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AvatarLinksTest {

    private final AvatarLinks links = new AvatarLinks("test-secret");

    @Test
    void shouldIssueALinkThatVerifies_whenItIsForTheSameUser() {
        String link = links.linkFor(42);
        String[] parts = link.substring(AvatarLinks.PATH_PREFIX.length()).split("/");

        assertThat(link).startsWith("/api/public/avatars/42/");
        assertThat(links.isValid(Long.parseLong(parts[0]), parts[1])).isTrue();
    }

    @Test
    void shouldRefuseTheSignatureOfAnotherUser_soIdsCannotBeWalked() {
        String signatureOf42 = links.linkFor(42).substring("/api/public/avatars/42/".length());

        assertThat(links.isValid(43, signatureOf42)).isFalse();
        assertThat(links.isValid(42, "wrong")).isFalse();
        assertThat(links.isValid(42, null)).isFalse();
        assertThat(links.isValid(42, "")).isFalse();
    }

    @Test
    void shouldNotAcceptALinkMadeWithAnotherSecret() {
        String foreign = new AvatarLinks("other-secret").linkFor(42).substring("/api/public/avatars/42/".length());

        assertThat(links.isValid(42, foreign)).isFalse();
    }
}
