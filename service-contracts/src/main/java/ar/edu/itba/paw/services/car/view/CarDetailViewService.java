package ar.edu.itba.paw.services.car.view;

import java.util.Locale;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.car.detail.CarDetailPageModel;

/**
 * Builds the {@link CarDetailPageModel} consumed by {@code GET /cars/{carId}}. Encapsulates the
 * orchestration that used to live in {@code CarDetailController.carDetail}: car lookup,
 * owner-blocked visibility rule, owner profile picture, gallery, similar listings, bookable
 * segments + JSON serialization, rating / review labels, paged reviews (with localized dates),
 * favorite flags, and the admin/owner branding flags.
 */
public interface CarDetailViewService {

    /**
     * Loads the public car-detail page for {@code carId}, applying every domain-level
     * visibility rule. Returns empty (instructing the controller to redirect to {@code /search})
     * when:
     *
     * <ul>
     *   <li>the car does not exist;</li>
     *   <li>the car's owner is blocked and the viewer is neither the owner nor an admin.</li>
     * </ul>
     *
     * @param carId            target car
     * @param viewer           authenticated user, or {@code null} when anonymous
     * @param viewerIsAdmin    {@code true} when the authenticated viewer has the admin role
     * @param reviewPageParam  raw {@code reviewPage} query parameter; clamped to {@code >= 0}
     * @param reviewsPageSize  number of public reviews per UI page (controller-supplied from
     *                         {@code AppPaginationProperties})
     * @param reviewsViewParam raw {@code reviewsView} query parameter; normalized to
     *                         {@code "carousel"} or {@code "list"}
     * @param locale           locale used to format review dates and rating labels
     */
    Optional<CarDetailPageModel> loadCarDetailPage(
            long carId,
            User viewer,
            boolean viewerIsAdmin,
            int reviewPageParam,
            int reviewsPageSize,
            String reviewsViewParam,
            Locale locale);
}
