package ar.edu.itba.paw.services.car.view;

import ar.edu.itba.paw.models.dto.car.MyCarsListPageModel;
import ar.edu.itba.paw.models.util.search.OwnerCarSearchCriteria;

/**
 * Read-only assembly for the {@code car/myCars.jsp} page rendered by
 * {@code MyCarsController.myCars}. Encapsulates the original two-service handoff the
 * controller used to do by hand: the {@link ar.edu.itba.paw.services.car.CarService}
 * search results and the {@link ar.edu.itba.paw.services.reservation.ReservationService}
 * "per-car refund-receipt pending" badge ids.
 */
public interface MyCarsListViewService {

    /**
     * @param criteria           owner-scoped search criteria built by the controller
     * @param currentSort        sanitized {@code sort,direction} string used by the JSP header
     * @param currentUserBlocked owner-blocked flag exposed to the view (drives blocked banners
     *                           and disables mutate actions); supplied by the controller from
     *                           the resolved {@code @CurrentUser} since the service may not
     *                           re-derive session state
     */
    MyCarsListPageModel loadMyCarsListPage(
            OwnerCarSearchCriteria criteria,
            String currentSort,
            boolean currentUserBlocked);
}
