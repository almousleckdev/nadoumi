package com.nadoumi.identity.web;

import com.nadoumi.common.media.MediaAccessLogContext;
import com.nadoumi.common.media.MediaCategory;
import com.nadoumi.common.media.MediaGateway;
import com.nadoumi.identity.mapper.PublicProfileMapper;
import com.nadoumi.identity.profile.AvatarLinks;
import com.ruoyi.common.annotation.Anonymous;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Serves a student's profile photo behind a signed link (see {@link AvatarLinks}). A wrong or missing signature,
 * a user without a photo, and a photo that is not an applicant photo all answer 404, so nothing about who has a
 * photo can be learned without a link the server issued. The answer is a short-lived redirect to the media URL.
 */
@RestController
public class PublicAvatarController {

    /** Shorter than the signed media URL it points at, so a cached redirect never outlives its target. */
    private static final Duration CACHE = Duration.ofSeconds(60);

    private final AvatarLinks links;
    private final PublicProfileMapper profiles;
    private final MediaGateway media;

    public PublicAvatarController(AvatarLinks links, PublicProfileMapper profiles, MediaGateway media) {
        this.links = links;
        this.profiles = profiles;
        this.media = media;
    }

    @Anonymous
    @GetMapping("/api/public/avatars/{userId}/{signature}")
    public ResponseEntity<Void> avatar(@PathVariable long userId, @PathVariable String signature, HttpServletRequest request) {
        if (!links.isValid(userId, signature)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        Long mediaId = profiles.findStudentPhotoMediaId(userId);
        if (mediaId == null || media.find(mediaId).filter(a -> a.category() == MediaCategory.APPLICANT_PHOTO).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        var signed = media.issueInlineSignedUrl(mediaId,
                new MediaAccessLogContext(0L, null, null, null, request.getRemoteAddr(), request.getHeader("User-Agent")));
        return ResponseEntity.status(HttpStatus.FOUND)
                .cacheControl(CacheControl.maxAge(CACHE).cachePublic())
                .location(URI.create(signed.url()))
                .build();
    }
}
