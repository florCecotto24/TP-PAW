package ar.edu.itba.paw.services.car.view;


import java.util.Optional;

import ar.edu.itba.paw.models.dto.car.PublishCarConfirmationPageModel;
import ar.edu.itba.paw.models.dto.car.PublishCarPendingPageModel;

/**
 * View facade for the post-publish confirmation screens served by
 * {@code GET /publish-car/pending/{carId}} and {@code GET /publish-car/confirmation/{carId}}.
 *
 * <p>Encapsulates the ownership check + catalog-validation lookups that used to live in
 * {@code PublishCarFormController#pendingView/confirmationView} so the controller only handles
 * mapping.</p>
 */
public interface CarPublishViewService {

    /**
     * Returns the page model for the "submission received, catalog entry pending" screen.
     *
     * <p>Empty {@link Optional} cases the controller must treat as redirects:</p>
     * <ul>
     *   <li>Car does not exist or does not belong to {@code viewerUserId} → redirect to {@code /my-cars}.</li>
     *   <li>Brand and model are already validated (nothing pending) → redirect to {@code /my-cars/car/{carId}}.</li>
     * </ul>
     *
     * <p>The {@link RedirectTarget} encoded on the {@link LoadResult} lets the controller
     * pick the right redirect without re-querying.</p>
     */
    LoadResult<PublishCarPendingPageModel> loadPendingView(long carId, long viewerUserId);

    /**
     * Returns the page model for the "publish confirmed" screen.
     *
     * <p>Empty result means: car does not exist or does not belong to {@code viewerUserId};
     * the controller should redirect to {@code /my-cars}.</p>
     */
    Optional<PublishCarConfirmationPageModel> loadConfirmationView(
            long carId, long viewerUserId, boolean newCatalogEntry);

    /** What the controller should do when the pending page model cannot be built. */
    enum RedirectTarget {
        /** Car missing or not owned → owner hub. */
        OWNER_HUB,
        /** Nothing pending anymore → car detail. */
        CAR_DETAIL
    }

    /** Either the page model, or a non-null redirect target. Never both. */
    final class LoadResult<T> {
        private final T pageModelOrNull;
        private final RedirectTarget redirectOrNull;

        private LoadResult(final T pageModelOrNull, final RedirectTarget redirectOrNull) {
            this.pageModelOrNull = pageModelOrNull;
            this.redirectOrNull = redirectOrNull;
        }

        public static <T> LoadResult<T> of(final T pageModel) {
            return new LoadResult<>(pageModel, null);
        }

        public static <T> LoadResult<T> redirect(final RedirectTarget target) {
            return new LoadResult<>(null, target);
        }

        public Optional<T> getPageModel() {
            return Optional.ofNullable(pageModelOrNull);
        }

        public Optional<RedirectTarget> getRedirect() {
            return Optional.ofNullable(redirectOrNull);
        }
    }
}
