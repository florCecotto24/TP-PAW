package ar.edu.itba.paw.services.reservation;


import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.reservation.ReservationCard;
import ar.edu.itba.paw.models.util.search.ReservationSearchCriteria;

/**
 * Read-only side of the reservation hub. Architectural rule: this service no longer touches
 * {@code ReservationDao} — all row reads are funneled through {@link ReservationService}
 * (the sole owner of the DAO). Pagination is also not read here: callers (controllers) inject
 * {@code AppPaginationProperties} and pass {@code pageSize} into the criteria builder.
 */
@Service
public final class ReservationQueryServiceImpl implements ReservationQueryService {

    private static final Set<String> RATING_BANDS = Set.of("UNDER_2", "2_TO_3", "3_TO_4", "OVER_4");

    private final ReservationService reservationService;

    @Autowired
    public ReservationQueryServiceImpl(
            @Lazy final ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Reservation> getReservationById(final long id) {
        return reservationService.getReservationById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Reservation> getRiderReservationById(final long riderId, final long reservationId) {
        return reservationService.getRiderReservationById(riderId, reservationId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Reservation> getOwnerReservationById(final long ownerId, final long reservationId) {
        return reservationService.getOwnerReservationById(ownerId, reservationId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReservationCard> getRiderReservationCards(final ReservationSearchCriteria criteria) {
        return reservationService.getRiderReservationCards(criteria);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReservationCard> getOwnerReservationCards(final ReservationSearchCriteria criteria) {
        return reservationService.getOwnerReservationCards(criteria);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReservationCard> getCarReservationCards(
            final long ownerId,
            final long carId,
            final int page,
            final int pageSize,
            final String statusFilter) {
        return reservationService.getCarReservationCards(ownerId, carId, page, pageSize, statusFilter);
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationSearchCriteria buildReservationSearchCriteria(
            final Long ownerId,
            final Long riderId,
            final List<Car.Type> category,
            final List<Car.Transmission> transmission,
            final List<Car.Powertrain> powertrain,
            final BigDecimal priceMin,
            final BigDecimal priceMax,
            final List<String> rating,
            final List<Reservation.Status> statusFilter,
            final int page,
            final int pageSize,
            final String sort,
            final String textQuery,
            final Long carId) {
        // Spring already validated each enum token via the Web converters; this layer just normalises
        // the lists. The criteria DTO and ReservationJpaDao expect uppercase Enum.name() for the car
        // attribute filters and lowercase DB tokens (matching Reservation.StatusConverter) for the
        // status filter — invalid values now produce 400 before reaching this method.
        final List<String> carTypes = enumNames(category);
        final List<String> transmissions = enumNames(transmission);
        final List<String> powertrains = enumNames(powertrain);
        final BigDecimal minPrice = priceMin != null && priceMin.compareTo(BigDecimal.ZERO) >= 0 ? priceMin : null;
        final BigDecimal maxPrice = priceMax != null && priceMax.compareTo(BigDecimal.ZERO) >= 0 ? priceMax : null;
        final List<String> statuses = enumDbTokens(statusFilter);
        final ArrayList<String> ratingBands = new ArrayList<>();
        if (rating != null) {
            for (final String r : rating) {
                if (r == null || r.isBlank()) {
                    continue;
                }
                final String u = r.trim().toUpperCase();
                if (RATING_BANDS.contains(u)) {
                    ratingBands.add(u);
                }
            }
        }
        final String[] sortParts = (sort != null && !sort.isBlank()) ? sort.split(",", 2) : new String[0];
        final String sortBy = sortParts.length > 0 ? sortParts[0].trim() : "date";
        final String sortDir = sortParts.length > 1 ? sortParts[1].trim() : "desc";
        return new ReservationSearchCriteria(
                ownerId, riderId, carId, page, pageSize, statuses,
                carTypes, transmissions, powertrains, minPrice, maxPrice, ratingBands, sortBy, sortDir, textQuery);
    }

    private static <E extends Enum<E>> List<String> enumNames(final List<E> raw) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        final LinkedHashSet<String> out = new LinkedHashSet<>();
        for (final E e : raw) {
            if (e != null) {
                out.add(e.name());
            }
        }
        return new ArrayList<>(out);
    }

    private static <E extends Enum<E>> List<String> enumDbTokens(final List<E> raw) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        final LinkedHashSet<String> out = new LinkedHashSet<>();
        for (final E e : raw) {
            if (e != null) {
                out.add(e.name().toLowerCase(Locale.ROOT));
            }
        }
        return new ArrayList<>(out);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reservation> findBlockingReservationsByCarId(final long carId) {
        return reservationService.findBlockingReservationsByCarId(carId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, List<Reservation>> findBlockingReservationsByCarIds(
            final Collection<Long> carIds) {
        return reservationService.findBlockingReservationsByCarIds(carIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reservation> findBlockingReservationsByCarIdExcluding(
            final long carId, final long excludingReservationId) {
        return reservationService.findBlockingReservationsByCarIdExcluding(carId, excludingReservationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reservation> findBlockingReservationsByCarIdInRange(
            final long carId, final OffsetDateTime from, final OffsetDateTime to) {
        return reservationService.findBlockingReservationsByCarIdInRange(carId, from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reservation> findReminderReservations(final OffsetDateTime from, final OffsetDateTime to) {
        return reservationService.findReminderReservations(from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReservationCard> findAllReservationCards(final int page, final int pageSize) {
        return reservationService.findAllReservationCards(page, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findOverdueRefundProofReservationIdsForOwner(final long ownerUserId) {
        return reservationService.findOverdueRefundProofReservationIdsForOwner(ownerUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> findOwnerReservationIdsRequiringRefundProof(final long ownerUserId) {
        return reservationService.findOwnerReservationIdsRequiringRefundProof(ownerUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> findOwnerCarIdsWithReservationRequiringRefundProof(final long ownerUserId) {
        return reservationService.findOwnerCarIdsWithReservationRequiringRefundProof(ownerUserId);
    }
}
