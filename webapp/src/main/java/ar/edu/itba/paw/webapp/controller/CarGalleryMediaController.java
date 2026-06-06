package ar.edu.itba.paw.webapp.controller;

import java.util.Optional;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import ar.edu.itba.paw.models.dto.file.BinaryContent;
import ar.edu.itba.paw.services.car.CarPictureService;
import ar.edu.itba.paw.services.file.StoredFileService;

/** Serves car gallery video bytes for {@code /car-media/{id}}. */
@Controller
@RequestMapping("/car-media")
public final class CarGalleryMediaController {

    private final StoredFileService storedFileService;
    private final CarPictureService carPictureService;

    public CarGalleryMediaController(
            final StoredFileService storedFileService, final CarPictureService carPictureService) {
        this.storedFileService = storedFileService;
        this.carPictureService = carPictureService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> getCarGalleryVideo(@PathVariable("id") final long id) {
        if (!carPictureService.isStoredFileInCarGallery(id)) {
            return ResponseEntity.notFound().build();
        }
        // Consume BinaryContent so the controller never sees the JPA entity:
        // byte access stays inside the persistence + service layers.
        final Optional<BinaryContent> contentOpt = storedFileService.findContentById(id);
        if (contentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        final BinaryContent content = contentOpt.get();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.getContentType()))
                .body(content.getBytes());
    }
}
