package ar.edu.itba.paw.services.user;

import java.util.Optional;

/**
 * Resolves payout CBU for a car's owner using {@link ar.edu.itba.paw.services.car.CarService}
 * and {@link UserService#getUserCbu(long)} — no direct user persistence access.
 */
public interface UserOwnerCbuService {

    Optional<String> findOwnerCbuForCar(long carId);
}
