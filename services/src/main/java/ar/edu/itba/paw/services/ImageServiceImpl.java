package ar.edu.itba.paw.services;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.image.ImageValidationException;
import ar.edu.itba.paw.models.Image;
import ar.edu.itba.paw.persistence.ImageDao;

@Service
public class ImageServiceImpl implements ImageService {

    private static final long MIB = 1024L * 1024L;

    private final ImageDao imageDao;
    private final long maxImageBytes;

    @Autowired
    public ImageServiceImpl(
            final ImageDao imageDao,
            @Value("${app.upload.max-image-bytes:20971520}") final String maxImageBytesProperty) {
        this.imageDao = imageDao;
        this.maxImageBytes = parseMaxBytes(maxImageBytesProperty);
    }

    private static long parseMaxBytes(final String raw) {
        try {
            final long v = Long.parseLong(raw.trim());
            if (v <= 0) {
                throw new IllegalArgumentException("app.upload.max-image-bytes must be positive");
            }
            return v;
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException("Invalid app.upload.max-image-bytes: " + raw, e);
        }
    }

    @Override
    public long getMaxImageBytes() {
        return maxImageBytes;
    }

    @Override
    public long getMaxImageMegabytesRoundedUp() {
        return (maxImageBytes + MIB - 1) / MIB;
    }

    @Override
    public Image createImage(final String name, final String contentType, final byte[] data) {
        if (!Image.isImageContentType(contentType)) {
            throw new ImageValidationException(MessageKeys.IMAGE_CONTENT_TYPE_NOT_IMAGE);
        }
        final int len = (data == null) ? 0 : data.length;
        validatePayloadLength(len);
        return imageDao.createImage(name, contentType, data);
    }

    private void validatePayloadLength(final long byteLength) {
        if (byteLength > maxImageBytes) {
            throw new ImageValidationException(MessageKeys.IMAGE_FILE_TOO_LARGE, getMaxImageMegabytesRoundedUp());
        }
    }

    @Override
    public Optional<Image> getImageById(final long id) {
        return imageDao.getImageById(id);
    }

    @Override
    public void deleteImage(final long id) {
        imageDao.deleteImage(id);
    }
}
