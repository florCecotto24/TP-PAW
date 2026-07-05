package ar.edu.itba.paw.services.car.view;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.car.CarCard;
import ar.edu.itba.paw.models.dto.car.MyCarsListPageModel;
import ar.edu.itba.paw.models.util.search.OwnerCarSearchCriteria;
import ar.edu.itba.paw.services.car.CarService;
import ar.edu.itba.paw.services.reservation.ReservationService;

/**
 * Owns the controller-side stitching that {@code MyCarsController.myCars} used to do by hand:
 * the owner-cars search and the per-card "refund-receipt pending" badge ids. Folding both into a
 * single page model keeps the GET handler down to one view-service call.
 */
@Service
public class MyCarsListViewServiceImpl implements MyCarsListViewService {

    private final CarService carService;
    private final ReservationService reservationService;

    @Autowired
    public MyCarsListViewServiceImpl(
            final CarService carService,
            final ReservationService reservationService) {
        this.carService = carService;
        this.reservationService = reservationService;
    }

    @Override
    @Transactional(readOnly = true)
    public MyCarsListPageModel loadMyCarsListPage(
            final OwnerCarSearchCriteria criteria,
            final String currentSort,
            final boolean currentUserBlocked) {
        final Page<CarCard> resultPage = carService.getOwnerCarCards(criteria);
        final Set<Long> pendingRefundCarIds =
                reservationService.findOwnerCarIdsWithReservationRequiringRefundProof(criteria.getOwnerId());
        return new MyCarsListPageModel(resultPage, currentSort, currentUserBlocked, pendingRefundCarIds);
    }
}
