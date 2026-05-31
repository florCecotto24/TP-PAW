package ar.edu.itba.paw.services;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.CarBrand;
import ar.edu.itba.paw.persistence.CarBrandDao;

/** Catalog reads via {@link CarBrandDao}; "Other" creation is normalized here before delegating. */
@Service
public final class CarBrandServiceImpl implements CarBrandService {

    private final CarBrandDao carBrandDao;

    @Autowired
    public CarBrandServiceImpl(final CarBrandDao carBrandDao) {
        this.carBrandDao = carBrandDao;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarBrand> findAllOrdered() {
        return carBrandDao.findAllOrdered();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarBrand> findValidatedOrdered() {
        return carBrandDao.findValidatedOrdered();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CarBrand> findById(final long brandId) {
        return carBrandDao.findById(brandId);
    }

    @Override
    @Transactional
    public Optional<CarBrand> findOrCreateUnvalidated(final String rawName) {
        if (rawName == null) {
            return Optional.empty();
        }
        final String normalized = rawName.trim();
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        final Optional<CarBrand> existing = carBrandDao.findByNameIgnoreCase(normalized);
        if (existing.isPresent()) {
            return existing;
        }
        return Optional.of(carBrandDao.create(normalized, false));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarBrand> findPendingOrdered() {
        return carBrandDao.findPendingOrdered();
    }

    @Override
    @Transactional
    public void markAsValidated(final long brandId) {
        carBrandDao.findById(brandId).ifPresent(brand -> brand.setValidated(true));
    }

    @Override
    @Transactional
    public void deleteById(final long brandId) {
        carBrandDao.deleteById(brandId);
    }
}
