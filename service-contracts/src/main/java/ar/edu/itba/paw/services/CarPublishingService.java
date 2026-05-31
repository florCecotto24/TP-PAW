package ar.edu.itba.paw.services;

import java.util.Locale;

import ar.edu.itba.paw.dto.PublishCarOutcome;
import ar.edu.itba.paw.dto.PublishCarRequest;

/**
 * Orchestrates the "publish car" submit. Encapsulates the decision the controller used to
 * make on its own ("admin auto-validates catalog entry / non-admin gets a pending car /
 * existing catalog entry just publishes") and the brand/model catalog resolution.
 *
 * Lives at the service layer because the decision is application-level orchestration of
 * {@link CarBrandService}, {@link CarModelService}, {@link AdminService} and
 * {@link CarService}. The controller is left with the parts that are genuinely web-only:
 * form binding, session stashes, gallery / insurance validation, and view selection.
 */
public interface CarPublishingService {

    /**
     * Resolves brand/model (creating unvalidated catalog rows when needed), then publishes the
     * car. If the catalog entry is new and the owner is not an admin, the resulting car is
     * persisted in pending-validation state — the caller should redirect to its pending-page
     * view. If the owner is an admin, the catalog entry is auto-validated before publishing.
     *
     * @param ownerId owner persisting the car
     * @param request payload coming from the publish form (already validated by the controller)
     * @param locale  locale used for admin notification messages
     * @throws ar.edu.itba.paw.exception.car.DuplicatePlateException when the plate is taken
     * @throws ar.edu.itba.paw.exception.RydenException for typed publish-time failures
     */
    PublishCarOutcome publishCar(long ownerId, PublishCarRequest request, Locale locale);
}
