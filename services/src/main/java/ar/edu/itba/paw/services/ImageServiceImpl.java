package ar.edu.itba.paw.services;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public ImageServiceImpl(final ImageDao imageDao, final Environment environment) {
        this.imageDao = imageDao;
        /* Environment avoids @Value to be unresolved if the bean is created too early (e.g. during initMessageSource). */
        final long v = environment.getProperty("app.upload.max-image-bytes", Long.class, 20971520L);
        if (v <= 0) {
            throw new IllegalArgumentException("app.upload.max-image-bytes must be positive, got " + v);
        }
        this.maxImageBytes = v;
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
    @Transactional
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
    @Transactional(readOnly = true)
    public Optional<Image> getImageById(final long id) {
        return imageDao.getImageById(id);
    }

    @Override
    @Transactional
    public void deleteImage(final long id) {
        imageDao.deleteImage(id);
    }
}
