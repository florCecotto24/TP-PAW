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

import ar.edu.itba.paw.models.domain.car.CarBrand;
import ar.edu.itba.paw.persistence.car.CarBrandDao;

@ExtendWith(MockitoExtension.class)
class CarBrandServiceImplTest {

    @Mock
    private CarBrandDao carBrandDao;

    @InjectMocks
    private CarBrandServiceImpl carBrandService;

    @Test
    void testFindAllOrderedReturnsWhateverDaoReturns() {
        // 1. Arrange
        final List<CarBrand> expected = List.of(
                CarBrand.builder().id(1L).name("Honda").validated(true).build());
        Mockito.when(carBrandDao.findAllOrdered()).thenReturn(expected);

        // 2. Act
        final List<CarBrand> result = carBrandService.findAllOrdered();

        // 3. Assert
        Assertions.assertSame(expected, result);
    }

    @Test
    void testFindOrCreateUnvalidatedReturnsEmptyForNullInput() {
        // 1. Arrange — no stubs.

        // 2. Act
        final Optional<CarBrand> result = carBrandService.findOrCreateUnvalidated(null);

        // 3. Assert
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void testFindOrCreateUnvalidatedReturnsEmptyForBlankInput() {
        // 1. Arrange — no stubs.

        // 2. Act
        final Optional<CarBrand> result = carBrandService.findOrCreateUnvalidated("   ");

        // 3. Assert
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void testFindOrCreateUnvalidatedReusesExistingMatchAndDoesNotCreate() {
        // 1. Arrange
        final CarBrand existing = CarBrand.builder().id(7L).name("Toyota").validated(true).build();
        Mockito.when(carBrandDao.findByNameIgnoreCase("Toyota")).thenReturn(Optional.of(existing));

        // 2. Act
        final Optional<CarBrand> result = carBrandService.findOrCreateUnvalidated("  Toyota  ");

        // 3. Assert
        Assertions.assertTrue(result.isPresent());
        Assertions.assertSame(existing, result.get());
    }

    @Test
    void testFindOrCreateUnvalidatedCreatesUnvalidatedBrandWhenAbsent() {
        // 1. Arrange
        final CarBrand created = CarBrand.builder().id(42L).name("Rivian").validated(false).build();
        Mockito.when(carBrandDao.findByNameIgnoreCase("Rivian")).thenReturn(Optional.empty());
        Mockito.when(carBrandDao.create("Rivian", false)).thenReturn(created);

        // 2. Act
        final Optional<CarBrand> result = carBrandService.findOrCreateUnvalidated("Rivian");

        // 3. Assert
        Assertions.assertTrue(result.isPresent());
        Assertions.assertSame(created, result.get());
    }
}
