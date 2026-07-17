package ar.edu.itba.paw.services.user;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.exception.user.CBUNotFoundException;
import ar.edu.itba.paw.exception.user.UserNotFoundException;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.services.car.CarService;

@Service
public class UserOwnerCbuServiceImpl implements UserOwnerCbuService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserOwnerCbuServiceImpl.class);

    private final CarService carService;
    private final UserService userService;

    @Autowired
    public UserOwnerCbuServiceImpl(final CarService carService, @Lazy final UserService userService) {
        this.carService = carService;
        this.userService = userService;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findOwnerCbuForCar(final long carId) {
        final Optional<Long> ownerIdOpt = carService.getCarById(carId)
                .map(Car::getOwner)
                .map(User::getId);
        if (ownerIdOpt.isEmpty()) {
            LOGGER.atWarn().addArgument(carId).log("Car owner missing when resolving CBU for car (carId={})");
            return Optional.empty();
        }
        final long ownerId = ownerIdOpt.get();
        try {
            return Optional.of(userService.getUserCbu(ownerId));
        } catch (final UserNotFoundException | CBUNotFoundException e) {
            LOGGER.atWarn()
                    .setCause(e)
                    .addArgument(carId)
                    .addArgument(ownerId)
                    .log("Owner CBU unavailable for car (carId={}, ownerId={})");
            return Optional.empty();
        }
    }
}
