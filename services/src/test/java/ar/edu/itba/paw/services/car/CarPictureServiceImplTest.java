package ar.edu.itba.paw.services.car;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.image.ImageValidationException;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.car.CarPicture;
import ar.edu.itba.paw.models.domain.file.Image;
import ar.edu.itba.paw.models.domain.file.StoredFile;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.persistence.car.CarPictureDao;
import ar.edu.itba.paw.services.file.ImageService;
import ar.edu.itba.paw.services.file.StoredFileService;

@ExtendWith(MockitoExtension.class)
public class CarPictureServiceImplTest {

    @Mock
    private CarPictureDao carPictureDao;

    @Mock
    private ImageService imageService;

    @Mock
    private StoredFileService storedFileService;

    @InjectMocks
    private CarPictureServiceImpl carPictureService;

    @Test
    public void testCreateCarPictureWhenImageExists() {
        // 1. Arrange
        final long carId = 1L;
        final long imageId = 2L;
        final int displayOrder = 3;
        final OffsetDateTime createdAt = OffsetDateTime.parse("2026-01-01T10:00:00Z");
        final OffsetDateTime updatedAt = OffsetDateTime.parse("2026-01-02T10:00:00Z");
        final byte[] data = {0x01, 0x02};
        final Image image = Image.identified(imageId, "photo.jpg", "image/jpeg", data);
        final CarPicture carPicture = CarPicture.identifiedForImage(10L, Mockito.mock(Car.class), image, displayOrder, createdAt, updatedAt);

        Mockito.when(imageService.getImageById(imageId)).thenReturn(Optional.of(image));
        Mockito.when(carPictureDao.createCarPicture(carId, imageId, displayOrder)).thenReturn(carPicture);

        // 2. Execute
        final CarPicture result = carPictureService.createCarPicture(carId, imageId, displayOrder);

        // 3. Assert
        Assertions.assertNotNull(result);
        Assertions.assertSame(carPicture, result);
    }

    @Test
    public void testCreateCarPictureWhenImageDoesNotExist() {
        // 1. Arrange
        final long carId = 1L;
        final long imageId = 2L;
        final int displayOrder = 1;
        Mockito.when(imageService.getImageById(imageId)).thenReturn(Optional.empty());

        // 2. Execute and 3. Assert
        final ImageValidationException ex = Assertions.assertThrows(
                ImageValidationException.class,
                () -> carPictureService.createCarPicture(carId, imageId, displayOrder));
        Assertions.assertEquals(MessageKeys.IMAGE_INVALID_ID, ex.getMessageCode());
    }

    @Test
    public void testCreateCarPictureWhenImageIdNotPositive() {
        // 1. Arrange
        final long carId = 1L;
        final long imageId = 0L;
        final int displayOrder = 1;

        // 2. Execute and 3. Assert
        final ImageValidationException ex = Assertions.assertThrows(
                ImageValidationException.class,
                () -> carPictureService.createCarPicture(carId, imageId, displayOrder));
        Assertions.assertEquals(MessageKeys.IMAGE_INVALID_ID, ex.getMessageCode());
    }

    @Test
    public void testGetCarPictureByIdWhenPictureExists() {
        // 1. Arrange
        final long pictureId = 50L;
        final long carId = 5L;
        final long imageId = 6L;
        final int displayOrder = 1;
        final OffsetDateTime createdAt = OffsetDateTime.parse("2026-03-01T12:00:00Z");
        final OffsetDateTime updatedAt = OffsetDateTime.parse("2026-03-02T12:00:00Z");
        final CarPicture carPicture = CarPicture.identifiedForImage(pictureId, Mockito.mock(Car.class), Image.identified(imageId, "img.jpg", "image/jpeg", new byte[0]), displayOrder, createdAt, updatedAt);
        Mockito.when(carPictureDao.getCarPictureById(pictureId)).thenReturn(Optional.of(carPicture));

        // 2. Execute
        final Optional<CarPicture> result = carPictureService.getCarPictureById(pictureId);

        // 3. Assert
        Assertions.assertTrue(result.isPresent());
        Assertions.assertSame(carPicture, result.get());
    }

    @Test
    public void testGetCarPictureByIdWhenPictureDoesNotExist() {
        // 1. Arrange
        final long pictureId = 51L;
        Mockito.when(carPictureDao.getCarPictureById(pictureId)).thenReturn(Optional.empty());

        // 2. Execute
        final Optional<CarPicture> result = carPictureService.getCarPictureById(pictureId);

        // 3. Assert
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testGetCarPicturesByCarIdWhenCarExists() {
        // 1. Arrange
        final long carId = 7L;
        final OffsetDateTime createdAt = OffsetDateTime.parse("2026-04-01T08:00:00Z");
        final OffsetDateTime updatedAt = OffsetDateTime.parse("2026-04-02T08:00:00Z");
        final CarPicture p1 = CarPicture.identifiedForImage(100L, Mockito.mock(Car.class), Image.identified(20L, "img.jpg", "image/jpeg", new byte[0]), 1, createdAt, updatedAt);
        final CarPicture p2 = CarPicture.identifiedForImage(101L, Mockito.mock(Car.class), Image.identified(21L, "img.jpg", "image/jpeg", new byte[0]), 2, createdAt, updatedAt);
        final List<CarPicture> pictures = new ArrayList<>();
        pictures.add(p1);
        pictures.add(p2);
        Mockito.when(carPictureDao.getCarPicturesByCarId(carId)).thenReturn(pictures);

        // 2. Execute
        final List<CarPicture> result = carPictureService.getCarPicturesByCarId(carId);

        // 3. Assert
        Assertions.assertEquals(pictures, result);
    }

    @Test
    public void testCreateCarPictureFromVideoWhenStoredFileExists() {
        // 1. Arrange
        final long carId = 1L;
        final long storedFileId = 99L;
        final int displayOrder = 1;
        final OffsetDateTime createdAt = OffsetDateTime.parse("2026-01-01T10:00:00Z");
        final OffsetDateTime updatedAt = OffsetDateTime.parse("2026-01-02T10:00:00Z");
        final StoredFile storedFile = StoredFile.identified(
                storedFileId,
                User.identities(2L, "u@test.com", "U", "U"),
                "clip.mp4",
                "video/mp4",
                new byte[] {1},
                createdAt);
        final CarPicture carPicture = CarPicture.forVideo(
                Mockito.mock(Car.class), storedFile, displayOrder, createdAt, updatedAt);

        Mockito.when(storedFileService.findById(storedFileId)).thenReturn(Optional.of(storedFile));
        Mockito.when(carPictureDao.createCarPictureFromVideo(carId, storedFileId, displayOrder))
                .thenReturn(carPicture);

        // 2. Execute
        final CarPicture result = carPictureService.createCarPictureFromVideo(carId, storedFileId, displayOrder);

        // 3. Assert
        Assertions.assertNotNull(result);
        Assertions.assertSame(carPicture, result);
    }

    @Test
    public void testCreateCarPictureFromVideoWhenStoredFileDoesNotExist() {
        // 1. Arrange
        final long carId = 1L;
        final long storedFileId = 100L;
        final int displayOrder = 1;
        Mockito.when(storedFileService.findById(storedFileId)).thenReturn(Optional.empty());

        // 2. Execute and 3. Assert
        final ImageValidationException ex = Assertions.assertThrows(
                ImageValidationException.class,
                () -> carPictureService.createCarPictureFromVideo(carId, storedFileId, displayOrder));
        Assertions.assertEquals(MessageKeys.IMAGE_INVALID_ID, ex.getMessageCode());
    }

    @Test
    public void testGetCarPicturesByCarIdWhenCarDoesNotExist() {
        // 1. Arrange
        final long carId = 8L;
        final List<CarPicture> empty = Collections.emptyList();
        Mockito.when(carPictureDao.getCarPicturesByCarId(carId)).thenReturn(empty);

        // 2. Execute
        final List<CarPicture> result = carPictureService.getCarPicturesByCarId(carId);

        // 3. Assert
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testFindPrimaryPictureByCarIdReturnsFirstInDisplayOrder() {
        // 1.Arrange
        final long carId = 9L;
        final OffsetDateTime createdAt = OffsetDateTime.parse("2026-03-01T12:00:00Z");
        final OffsetDateTime updatedAt = OffsetDateTime.parse("2026-03-02T12:00:00Z");
        final CarPicture first = CarPicture.identifiedForImage(
                1L, Mockito.mock(Car.class), Image.identified(11L, "a.jpg", "image/jpeg", new byte[0]), 1, createdAt, updatedAt);
        Mockito.when(carPictureDao.findFirstByCarIdOrderByDisplayOrderAsc(carId)).thenReturn(Optional.of(first));

        // 2. Execute
        final Optional<CarPicture> primary = carPictureService.findPrimaryPictureByCarId(carId);

        // 3. Assert
        Assertions.assertTrue(primary.isPresent());
        Assertions.assertEquals(1L, primary.get().getId());
    }

    @Test
    public void testFindPrimaryPictureByCarIdWhenGalleryEmpty() {
        // 1.Arrange
        final long carId = 10L;
        Mockito.when(carPictureDao.findFirstByCarIdOrderByDisplayOrderAsc(carId)).thenReturn(Optional.empty());

        // 2. Execute
        final Optional<CarPicture> primary = carPictureService.findPrimaryPictureByCarId(carId);

        // 3. Assert
        Assertions.assertTrue(primary.isEmpty());
    }

    @Test
    public void testFindByCarPaginatedReturnsSqlPage() {
        // 1.Arrange
        final long carId = 11L;
        final OffsetDateTime createdAt = OffsetDateTime.parse("2026-03-01T12:00:00Z");
        final OffsetDateTime updatedAt = OffsetDateTime.parse("2026-03-02T12:00:00Z");
        final CarPicture first = CarPicture.identifiedForImage(
                1L, Mockito.mock(Car.class), Image.identified(11L, "a.jpg", "image/jpeg", new byte[0]), 1, createdAt, updatedAt);
        Mockito.when(carPictureDao.countByCarId(carId)).thenReturn(3L);
        Mockito.when(carPictureDao.findByCarIdOrderByDisplayOrderAsc(carId, 0, 2)).thenReturn(List.of(first));

        // 2. Execute
        final Page<CarPicture> page =
                carPictureService.findByCarPaginated(carId, 0, 2);

        // 3. Assert
        Assertions.assertEquals(1, page.getContent().size());
        Assertions.assertEquals(3L, page.getTotalItems());
        Assertions.assertEquals(2, page.getPageSize());
    }
}
