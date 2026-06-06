package ar.edu.itba.paw.services.car;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.CarBrand;
import ar.edu.itba.paw.models.domain.CarModel;
import ar.edu.itba.paw.persistence.CarModelDao;

@ExtendWith(MockitoExtension.class)
class CarModelServiceImplTest {

    private static final long BRAND_ID = 7L;

    @Mock
    private CarModelDao carModelDao;

    @InjectMocks
    private CarModelServiceImpl carModelService;

    private static CarModel sampleModel(final long id, final String name, final boolean validated) {
        return CarModel.builder()
                .id(id)
                .brand(CarBrand.builder().id(BRAND_ID).name("Honda").validated(true).build())
                .name(name)
                .validated(validated)
                .type(Car.Type.SEDAN)
                .build();
    }

    @Test
    void testFindByBrandIdOrderedReturnsWhateverDaoReturns() {
        // 1. Arrange
        final List<CarModel> expected = List.of(sampleModel(1L, "Civic", true));
        Mockito.when(carModelDao.findByBrandIdOrdered(BRAND_ID)).thenReturn(expected);

        // 2. Act
        final List<CarModel> result = carModelService.findByBrandIdOrdered(BRAND_ID);

        // 3. Assert
        Assertions.assertSame(expected, result);
    }

    @Test
    void testFindOrCreateUnvalidatedReturnsEmptyForNullName() {
        // 1. Arrange — no stubs.

        // 2. Act
        final Optional<CarModel> result = carModelService.findOrCreateUnvalidated(BRAND_ID, null, Car.Type.SEDAN);

        // 3. Assert
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void testFindOrCreateUnvalidatedReturnsEmptyWhenTypeMissingAndModelDoesNotExist() {
        // 1. Arrange — name is not present in the catalog, so creation would be needed but {@code type} is null.
        Mockito.when(carModelDao.findByBrandIdAndNameIgnoreCase(BRAND_ID, "Civic")).thenReturn(Optional.empty());

        // 2. Act
        final Optional<CarModel> result = carModelService.findOrCreateUnvalidated(BRAND_ID, "Civic", null);

        // 3. Assert
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void testFindOrCreateUnvalidatedReusesExistingMatchWhenTypeIsNull() {
        // 1. Arrange — when the user picks an existing catalog model, the publish form omits {@code type}.
        final CarModel existing = sampleModel(33L, "Civic", true);
        Mockito.when(carModelDao.findByBrandIdAndNameIgnoreCase(BRAND_ID, "Civic")).thenReturn(Optional.of(existing));

        // 2. Act
        final Optional<CarModel> result = carModelService.findOrCreateUnvalidated(BRAND_ID, "Civic", null);

        // 3. Assert
        Assertions.assertTrue(result.isPresent());
        Assertions.assertSame(existing, result.get());
    }

    @Test
    void testFindOrCreateUnvalidatedReturnsEmptyForBlankName() {
        // 1. Arrange — no stubs.

        // 2. Act
        final Optional<CarModel> result = carModelService.findOrCreateUnvalidated(BRAND_ID, "   ", Car.Type.SEDAN);

        // 3. Assert
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void testFindOrCreateUnvalidatedReusesExistingMatchAndDoesNotCreate() {
        // 1. Arrange
        final CarModel existing = sampleModel(33L, "Civic", true);
        Mockito.when(carModelDao.findByBrandIdAndNameIgnoreCase(BRAND_ID, "Civic")).thenReturn(Optional.of(existing));

        // 2. Act
        final Optional<CarModel> result = carModelService.findOrCreateUnvalidated(BRAND_ID, "  Civic ", Car.Type.SEDAN);

        // 3. Assert
        Assertions.assertTrue(result.isPresent());
        Assertions.assertSame(existing, result.get());
    }

    @Test
    void testFindOrCreateUnvalidatedCreatesUnvalidatedModelWhenAbsent() {
        // 1. Arrange
        final CarModel created = sampleModel(99L, "Cybertruck", false);
        Mockito.when(carModelDao.findByBrandIdAndNameIgnoreCase(BRAND_ID, "Cybertruck")).thenReturn(Optional.empty());
        Mockito.when(carModelDao.create(BRAND_ID, "Cybertruck", false, Car.Type.PICKUP)).thenReturn(created);

        // 2. Act
        final Optional<CarModel> result = carModelService.findOrCreateUnvalidated(BRAND_ID, "Cybertruck", Car.Type.PICKUP);

        // 3. Assert
        Assertions.assertTrue(result.isPresent());
        Assertions.assertSame(created, result.get());
    }
}
