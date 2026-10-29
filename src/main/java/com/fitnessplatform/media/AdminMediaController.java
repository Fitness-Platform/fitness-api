package com.fitnessplatform.media;

import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/media/uploads")
public class AdminMediaController {
    private final MediaService mediaService;
    public AdminMediaController(MediaService mediaService) { this.mediaService = mediaService; }

    @PostMapping
    public ResponseEntity<MediaUploadResponse> authorize(@Valid @RequestBody MediaUploadRequest request,
                                                         Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .body(mediaService.authorize((UUID) authentication.getPrincipal(), request));
    }

    @PostMapping("/{uploadId}/complete")
    public ResponseEntity<MediaMetadataResponse> complete(@PathVariable UUID uploadId, Authentication authentication) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(mediaService.complete((UUID) authentication.getPrincipal(), uploadId));
    }
}
