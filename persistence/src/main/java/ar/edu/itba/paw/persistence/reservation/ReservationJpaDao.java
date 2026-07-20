package ar.edu.itba.paw.persistence.reservation;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.car.CarModel;
import ar.edu.itba.paw.models.domain.file.StoredFile;
import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.reservation.BlockingReservationProjection;
import ar.edu.itba.paw.models.dto.reservation.ReservationCard;
import ar.edu.itba.paw.models.util.search.ReservationSearchCriteria;
import ar.edu.itba.paw.models.util.time.BillableDays;
import ar.edu.itba.paw.persistence.car.CarPictureDao;
import static ar.edu.itba.paw.persistence.util.JpaQueryUtils.bindParams;

@Transactional(readOnly = true)
@Repository
public class ReservationJpaDao implements ReservationDao {

    private static final String SELECT_BLOCKING_PROJECTION =
            "SELECT new " + BlockingReservationProjection.class.getName() + "(";

    private static final List<Reservation.Status> BLOCKING_STATUSES = Arrays.asList(
            Reservation.Status.PENDING, Reservation.Status.ACCEPTED, Reservation.Status.STARTED);

    private static final List<Reservation.Status> REFUND_ELIGIBLE_CANCELLED_STATUSES = Arrays.asList(
            Reservation.Status.CANCELLED_BY_OWNER, Reservation.Status.CANCELLED_BY_RIDER);

    private static final List<Reservation.Status> RETURN_EMAIL_ACTIVE_STATUSES = Arrays.asList(
            Reservation.Status.ACCEPTED, Reservation.Status.STARTED);

    private static final List<Reservation.Status> RIDER_REVIEW_INVITE_STATUSES = Arrays.asList(
            Reservation.Status.ACCEPTED, Reservation.Status.STARTED, Reservation.Status.FINISHED);

    private static final List<Reservation.Status> REVIEW_AUTO_SKIP_STATUSES = Arrays.asList(
            Reservation.Status.ACCEPTED, Reservation.Status.STARTED, Reservation.Status.FINISHED);

    /**
     * Preloads rider, car, owner and catalog labels for batch mail/scheduler jobs that call
     * {@code resolveOwnerFromReservation} / {@code resolveVehicleLabelFromReservation} / rider
     * display per row (avoids N {@code getUserById} / brand loads).
     */
    private static final String FETCH_RESERVATION_CAR_CATALOG_FOR_MAIL =
            "JOIN FETCH r.rider JOIN FETCH r.car c JOIN FETCH c.owner "
                    + "LEFT JOIN FETCH c.carModel cm LEFT JOIN FETCH cm.brand ";

    /**
     * Cross-aggregate DAO intentionally injected to keep {@link #loadReservationCardsByIdNativeQuery}
     * down to three queries per page (id-pagination native SQL, JPQL {@code JOIN FETCH} for the
     * reservation + car catalog, and a batched cover-image lookup). Composing this at the service
     * layer would either reintroduce N+1 reads against {@code car_pictures} or force every
     * {@code ReservationCard} caller through a view-service shim. {@code AGENTS.md} cites
     * {@code loadReservationCardsByIdNativeQuery} as the reference example for the N+1 rule and
     * the same pattern lives in {@link ar.edu.itba.paw.persistence.car.CarJpaDao} and
     * {@link ar.edu.itba.paw.persistence.car.FavCarJpaDao}.
     */
    private final CarPictureDao carPictureDao;
    private final int paymentProofReminderLeadHours;

    @PersistenceContext
    private EntityManager em;

    @Autowired
    public ReservationJpaDao(
            final CarPictureDao carPictureDao,
            @Value("${app.reservation.payment-proof-reminder-lead-hours:2}") final int paymentProofReminderLeadHours) {
        this.carPictureDao = carPictureDao;
        this.paymentProofReminderLeadHours = paymentProofReminderLeadHours;
    }

    @Override
    public boolean hasActiveOverlapByCar(final long carId, final OffsetDateTime startDate, final OffsetDateTime endDate) {
        final Number count = (Number) em.createQuery(
                        "SELECT COUNT(r) FROM Reservation r "
                                + "WHERE r.car.id = :carId "
                                + "AND r.status IN :statuses "
                                + "AND r.startDate < :endDate "
                                + "AND r.endDate > :startDate")
                .setParameter("carId", carId)
                .setParameter("statuses", BLOCKING_STATUSES)
                .setParameter("endDate", endDate)
                .setParameter("startDate", startDate)
                .getSingleResult();
        return count != null && count.longValue() > 0;
    }

    @Override
    public boolean hasActiveOverlapByCarExcluding(
            final long carId,
            final OffsetDateTime startDate,
            final OffsetDateTime endDate,
            final long excludingReservationId) {
        final Number count = (Number) em.createQuery(
                        "SELECT COUNT(r) FROM Reservation r "
                                + "WHERE r.car.id = :carId "
                                + "AND r.status IN :statuses "
                                + "AND r.id <> :excludeId "
                                + "AND r.startDate < :endDate "
                                + "AND r.endDate > :startDate")
                .setParameter("carId", carId)
                .setParameter("statuses", BLOCKING_STATUSES)
                .setParameter("excludeId", excludingReservationId)
                .setParameter("endDate", endDate)
                .setParameter("startDate", startDate)
                .getSingleResult();
        return count != null && count.longValue() > 0;
    }

    @Override
    public List<BlockingReservationProjection> findBlockingByCarId(final long carId) {
        return em.createQuery(
                        SELECT_BLOCKING_PROJECTION
                                + "r.id, r.car.id, r.startDate, r.endDate, r.status) "
                                + "FROM Reservation r WHERE r.car.id = :carId AND r.status IN :statuses "
                                + "ORDER BY r.startDate ASC",
                        BlockingReservationProjection.class)
                .setParameter("carId", carId)
                .setParameter("statuses", BLOCKING_STATUSES)
                .getResultList();
    }

    @Override
    public List<BlockingReservationProjection> findBlockingByCarIds(final Collection<Long> carIds) {
        if (carIds == null || carIds.isEmpty()) {
            return List.of();
        }
        return em.createQuery(
                        SELECT_BLOCKING_PROJECTION
                                + "r.id, r.car.id, r.startDate, r.endDate, r.status) "
                                + "FROM Reservation r WHERE r.car.id IN :carIds AND r.status IN :statuses "
                                + "ORDER BY r.car.id ASC, r.startDate ASC",
                        BlockingReservationProjection.class)
                .setParameter("carIds", carIds)
                .setParameter("statuses", BLOCKING_STATUSES)
                .getResultList();
    }

    @Override
    public List<BlockingReservationProjection> findBlockingByCarIdExcluding(
            final long carId, final long excludingReservationId) {
        return em.createQuery(
                        SELECT_BLOCKING_PROJECTION
                                + "r.id, r.car.id, r.startDate, r.endDate, r.status) FROM Reservation r "
                                + "WHERE r.car.id = :carId AND r.status IN :statuses AND r.id <> :excludeId "
                                + "ORDER BY r.startDate ASC",
                        BlockingReservationProjection.class)
                .setParameter("carId", carId)
                .setParameter("statuses", BLOCKING_STATUSES)
                .setParameter("excludeId", excludingReservationId)
                .getResultList();
    }

    @Override
    public List<BlockingReservationProjection> findBlockingByCarIdInRange(
            final long carId, final OffsetDateTime from, final OffsetDateTime to) {
        return em.createQuery(
                        SELECT_BLOCKING_PROJECTION
                                + "r.id, r.car.id, r.startDate, r.endDate, r.status) "
                                + "FROM Reservation r WHERE r.car.id = :carId AND r.status IN :statuses "
                                + "AND r.startDate < :to AND r.endDate > :from ORDER BY r.startDate ASC",
                        BlockingReservationProjection.class)
                .setParameter("carId", carId)
                .setParameter("statuses", BLOCKING_STATUSES)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }

    @Override
    @Transactional
    public Reservation createReservationForCar(
            final long riderId,
            final long carId,
            final OffsetDateTime startDate,
            final OffsetDateTime endDate,
            final Reservation.Status status,
            final BigDecimal totalPrice,
            final OffsetDateTime paymentProofDeadlineAt) {
        final User riderRef = em.getReference(User.class, riderId);
        final Car carRef = em.getReference(Car.class, carId);
        final OffsetDateTime now = OffsetDateTime.now();
        final Reservation reservation = Reservation.builder()
                .rider(riderRef)
                .car(carRef)
                .startDate(startDate)
                .endDate(endDate)
                .status(status)
                .totalPrice(totalPrice)
                .createdAt(now)
                .updatedAt(now)
                .paymentProofDeadlineAt(paymentProofDeadlineAt)
                .carReturned(false)
                .paymentRefundRequired(false)
                .returnReminderEmailSent(false)
                .returnCheckoutEmailSent(false)
                .riderReviewInviteEmailSent(false)
                .pendingPaymentProofEmailSent(false)
                .pendingRefundEmailSent(false)
                .pickupReminderEmailSent(false)
                .build();
        em.persist(reservation);
        return reservation;
    }

    @Override
    public List<Reservation> findPendingPaymentPastDeadline(final OffsetDateTime now) {
        return em.createQuery(
                        "FROM Reservation r "
                                + FETCH_RESERVATION_CAR_CATALOG_FOR_MAIL
                                + "WHERE r.status = :status "
                                + "AND r.paymentProofDeadlineAt IS NOT NULL AND r.paymentProofDeadlineAt < :now "
                                + "ORDER BY r.id ASC",
                        Reservation.class)
                .setParameter("status", Reservation.Status.PENDING)
                .setParameter("now", now)
                .getResultList();
    }

    @Override
    @Transactional
    public int cancelPendingMissingPaymentProofIfEligible(final long reservationId, final OffsetDateTime now) {
        final Reservation r = em.find(Reservation.class, reservationId, LockModeType.PESSIMISTIC_WRITE);
        if (r == null
                || r.getStatus() != Reservation.Status.PENDING
                || hasPaymentReceiptFileId(reservationId)) {
            return 0;
        }
        if (r.getPaymentProofDeadlineAt().map(deadline -> !now.isAfter(deadline)).orElse(true)) {
            return 0;
        }
        r.setStatus(Reservation.Status.CANCELLED_DUE_TO_MISSING_PAYMENT_PROOF);
        r.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return 1;
    }

    private boolean hasPaymentReceiptFileId(final long reservationId) {
        final Number count = (Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM reservations WHERE id = :id AND payment_receipt_file_id IS NOT NULL")
                .setParameter("id", reservationId)
                .getSingleResult();
        return count != null && count.longValue() > 0L;
    }

    @Override
    @Transactional
    public int attachPaymentReceiptAndAccept(final long reservationId, final long riderId, final long storedFileId) {
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        final Reservation r = em.find(Reservation.class, reservationId, LockModeType.PESSIMISTIC_WRITE);
        if (r == null
                || r.getRiderId() != riderId
                || r.getStatus() != Reservation.Status.PENDING
                || r.getPaymentReceiptFileId().isPresent()) {
            return 0;
        }
        if (r.getPaymentProofDeadlineAt().map(now::isAfter).orElse(false)) {
            return 0;
        }
        r.setStatus(Reservation.Status.ACCEPTED);
        r.setPaymentReceiptFile(em.getReference(StoredFile.class, storedFileId));
        r.setUpdatedAt(now);
        return 1;
    }

    @Override
    public Optional<Reservation> getReservationById(final long id) {
        return em.createQuery(
                        "FROM Reservation r "
                                + FETCH_RESERVATION_CAR_CATALOG_FOR_MAIL
                                + "WHERE r.id = :id",
                        Reservation.class)
                .setParameter("id", id)
                .getResultList().stream().findAny();
    }

    @Override
    public Optional<Reservation> getOwnerReservationById(final long ownerId, final long reservationId) {
        return em.createQuery(
                        "FROM Reservation r "
                                + FETCH_RESERVATION_CAR_CATALOG_FOR_MAIL
                                + "WHERE r.id = :reservationId AND c.owner.id = :ownerId",
                        Reservation.class)
                .setParameter("reservationId", reservationId)
                .setParameter("ownerId", ownerId)
                .getResultList().stream().findAny();
    }

    @Override
    public boolean existsByRiderIdAndCarId(final long riderId, final long carId) {
        final Number count = (Number) em.createQuery(
                        "SELECT COUNT(r) FROM Reservation r "
                                + "WHERE r.rider.id = :riderId AND r.car.id = :carId")
                .setParameter("riderId", riderId)
                .setParameter("carId", carId)
                .getSingleResult();
        return count != null && count.longValue() > 0;
    }

    @Override
    public Page<ReservationCard> getRiderReservationCards(final ReservationSearchCriteria criteria) {
        final int page = criteria.getPage();
        final int pageSize = criteria.getPageSize();
        final Map<String, Object> countParams = new HashMap<>();
        countParams.put("riderId", criteria.getRiderId());
        final StringBuilder countSql = new StringBuilder(
                "SELECT COUNT(*) FROM reservations r "
                        + "JOIN cars c ON c.id = r.car_id "
                        + "WHERE r.rider_id = :riderId ");
        appendReservationFilters(countSql, countParams, criteria);
        final Number total = (Number) bindParams(em.createNativeQuery(countSql.toString()), countParams).getSingleResult();

        final int offset = page * pageSize;
        final Map<String, Object> listParams = new HashMap<>();
        listParams.put("riderId", criteria.getRiderId());
        listParams.put("limit", pageSize);
        listParams.put("offset", offset);
        final StringBuilder idSql = new StringBuilder(
                "SELECT r.id FROM reservations r "
                + "JOIN cars c ON c.id = r.car_id "
                + "WHERE r.rider_id = :riderId ");
        appendReservationFilters(idSql, listParams, criteria);
        idSql.append("ORDER BY ").append(buildReservationOrderBy(criteria.getSortBy(), criteria.getSortDirection()))
             .append(" LIMIT :limit OFFSET :offset");
        final List<ReservationCard> content = loadReservationCardsByIdNativeQuery(idSql.toString(), listParams);
        return new Page<>(content, page, pageSize, total != null ? total.longValue() : 0L);
    }

    @Override
    public List<Reservation> getReminderReservations(final OffsetDateTime from, final OffsetDateTime to) {
        // ReservationReminderScheduler iterates this list outside any transaction (the @Transactional
        // boundary on findReminderReservations closes when the query returns) and reads
        // reservation.getCar(), car.getOwner(), car.getBrand()/getModel() and reservation.getRider().
        // Without JOIN FETCH those LAZY associations would trip LazyInitializationException once the
        // session is closed; mirror the patterns used by getOwner/getRiderReservationById further down.
        return em.createQuery(
                        "FROM Reservation r "
                                + "JOIN FETCH r.rider rider "
                                + "JOIN FETCH r.car c "
                                + "JOIN FETCH c.owner owner "
                                + "LEFT JOIN FETCH c.carModel cm "
                                + "LEFT JOIN FETCH cm.brand "
                                + "WHERE r.status = :status AND r.startDate >= :from AND r.startDate < :to "
                                + "AND r.pickupReminderEmailSent = FALSE",
                        Reservation.class)
                .setParameter("status", Reservation.Status.ACCEPTED)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }

    @Override
    public List<Long> findAcceptedReservationIdsWithStartOnOrBefore(final OffsetDateTime now) {
        return em.createQuery(
                        "SELECT r.id FROM Reservation r "
                                + "WHERE r.status = :status AND r.startDate <= :now "
                                + "ORDER BY r.startDate ASC, r.id ASC",
                        Long.class)
                .setParameter("status", Reservation.Status.ACCEPTED)
                .setParameter("now", now)
                .getResultList();
    }

    @Override
    @Transactional
    public int transitionAcceptedToStartedIfDue(final long reservationId, final OffsetDateTime now) {
        final Reservation r = em.find(Reservation.class, reservationId, LockModeType.PESSIMISTIC_WRITE);
        if (r == null
                || r.getStatus() != Reservation.Status.ACCEPTED
                || r.getStartDate().isAfter(now)) {
            return 0;
        }
        r.setStatus(Reservation.Status.STARTED);
        r.setUpdatedAt(OffsetDateTime.now());
        return 1;
    }

    @Override
    public Page<ReservationCard> getOwnerReservationCards(final ReservationSearchCriteria criteria) {
        final int page = criteria.getPage();
        final int pageSize = criteria.getPageSize();
        final Map<String, Object> countParams = new HashMap<>();
        countParams.put("ownerId", criteria.getOwnerId());
        final StringBuilder countSql = new StringBuilder(
                "SELECT COUNT(*) FROM reservations r "
                        + "JOIN cars c ON c.id = r.car_id "
                        + "WHERE c.owner_id = :ownerId ");
        appendReservationFilters(countSql, countParams, criteria);
        final Number total = (Number) bindParams(em.createNativeQuery(countSql.toString()), countParams).getSingleResult();

        final int offset = page * pageSize;
        final Map<String, Object> listParams = new HashMap<>();
        listParams.put("ownerId", criteria.getOwnerId());
        listParams.put("limit", pageSize);
        listParams.put("offset", offset);
        final StringBuilder idSql = new StringBuilder(
                "SELECT r.id FROM reservations r "
                + "JOIN cars c ON c.id = r.car_id "
                + "WHERE c.owner_id = :ownerId ");
        appendReservationFilters(idSql, listParams, criteria);
        idSql.append("ORDER BY ").append(buildReservationOrderBy(criteria.getSortBy(), criteria.getSortDirection()))
             .append(" LIMIT :limit OFFSET :offset");
        final List<ReservationCard> content = loadReservationCardsByIdNativeQuery(idSql.toString(), listParams);
        return new Page<>(content, page, pageSize, total != null ? total.longValue() : 0L);
    }

    @Override
    @Transactional
    public int updateReservationStatus(final long reservationId, final String status) {
        final Reservation r = em.find(Reservation.class, reservationId, LockModeType.PESSIMISTIC_WRITE);
        if (r == null) {
            return 0;
        }
        r.setStatus(Reservation.Status.valueOf(status.toUpperCase(Locale.ROOT)));
        r.setUpdatedAt(OffsetDateTime.now());
        return 1;
    }

    @Override
    @Transactional
    public int updateRiderPendingReservationPeriod(
            final long reservationId,
            final long riderId,
            final OffsetDateTime newStartDate,
            final OffsetDateTime newEndDate,
            final BigDecimal newTotalPrice,
            final OffsetDateTime newPaymentProofDeadlineAt) {
        // Pessimistic write lock so a concurrent overlap check / payment-proof upload cannot race with
        // the period mutation: the row must stay in PENDING + no-receipt while we re-validate and write.
        final Reservation r = em.find(Reservation.class, reservationId, LockModeType.PESSIMISTIC_WRITE);
        if (r == null
                || r.getRiderId() != riderId
                || r.getStatus() != Reservation.Status.PENDING
                || r.getPaymentReceiptFileId().isPresent()) {
            return 0;
        }
        r.setStartDate(newStartDate);
        r.setEndDate(newEndDate);
        r.setTotalPrice(newTotalPrice);
        r.setPaymentProofDeadlineAt(newPaymentProofDeadlineAt);
        r.setPendingPaymentproofEmailSent(false);
        r.setUpdatedAt(OffsetDateTime.now());
        return 1;
    }

    @Override
    public Page<ReservationCard> getCarReservationCards(
            final long ownerId,
            final long carId,
            final int page,
            final int pageSize,
            final String statusFilter) {
        final Map<String, Object> countParams = new HashMap<>();
        countParams.put("ownerId", ownerId);
        countParams.put("carId", carId);
        final StringBuilder countSql = new StringBuilder(
                "SELECT COUNT(*) FROM reservations r "
                        + "JOIN cars c ON c.id = r.car_id "
                        + "WHERE c.owner_id = :ownerId AND r.car_id = :carId ");
        if (statusFilter != null) {
            countSql.append("AND LOWER(r.status) = :statusFilter ");
            countParams.put("statusFilter", statusFilter);
        }
        final Number total = (Number) bindParams(em.createNativeQuery(countSql.toString()), countParams).getSingleResult();

        final int offset = page * pageSize;
        final Map<String, Object> listParams = new HashMap<>();
        listParams.put("ownerId", ownerId);
        listParams.put("carId", carId);
        listParams.put("limit", pageSize);
        listParams.put("offset", offset);
        final StringBuilder idSql = new StringBuilder(
                "SELECT r.id FROM reservations r "
                + "JOIN cars c ON c.id = r.car_id "
                + "WHERE c.owner_id = :ownerId AND r.car_id = :carId ");
        if (statusFilter != null) {
            idSql.append("AND LOWER(r.status) = :statusFilter ");
            listParams.put("statusFilter", statusFilter);
        }
        idSql.append("ORDER BY r.created_at DESC, r.id DESC LIMIT :limit OFFSET :offset");
        final List<ReservationCard> content = loadReservationCardsByIdNativeQuery(idSql.toString(), listParams);
        return new Page<>(content, page, pageSize, total != null ? total.longValue() : 0L);
    }

    @Override
    public Map<String, Long> countCarReservationsByStatus(final long ownerId, final long carId) {
        final List<Object[]> rows = em.createQuery(
                        "SELECT r.status, COUNT(r) FROM Reservation r "
                                + "WHERE r.car.id = :carId AND r.car.owner.id = :ownerId "
                                + "GROUP BY r.status",
                        Object[].class)
                .setParameter("ownerId", ownerId)
                .setParameter("carId", carId)
                .getResultList();
        final Map<String, Long> result = new LinkedHashMap<>();
        for (final Object[] row : rows) {
            final Reservation.Status status = (Reservation.Status) row[0];
            result.put(status.name().toLowerCase(Locale.ROOT), ((Number) row[1]).longValue());
        }
        return result;
    }

    @Override
    public BigDecimal sumCarRevenueByStatuses(
            final long ownerId, final long carId, final Collection<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return BigDecimal.ZERO;
        }
        final List<Reservation.Status> statusEnums = new ArrayList<>(statuses.size());
        for (final String s : statuses) {
            statusEnums.add(Reservation.Status.valueOf(s.toUpperCase(Locale.ROOT)));
        }
        final Object result = em.createQuery(
                        "SELECT COALESCE(SUM(r.totalPrice), 0) FROM Reservation r "
                                + "WHERE r.car.id = :carId AND r.car.owner.id = :ownerId "
                                + "AND r.status IN :statuses")
                .setParameter("ownerId", ownerId)
                .setParameter("carId", carId)
                .setParameter("statuses", statusEnums)
                .getSingleResult();
        if (result == null) {
            return BigDecimal.ZERO;
        }
        if (result instanceof BigDecimal) {
            return (BigDecimal) result;
        }
        if (result instanceof Number) {
            return BigDecimal.valueOf(((Number) result).doubleValue());
        }
        return new BigDecimal(result.toString());
    }

    @Override
    public long countCarReservationsCreatedBetween(
            final long ownerId, final long carId, final OffsetDateTime from, final OffsetDateTime until) {
        final Number count = (Number) em.createQuery(
                        "SELECT COUNT(r) FROM Reservation r "
                                + "WHERE r.car.owner.id = :ownerId AND r.car.id = :carId "
                                + "AND r.createdAt >= :from AND r.createdAt < :until")
                .setParameter("ownerId", ownerId)
                .setParameter("carId", carId)
                .setParameter("from", from)
                .setParameter("until", until)
                .getSingleResult();
        return count != null ? count.longValue() : 0L;
    }

    @Override
    public Optional<OffsetDateTime> findCarNextActiveReservationDate(
            final long ownerId, final long carId, final OffsetDateTime after) {
        /*
         * 1+1 query pattern (project rule: no LIMIT/OFFSET on JPQL):
         * - Step 1 (native): filter, order by {@code start_date ASC} and apply {@code LIMIT 1} on
         *   the {@code reservations} table to obtain the top-1 reservation id for the window. The
         *   WHERE clause does NOT guarantee uniqueness (status IN + start_date > :after can match
         *   multiple rows), so this is a "top 1" pagination case and must use the 1+1 pattern.
         * - Step 2 (JPQL):   hydrate only the {@code startDate} scalar field of the chosen
         *   reservation. No JOIN FETCH needed because the consumer reads a single scalar value.
         */
        @SuppressWarnings("unchecked")
        final List<Number> ids = em.createNativeQuery(
                        "SELECT r.id FROM reservations r "
                                + "JOIN cars c ON c.id = r.car_id "
                                + "WHERE c.owner_id = :ownerId AND r.car_id = :carId "
                                + "AND r.status IN (:statuses) AND r.start_date > :after "
                                + "ORDER BY r.start_date ASC "
                                + "LIMIT 1")
                .setParameter("ownerId", ownerId)
                .setParameter("carId", carId)
                .setParameter("statuses", Arrays.asList(
                        Reservation.Status.ACCEPTED.name().toLowerCase(Locale.ROOT),
                        Reservation.Status.STARTED.name().toLowerCase(Locale.ROOT)))
                .setParameter("after", after)
                .getResultList();
        if (ids.isEmpty()) {
            return Optional.empty();
        }
        final long reservationId = ids.get(0).longValue();
        return em.createQuery(
                        "SELECT r.startDate FROM Reservation r WHERE r.id = :id",
                        OffsetDateTime.class)
                .setParameter("id", reservationId)
                .getResultList().stream().findFirst();
    }

    @Override
    public List<OffsetDateTime[]> findCarFinishedReservationBounds(final long ownerId, final long carId) {
        final List<Object[]> rows = em.createQuery(
                        "SELECT r.startDate, r.endDate FROM Reservation r "
                                + "WHERE r.car.owner.id = :ownerId AND r.car.id = :carId "
                                + "AND r.status = :status",
                        Object[].class)
                .setParameter("ownerId", ownerId)
                .setParameter("carId", carId)
                .setParameter("status", Reservation.Status.FINISHED)
                .getResultList();
        return rows.stream()
                .map(row -> new OffsetDateTime[] {(OffsetDateTime) row[0], (OffsetDateTime) row[1]})
                .toList();
    }

    @Override
    public long sumCarFinishedBillableDays(final long ownerId, final long carId) {
        long total = 0L;
        for (final OffsetDateTime[] bounds : findCarFinishedReservationBounds(ownerId, carId)) {
            total += BillableDays.between(bounds[0], bounds[1]);
        }
        return total;
    }

    @Override
    @Transactional
    public int markCarReturned(final long reservationId, final long ownerUserId) {
        final Reservation r = em.find(Reservation.class, reservationId, LockModeType.PESSIMISTIC_WRITE);
        if (r == null || r.getCar().getOwner().getId() != ownerUserId) {
            return 0;
        }
        if (r.isCarReturned()) {
            return 1;
        }
        final Reservation.Status status = r.getStatus();
        if (status != Reservation.Status.ACCEPTED
                && status != Reservation.Status.STARTED
                && status != Reservation.Status.FINISHED) {
            return 0;
        }
        final OffsetDateTime now = OffsetDateTime.now();
        if (!now.isAfter(r.getEndDate())) {
            return 0;
        }
        r.setCarReturned(true);
        r.setCarReturnedAt(now);
        r.setUpdatedAt(now);
        return 1;
    }

    @Override
    public List<Reservation> findReservationsForReturnReminderEmail(final OffsetDateTime now, final int hoursBeforeCheckout) {
        final OffsetDateTime windowEnd = now.plusHours(hoursBeforeCheckout);
        return em.createQuery(
                        "FROM Reservation r "
                                + FETCH_RESERVATION_CAR_CATALOG_FOR_MAIL
                                + "WHERE r.status IN :statuses "
                                + "AND r.returnReminderEmailSent = FALSE "
                                + "AND r.endDate > :now "
                                + "AND r.endDate <= :windowEnd",
                        Reservation.class)
                .setParameter("statuses", RETURN_EMAIL_ACTIVE_STATUSES)
                .setParameter("now", now)
                .setParameter("windowEnd", windowEnd)
                .getResultList();
    }

    @Override
    public List<Reservation> findReservationsForReturnCheckoutEmail(final OffsetDateTime now) {
        return em.createQuery(
                        "FROM Reservation r "
                                + FETCH_RESERVATION_CAR_CATALOG_FOR_MAIL
                                + "WHERE r.status IN :statuses "
                                + "AND r.returnCheckoutEmailSent = FALSE "
                                + "AND r.carReturned = FALSE "
                                + "AND r.endDate <= :now",
                        Reservation.class)
                .setParameter("statuses", RETURN_EMAIL_ACTIVE_STATUSES)
                .setParameter("now", now)
                .getResultList();
    }

    @Override
    public List<Reservation> findReservationsForRiderReviewInviteEmail(final OffsetDateTime now) {
        // NOT EXISTS sub-query navigates the Review entity (no native join to the reviews table).
        // JOIN FETCH rider + car + carModel + brand so callers that read resolveRider() and
        // resolveVehicleLabelFromReservation() (which touch rider, car.getBrand() and
        // car.getModel()) do not trigger per-row SELECTs on users, cars, car_models and
        // car_brands. LEFT JOIN FETCH on the catalog side keeps the query safe for any
        // pre-backfill cars that still have model_id = NULL.
        return em.createQuery(
                        "FROM Reservation r "
                                + "JOIN FETCH r.rider "
                                + "JOIN FETCH r.car c "
                                + "LEFT JOIN FETCH c.carModel cm "
                                + "LEFT JOIN FETCH cm.brand "
                                + "WHERE r.status IN :statuses "
                                + "AND r.riderReviewInviteEmailSent = FALSE "
                                + "AND r.endDate < :now "
                                + "AND NOT EXISTS ("
                                + "  SELECT 1 FROM Review rv "
                                + "  WHERE rv.reservation = r AND rv.madeByRider = TRUE)",
                        Reservation.class)
                .setParameter("statuses", RIDER_REVIEW_INVITE_STATUSES)
                .setParameter("now", now)
                .getResultList();
    }

    @Override
    public List<Reservation> findReservationsForRiderReviewAutoSkip(
            final OffsetDateTime now, final OffsetDateTime endDateCutoff) {
        return em.createQuery(
                        "FROM Reservation r "
                                + "WHERE r.status IN :statuses "
                                + "AND r.endDate <= :endDateCutoff "
                                + "AND NOT EXISTS ("
                                + "  SELECT 1 FROM Review rv "
                                + "  WHERE rv.reservation = r AND rv.madeByRider = TRUE)",
                        Reservation.class)
                .setParameter("statuses", REVIEW_AUTO_SKIP_STATUSES)
                .setParameter("endDateCutoff", endDateCutoff)
                .getResultList();
    }

    @Override
    public List<Reservation> findReservationsForOwnerReviewAutoSkip(
            final OffsetDateTime now, final OffsetDateTime carReturnedAtCutoff) {
        return em.createQuery(
                        "FROM Reservation r "
                                + "JOIN FETCH r.car c "
                                + "JOIN FETCH c.owner "
                                + "WHERE r.status IN :statuses "
                                + "AND r.carReturned = TRUE "
                                + "AND r.carReturnedAt IS NOT NULL "
                                + "AND r.carReturnedAt <= :carReturnedAtCutoff "
                                + "AND NOT EXISTS ("
                                + "  SELECT 1 FROM Review rv "
                                + "  WHERE rv.reservation = r AND rv.madeByRider = FALSE)",
                        Reservation.class)
                .setParameter("statuses", REVIEW_AUTO_SKIP_STATUSES)
                .setParameter("carReturnedAtCutoff", carReturnedAtCutoff)
                .getResultList();
    }

    @Override
    @Transactional
    public int claimReturnReminderEmailSent(final long reservationId) {
        final Reservation r = em.find(Reservation.class, reservationId, LockModeType.PESSIMISTIC_WRITE);
        if (r == null || r.isReturnReminderEmailSent()) {
            return 0;
        }
        r.setReturnReminderEmailSent(true);
        r.setUpdatedAt(OffsetDateTime.now());
        return 1;
    }

    @Override
    @Transactional
    public int claimPickupReminderEmailSent(final long reservationId) {
        final Reservation r = em.find(Reservation.class, reservationId, LockModeType.PESSIMISTIC_WRITE);
        if (r == null || r.isPickupReminderEmailSent()) {
            return 0;
        }
        r.setPickupReminderEmailSent(true);
        r.setUpdatedAt(OffsetDateTime.now());
        return 1;
    }

    @Override
    @Transactional
    public int claimReturnCheckoutEmailSent(final long reservationId) {
        final Reservation r = em.find(Reservation.class, reservationId, LockModeType.PESSIMISTIC_WRITE);
        if (r == null || r.isReturnCheckoutEmailSent()) {
            return 0;
        }
        r.setReturnCheckoutEmailSent(true);
        r.setUpdatedAt(OffsetDateTime.now());
        return 1;
    }

    @Override
    @Transactional
    public int claimRiderReviewInviteEmailSent(final long reservationId) {
        final Reservation r = em.find(Reservation.class, reservationId, LockModeType.PESSIMISTIC_WRITE);
        if (r == null || r.isRiderReviewInviteEmailSent()) {
            return 0;
        }
        r.setRiderReviewInviteEmailSent(true);
        r.setUpdatedAt(OffsetDateTime.now());
        return 1;
    }

    @Override
    public List<Reservation> findReservationsWithDuePendingPaymentProof(final OffsetDateTime now) {
        final int leadHours = Math.max(1, paymentProofReminderLeadHours);
        final OffsetDateTime windowEnd = now.plusHours(leadHours);
        return em.createQuery(
                        "FROM Reservation r "
                                + FETCH_RESERVATION_CAR_CATALOG_FOR_MAIL
                                + "WHERE r.status = :status "
                                + "AND r.paymentProofDeadlineAt IS NOT NULL "
                                + "AND r.paymentProofDeadlineAt <= :windowEnd "
                                + "AND r.paymentReceiptFile IS NULL "
                                + "AND r.pendingPaymentProofEmailSent = FALSE",
                        Reservation.class)
                .setParameter("status", Reservation.Status.PENDING)
                .setParameter("windowEnd", windowEnd)
                .getResultList();
    }

    @Override
    @Transactional
    public int claimPendingPaymentProofEmailSent(final long reservationId) {
        final Reservation r = em.find(Reservation.class, reservationId, LockModeType.PESSIMISTIC_WRITE);
        if (r == null || r.isPendingPaymentproofEmailSent()) {
            return 0;
        }
        r.setPendingPaymentproofEmailSent(true);
        r.setUpdatedAt(OffsetDateTime.now());
        return 1;
    }

    @Override
    @Transactional
    public int updateParticipantCancellationWithRefundMeta(
            final long reservationId,
            final String statusLower,
            final boolean paymentRefundRequired,
            final OffsetDateTime refundProofDeadlineAtOrNull) {
        final Reservation r = em.find(Reservation.class, reservationId, LockModeType.PESSIMISTIC_WRITE);
        if (r == null) {
            return 0;
        }
        final Reservation.Status target = Reservation.Status.valueOf(statusLower.toUpperCase(Locale.ROOT));
        if (target != Reservation.Status.CANCELLED_BY_RIDER && target != Reservation.Status.CANCELLED_BY_OWNER) {
            return 0;
        }
        final Reservation.Status current = r.getStatus();
        if (current == Reservation.Status.PENDING) {
            if (r.getPaymentReceiptFileId().isPresent() || paymentRefundRequired) {
                return 0;
            }
        } else if (current == Reservation.Status.ACCEPTED) {
            final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            if (!now.isBefore(r.getStartDate())) {
                return 0;
            }
        } else {
            return 0;
        }
        r.setStatus(target);
        r.setPaymentRefundRequired(paymentRefundRequired);
        r.setRefundProofDeadlineAt(refundProofDeadlineAtOrNull);
        r.setPaymentRefundReceiptFile(null);
        r.setPendingRefundEmailSent(false);
        r.setUpdatedAt(OffsetDateTime.now());
        return 1;
    }

    @Override
    @Transactional
    public int applyAdminCarPauseCancellation(
            final long reservationId,
            final boolean paymentRefundRequired,
            final OffsetDateTime refundProofDeadlineAtOrNull) {
        final Reservation r = em.find(Reservation.class, reservationId, LockModeType.PESSIMISTIC_WRITE);
        if (r == null) {
            return 0;
        }
        switch (r.getStatus()) {
            case PENDING -> {
                if (r.getPaymentReceiptFileId().isPresent()) {
                    return applyConfirmedAdminPauseCancellation(
                            r, paymentRefundRequired, refundProofDeadlineAtOrNull);
                }
                r.setStatus(Reservation.Status.CANCELLED_BY_OWNER);
                r.setPaymentRefundRequired(false);
                r.setRefundProofDeadlineAt(null);
                r.setPaymentRefundReceiptFile(null);
                r.setPendingRefundEmailSent(false);
            }
            case ACCEPTED, STARTED -> {
                final int updated = applyConfirmedAdminPauseCancellation(
                        r, paymentRefundRequired, refundProofDeadlineAtOrNull);
                if (updated == 0) {
                    return 0;
                }
            }
            default -> {
                return 0;
            }
        }
        r.setUpdatedAt(OffsetDateTime.now());
        return 1;
    }

    private static int applyConfirmedAdminPauseCancellation(
            final Reservation r,
            final boolean paymentRefundRequired,
            final OffsetDateTime refundProofDeadlineAtOrNull) {
        final boolean refundRequired = paymentRefundRequired || r.getPaymentReceiptFileId().isPresent();
        r.setStatus(Reservation.Status.CANCELLED_BY_OWNER);
        r.setPaymentRefundRequired(refundRequired);
        r.setRefundProofDeadlineAt(refundRequired ? refundProofDeadlineAtOrNull : null);
        r.setPaymentRefundReceiptFile(null);
        r.setPendingRefundEmailSent(false);
        return 1;
    }

    @Override
    @Transactional
    public int attachRefundReceipt(final long reservationId, final long ownerUserId, final long storedFileId) {
        // PESSIMISTIC_WRITE on the reservation row only (same pattern as attachPaymentReceiptAndAccept).
        // Ownership uses getOwnerId() on the car proxy — no JOIN FETCH that would widen FOR UPDATE.
        final Reservation r = em.find(Reservation.class, reservationId, LockModeType.PESSIMISTIC_WRITE);
        if (r == null) {
            return 0;
        }
        if (r.getCar().getOwnerId() != ownerUserId
                || !r.isPaymentRefundRequired()
                || r.getPaymentRefundReceiptFileId().isPresent()) {
            return 0;
        }
        final Reservation.Status s = r.getStatus();
        if (s != Reservation.Status.CANCELLED_BY_OWNER && s != Reservation.Status.CANCELLED_BY_RIDER) {
            return 0;
        }
        r.setPaymentRefundReceiptFile(em.getReference(StoredFile.class, storedFileId));
        r.setPendingRefundEmailSent(true);
        r.setUpdatedAt(OffsetDateTime.now());
        return 1;
    }

    @Override
    public List<Reservation> findReservationsWithDuePendingRefundProof(final OffsetDateTime now) {
        final int leadHours = Math.max(1, paymentProofReminderLeadHours);
        final OffsetDateTime windowEnd = now.plusHours(leadHours);
        return em.createQuery(
                        "FROM Reservation r "
                                + FETCH_RESERVATION_CAR_CATALOG_FOR_MAIL
                                + "WHERE r.paymentRefundRequired = TRUE "
                                + "AND r.paymentRefundReceiptFile IS NULL "
                                + "AND r.refundProofDeadlineAt IS NOT NULL "
                                + "AND r.refundProofDeadlineAt <= :windowEnd "
                                + "AND r.pendingRefundEmailSent = FALSE "
                                + "AND r.status IN :statuses",
                        Reservation.class)
                .setParameter("windowEnd", windowEnd)
                .setParameter("statuses", REFUND_ELIGIBLE_CANCELLED_STATUSES)
                .getResultList();
    }

    @Override
    @Transactional
    public int claimPendingRefundEmailSent(final long reservationId) {
        final Reservation r = em.find(Reservation.class, reservationId, LockModeType.PESSIMISTIC_WRITE);
        if (r == null || r.isPendingRefundEmailSent()) {
            return 0;
        }
        r.setPendingRefundEmailSent(true);
        r.setUpdatedAt(OffsetDateTime.now());
        return 1;
    }

    @Override
    public List<Reservation> findReservationsWithOverdueRefundProof(final OffsetDateTime now) {
        // JOIN FETCH car + owner so callers can group by owner and read profile fields without N+1.
        return em.createQuery(
                        "FROM Reservation r "
                                + FETCH_RESERVATION_CAR_CATALOG_FOR_MAIL
                                + "WHERE r.paymentRefundRequired = TRUE "
                                + "AND r.paymentRefundReceiptFile IS NULL "
                                + "AND r.refundProofDeadlineAt IS NOT NULL "
                                + "AND r.refundProofDeadlineAt < :now "
                                + "AND r.status IN :cancelledStatuses",
                        Reservation.class)
                .setParameter("now", now)
                .setParameter("cancelledStatuses", REFUND_ELIGIBLE_CANCELLED_STATUSES)
                .getResultList();
    }

    @Override
    public long countOverdueRefundProofsForOwner(final long ownerUserId, final OffsetDateTime now) {
        final Number count = (Number) em.createQuery(
                        "SELECT COUNT(r) FROM Reservation r "
                                + "WHERE r.car.owner.id = :ownerUserId "
                                + "AND r.paymentRefundRequired = TRUE "
                                + "AND r.paymentRefundReceiptFile IS NULL "
                                + "AND r.refundProofDeadlineAt IS NOT NULL "
                                + "AND r.refundProofDeadlineAt < :now "
                                + "AND r.status IN :cancelledStatuses")
                .setParameter("ownerUserId", ownerUserId)
                .setParameter("now", now)
                .setParameter("cancelledStatuses", REFUND_ELIGIBLE_CANCELLED_STATUSES)
                .getSingleResult();
        return count == null ? 0L : count.longValue();
    }

    @Override
    public List<Reservation> findReservationsRequiringRefundProofForOwner(final long ownerUserId) {
        // Entity-shaped JPQL. NO deadline filter on purpose: callers want to surface a "you must upload
        // a refund receipt" badge even before the deadline expires (the owner-blocked state is a separate,
        // stricter check). JOIN FETCH r.car so callers can read r.getCar().getId() in the streaming map
        // without a per-row lazy load. Ordered by deadline ascending so callers can prioritise the most
        // urgent ones if they need a single one.
        return em.createQuery(
                        "FROM Reservation r "
                                + "JOIN FETCH r.car c "
                                + "WHERE c.owner.id = :ownerUserId "
                                + "AND r.paymentRefundRequired = TRUE "
                                + "AND r.paymentRefundReceiptFile IS NULL "
                                + "AND r.status IN :cancelledStatuses "
                                + "ORDER BY r.refundProofDeadlineAt ASC",
                        Reservation.class)
                .setParameter("ownerUserId", ownerUserId)
                .setParameter("cancelledStatuses", REFUND_ELIGIBLE_CANCELLED_STATUSES)
                .getResultList();
    }

    @Override
    public List<Reservation> findOverdueRefundProofReservationsForOwner(final long ownerUserId, final OffsetDateTime now) {
        // Entity-shaped JPQL ("entities not tables" rule). Ordered by deadline so the first row is the
        // most overdue one — the navbar banner advice uses that to deep-link the CTA when exactly one
        // overdue refund proof is pending. No JOIN FETCH: callers only read the id (the @Transactional
        // scope keeps Hibernate happy without forcing eager hydration of car/owner here).
        return em.createQuery(
                        "FROM Reservation r "
                                + "WHERE r.car.owner.id = :ownerUserId "
                                + "AND r.paymentRefundRequired = TRUE "
                                + "AND r.paymentRefundReceiptFile IS NULL "
                                + "AND r.refundProofDeadlineAt IS NOT NULL "
                                + "AND r.refundProofDeadlineAt < :now "
                                + "AND r.status IN :cancelledStatuses "
                                + "ORDER BY r.refundProofDeadlineAt ASC",
                        Reservation.class)
                .setParameter("ownerUserId", ownerUserId)
                .setParameter("now", now)
                .setParameter("cancelledStatuses", REFUND_ELIGIBLE_CANCELLED_STATUSES)
                .getResultList();
    }

    @Override
    public Map<Long, List<Long>> findOverdueRefundProofReservationIdsByOwnerIds(
            final Collection<Long> ownerUserIds,
            final OffsetDateTime now) {
        if (ownerUserIds == null || ownerUserIds.isEmpty()) {
            return Map.of();
        }
        final List<Object[]> rows = em.createQuery(
                        "SELECT r.car.owner.id, r.id FROM Reservation r "
                                + "WHERE r.car.owner.id IN :ownerUserIds "
                                + "AND r.paymentRefundRequired = TRUE "
                                + "AND r.paymentRefundReceiptFile IS NULL "
                                + "AND r.refundProofDeadlineAt IS NOT NULL "
                                + "AND r.refundProofDeadlineAt < :now "
                                + "AND r.status IN :cancelledStatuses "
                                + "ORDER BY r.car.owner.id ASC, r.refundProofDeadlineAt ASC",
                        Object[].class)
                .setParameter("ownerUserIds", ownerUserIds)
                .setParameter("now", now)
                .setParameter("cancelledStatuses", REFUND_ELIGIBLE_CANCELLED_STATUSES)
                .getResultList();
        final Map<Long, List<Long>> grouped = new java.util.HashMap<>();
        for (final Object[] row : rows) {
            grouped.computeIfAbsent((Long) row[0], ignored -> new java.util.ArrayList<>())
                    .add((Long) row[1]);
        }
        return grouped;
    }

    @Override
    public Page<ReservationCard> findAllReservationCards(final ReservationSearchCriteria criteria) {
        final int page = criteria.getPage();
        final int pageSize = criteria.getPageSize();
        final Map<String, Object> countParams = new HashMap<>();
        final StringBuilder countSql = new StringBuilder(
                "SELECT COUNT(*) FROM reservations r "
                        + "JOIN cars c ON c.id = r.car_id "
                        + "WHERE 1=1 ");
        appendReservationFilters(countSql, countParams, criteria);
        final Number total = (Number) bindParams(em.createNativeQuery(countSql.toString()), countParams).getSingleResult();

        final int offset = page * pageSize;
        final Map<String, Object> listParams = new HashMap<>();
        listParams.put("limit", pageSize);
        listParams.put("offset", offset);
        final StringBuilder idSql = new StringBuilder(
                "SELECT r.id FROM reservations r "
                        + "JOIN cars c ON c.id = r.car_id "
                        + "WHERE 1=1 ");
        appendReservationFilters(idSql, listParams, criteria);
        idSql.append("ORDER BY ").append(buildReservationOrderBy(criteria.getSortBy(), criteria.getSortDirection()))
                .append(" LIMIT :limit OFFSET :offset");
        final List<ReservationCard> content = loadReservationCardsByIdNativeQuery(idSql.toString(), listParams);
        return new Page<>(content, page, pageSize, total != null ? total.longValue() : 0L);
    }

    // ---- SQL helpers ----

    private static void appendReservationFilters(
            final StringBuilder sql,
            final Map<String, Object> params,
            final ReservationSearchCriteria criteria) {
        if (criteria.getCarId() != null) {
            sql.append("AND r.car_id = :carId ");
            params.put("carId", criteria.getCarId());
        }
        if (!criteria.getStatusFilters().isEmpty()) {
            sql.append("AND LOWER(r.status) IN (:resStatuses) ");
            params.put("resStatuses", criteria.getStatusFilters());
        }
        final String textQuery = criteria.getTextQuery();
        if (textQuery != null) {
            final String q = "%" + escapeLike(textQuery) + "%";
            sql.append("AND EXISTS (SELECT 1 FROM car_models cm "
                    + "JOIN car_brands cb ON cb.id = cm.brand_id "
                    + "WHERE cm.id = c.model_id "
                    + "AND (LOWER(cb.name) LIKE LOWER(:resSearch) ESCAPE '\\' "
                    + "OR LOWER(cm.name) LIKE LOWER(:resSearch) ESCAPE '\\')) ");
            params.put("resSearch", q);
        }
        if (!criteria.getCarTypes().isEmpty()) {
            sql.append("AND EXISTS (SELECT 1 FROM car_models cm "
                    + "WHERE cm.id = c.model_id AND cm.type IN (:resCarTypes)) ");
            params.put("resCarTypes", criteria.getCarTypes());
        }
        if (!criteria.getTransmissions().isEmpty()) {
            sql.append("AND c.transmission IN (:resTransmissions) ");
            params.put("resTransmissions", criteria.getTransmissions());
        }
        if (!criteria.getPowertrains().isEmpty()) {
            sql.append("AND c.powertrain IN (:resPowertrains) ");
            params.put("resPowertrains", criteria.getPowertrains());
        }
        if (criteria.getMinPrice() != null) {
            sql.append("AND r.total_price >= :resMinPrice ");
            params.put("resMinPrice", criteria.getMinPrice());
        }
        if (criteria.getMaxPrice() != null) {
            sql.append("AND r.total_price <= :resMaxPrice ");
            params.put("resMaxPrice", criteria.getMaxPrice());
        }
        final List<String> ratingBands = criteria.getRatingBands();
        if (!ratingBands.isEmpty()) {
            final List<String> conditions = new ArrayList<>();
            if (ratingBands.contains("UNDER_2")) {
                conditions.add("c.rating_avg < 2");
            }
            if (ratingBands.contains("2_TO_3")) {
                conditions.add("(c.rating_avg >= 2 AND c.rating_avg < 3)");
            }
            if (ratingBands.contains("3_TO_4")) {
                conditions.add("(c.rating_avg >= 3 AND c.rating_avg < 4)");
            }
            if (ratingBands.contains("OVER_4")) {
                conditions.add("c.rating_avg >= 4");
            }
            if (!conditions.isEmpty()) {
                sql.append("AND (").append(String.join(" OR ", conditions)).append(") ");
            }
        }
    }

    private static String buildReservationOrderBy(final String sortBy, final String sortDirection) {
        final String dir = "asc".equalsIgnoreCase(sortDirection) ? "ASC" : "DESC";
        if ("price".equals(sortBy)) {
            return "r.total_price " + dir + ", r.id DESC";
        } else if ("rating".equals(sortBy)) {
            return "c.rating_avg " + dir + " NULLS LAST, r.id DESC";
        } else {
            return "r.created_at " + dir + ", r.id DESC";
        }
    }

    private static String escapeLike(final String raw) {
        return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /**
     * Real "1+1" pattern: a NATIVE query selects just the reservation IDs (the only place where
     * pagination forces native SQL because we need to keep dialect-specific {@code LIMIT/OFFSET}
     * and the filter SQL), and then JPQL with {@code JOIN FETCH} hydrates each {@link Reservation}
     * along with its catalog associations. The cover image is asked to {@link CarPictureDao} so
     * this DAO does not touch the {@code car_pictures} table itself.
     */
    private List<ReservationCard> loadReservationCardsByIdNativeQuery(
            final String idSql, final Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        final List<Number> rawIds = bindParams(em.createNativeQuery(idSql), params).getResultList();
        if (rawIds.isEmpty()) {
            return List.of();
        }
        final List<Long> orderedIds = new ArrayList<>(rawIds.size());
        for (final Number n : rawIds) {
            orderedIds.add(n.longValue());
        }
        final List<Reservation> reservations = em.createQuery(
                        "FROM Reservation r "
                                + "JOIN FETCH r.car c "
                                + "LEFT JOIN FETCH c.carModel cm "
                                + "LEFT JOIN FETCH cm.brand "
                                + "WHERE r.id IN :ids",
                        Reservation.class)
                .setParameter("ids", orderedIds)
                .getResultList();
        final Map<Long, Reservation> byId = new HashMap<>(reservations.size());
        final LinkedHashSet<Long> carIds = new LinkedHashSet<>(reservations.size());
        for (final Reservation r : reservations) {
            byId.put(r.getId(), r);
            carIds.add(r.getCar().getId());
        }
        final Map<Long, Long> coverByCar = carPictureDao.findCoverImageIdsByCarIds(carIds);
        final List<ReservationCard> cards = new ArrayList<>(orderedIds.size());
        for (final Long id : orderedIds) {
            final Reservation r = byId.get(id);
            if (r == null) {
                continue;
            }
            final Car c = r.getCar();
            final CarModel model = c.getCarModel().orElse(null);
            final String brandName = model == null || model.getBrand() == null ? null : model.getBrand().getName();
            final String modelName = model == null ? null : model.getName();
            final long imageId = coverByCar.getOrDefault(c.getId(), 0L);
            cards.add(new ReservationCard(
                    r.getId(), c.getId(), brandName, modelName,
                    r.getTotalPrice(), imageId,
                    r.getStartDate(), r.getEndDate(), r.getStatus()));
        }
        return cards;
    }

    @Override
    public Optional<ReservationCard> findReservationCardById(final long reservationId) {
        final List<ReservationCard> cards = loadReservationCardsByIdNativeQuery(
                "SELECT r.id FROM reservations r WHERE r.id = :reservationId",
                Map.of("reservationId", reservationId));
        return cards.isEmpty() ? Optional.empty() : Optional.of(cards.get(0));
    }
}
