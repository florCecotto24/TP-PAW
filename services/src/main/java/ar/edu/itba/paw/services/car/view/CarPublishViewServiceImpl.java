package ar.edu.itba.paw.services.car.view;


import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.car.CarModel;
import ar.edu.itba.paw.models.dto.car.PublishCarConfirmationPageModel;
import ar.edu.itba.paw.models.dto.car.PublishCarPendingPageModel;

import ar.edu.itba.paw.services.car.CarService;
@Service
public final class CarPublishViewServiceImpl implements CarPublishViewService {

    private final CarService carService;

    @Autowired
    public CarPublishViewServiceImpl(final CarService carService) {
        this.carService = carService;
    }

    @Override
    @Transactional(readOnly = true)
    public LoadResult<PublishCarPendingPageModel> loadPendingView(
            final long carId, final long viewerUserId) {
        final Optional<Car> carOpt = carService.getCarById(carId);
        if (carOpt.isEmpty() || carOpt.get().getOwnerId() != viewerUserId) {
            return LoadResult.redirect(RedirectTarget.OWNER_HUB);
        }
        final Car car = carOpt.get();
        final Optional<CarModel> carModelOpt = car.getCarModel();
        final boolean brandPending = carModelOpt.map(m -> !m.getBrand().isValidated()).orElse(false);
        final boolean modelPending = carModelOpt.map(m -> !m.isValidated()).orElse(false);
        if (!brandPending && !modelPending) {
            return LoadResult.redirect(RedirectTarget.CAR_DETAIL);
        }
        final String pendingBrand = brandPending ? carModelOpt.get().getBrand().getName() : null;
        final String pendingModel = modelPending ? carModelOpt.get().getName() : null;
        return LoadResult.of(new PublishCarPendingPageModel(carId, pendingBrand, pendingModel));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PublishCarConfirmationPageModel> loadConfirmationView(
            final long carId, final long viewerUserId, final boolean newCatalogEntry) {
        return carService.getCarById(carId)
                .filter(c -> c.getOwnerId() == viewerUserId)
                .map(c -> new PublishCarConfirmationPageModel(c, newCatalogEntry));
    }
}
