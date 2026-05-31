package ar.edu.itba.paw.persistence;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.domain.StoredFile;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.ReservationCard;
import ar.edu.itba.paw.models.util.ReservationSearchCriteria;
import ar.edu.itba.paw.persistence.ReservationDao;

@Transactional(readOnly = true)
@Repository
public class ReservationJpaDao implements ReservationDao {

    private static final List<Reservation.Status> BLOCKING_STATUSES = Arrays.asList(
            Reservation.Status.PENDING, Reservation.Status.ACCEPTED, Reservation.Status.STARTED);

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
                        "SELECT r FROM Reservation r "
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
        final StringBuilder listSql = new StringBuilder(reservationCardSelectSql()
                + "FROM reservations r "
                + "JOIN cars c ON c.id = r.car_id "
                + reservationCardCatalogJoins()
                + "WHERE r.rider_id = :riderId ");
        appendReservationFilters(listSql, listParams, criteria);
        listSql.append("ORDER BY ").append(buildReservationOrderBy(criteria.getSortBy(), criteria.getSortDirection()))
               .append(" LIMIT :limit OFFSET :offset");
        final List<ReservationCard> content = runReservationCardNativeQuery(listSql.toString(), listParams);
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
        final StringBuilder listSql = new StringBuilder(reservationCardSelectSql()
                + "FROM reservations r "
                + "JOIN cars c ON c.id = r.car_id "
                + reservationCardCatalogJoins()
                + "WHERE c.owner_id = :ownerId ");
        appendReservationFilters(listSql, listParams, criteria);
        listSql.append("ORDER BY ").append(buildReservationOrderBy(criteria.getSortBy(), criteria.getSortDirection()))
               .append(" LIMIT :limit OFFSET :offset");
        final List<ReservationCard> content = runReservationCardNativeQuery(listSql.toString(), listParams);
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
        final StringBuilder listSql = new StringBuilder(reservationCardSelectSql()
                + "FROM reservations r "
                + "JOIN cars c ON c.id = r.car_id "
                + reservationCardCatalogJoins()
                + "WHERE c.owner_id = :ownerId AND r.car_id = :carId ");
        if (statusFilter != null) {
            listSql.append("AND LOWER(r.status) = :statusFilter ");
            listParams.put("statusFilter", statusFilter);
        }
        listSql.append("ORDER BY r.created_at DESC LIMIT :limit OFFSET :offset");
        final List<ReservationCard> content = runReservationCardNativeQuery(listSql.toString(), listParams);
        return new Page<>(content, page, pageSize, total != null ? total.longValue() : 0L);
    }

    @Override
    public Map<String, Long> countCarReservationsByStatus(final long ownerId, final long carId) {
        @SuppressWarnings("unchecked")
        final List<Object[]> rows = em.createNativeQuery(
                        "SELECT LOWER(r.status) AS status, COUNT(*) AS cnt "
                                + "FROM reservations r "
                                + "JOIN cars c ON c.id = r.car_id "
                                + "WHERE c.owner_id = :ownerId AND r.car_id = :carId "
                                + "GROUP BY LOWER(r.status)")
                .setParameter("ownerId", ownerId)
                .setParameter("carId", carId)
                .getResultList();
        final Map<String, Long> result = new LinkedHashMap<>();
        for (final Object[] row : rows) {
            result.put((String) row[0], ((Number) row[1]).longValue());
        }
        return result;
    }

    @Override
    public BigDecimal sumCarRevenueByStatuses(
            final long ownerId, final long carId, final Collection<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return BigDecimal.ZERO;
        }
        final Object result = em.createNativeQuery(
                        "SELECT COALESCE(SUM(r.total_price), 0) FROM reservations r "
                                + "JOIN cars c ON c.id = r.car_id "
                                + "WHERE c.owner_id = :ownerId AND r.car_id = :carId "
                                + "AND LOWER(r.status) IN (:statuses)")
                .setParameter("ownerId", ownerId)
                .setParameter("carId", carId)
                .setParameter("statuses", statuses)
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
                        "SELECT r FROM Reservation r "
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
    @SuppressWarnings("unchecked")
    public List<Reservation> findReservationsForReturnReminderEmail(final OffsetDateTime now, final int hoursBeforeCheckout) {
        final OffsetDateTime windowEnd = now.plusHours(hoursBeforeCheckout);
        return em.createNativeQuery(
                        "SELECT * FROM reservations r "
                                + "WHERE LOWER(r.status) IN ('accepted','started') "
                                + "AND r.return_reminder_email_sent = FALSE "
                                + "AND r.end_date > :now "
                                + "AND r.end_date <= :windowEnd",
                        Reservation.class)
                .setParameter("now", toTimestamp(now))
                .setParameter("windowEnd", toTimestamp(windowEnd))
                .getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Reservation> findReservationsForReturnCheckoutEmail(final OffsetDateTime now) {
        return em.createNativeQuery(
                        "SELECT * FROM reservations r "
                                + "WHERE LOWER(r.status) IN ('accepted','started') "
                                + "AND r.return_checkout_email_sent = FALSE "
                                + "AND r.car_returned = FALSE "
                                + "AND r.end_date <= :now",
                        Reservation.class)
                .setParameter("now", toTimestamp(now))
                .getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Reservation> findReservationsForRiderReviewInviteEmail(final OffsetDateTime now) {
        return em.createNativeQuery(
                        "SELECT r.* FROM reservations r "
                                + "WHERE LOWER(r.status) IN ('accepted','started','finished') "
                                + "AND r.rider_review_invite_email_sent = FALSE "
                                + "AND r.end_date < :now "
                                + "AND NOT EXISTS (SELECT 1 FROM reviews rv WHERE rv.reservation_id = r.id AND rv.made_by_rider = TRUE)",
                        Reservation.class)
                .setParameter("now", toTimestamp(now))
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
    @SuppressWarnings("unchecked")
    public List<Reservation> findReservationsWithDuePendingPaymentProof(final OffsetDateTime now) {
        final int leadHours = Math.max(1, paymentProofReminderLeadHours);
        final OffsetDateTime windowEnd = now.plusHours(leadHours);
        return em.createNativeQuery(
                        "SELECT * FROM reservations r "
                                + "WHERE r.payment_proof_deadline_at IS NOT NULL "
                                + "AND r.payment_proof_deadline_at <= :windowEnd "
                                + "AND r.payment_receipt_file_id IS NULL "
                                + "AND r.pending_paymentproof_email_sent = FALSE",
                        Reservation.class)
                .setParameter("windowEnd", toTimestamp(windowEnd))
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
    @SuppressWarnings("unchecked")
    public List<Reservation> findReservationsWithDuePendingRefundProof(final OffsetDateTime now) {
        final int leadHours = Math.max(1, paymentProofReminderLeadHours);
        final OffsetDateTime windowEnd = now.plusHours(leadHours);
        return em.createNativeQuery(
                        "SELECT * FROM reservations r "
                                + "WHERE r.payment_refund_required = TRUE "
                                + "AND r.payment_refund_receipt_file_id IS NULL "
                                + "AND r.refund_proof_deadline_at IS NOT NULL "
                                + "AND r.refund_proof_deadline_at <= :windowEnd "
                                + "AND r.pending_refund_email_sent = FALSE "
                                + "AND LOWER(TRIM(r.status)) IN ('cancelled_by_owner', 'cancelled_by_rider')",
                        Reservation.class)
                .setParameter("windowEnd", toTimestamp(windowEnd))
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
    public Page<ReservationCard> findAllReservationCards(final int page, final int pageSize) {
        final String countSql = "SELECT COUNT(*) FROM reservations";
        final Number total = (Number) em.createNativeQuery(countSql).getSingleResult();
        final String listSql = reservationCardSelectSql()
                + "FROM reservations r "
                + "JOIN cars c ON c.id = r.car_id "
                + reservationCardCatalogJoins()
                + "ORDER BY r.created_at DESC "
                + "LIMIT :limit OFFSET :offset";
        final Map<String, Object> params = new java.util.LinkedHashMap<>();
        params.put("limit", pageSize);
        params.put("offset", page * pageSize);
        final List<ReservationCard> content = runReservationCardNativeQuery(listSql, params);
        return new Page<>(content, page, pageSize, total != null ? total.longValue() : 0L);
    }

    // ---- SQL helpers ----

    private static String reservationCardSelectSql() {
        return "SELECT r.id AS reservation_id, c.id AS car_id, r.start_date, r.end_date, r.status, "
                + "cb.name AS brand, cm.name AS model, r.total_price, "
                + "(SELECT cp.image_id FROM car_pictures cp WHERE cp.car_id = c.id "
                +    "ORDER BY cp.display_order ASC LIMIT 1) AS image_id ";
    }

    /** Joins required by {@link #reservationCardSelectSql()} (brand/model display columns). */
    private static String reservationCardCatalogJoins() {
        return "LEFT JOIN car_models cm ON cm.id = c.model_id "
                + "LEFT JOIN car_brands cb ON cb.id = cm.brand_id ";
    }

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

    @SuppressWarnings("unchecked")
    private List<ReservationCard> runReservationCardNativeQuery(final String sql, final Map<String, Object> params) {
        final List<Object[]> rows = bindParams(em.createNativeQuery(sql), params).getResultList();
        final List<ReservationCard> result = new ArrayList<>(rows.size());
        for (final Object[] row : rows) {
            result.add(mapReservationCard(row));
        }
        return result;
    }

    private static ReservationCard mapReservationCard(final Object[] row) {
        final long reservationId = ((Number) row[0]).longValue();
        final long carId = ((Number) row[1]).longValue();
        final OffsetDateTime startDate = toOffsetDateTime(row[2]);
        final OffsetDateTime endDate = toOffsetDateTime(row[3]);
        final String statusStr = (String) row[4];
        final Reservation.Status status = Reservation.Status.valueOf(statusStr.toUpperCase());
        final String brand = (String) row[5];
        final String model = (String) row[6];
        final BigDecimal totalPrice = (BigDecimal) row[7];
        final Object rawImageId = row[8];
        final long imageId = rawImageId == null ? 0L : ((Number) rawImageId).longValue();
        return new ReservationCard(reservationId, carId, brand, model, totalPrice, imageId, startDate, endDate, status);
    }

    private static Timestamp toTimestamp(final OffsetDateTime odt) {
        if (odt == null) {
            return null;
        }
        return Timestamp.from(odt.toInstant());
    }

    private static OffsetDateTime toOffsetDateTime(final Object o) {
        if (o instanceof OffsetDateTime) {
            return ((OffsetDateTime) o).withOffsetSameInstant(ZoneOffset.UTC);
        }
        if (o instanceof Timestamp) {
            return ((Timestamp) o).toInstant().atOffset(ZoneOffset.UTC);
        }
        return OffsetDateTime.parse(o.toString());
    }
}
