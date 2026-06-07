package ar.edu.itba.paw.services.file;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.image.ImageValidationException;
import ar.edu.itba.paw.models.domain.file.Image;
import ar.edu.itba.paw.models.dto.file.BinaryContent;
import ar.edu.itba.paw.persistence.file.ImageDao;
import ar.edu.itba.paw.util.UploadBinaryMegabyte;

/** Validates upload size then persists via {@link ImageDao}. */
@Service
public final class ImageServiceImpl implements ImageService {

    private final ImageDao imageDao;
    private final long maxImageBytes;
    private final long bytesPerBinaryMegabyte;

    @Autowired
    public ImageServiceImpl(final ImageDao imageDao, final Environment environment) {
        this.imageDao = imageDao;
        /* Environment avoids @Value to be unresolved if the bean is created too early (e.g. during initMessageSource). */
        this.bytesPerBinaryMegabyte = UploadBinaryMegabyte.bytesPerBinaryMegabyte(environment);
        final long v = UploadBinaryMegabyte.maxBytesFromConfiguredMegabytes(
                environment, UploadBinaryMegabyte.PROPERTY_MAX_IMAGE_MB, 20L);
        if (v <= 0) {
            throw new IllegalArgumentException(
                    UploadBinaryMegabyte.PROPERTY_MAX_IMAGE_MB + " resolved to non-positive bytes: " + v);
        }
        this.maxImageBytes = v;
    }

    @Override
    @Transactional(readOnly = true)
    public long getMaxImageBytes() {
        return maxImageBytes;
    }

    @Override
    @Transactional(readOnly = true)
    public long getMaxImageMegabytesRoundedUp() {
        return (maxImageBytes + bytesPerBinaryMegabyte - 1) / bytesPerBinaryMegabyte;
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
    @Transactional(readOnly = true)
    public Optional<BinaryContent> getImageContent(final long id) {
        // Map to a detached value object inside this transaction so the controller doesn't
        // depend on the JPA entity (issue #16).
        return imageDao.getImageById(id)
                .map(img -> new BinaryContent(img.getData(), img.getContentType(), img.getName()));
    }

    @Override
    @Transactional
    public void deleteImage(final long id) {
        imageDao.deleteImage(id);
    }
}
