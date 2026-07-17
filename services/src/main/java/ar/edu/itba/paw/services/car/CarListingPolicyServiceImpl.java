package ar.edu.itba.paw.services.car;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.car.CarValidationException;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.file.StoredFile;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.util.media.BinaryMagicBytes;
import ar.edu.itba.paw.services.user.UserService;

@Service
public class CarListingPolicyServiceImpl implements CarListingPolicyService {

    private final UserService userService;

    @Autowired
    public CarListingPolicyServiceImpl(final UserService userService) {
        this.userService = userService;
    }

    @Override
    public List<Car.Status> resolveOwnerListingStatuses(
            final List<Car.Status> requestedStatuses, final boolean viewerIsSelfOrAdmin) {
        if (requestedStatuses != null && !requestedStatuses.isEmpty()) {
            return requestedStatuses;
        }
        return viewerIsSelfOrAdmin ? null : List.of(Car.Status.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public void requireOwnerNotBlocked(final long ownerId) {
        final Optional<User> ownerOpt = userService.getUserById(ownerId);
        if (ownerOpt.isPresent() && ownerOpt.get().isBlocked()) {
            throw new CarValidationException(MessageKeys.CAR_MUTATION_OWNER_BLOCKED);
        }
    }

    @Override
    public void assertInsurancePayloadValid(final String contentType, final byte[] data) {
        if (data == null || data.length == 0) {
            throw new CarValidationException(MessageKeys.CAR_INSURANCE_INVALID);
        }
        if (!StoredFile.isAllowedPaymentReceiptContentType(contentType)
                || !BinaryMagicBytes.matchesDeclared(contentType, data)) {
            throw new CarValidationException(MessageKeys.CAR_INSURANCE_INVALID);
        }
    }
}
