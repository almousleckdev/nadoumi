package com.nadoumi.media.web;

import com.nadoumi.common.media.MediaAccessClass;
import com.nadoumi.common.media.MediaGateway;
import com.nadoumi.common.media.StoredAsset;
import com.nadoumi.media.NadMediaNotFoundException;
import com.ruoyi.common.annotation.Anonymous;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stable indirection for PUBLIC media (spec §I.6): {@code GET /api/media/{id}}
 * redirects ({@code 302}) to the stored {@code secure_url}, so links survive a
 * future provider swap or added signing. PROTECTED / SENSITIVE assets are never
 * disclosed here — the route 404s exactly as it would for a missing id; their
 * delivery lives on the owning domain module's authorized endpoints.
 */
@RestController
@RequestMapping("/api/media")
public class MediaController {

    private final MediaGateway media;

    public MediaController(MediaGateway media) {
        this.media = media;
    }

    @GetMapping("/{id}")
    @Anonymous
    public ResponseEntity<Void> get(@PathVariable long id) {
        StoredAsset asset = media.find(id)
                .orElseThrow(() -> new NadMediaNotFoundException("media not found"));
        if (asset.accessClass() != MediaAccessClass.PUBLIC) {
            throw new NadMediaNotFoundException("media not found");
        }
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(asset.secureUrl())).build();
    }
}
