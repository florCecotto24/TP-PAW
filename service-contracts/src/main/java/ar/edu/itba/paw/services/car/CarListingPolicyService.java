package ar.edu.itba.paw.services.car;

import java.util.List;

import ar.edu.itba.paw.models.domain.car.Car;

/**
 * In-memory listing policy and owner mutation guards.
 * No {@code CarDao} — blocked-owner checks go through {@link ar.edu.itba.paw.services.user.UserService}.
 */
public interface CarListingPolicyService {

    List<Car.Status> resolveOwnerListingStatuses(
            List<Car.Status> requestedStatuses, boolean viewerIsSelfOrAdmin);

    /**
     * @throws ar.edu.itba.paw.exception.car.CarValidationException when the owner is blocked
     */
    void requireOwnerNotBlocked(long ownerId);

    /**
     * @throws ar.edu.itba.paw.exception.car.CarValidationException when bytes/type are invalid
     */
    void assertInsurancePayloadValid(String contentType, byte[] data);
}
