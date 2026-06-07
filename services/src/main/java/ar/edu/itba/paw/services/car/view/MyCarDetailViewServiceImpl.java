package ar.edu.itba.paw.services.car.view;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.dto.car.MyCarDetailPageModel;
import ar.edu.itba.paw.models.dto.car.OwnerCarDetailPageModel;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.reservation.ReservationCard;
import ar.edu.itba.paw.models.dto.reservation.ReservationCardDisplayRow;

import ar.edu.itba.paw.services.car.CarPictureService;
import ar.edu.itba.paw.services.car.CarService;
import ar.edu.itba.paw.services.reservation.ReservationService;
import ar.edu.itba.paw.services.reservation.view.ReservationViewService;
/**
 * Implementation of {@link MyCarDetailViewService}. Decides between the rich and fallback variants
 * and, in the rich case, fetches the top-N preview reservations and maps them to display rows.
 */
@Service
public final class MyCarDetailViewServiceImpl implements MyCarDetailViewService {

    private final CarService carService;
    private final CarPictureService carPictureService;
    private final ReservationService reservationService;
    private final ReservationViewService reservationViewService;

    @Autowired
    public MyCarDetailViewServiceImpl(
            final CarService carService,
            final CarPictureService carPictureService,
            final ReservationService reservationService,
            final ReservationViewService reservationViewService) {
        this.carService = carService;
        this.carPictureService = carPictureService;
        this.reservationService = reservationService;
        this.reservationViewService = reservationViewService;
    }

    @Override
    @Transactional(readOnly = true)
    public MyCarDetailPageModel loadMyCarDetailPage(
            final long ownerUserId, final Car car, final Locale locale, final int previewLimit) {
        final Optional<OwnerCarDetailPageModel> pmOpt =
                carService.buildOwnerCarDetailPageModel(car.getId(), locale);
        if (pmOpt.isPresent()) {
            final Page<ReservationCard> previewPage =
                    reservationService.getCarReservationCards(ownerUserId, car.getId(), 0, previewLimit, null);
            final List<ReservationCardDisplayRow> previewRows = previewPage.getContent().stream()
                    .map(card -> reservationViewService.toReservationCardDisplayRow(card, locale))
                    .toList();
            return MyCarDetailPageModel.rich(pmOpt.get(), previewRows, previewPage.getTotalItems());
        }
        final long firstImageId = carPictureService.getCarPicturesByCarId(car.getId()).stream()
                .map(p -> p.getImageId())
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(0L);
        return MyCarDetailPageModel.fallback(firstImageId);
    }
}
