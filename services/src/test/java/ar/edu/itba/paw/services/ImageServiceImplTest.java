package ar.edu.itba.paw.services;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import ar.edu.itba.paw.models.Image;
import ar.edu.itba.paw.persistence.ImageDao;

@ExtendWith(MockitoExtension.class)
public class ImageServiceImplTest {

    private static final long LIMIT_ONE_MIB = 1024L * 1024L;

    @Mock
    private ImageDao imageDao;

    @Mock
    private Environment environment;

    private ImageServiceImpl imageService;

    @BeforeEach
    public void setUp() {
        Mockito.when(environment.getProperty("app.upload.max-image-bytes", Long.class, 20971520L))
                .thenReturn(LIMIT_ONE_MIB);
        imageService = new ImageServiceImpl(imageDao, environment);
    }

    @Test
    public void testGetImageByIdWhenImageExists() {
        // 1. Arrange
        final byte[] imageData = {0x00, 0x01, 0x02, 0x03};
        final Image image = new Image(1L, "imageName", "image/png", imageData);
        Mockito.when(imageDao.getImageById(1L)).thenReturn(Optional.of(image));

        // 2. Execute
        final Optional<Image> result = imageService.getImageById(1L);

        // 3. Assert
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(image.getId(), result.get().getId());
        Assertions.assertEquals(image.getName(), result.get().getName());
        Assertions.assertEquals(image.getContentType(), result.get().getContentType());
        Assertions.assertArrayEquals(image.getData(), result.get().getData());
    }

    @Test
    public void testGetImageByIdWhenImageDoesNotExist() {
        // 1. Arrange
        Mockito.when(imageDao.getImageById(Mockito.anyLong())).thenReturn(Optional.empty());

        // 2. Execute
        final Optional<Image> result = imageService.getImageById(1L);

        // 3. Assert
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testCreateImage() {
        // 1. Arrange
        final byte[] imageData = {0x00, 0x01, 0x02, 0x03};
        final Image image = new Image(1L, "imageName", "image/png", imageData);
        Mockito.when(imageDao.createImage(Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(image);

        // 2. Execute
        final Image result = imageService.createImage("imageName", "image/png", imageData);

        // 3. Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(result.getId(), image.getId());
        Assertions.assertEquals(result.getName(), image.getName());
        Assertions.assertEquals(result.getContentType(), image.getContentType());
        Assertions.assertEquals(result.getData(), image.getData());
    }

}
