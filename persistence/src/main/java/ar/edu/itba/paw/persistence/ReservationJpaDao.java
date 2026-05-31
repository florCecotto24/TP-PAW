package ar.edu.itba.paw.persistence;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static ar.edu.itba.paw.persistence.util.JpaQueryUtils.bindParams;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.CarModel;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.domain.StoredFile;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.reservation.ReservationCard;
import ar.edu.itba.paw.models.util.search.ReservationSearchCriteria;
import ar.edu.itba.paw.persistence.ReservationDao;

@Transactional(readOnly = true)
@Repository
public class ReservationJpaDao implements ReservationDao {

    private static final List<Reservation.Status> BLOCKING_STATUSES = Arrays.asList(
            Reservation.Status.PENDING, Reservation.Status.ACCEPTED, Reservation.Status.STARTED);

    private final CarPictureDao carPictureDao;

    @org.springframework.beans.factory.annotation.Autowired
    public ReservationJpaDao(final CarPictureDao carPictureDao) {
        this.carPictureDao = carPictureDao;
    }

    private static final List<Reservation.Status> REFUND_ELIGIBLE_CANCELLED_STATUSES = Arrays.asList(
            Reservation.Status.CANCELLED_BY_OWNER, Reservation.Status.CANCELLED_BY_RIDER);

    private static final List<Reservation.Status> RETURN_EMAIL_ACTIVE_STATUSES = Arrays.asList(
            Reservation.Status.ACCEPTED, Reservation.Status.STARTED);

    private static final List<Reservation.Status> RIDER_REVIEW_INVITE_STATUSES = Arrays.asList(
            Reservation.Status.ACCEPTED, Reservation.Status.STARTED, Reservation.Status.FINISHED);

    @PersistenceContext
    private EntityManager em;

    @Value("${app.reservation.payment-proof-reminder-lead-hours:2}")
    private int paymentProofReminderLeadHours;

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
    public List<Reservation> findBlockingByCarId(final long carId) {
        return em.createQuery(
                        "FROM Reservation r WHERE r.car.id = :carId AND r.status IN :statuses ORDER BY r.startDate ASC",
                        Reservation.class)
                .setParameter("carId", carId)
                .setParameter("statuses", BLOCKING_STATUSES)
                .getResultList();
    }

    @Override
    public List<Reservation> findBlockingByCarIdInRange(
            final long carId, final OffsetDateTime from, final OffsetDateTime to) {
        return em.createQuery(
                        "FROM Reservation r WHERE r.car.id = :carId AND r.status IN :statuses "
                                + "AND r.startDate < :to AND r.endDate > :from ORDER BY r.startDate ASC",
                        Reservation.class)
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
                .pendingPaymentproofEmailSent(false)
                .pendingRefundEmailSent(false)
                .build();
        em.persist(reservation);
        return reservation;
    }

    @Override
    public List<Reservation> findPendingPaymentPastDeadline(final OffsetDateTime now) {
        return em.createQuery(
                        "FROM Reservation r WHERE r.status = :status "
                                + "AND r.paymentProofDeadlineAt IS NOT NULL AND r.paymentProofDeadlineAt < :now "
                                + "ORDER BY r.id ASC",
                        Reservation.class)
                .setParameter("status", Reservation.Status.PENDING)
                .setParameter("now", now)
                .getResultList();
    }

    @Override
    @Transactional
    public int attachPaymentReceiptAndAccept(final long reservationId, final long riderId, final long storedFileId) {
        final Reservation r = em.find(Reservation.class, reservationId, LockModeType.PESSIMISTIC_WRITE);
        if (r == null || r.getRiderId() != riderId || r.getStatus() != Reservation.Status.PENDING) {
            return 0;
        }
        r.setStatus(Reservation.Status.ACCEPTED);
        r.setPaymentReceiptFile(em.getReference(StoredFile.class, storedFileId));
        r.setUpdatedAt(OffsetDateTime.now());
        return 1;
    }

    @Override
    public Optional<Reservation> getReservationById(final long id) {
        return Optional.ofNullable(em.find(Reservation.class, id));
    }

    @Override
    public Optional<Reservation> getOwnerReservationById(final long ownerId, final long reservationId) {
        return em.createQuery(
                        "FROM Reservation r "
                                + "WHERE r.id = :reservationId AND r.car.owner.id = :ownerId",
                        Reservation.class)
                .setParameter("reservationId", reservationId)
                .setParameter("ownerId", ownerId)
                .getResultList().stream().findAny();
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
        return em.createQuery(
                        "FROM Reservation r WHERE r.status = :status AND r.startDate >= :from AND r.startDate < :to",
                        Reservation.class)
                .setParameter("status", Reservation.Status.ACCEPTED)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
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
        final Reservation r = em.find(Reservation.class, reservationId);
        if (r == null) {
            return 0;
        }
        r.setStatus(Reservation.Status.valueOf(status.toUpperCase(Locale.ROOT)));
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
        idSql.append("ORDER BY r.created_at DESC LIMIT :limit OFFSET :offset");
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
        return em.createQuery(
                        "SELECT r.startDate FROM Reservation r "
                                + "WHERE r.car.owner.id = :ownerId AND r.car.id = :carId "
                                + "AND r.status IN :statuses AND r.startDate > :after "
                                + "ORDER BY r.startDate ASC",
                        OffsetDateTime.class)
                .setParameter("ownerId", ownerId)
                .setParameter("carId", carId)
                .setParameter("statuses", Arrays.asList(Reservation.Status.ACCEPTED, Reservation.Status.STARTED))
                .setParameter("after", after)
                .setMaxResults(1)
                .getResultList().stream().findFirst();
    }

    @Override
    public List<Reservation> findCarFinishedReservations(final long ownerId, final long carId) {
        return em.createQuery(
                        "FROM Reservation r "
                                + "WHERE r.car.owner.id = :ownerId AND r.car.id = :carId "
                                + "AND r.status = :status",
                        Reservation.class)
                .setParameter("ownerId", ownerId)
                .setParameter("carId", carId)
                .setParameter("status", Reservation.Status.FINISHED)
                .getResultList();
    }

    @Override
    @Transactional
    public int markCarReturned(final long reservationId, final long ownerUserId) {
        final Reservation r = em.find(Reservation.class, reservationId);
        if (r == null || r.getCar().getOwner().getId() != ownerUserId) {
            return 0;
        }
        r.setCarReturned(true);
        r.setUpdatedAt(OffsetDateTime.now());
        return 1;
    }

    @Override
    public List<Reservation> findReservationsForReturnReminderEmail(final OffsetDateTime now, final int hoursBeforeCheckout) {
        final OffsetDateTime windowEnd = now.plusHours(hoursBeforeCheckout);
        return em.createQuery(
                        "FROM Reservation r "
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
        return em.createQuery(
                        "FROM Reservation r "
                                + "WHERE r.status IN :statuses "
                                + "AND r.riderReviewInviteEmailSent = FALSE "
                                + "AND r.endDate < :now "
                                + "AND NOT EXISTS ("
                                + "  SELECT 1 FROM Review rv "
                                + "  WHERE rv.reservation = r AND rv.id.madeByRider = TRUE)",
                        Reservation.class)
                .setParameter("statuses", RIDER_REVIEW_INVITE_STATUSES)
                .setParameter("now", now)
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
                                + "WHERE r.paymentProofDeadlineAt IS NOT NULL "
                                + "AND r.paymentProofDeadlineAt <= :windowEnd "
                                + "AND r.paymentReceiptFile IS NULL "
                                + "AND r.pendingPaymentproofEmailSent = FALSE",
                        Reservation.class)
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
        final Reservation r = em.find(Reservation.class, reservationId);
        if (r == null) {
            return 0;
        }
        r.setStatus(Reservation.Status.valueOf(statusLower.toUpperCase(Locale.ROOT)));
        r.setPaymentRefundRequired(paymentRefundRequired);
        r.setRefundProofDeadlineAt(refundProofDeadlineAtOrNull);
        r.setPaymentRefundReceiptFile(null);
        r.setPendingRefundEmailSent(false);
        r.setUpdatedAt(OffsetDateTime.now());
        return 1;
    }

    @Override
    @Transactional
    public int attachRefundReceipt(final long reservationId, final long ownerUserId, final long storedFileId) {
        final Reservation r = em.find(Reservation.class, reservationId);
        if (r == null
                || r.getCar().getOwner().getId() != ownerUserId
                || !r.isPaymentRefundRequired()
                || r.getPaymentRefundReceiptFile().isPresent()) {
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
                                + "JOIN FETCH r.car c "
                                + "JOIN FETCH c.owner "
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
    public Page<ReservationCard> findAllReservationCards(final int page, final int pageSize) {
        final Number total = (Number) em.createQuery("SELECT COUNT(r) FROM Reservation r").getSingleResult();
        final String idSql = "SELECT r.id FROM reservations r ORDER BY r.created_at DESC LIMIT :limit OFFSET :offset";
        final Map<String, Object> params = new java.util.LinkedHashMap<>();
        params.put("limit", pageSize);
        params.put("offset", page * pageSize);
        final List<ReservationCard> content = loadReservationCardsByIdNativeQuery(idSql, params);
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
            return "r.total_price " + dir + ", r.created_at DESC";
        } else if ("rating".equals(sortBy)) {
            return "c.rating_avg " + dir + " NULLS LAST, r.created_at DESC";
        } else {
            return "r.created_at " + dir;
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
        final java.util.LinkedHashSet<Long> carIds = new java.util.LinkedHashSet<>(reservations.size());
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
}
