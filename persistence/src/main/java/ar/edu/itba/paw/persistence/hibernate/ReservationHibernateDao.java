package ar.edu.itba.paw.persistence.hibernate;

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
import java.util.Map;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.ReservationCard;
import ar.edu.itba.paw.models.util.ReservationSearchCriteria;
import ar.edu.itba.paw.persistence.ReservationDao;

@Transactional
@Repository
public class ReservationHibernateDao implements ReservationDao {

    private static final List<Reservation.Status> BLOCKING_STATUSES = Arrays.asList(
            Reservation.Status.PENDING, Reservation.Status.ACCEPTED, Reservation.Status.STARTED);

    @PersistenceContext
    private EntityManager em;

    @Value("${app.reservation.payment-proof-reminder-lead-hours:2}")
    private int paymentProofReminderLeadHours;

    @Override
    public boolean hasActiveOverlap(final long listingId, final OffsetDateTime startDate, final OffsetDateTime endDate) {
        final Number count = (Number) em.createQuery(
                        "SELECT COUNT(r) FROM Reservation r "
                                + "WHERE r.listingId = :listingId "
                                + "AND r.status IN :statuses "
                                + "AND r.startDate < :endDate "
                                + "AND r.endDate > :startDate")
                .setParameter("listingId", listingId)
                .setParameter("statuses", BLOCKING_STATUSES)
                .setParameter("endDate", endDate)
                .setParameter("startDate", startDate)
                .getSingleResult();
        return count != null && count.longValue() > 0;
    }

    @Override
    public List<Reservation> findBlockingByListingId(final long listingId) {
        return em.createQuery(
                        "FROM Reservation r WHERE r.listingId = :listingId AND r.status IN :statuses ORDER BY r.startDate ASC",
                        Reservation.class)
                .setParameter("listingId", listingId)
                .setParameter("statuses", BLOCKING_STATUSES)
                .getResultList();
    }

    @Override
    public List<Reservation> findBlockingByListingIds(final Collection<Long> listingIds) {
        if (listingIds == null || listingIds.isEmpty()) {
            return List.of();
        }
        return em.createQuery(
                        "FROM Reservation r WHERE r.listingId IN :listingIds AND r.status IN :statuses "
                                + "ORDER BY r.listingId ASC, r.startDate ASC",
                        Reservation.class)
                .setParameter("listingIds", listingIds)
                .setParameter("statuses", BLOCKING_STATUSES)
                .getResultList();
    }

    @Override
    public Reservation createReservation(
            final long riderId,
            final long listingId,
            final OffsetDateTime startDate,
            final OffsetDateTime endDate,
            final Reservation.Status status,
            final BigDecimal totalPrice,
            final OffsetDateTime paymentProofDeadlineAt) {
        final OffsetDateTime now = OffsetDateTime.now();
        final Reservation reservation = Reservation.builder()
                .riderId(riderId)
                .listingId(listingId)
                .startDate(startDate)
                .endDate(endDate)
                .status(status)
                .totalPrice(totalPrice)
                .createdAt(now)
                .updatedAt(now)
                .paymentProofDeadlineAt(paymentProofDeadlineAt)
                .paymentApproved(false)
                .carReturned(false)
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
    public int attachPaymentReceiptAndAccept(final long reservationId, final long riderId, final long storedFileId) {
        return em.createNativeQuery(
                        "UPDATE reservations SET status = 'accepted', payment_receipt_file_id = :fileId, updated_at = :now "
                                + "WHERE id = :id AND rider_id = :riderId AND LOWER(status) = 'pending'")
                .setParameter("fileId", storedFileId)
                .setParameter("now", new Timestamp(System.currentTimeMillis()))
                .setParameter("id", reservationId)
                .setParameter("riderId", riderId)
                .executeUpdate();
    }

    @Override
    public int updatePaymentApproved(final long reservationId, final long ownerUserId, final boolean approved) {
        return em.createNativeQuery(
                        "UPDATE reservations SET payment_approved = :approved, updated_at = :now "
                                + "WHERE id = :id AND listing_id IN ("
                                + "SELECT l.id FROM listings l JOIN cars c ON c.id = l.car_id WHERE c.owner_id = :ownerId)")
                .setParameter("approved", approved)
                .setParameter("now", new Timestamp(System.currentTimeMillis()))
                .setParameter("id", reservationId)
                .setParameter("ownerId", ownerUserId)
                .executeUpdate();
    }

    @Override
    public Optional<Reservation> getReservationById(final long id) {
        return Optional.ofNullable(em.find(Reservation.class, id));
    }

    @Override
    public Optional<Reservation> getOwnerReservationById(final long ownerId, final long reservationId) {
        return em.createQuery(
                        "SELECT r FROM Reservation r, Listing l, Car c "
                                + "WHERE l.id = r.listingId AND c.id = l.carId "
                                + "AND r.id = :reservationId AND c.ownerId = :ownerId",
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
                        + "JOIN listings l ON l.id = r.listing_id "
                        + "JOIN cars c ON c.id = l.car_id "
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
                + "JOIN listings l ON l.id = r.listing_id "
                + "JOIN cars c ON c.id = l.car_id "
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
                        + "JOIN listings l ON l.id = r.listing_id "
                        + "JOIN cars c ON c.id = l.car_id "
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
                + "JOIN listings l ON l.id = r.listing_id "
                + "JOIN cars c ON c.id = l.car_id "
                + "WHERE c.owner_id = :ownerId ");
        appendReservationFilters(listSql, listParams, criteria);
        listSql.append("ORDER BY ").append(buildReservationOrderBy(criteria.getSortBy(), criteria.getSortDirection()))
               .append(" LIMIT :limit OFFSET :offset");
        final List<ReservationCard> content = runReservationCardNativeQuery(listSql.toString(), listParams);
        return new Page<>(content, page, pageSize, total != null ? total.longValue() : 0L);
    }

    @Override
    public int updateReservationStatus(final long reservationId, final String status) {
        return em.createNativeQuery(
                        "UPDATE reservations SET status = :status, updated_at = :now WHERE id = :id")
                .setParameter("status", status)
                .setParameter("now", new Timestamp(System.currentTimeMillis()))
                .setParameter("id", reservationId)
                .executeUpdate();
    }

    @Override
    public List<Reservation> getListingActiveReservations(final long listingId) {
        return em.createQuery(
                        "FROM Reservation r WHERE r.listingId = :listingId AND r.status IN :statuses ORDER BY r.startDate ASC",
                        Reservation.class)
                .setParameter("listingId", listingId)
                .setParameter("statuses", BLOCKING_STATUSES)
                .getResultList();
    }

    @Override
    public Page<ReservationCard> getListingReservationCards(
            final long ownerId,
            final long listingId,
            final int page,
            final int pageSize,
            final String statusFilter) {
        final Map<String, Object> countParams = new HashMap<>();
        countParams.put("ownerId", ownerId);
        countParams.put("listingId", listingId);
        final StringBuilder countSql = new StringBuilder(
                "SELECT COUNT(*) FROM reservations r "
                        + "JOIN listings l ON l.id = r.listing_id "
                        + "JOIN cars c ON c.id = l.car_id "
                        + "WHERE c.owner_id = :ownerId AND r.listing_id = :listingId ");
        if (statusFilter != null) {
            countSql.append("AND LOWER(r.status) = :statusFilter ");
            countParams.put("statusFilter", statusFilter);
        }
        final Number total = (Number) bindParams(em.createNativeQuery(countSql.toString()), countParams).getSingleResult();

        final int offset = page * pageSize;
        final Map<String, Object> listParams = new HashMap<>();
        listParams.put("ownerId", ownerId);
        listParams.put("listingId", listingId);
        listParams.put("limit", pageSize);
        listParams.put("offset", offset);
        final StringBuilder listSql = new StringBuilder(reservationCardSelectSql()
                + "FROM reservations r "
                + "JOIN listings l ON l.id = r.listing_id "
                + "JOIN cars c ON c.id = l.car_id "
                + "WHERE c.owner_id = :ownerId AND r.listing_id = :listingId ");
        if (statusFilter != null) {
            listSql.append("AND LOWER(r.status) = :statusFilter ");
            listParams.put("statusFilter", statusFilter);
        }
        listSql.append("ORDER BY r.created_at DESC LIMIT :limit OFFSET :offset");
        final List<ReservationCard> content = runReservationCardNativeQuery(listSql.toString(), listParams);
        return new Page<>(content, page, pageSize, total != null ? total.longValue() : 0L);
    }

    @Override
    public Map<String, Long> countListingReservationsByStatus(final long ownerId, final long listingId) {
        @SuppressWarnings("unchecked")
        final List<Object[]> rows = em.createNativeQuery(
                        "SELECT LOWER(r.status) AS status, COUNT(*) AS cnt "
                                + "FROM reservations r "
                                + "JOIN listings l ON l.id = r.listing_id "
                                + "JOIN cars c ON c.id = l.car_id "
                                + "WHERE c.owner_id = :ownerId AND r.listing_id = :listingId "
                                + "GROUP BY LOWER(r.status)")
                .setParameter("ownerId", ownerId)
                .setParameter("listingId", listingId)
                .getResultList();
        final Map<String, Long> result = new LinkedHashMap<>();
        for (final Object[] row : rows) {
            result.put((String) row[0], ((Number) row[1]).longValue());
        }
        return result;
    }

    @Override
    public BigDecimal sumListingRevenueByStatuses(
            final long ownerId, final long listingId, final Collection<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return BigDecimal.ZERO;
        }
        final Object result = em.createNativeQuery(
                        "SELECT COALESCE(SUM(r.total_price), 0) FROM reservations r "
                                + "JOIN listings l ON l.id = r.listing_id "
                                + "JOIN cars c ON c.id = l.car_id "
                                + "WHERE c.owner_id = :ownerId AND r.listing_id = :listingId "
                                + "AND LOWER(r.status) IN (:statuses)")
                .setParameter("ownerId", ownerId)
                .setParameter("listingId", listingId)
                .setParameter("statuses", statuses)
                .getSingleResult();
        if (result == null) {
            return BigDecimal.ZERO;
        }
        if (result instanceof BigDecimal) {
            return (BigDecimal) result;
        }
        return new BigDecimal(result.toString());
    }

    @Override
    public long countListingReservationsCreatedBetween(
            final long ownerId, final long listingId, final OffsetDateTime from, final OffsetDateTime until) {
        final Number count = (Number) em.createQuery(
                        "SELECT COUNT(r) FROM Reservation r, Listing l, Car c "
                                + "WHERE l.id = r.listingId AND c.id = l.carId "
                                + "AND c.ownerId = :ownerId AND r.listingId = :listingId "
                                + "AND r.createdAt >= :from AND r.createdAt < :until")
                .setParameter("ownerId", ownerId)
                .setParameter("listingId", listingId)
                .setParameter("from", from)
                .setParameter("until", until)
                .getSingleResult();
        return count != null ? count.longValue() : 0L;
    }

    @Override
    public Optional<OffsetDateTime> findListingNextActiveReservationDate(
            final long ownerId, final long listingId, final OffsetDateTime after) {
        return em.createQuery(
                        "SELECT r.startDate FROM Reservation r, Listing l, Car c "
                                + "WHERE l.id = r.listingId AND c.id = l.carId "
                                + "AND c.ownerId = :ownerId AND r.listingId = :listingId "
                                + "AND r.status IN :statuses AND r.startDate > :after "
                                + "ORDER BY r.startDate ASC",
                        OffsetDateTime.class)
                .setParameter("ownerId", ownerId)
                .setParameter("listingId", listingId)
                .setParameter("statuses", Arrays.asList(Reservation.Status.ACCEPTED, Reservation.Status.STARTED))
                .setParameter("after", after)
                .setMaxResults(1)
                .getResultList().stream().findFirst();
    }

    @Override
    public List<Reservation> findListingFinishedReservations(final long ownerId, final long listingId) {
        return em.createQuery(
                        "SELECT r FROM Reservation r, Listing l, Car c "
                                + "WHERE l.id = r.listingId AND c.id = l.carId "
                                + "AND c.ownerId = :ownerId AND r.listingId = :listingId "
                                + "AND r.status = :status",
                        Reservation.class)
                .setParameter("ownerId", ownerId)
                .setParameter("listingId", listingId)
                .setParameter("status", Reservation.Status.FINISHED)
                .getResultList();
    }

    @Override
    public int markCarReturned(final long reservationId, final long ownerUserId) {
        return em.createNativeQuery(
                        "UPDATE reservations SET car_returned = TRUE, updated_at = :now WHERE id = :id AND listing_id IN ("
                                + "SELECT l.id FROM listings l JOIN cars c ON c.id = l.car_id WHERE c.owner_id = :ownerId)")
                .setParameter("now", new Timestamp(System.currentTimeMillis()))
                .setParameter("id", reservationId)
                .setParameter("ownerId", ownerUserId)
                .executeUpdate();
    }

    @Override
    public int unmarkCarReturned(final long reservationId, final long ownerUserId) {
        return em.createNativeQuery(
                        "UPDATE reservations SET car_returned = FALSE, updated_at = :now WHERE id = :id "
                                + "AND listing_id IN ("
                                + "SELECT l.id FROM listings l JOIN cars c ON c.id = l.car_id WHERE c.owner_id = :ownerId) "
                                + "AND car_returned = TRUE "
                                + "AND LOWER(TRIM(status)) IN ('accepted', 'started', 'finished')")
                .setParameter("now", new Timestamp(System.currentTimeMillis()))
                .setParameter("id", reservationId)
                .setParameter("ownerId", ownerUserId)
                .executeUpdate();
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
    public int claimReturnReminderEmailSent(final long reservationId) {
        return em.createNativeQuery(
                        "UPDATE reservations SET return_reminder_email_sent = TRUE, updated_at = :now "
                                + "WHERE id = :id AND return_reminder_email_sent = FALSE")
                .setParameter("now", new Timestamp(System.currentTimeMillis()))
                .setParameter("id", reservationId)
                .executeUpdate();
    }

    @Override
    public int claimReturnCheckoutEmailSent(final long reservationId) {
        return em.createNativeQuery(
                        "UPDATE reservations SET return_checkout_email_sent = TRUE, updated_at = :now "
                                + "WHERE id = :id AND return_checkout_email_sent = FALSE")
                .setParameter("now", new Timestamp(System.currentTimeMillis()))
                .setParameter("id", reservationId)
                .executeUpdate();
    }

    @Override
    public int claimRiderReviewInviteEmailSent(final long reservationId) {
        return em.createNativeQuery(
                        "UPDATE reservations SET rider_review_invite_email_sent = TRUE, updated_at = :now "
                                + "WHERE id = :id AND rider_review_invite_email_sent = FALSE")
                .setParameter("now", new Timestamp(System.currentTimeMillis()))
                .setParameter("id", reservationId)
                .executeUpdate();
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
                                + "AND r.payment_approved = FALSE "
                                + "AND r.pending_paymentproof_email_sent = FALSE",
                        Reservation.class)
                .setParameter("windowEnd", toTimestamp(windowEnd))
                .getResultList();
    }

    @Override
    public int claimPendingPaymentProofEmailSent(final long reservationId) {
        return em.createNativeQuery(
                        "UPDATE reservations SET pending_paymentproof_email_sent = TRUE, updated_at = :now "
                                + "WHERE id = :id AND pending_paymentproof_email_sent = FALSE")
                .setParameter("now", new Timestamp(System.currentTimeMillis()))
                .setParameter("id", reservationId)
                .executeUpdate();
    }

    @Override
    public int updateParticipantCancellationWithRefundMeta(
            final long reservationId,
            final String statusLower,
            final boolean paymentRefundRequired,
            final OffsetDateTime refundProofDeadlineAtOrNull) {
        return em.createNativeQuery(
                        "UPDATE reservations SET status = :status, payment_refund_required = :refundRequired, "
                                + "refund_proof_deadline_at = :refundDeadline, payment_refund_receipt_file_id = NULL, "
                                + "payment_refund_approved = FALSE, pending_refund_email_sent = FALSE, updated_at = :now "
                                + "WHERE id = :id")
                .setParameter("status", statusLower)
                .setParameter("refundRequired", paymentRefundRequired)
                .setParameter(
                        "refundDeadline",
                        refundProofDeadlineAtOrNull != null ? toTimestamp(refundProofDeadlineAtOrNull) : null)
                .setParameter("now", new Timestamp(System.currentTimeMillis()))
                .setParameter("id", reservationId)
                .executeUpdate();
    }

    @Override
    public int attachRefundReceipt(final long reservationId, final long ownerUserId, final long storedFileId) {
        return em.createNativeQuery(
                        "UPDATE reservations SET payment_refund_receipt_file_id = :fileId, pending_refund_email_sent = TRUE, "
                                + "updated_at = :now WHERE id = :id AND listing_id IN ("
                                + "SELECT l.id FROM listings l JOIN cars c ON c.id = l.car_id WHERE c.owner_id = :ownerId) "
                                + "AND payment_refund_required = TRUE "
                                + "AND payment_refund_receipt_file_id IS NULL "
                                + "AND LOWER(TRIM(status)) IN ('cancelled_by_owner', 'cancelled_by_rider')")
                .setParameter("fileId", storedFileId)
                .setParameter("now", new Timestamp(System.currentTimeMillis()))
                .setParameter("id", reservationId)
                .setParameter("ownerId", ownerUserId)
                .executeUpdate();
    }

    @Override
    public int updatePaymentRefundApproved(final long reservationId, final long riderUserId, final boolean approved) {
        return em.createNativeQuery(
                        "UPDATE reservations SET payment_refund_approved = :approved, updated_at = :now WHERE id = :id "
                                + "AND rider_id = :riderId "
                                + "AND payment_refund_required = TRUE AND payment_refund_receipt_file_id IS NOT NULL "
                                + "AND LOWER(TRIM(status)) IN ('cancelled_by_owner', 'cancelled_by_rider')")
                .setParameter("approved", approved)
                .setParameter("now", new Timestamp(System.currentTimeMillis()))
                .setParameter("id", reservationId)
                .setParameter("riderId", riderUserId)
                .executeUpdate();
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
    public int claimPendingRefundEmailSent(final long reservationId) {
        return em.createNativeQuery(
                        "UPDATE reservations SET pending_refund_email_sent = TRUE, updated_at = :now "
                                + "WHERE id = :id AND pending_refund_email_sent = FALSE")
                .setParameter("now", new Timestamp(System.currentTimeMillis()))
                .setParameter("id", reservationId)
                .executeUpdate();
    }

    // ---- SQL helpers ----

    private static String reservationCardSelectSql() {
        return "SELECT r.id AS reservation_id, r.listing_id, r.start_date, r.end_date, r.status, "
                + "c.brand, c.model, l.day_price, "
                + "(SELECT cp.image_id FROM car_pictures cp WHERE cp.car_id = c.id "
                + "ORDER BY cp.display_order ASC LIMIT 1) AS image_id ";
    }

    private static void appendReservationFilters(
            final StringBuilder sql,
            final Map<String, Object> params,
            final ReservationSearchCriteria criteria) {
        if (!criteria.getStatusFilters().isEmpty()) {
            sql.append("AND LOWER(r.status) IN (:resStatuses) ");
            params.put("resStatuses", criteria.getStatusFilters());
        }
        final String textQuery = criteria.getTextQuery();
        if (textQuery != null) {
            final String q = "%" + escapeLike(textQuery) + "%";
            sql.append("AND (LOWER(c.brand) LIKE LOWER(:resSearch) ESCAPE '\\' "
                    + "OR LOWER(c.model) LIKE LOWER(:resSearch) ESCAPE '\\' "
                    + "OR LOWER(l.title) LIKE LOWER(:resSearch) ESCAPE '\\') ");
            params.put("resSearch", q);
        }
        if (!criteria.getCarTypes().isEmpty()) {
            sql.append("AND c.type IN (:resCarTypes) ");
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
            sql.append("AND l.day_price >= :resMinPrice ");
            params.put("resMinPrice", criteria.getMinPrice());
        }
        if (criteria.getMaxPrice() != null) {
            sql.append("AND l.day_price <= :resMaxPrice ");
            params.put("resMaxPrice", criteria.getMaxPrice());
        }
        final List<String> ratingBands = criteria.getRatingBands();
        if (!ratingBands.isEmpty()) {
            final List<String> conditions = new ArrayList<>();
            if (ratingBands.contains("UNDER_2")) {
                conditions.add("l.rating_avg < 2");
            }
            if (ratingBands.contains("2_TO_3")) {
                conditions.add("(l.rating_avg >= 2 AND l.rating_avg < 3)");
            }
            if (ratingBands.contains("3_TO_4")) {
                conditions.add("(l.rating_avg >= 3 AND l.rating_avg < 4)");
            }
            if (ratingBands.contains("OVER_4")) {
                conditions.add("l.rating_avg >= 4");
            }
            if (!conditions.isEmpty()) {
                sql.append("AND (").append(String.join(" OR ", conditions)).append(") ");
            }
        }
    }

    private static String buildReservationOrderBy(final String sortBy, final String sortDirection) {
        final String dir = "asc".equalsIgnoreCase(sortDirection) ? "ASC" : "DESC";
        if ("price".equals(sortBy)) {
            return "l.day_price " + dir;
        } else if ("rating".equals(sortBy)) {
            return "l.rating_avg " + dir + " NULLS LAST, r.created_at DESC";
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
        final long listingId = ((Number) row[1]).longValue();
        final OffsetDateTime startDate = toOffsetDateTime(row[2]);
        final OffsetDateTime endDate = toOffsetDateTime(row[3]);
        final String statusStr = (String) row[4];
        final Reservation.Status status = Reservation.Status.valueOf(statusStr.toUpperCase());
        final String brand = (String) row[5];
        final String model = (String) row[6];
        final BigDecimal dayPrice = (BigDecimal) row[7];
        final Object rawImageId = row[8];
        final long imageId = rawImageId == null ? 0L : ((Number) rawImageId).longValue();
        return new ReservationCard(reservationId, listingId, brand, model, dayPrice, imageId, startDate, endDate, status);
    }

    private static Query bindParams(final Query q, final Map<String, Object> params) {
        params.forEach(q::setParameter);
        return q;
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
