package ar.edu.itba.paw.services.car.view;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.car.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.car.CarAvailability;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.dto.car.CarAvailabilityEditorPageModel;
import ar.edu.itba.paw.models.dto.car.ManageCarPeriodsPageModel;
import ar.edu.itba.paw.models.util.time.BookableWallRangesJson;

import ar.edu.itba.paw.services.car.CarAvailabilityService;
import ar.edu.itba.paw.services.car.CarPictureService;
@Service
public final class ManageCarPeriodsViewServiceImpl implements ManageCarPeriodsViewService {

    private final CarAvailabilityService carAvailabilityService;
    private final CarPictureService carPictureService;
    private final CarAvailabilityEditorViewService carAvailabilityEditorViewService;

    @Autowired
    public ManageCarPeriodsViewServiceImpl(
            final CarAvailabilityService carAvailabilityService,
            final CarPictureService carPictureService,
            final CarAvailabilityEditorViewService carAvailabilityEditorViewService) {
        this.carAvailabilityService = carAvailabilityService;
        this.carPictureService = carPictureService;
        this.carAvailabilityEditorViewService = carAvailabilityEditorViewService;
    }

    @Override
    @Transactional(readOnly = true)
    public ManageCarPeriodsPageModel loadManageCarPeriodsPage(
            final Car car, final YearMonth activeMonth, final Locale locale) {
        final long carId = car.getId();
        final User owner = car.getOwner();

        final List<CarAvailability> allAvailabilities =
                carAvailabilityService.findEffectiveOfferedByCar(carId);

        final LocalDate firstDay = activeMonth.atDay(1);
        final LocalDate lastDay = activeMonth.atEndOfMonth();
        final List<CarAvailability> monthAvailabilities = allAvailabilities.stream()
                .filter(a -> !a.getStartInclusive().isAfter(lastDay)
                        && !a.getEndInclusive().isBefore(firstDay))
                .collect(Collectors.toList());

        final String allSegmentsJson = BookableWallRangesJson.toJsonArray(
                carAvailabilityService.getAllEffectiveSegmentsForOwnerCalendar(carId));

        final List<AvailabilityPeriod> reservationBlockedRanges =
                carAvailabilityService.findReservationBlockedWallRangesByCar(carId);
        final String reservationBlockedRangesJson =
                BookableWallRangesJson.availabilityPeriodsToJsonArray(reservationBlockedRanges);

        // Per-period reserved sub-ranges: for each availability on screen, intersect the
        // car's blocked ranges (= active reservations) with the period window and clip to
        // the period bounds. Used by the JSP to expose data-period-reserved-ranges so the
        // edit-picker can mark those days and reject shrinks that would orphan a reservation.
        final Map<Long, String> reservedRangesByAvailabilityIdJson =
                new HashMap<>(monthAvailabilities.size());
        for (final CarAvailability av : monthAvailabilities) {
            final LocalDate aStart = av.getStartInclusive();
            final LocalDate aEnd = av.getEndInclusive();
            final List<AvailabilityPeriod> insidePeriod = reservationBlockedRanges.stream()
                    .filter(r -> !r.getEndInclusive().isBefore(aStart)
                            && !r.getStartInclusive().isAfter(aEnd))
                    .map(r -> new AvailabilityPeriod(
                            r.getStartInclusive().isBefore(aStart) ? aStart : r.getStartInclusive(),
                            r.getEndInclusive().isAfter(aEnd) ? aEnd : r.getEndInclusive()))
                    .collect(Collectors.toList());
            reservedRangesByAvailabilityIdJson.put(
                    av.getId(),
                    BookableWallRangesJson.availabilityPeriodsToJsonArray(insidePeriod));
        }

        final long carImageId = carPictureService.getCarPicturesByCarId(carId).stream()
                .map(p -> p.getImageId())
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(0L);

        final String statusKey = (allAvailabilities.isEmpty() && car.getStatus() == Car.Status.ACTIVE
                ? Car.Status.UNAVAILABLE : car.getStatus()).name();

        final boolean canManage = !Car.Status.DEACTIVATED.name().equals(statusKey)
                && !Car.Status.ADMIN_PAUSED.name().equals(statusKey)
                && !owner.isBlocked();

        // Single fetch of the most-recent availability: feeds the editor's checkInTime hint AND
        // the GET handler's create-form prefill (street, neighborhood, times). Returning the row
        // through the page model keeps the controller from issuing the same query a second time.
        final CarAvailability mostRecentOrNull =
                carAvailabilityService.findMostRecentByCarId(carId).orElse(null);
        final LocalTime checkInTime = mostRecentOrNull != null ? mostRecentOrNull.getCheckInTime() : null;
        final CarAvailabilityEditorPageModel editorCtx =
                carAvailabilityEditorViewService.loadEditorContext(car, owner.getId(), checkInTime);

        final boolean isFirstPeriod = allAvailabilities.isEmpty();

        final String activeMonthName = DateTimeFormatter.ofPattern("MMMM", locale).format(activeMonth);

        return new ManageCarPeriodsPageModel(
                car,
                owner,
                statusKey,
                carImageId,
                allSegmentsJson,
                monthAvailabilities,
                activeMonth,
                activeMonthName,
                canManage,
                isFirstPeriod,
                editorCtx.isUserHasCbu(),
                editorCtx.getAllNeighborhoods(),
                editorCtx.getPublishMinAvailabilityFrom(),
                editorCtx.getPickupLeadHours(),
                editorCtx.getMaxAvailabilityForwardWallDays(),
                editorCtx.getPublishMaxAvailabilityWallInclusive(),
                editorCtx.getPublisherEmail(),
                editorCtx.getPriceMarketInsight().orElse(null),
                reservationBlockedRangesJson,
                reservedRangesByAvailabilityIdJson,
                mostRecentOrNull);
    }
}
