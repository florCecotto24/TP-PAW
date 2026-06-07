package ar.edu.itba.paw.webapp.controller.car;

import java.util.Optional;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import ar.edu.itba.paw.models.dto.file.BinaryContent;
import ar.edu.itba.paw.services.file.ImageService;

/**
 * Serves image bytes for {@code /image/{id}}.
 *
 * Security note: anonymous read is intentional
 * {@code /image/**} is explicitly {@code permitAll()} in {@code WebAuthConfig}. Every image we persist (profile pictures, car gallery shots, review images, owner logos, etc.) is meant to be surfaced as part of a publicly browsable listing or counterparty profile, so the bytes themselves do not carry tenant boundaries. We accept that an attacker can enumerate ids by iterating positive integers and harvest the public catalog faster than crawling pages; the trade-off is that requiring authentication here would break server-side rendered {@code <img>} tags in pages that are also public (home, search, car detail, public profile), without adding meaningful protection over data that is already discoverable by listing browse.
 *
 * Documents that must not leak (driver's licenses, identity scans, signed payment/refund proofs, insurance PDFs) are <strong>not</strong> served from this endpoint: they live behind dedicated controllers under {@code /profile/document/**}, {@code /admin/document/**}, {@code /my-reservations/{id}/payment-receipt/**}, {@code /my-reservations/{id}/refund-receipt/**}, and {@code /my-cars/car/{carId}/insurance/**}, each guarded by a method-security expression in {@code WebAuthConfig}. If you ever need to host an image that is <em>not</em> public, do not add a permission check here - serve it from one of those private endpoints (or a sibling) so the authorization rule sits with the rest of the file-access policy.
 */
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


