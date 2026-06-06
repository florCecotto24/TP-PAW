package ar.edu.itba.paw.webapp.controller;

import java.util.Optional;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import ar.edu.itba.paw.models.dto.file.BinaryContent;
import ar.edu.itba.paw.services.file.ImageService;

/** Serves image bytes for {@code /image/{id}}. */
@Controller
@RequestMapping("/image")
public final class ImageController {

    private final ImageService imageService;

    public ImageController(final ImageService imageService) {
        this.imageService = imageService;
    }

    /**
     * Retrieve and serve an image by ID
     * @param id The image ID
     * @return The image binary data with appropriate content type
     */
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> getImage(@PathVariable("id") final long id) {
        // Consume BinaryContent so the controller never sees the Image entity (issue #16):
        // byte access stays inside the persistence + service layers.
        final Optional<BinaryContent> contentOpt = imageService.getImageContent(id);

        if (contentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        final BinaryContent content = contentOpt.get();

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(content.getContentType()))
            .body(content.getBytes());
    }
}


