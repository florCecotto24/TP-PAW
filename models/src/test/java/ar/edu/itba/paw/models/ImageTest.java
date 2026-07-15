package ar.edu.itba.paw.models;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ar.edu.itba.paw.models.domain.file.Image;

public class ImageTest {

    @Test
    public void testIsImageContentTypeAcceptsCommonImageTypes() {
        // 3. Assert
        Assertions.assertTrue(Image.isImageContentType("image/png"));
        Assertions.assertTrue(Image.isImageContentType("IMAGE/JPEG"));
        Assertions.assertTrue(Image.isImageContentType(" image/webp "));
    }

    @Test
    public void testIsImageContentTypeRejectsNonImageOrBlank() {
        // 3. Assert
        Assertions.assertFalse(Image.isImageContentType(null));
        Assertions.assertFalse(Image.isImageContentType(""));
        Assertions.assertFalse(Image.isImageContentType("   "));
        Assertions.assertFalse(Image.isImageContentType("text/plain"));
        Assertions.assertFalse(Image.isImageContentType("application/pdf"));
        Assertions.assertFalse(Image.isImageContentType("image"));
    }

    @Test
    public void testIsImageContentTypeRejectsScriptableSvg() {
        // 3. Assert — SVG is XML that can carry <script>; serving it inline is a stored-XSS vector.
        Assertions.assertFalse(Image.isImageContentType("image/svg+xml"));
        Assertions.assertFalse(Image.isImageContentType("IMAGE/SVG+XML"));
    }
}
