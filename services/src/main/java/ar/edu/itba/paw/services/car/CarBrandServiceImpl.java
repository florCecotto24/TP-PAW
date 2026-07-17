package ar.edu.itba.paw.services.car;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.exception.car.CarBrandConflictException;
import ar.edu.itba.paw.models.domain.car.CarBrand;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.persistence.car.CarBrandDao;

/** Catalog reads via {@link CarBrandDao}; "Other" creation is normalized here before delegating. */
@Service
public class CarBrandServiceImpl implements CarBrandService {

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
    public Page<CarBrand> findPage(final Boolean validated, final int page, final int pageSize) {
        return carBrandDao.findPage(validated, page, pageSize);
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
        try {
            return Optional.of(carBrandDao.create(normalized, false));
        } catch (final DataIntegrityViolationException ex) {
            // Concurrent publish created the same unvalidated brand name (V45 unique).
            return carBrandDao.findByNameIgnoreCase(normalized);
        }
    }

    @Override
    @Transactional
    public Optional<CarBrand> createUnvalidated(final String rawName) {
        if (rawName == null) {
            return Optional.empty();
        }
        final String normalized = rawName.trim();
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        final Optional<CarBrand> existing = carBrandDao.findByNameIgnoreCase(normalized);
        if (existing.isPresent()) {
            throw new CarBrandConflictException(existing.get().getId());
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
