package ar.edu.itba.paw.services;

import java.time.LocalTime;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.dto.car.CarAvailabilityEditorPageModel;

/**
 * Builds the {@link CarAvailabilityEditorPageModel} shared by both editor views in {@code MyCarsController}:
 * the publish flow ({@code car/createCarAvailability.jsp}) and the per-availability edit flow
 * ({@code car/editCarAvailability.jsp}).
 *
 * <p>Before this service, the same orchestration (publisher CBU lookup, neighborhood list, min/max
 * wall-time availability window, pickup-lead policy, publisher email, market-price insight) was
 * duplicated across the controller's {@code buildCreateListingView} and
 * {@code buildEditAvailabilityView} helpers.</p>
 */
public interface CarAvailabilityEditorViewService {

    /**
     * Loads the shared editor context bundle.
     *
     * @param car              owner-resolved car (controller has already vetted ownership / existence)
     * @param userId           publishing/editing user id; used for CBU validation and email lookup
     * @param checkInTime      currently entered pickup time on the form; drives the minimum wall-day
     *                         the user is allowed to publish/edit from (defaults to the default
     *                         pickup time when the form has not bound a value yet)
     */
    CarAvailabilityEditorPageModel loadEditorContext(Car car, long userId, LocalTime checkInTime);
}
