package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.domain.Image;
import ar.edu.itba.paw.services.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Optional;

// No sé si lo tenemos que manejar con un controller...
@Controller
@RequestMapping("/image")
public class ImageController {
    
    private final ImageService imageService;
    
    @Autowired
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
        final Optional<Image> imageOpt = imageService.getImageById(id);
        
        if (imageOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        final Image image = imageOpt.get();
        
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(image.getContentType()))
            .body(image.getData());
    }
}


