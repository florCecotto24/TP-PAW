package ar.edu.itba.paw.services;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.CarModel;
import ar.edu.itba.paw.persistence.CarModelDao;

/** Catalog reads via {@link CarModelDao}; "Other" creation is normalized here before delegating. */
@Service
public final class CarModelServiceImpl implements CarModelService {

    private final CarModelDao carModelDao;

    @Autowired
    public CarModelServiceImpl(final CarModelDao carModelDao) {
        this.carModelDao = carModelDao;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarModel> findByBrandIdOrdered(final long brandId) {
        return carModelDao.findByBrandIdOrdered(brandId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarModel> findValidatedByBrandIdOrdered(final long brandId) {
        return carModelDao.findValidatedByBrandIdOrdered(brandId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarModel> findAllOrderedGroupedByBrand() {
        return carModelDao.findAllOrderedGroupedByBrand();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CarModel> findById(final long modelId) {
        return carModelDao.findById(modelId);
    }

    @Override
    @Transactional
    public Optional<CarModel> findOrCreateUnvalidated(final long brandId, final String rawName, final Car.Type type) {
        if (rawName == null) {
            return Optional.empty();
        }
        final String normalized = rawName.trim();
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        final Optional<CarModel> existing = carModelDao.findByBrandIdAndNameIgnoreCase(brandId, normalized);
        if (existing.isPresent()) {
            // When the user picks an existing catalog model, the publish form does not ask for {@code type}
            // (the body type is already stored on car_models). Reuse the existing row regardless of {@code type}.
            return existing;
        }
        // Creating a brand-new car_models row requires the body type; without it the model is unresolvable.
        if (type == null) {
            return Optional.empty();
        }
        return Optional.of(carModelDao.create(brandId, normalized, false, type));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarModel> findPendingOrdered() {
        return carModelDao.findPendingOrdered();
    }

    @Override
    @Transactional(readOnly = true)
    public int countByBrandId(final long brandId) {
        return carModelDao.countByBrandId(brandId);
    }

    @Override
    @Transactional
    public void markAsValidated(final long modelId) {
        carModelDao.findById(modelId).ifPresent(model -> model.setValidated(true));
    }

    @Override
    @Transactional
    public void deleteById(final long modelId) {
        carModelDao.deleteById(modelId);
    }
}
