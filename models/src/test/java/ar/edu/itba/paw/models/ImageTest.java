package ar.edu.itba.paw.models;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ar.edu.itba.paw.models.domain.Image;

public class ImageTest {

    @Test
    public void isImageContentTypeAcceptsCommonImageTypes() {
        // 3. Assert
        Assertions.assertTrue(Image.isImageContentType("image/png"));
        Assertions.assertTrue(Image.isImageContentType("IMAGE/JPEG"));
        Assertions.assertTrue(Image.isImageContentType(" image/webp "));
    }

    @Test
    public void isImageContentTypeRejectsNonImageOrBlank() {
        // 3. Assert
        Assertions.assertFalse(Image.isImageContentType(null));
        Assertions.assertFalse(Image.isImageContentType(""));
        Assertions.assertFalse(Image.isImageContentType("   "));
        Assertions.assertFalse(Image.isImageContentType("text/plain"));
        Assertions.assertFalse(Image.isImageContentType("application/pdf"));
        Assertions.assertFalse(Image.isImageContentType("image"));
    }
}
