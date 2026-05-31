package ar.edu.itba.paw.services;

import java.util.Locale;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.reservation.ReservationConfirmationPageModel;
import ar.edu.itba.paw.models.dto.reservation.ReservationFormPageModel;

/**
 * Read-only API for the rider reservation form (GET form + POST error re-renders + POST confirmation).
 * Encapsulates the five-service bundle that {@code ReservationFormController} used to repeat in four
 * branches of {@code formSubmit} (initial render, hasErrors, blank carName, ReservationException),
 * plus the day-effective pickup location resolution.
 */
public interface ReservationFormViewService {

    /**
     * Loads the bundle of model attributes the {@code reservationForm} JSP needs. Used both for
     * the initial GET render and for every error-path re-render of {@code formSubmit}.
     *
     * @param carId            target car
     * @param rider            authenticated rider (resolved upstream); never null
     * @param fromDateTime     raw wall-local pickup datetime input (may be null/blank)
     * @param untilDateTime    raw wall-local return datetime input (may be null/blank)
     * @param reservationTotal raw client-submitted total (echoed back for the confirmation guard)
     * @param carNameOverride  optional car-name override coming from the query string; falls back
     *                         to {@code car.brand + " " + car.model}
     * @param locale           locale used for the formatted display strings
     * @return empty when the car does not exist (controller should redirect to {@code /search});
     *         otherwise the populated model
     */
    Optional<ReservationFormPageModel> loadReservationFormPage(
            long carId,
            User rider,
            String fromDateTime,
            String untilDateTime,
            String reservationTotal,
            String carNameOverride,
            Locale locale);

    /**
     * Builds the {@code reservationConfirmation} JSP page model after a successful submit.
     */
    ReservationConfirmationPageModel buildReservationConfirmation(
            long carId,
            User rider,
            String carName,
            String fromDateTime,
            String untilDateTime,
            Long availabilityId,
            Reservation reservation,
            Locale locale);
}
