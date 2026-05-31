package ar.edu.itba.paw.models.util.media;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CarGalleryMediaContentTypesTest {

    @Test
    void testAllowsImagesAndKnownVideoTypes() {
        Assertions.assertTrue(CarGalleryMediaContentTypes.isAllowed("image/jpeg", "photo.jpg"));
        Assertions.assertTrue(CarGalleryMediaContentTypes.isAllowed("video/mp4", "clip.mp4"));
        Assertions.assertTrue(CarGalleryMediaContentTypes.isAllowed("application/octet-stream", "clip.mov"));
    }

    @Test
    void testRejectsUnsupportedTypes() {
        Assertions.assertFalse(CarGalleryMediaContentTypes.isAllowed("application/pdf", "doc.pdf"));
        Assertions.assertFalse(CarGalleryMediaContentTypes.isAllowed("video/x-msvideo", "clip.avi"));
    }
}
