package ar.edu.itba.paw.webapp.controller;

import java.util.Optional;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import ar.edu.itba.paw.models.domain.StoredFile;
import ar.edu.itba.paw.services.CarPictureService;
import ar.edu.itba.paw.services.StoredFileService;

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
        final Optional<StoredFile> fileOpt = storedFileService.findById(id);
        if (fileOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        final StoredFile file = fileOpt.get();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .body(file.getData());
    }
}
