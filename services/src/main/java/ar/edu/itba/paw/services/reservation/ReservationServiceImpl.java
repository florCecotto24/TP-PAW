package ar.edu.itba.paw.services.reservation;


import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.models.domain.reservation.ReservationParticipantRole;
import ar.edu.itba.paw.models.domain.file.StoredFile;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.file.BinaryContent;
import ar.edu.itba.paw.models.dto.reservation.BlockingReservationProjection;
import ar.edu.itba.paw.models.dto.reservation.ReservationCard;
import ar.edu.itba.paw.models.util.search.ReservationSearchCriteria;
import ar.edu.itba.paw.persistence.reservation.ReservationDao;

/**
 * Facade over the split reservation services. The legacy 1.6k-line god class is gone; the
 * implementation now lives in five focused services:
 *
 * <ul>
 *   <li>{@link ReservationQueryService} — reads, search criteria, blocking lookups,
 *       admin / refund overdue lists.</li>
 *   <li>{@link ReservationPricingService} — pricing, billable day math, timing-policy
 *       getters, rider input validations.</li>
 *   <li>{@link ReservationWorkflowService} — submit, edit, cancel, markCarReturned.</li>
 *   <li>{@link ReservationLifecycleSchedulerService} — return reminder / checkout / review
 *       invite / review auto-skip jobs and per-car analytics.</li>
 *   <li>{@link ReservationPaymentService} — payment / refund receipts, payment-proof
 *       expiration sweep, refund reminders, refund-overdue sweep + owner blocking.</li>
 * </ul>
 *
 * <p>New consumers should depend on the focused service that matches their use case. This
 * facade exists so the legacy {@link ReservationService} surface keeps working for existing
 * controllers, schedulers, security adapters, JSP views, and tests without having to migrate
 * every call site in the same change.
 */
@Service
public class ReservationServiceImpl implements ReservationService {

    /**
     * The architectural rule "each ServiceImpl may only call its own DAO" makes this facade the sole
     * owner of {@link ReservationDao}. The four sub-services no longer inject the DAO; they call back
     * through {@link ReservationService} for any row read/write. All low-level pass-throughs to the DAO
     * live in the "Sub-service-orchestrated operations" section below.
     *
     * <p>{@code @Lazy} on the sub-services breaks the bidirectional cycle: this facade injects them
     * eagerly for high-level orchestration; each one injects this facade lazily for DAO access.</p>
     */
    private final ReservationDao reservationDao;
    private final ReservationQueryService queryService;
    private final ReservationPricingService pricingService;
    private final ReservationWorkflowService workflowService;
    private final ReservationLifecycleSchedulerService schedulerService;
    private final ReservationPaymentService paymentService;

    @Autowired
    public ReservationServiceImpl(
            final ReservationDao reservationDao,
            @Lazy final ReservationQueryService queryService,
            @Lazy final ReservationPricingService pricingService,
            @Lazy final ReservationWorkflowService workflowService,
            @Lazy final ReservationLifecycleSchedulerService schedulerService,
            @Lazy final ReservationPaymentService paymentService) {
        this.reservationDao = reservationDao;
        this.queryService = queryService;
        this.pricingService = pricingService;
        this.workflowService = workflowService;
        this.schedulerService = schedulerService;
        this.paymentService = paymentService;
    }

    // ---------------------------------------------------------------------------------------
    // Timing policy getters → pricing
    // ---------------------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public int getConfiguredPickupLeadHours() {
        return pricingService.getConfiguredPickupLeadHours();
    }

    @Override
    @Transactional(readOnly = true)
    public int getConfiguredPaymentProofDeadlineHours() {
        return pricingService.getConfiguredPaymentProofDeadlineHours();
    }

    @Override
    @Transactional(readOnly = true)
    public int getConfiguredReturnReminderHoursBeforeCheckout() {
        return pricingService.getConfiguredReturnReminderHoursBeforeCheckout();
    }

    @Override
    @Transactional(readOnly = true)
    public int getConfiguredMaxReservationBillableDays() {
        return pricingService.getConfiguredMaxReservationBillableDays();
    }

    // ---------------------------------------------------------------------------------------
    // Read queries → query service
    // ---------------------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public Optional<Reservation> getReservationById(final long id) {
        return reservationDao.getReservationById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Reservation> getRiderReservationById(final long riderId, final long reservationId) {
        // Rider-scope filter applied here (DAO has no rider-scoped variant); see queryService docs.
        return reservationDao.getReservationById(reservationId)
                .filter(reservation -> reservation.getRiderId() == riderId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Reservation> getOwnerReservationById(final long ownerId, final long reservationId) {
        return reservationDao.getOwnerReservationById(ownerId, reservationId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsRiderReservationForCar(final long riderId, final long carId) {
        return reservationDao.existsByRiderIdAndCarId(riderId, carId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReservationCard> getRiderReservationCards(final ReservationSearchCriteria criteria) {
        return reservationDao.getRiderReservationCards(criteria);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReservationCard> getOwnerReservationCards(final ReservationSearchCriteria criteria) {
        return reservationDao.getOwnerReservationCards(criteria);
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationSearchCriteria buildReservationSearchCriteria(
            final Long ownerId, final Long riderId,
            final List<Car.Type> category, final List<Car.Transmission> transmission,
            final List<Car.Powertrain> powertrain,
            final BigDecimal priceMin, final BigDecimal priceMax,
            final List<String> rating, final List<Reservation.Status> statusFilter,
            final int page, final int pageSize, final String sort, final String textQuery, final Long carId) {
        return queryService.buildReservationSearchCriteria(
                ownerId, riderId, category, transmission, powertrain,
                priceMin, priceMax, rating, statusFilter, page, pageSize, sort, textQuery, carId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReservationCard> getCarReservationCards(
            final long ownerId, final long carId, final int page, final int pageSize, final String statusFilter) {
        return reservationDao.getCarReservationCards(ownerId, carId, page, pageSize, statusFilter);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlockingReservationProjection> findBlockingReservationsByCarId(final long carId) {
        return reservationDao.findBlockingByCarId(carId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, List<BlockingReservationProjection>> findBlockingReservationsByCarIds(
            final Collection<Long> carIds) {
        if (carIds == null || carIds.isEmpty()) {
            return Map.of();
        }
        final List<BlockingReservationProjection> rows = reservationDao.findBlockingByCarIds(carIds);
        return rows.stream().collect(Collectors.groupingBy(BlockingReservationProjection::getCarId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlockingReservationProjection> findBlockingReservationsByCarIdExcluding(
            final long carId, final long excludingReservationId) {
        return reservationDao.findBlockingByCarIdExcluding(carId, excludingReservationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlockingReservationProjection> findBlockingReservationsByCarIdInRange(
            final long carId, final OffsetDateTime from, final OffsetDateTime to) {
        return reservationDao.findBlockingByCarIdInRange(carId, from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reservation> findReminderReservations(final OffsetDateTime from, final OffsetDateTime to) {
        return reservationDao.getReminderReservations(from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReservationCard> findAllReservationCards(final ReservationSearchCriteria criteria) {
        return reservationDao.findAllReservationCards(criteria);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReservationCard> findReservationCardById(final long reservationId) {
        return reservationDao.findReservationCardById(reservationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findOverdueRefundProofReservationIdsForOwner(final long ownerUserId) {
        return reservationDao.findOverdueRefundProofReservationsForOwner(
                        ownerUserId, OffsetDateTime.now(ZoneOffset.UTC))
                .stream()
                .map(Reservation::getId)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, List<Long>> findOverdueRefundProofReservationIdsByOwnerIds(
            final Collection<Long> ownerUserIds) {
        if (ownerUserIds == null || ownerUserIds.isEmpty()) {
            return Map.of();
        }
        return reservationDao.findOverdueRefundProofReservationIdsByOwnerIds(
                ownerUserIds, OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> findOwnerReservationIdsRequiringRefundProof(final long ownerUserId) {
        return reservationDao.findReservationsRequiringRefundProofForOwner(ownerUserId)
                .stream()
                .map(Reservation::getId)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> findOwnerCarIdsWithReservationRequiringRefundProof(final long ownerUserId) {
        return reservationDao.findReservationsRequiringRefundProofForOwner(ownerUserId)
                .stream()
                .map(r -> r.getCar().getId())
                .collect(Collectors.toUnmodifiableSet());
    }

    // ---------------------------------------------------------------------------------------
    // Pricing → pricing service
    // ---------------------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public Optional<String> normalizeClientReservationTotal(final String reservationTotal) {
        return pricingService.normalizeClientReservationTotal(reservationTotal);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> reservationTotalDisplayByCar(
            final Long carId, final String fromDateTime, final String untilDateTime) {
        return pricingService.reservationTotalDisplayByCar(carId, fromDateTime, untilDateTime);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BigDecimal> calculateTotalByCar(
            final long carId, final OffsetDateTime startDate, final OffsetDateTime endDate) {
        return pricingService.calculateTotalByCar(carId, startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public long calculateBillableDays(final OffsetDateTime startDate, final OffsetDateTime endDate) {
        return pricingService.calculateBillableDays(startDate, endDate);
    }

    // ---------------------------------------------------------------------------------------
    // Lifecycle → workflow
    // ---------------------------------------------------------------------------------------

    @Override
    @Transactional
    public Reservation submitRiderReservationByCar(
            final long riderId, final long carId, final Long availabilityId,
            final String fromDateTime, final String untilDateTime) {
        return workflowService.submitRiderReservationByCar(riderId, carId, availabilityId, fromDateTime, untilDateTime);
    }

    @Override
    @Transactional
    public Reservation editPendingReservationByRider(
            final long riderId, final long reservationId,
            final String fromDateTime, final String untilDateTime) {
        return workflowService.editPendingReservationByRider(riderId, reservationId, fromDateTime, untilDateTime);
    }

    @Override
    @Transactional
    public Optional<Reservation> cancelReservation(final long reservationId) {
        return workflowService.cancelReservation(reservationId);
    }

    @Override
    @Transactional
    public Optional<Reservation> cancelReservationAsParticipant(final long userId, final long reservationId) {
        return workflowService.cancelReservationAsParticipant(userId, reservationId);
    }

    @Override
    @Transactional
    public Reservation cancelReservationAsParticipantScoped(
            final long viewerUserId, final long reservationId, final ReservationParticipantRole viewerRole) {
        return workflowService.cancelReservationAsParticipantScoped(viewerUserId, reservationId, viewerRole);
    }

    @Override
    @Transactional
    public void markCarReturnedByOwner(final long ownerUserId, final long reservationId) {
        workflowService.markCarReturnedByOwner(ownerUserId, reservationId);
    }

    @Override
    @Transactional
    public Reservation patchReservation(
            final long viewerUserId,
            final long reservationId,
            final Reservation.Status cancellationStatus,
            final Boolean carReturned,
            final String fromDateTimeWall,
            final String untilDateTimeWall) {
        return workflowService.patchReservation(
                viewerUserId, reservationId, cancellationStatus, carReturned, fromDateTimeWall, untilDateTimeWall);
    }

    // ---------------------------------------------------------------------------------------
    // Payment side → payment service
    // ---------------------------------------------------------------------------------------

    // Deliberately NOT @Transactional: each cancellation commits in ExpiredPaymentProofRowCanceller
    // (REQUIRES_NEW); an outer TX would nest connections and hold locks for the whole batch.
    @Override
    public void cancelExpiredPendingPaymentReservations() {
        paymentService.cancelExpiredPendingPaymentReservations();
    }

    // Deliberately NOT @Transactional: same pattern as payment sweeps — each row commits in
    // ReservationLifecycleRowProcessor (REQUIRES_NEW); an outer TX would nest connections.
    @Override
    public int transitionAcceptedReservationsToStarted() {
        return schedulerService.transitionAcceptedReservationsToStarted();
    }

    @Override
    @Transactional
    public void attachPaymentReceipt(
            final long riderId, final long reservationId,
            final String originalFilename, final String contentType, final byte[] data) {
        paymentService.attachPaymentReceipt(riderId, reservationId, originalFilename, contentType, data);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoredFile> findPaymentReceiptForParticipant(final long userId, final long reservationId) {
        return paymentService.findPaymentReceiptForParticipant(userId, reservationId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BinaryContent> findPaymentReceiptContentForParticipant(
            final long userId, final long reservationId) {
        return paymentService.findPaymentReceiptContentForParticipant(userId, reservationId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BinaryContent> findPaymentReceiptContentForAdmin(final long reservationId) {
        return paymentService.findPaymentReceiptContentForAdmin(reservationId);
    }

    @Override
    @Transactional
    public void attachRefundReceiptByOwner(
            final long ownerUserId, final long reservationId,
            final String originalFilename, final String contentType, final byte[] data) {
        paymentService.attachRefundReceiptByOwner(ownerUserId, reservationId, originalFilename, contentType, data);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoredFile> findRefundReceiptForParticipant(final long userId, final long reservationId) {
        return paymentService.findRefundReceiptForParticipant(userId, reservationId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BinaryContent> findRefundReceiptContentForParticipant(
            final long userId, final long reservationId) {
        return paymentService.findRefundReceiptContentForParticipant(userId, reservationId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BinaryContent> findRefundReceiptContentForAdmin(final long reservationId) {
        return paymentService.findRefundReceiptContentForAdmin(reservationId);
    }

    // Deliberately NOT @Transactional: each reminder is claimed in ReservationSweepRowProcessor
    // (REQUIRES_NEW); an outer TX would nest connections.
    @Override
    public int dispatchDuePaymentProofReminderEmails() {
        return paymentService.dispatchDuePaymentProofReminderEmails();
    }

    // Deliberately NOT @Transactional: each reminder is claimed in ReservationSweepRowProcessor
    // (REQUIRES_NEW); an outer TX would nest connections.
    @Override
    public int dispatchDueRefundProofReminderEmails() {
        return paymentService.dispatchDueRefundProofReminderEmails();
    }

    // Deliberately NOT @Transactional: each owner block commits in REQUIRES_NEW.
    @Override
    public int sweepRefundOverdueAndBlockOwners() {
        return paymentService.sweepRefundOverdueAndBlockOwners();
    }

    // ---------------------------------------------------------------------------------------
    // Scheduled lifecycle jobs + per-car analytics → scheduler service
    // ---------------------------------------------------------------------------------------

    // Deliberately NOT @Transactional: claim-then-mail-after-commit per row (REQUIRES_NEW).
    @Override
    public int dispatchReservationReminderEmails() {
        return schedulerService.dispatchReservationReminderEmails();
    }

    // Deliberately NOT @Transactional: claim-then-mail-after-commit per row in the lifecycle
    // processor (REQUIRES_NEW), matching payment-proof reminder sweeps.
    @Override
    public int dispatchReturnReminderEmails() {
        return schedulerService.dispatchReturnReminderEmails();
    }

    // Deliberately NOT @Transactional: claim-then-mail-after-commit per row (REQUIRES_NEW).
    @Override
    public int dispatchReturnCheckoutEmails() {
        return schedulerService.dispatchReturnCheckoutEmails();
    }

    // Deliberately NOT @Transactional: claim-then-mail-after-commit per row (REQUIRES_NEW).
    @Override
    public int dispatchRiderReviewInviteEmails() {
        return schedulerService.dispatchRiderReviewInviteEmails();
    }

    // Deliberately NOT @Transactional: each auto-skip commits in REQUIRES_NEW.
    @Override
    public int dispatchReviewAutoSkips() {
        return schedulerService.dispatchReviewAutoSkips();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> countCarReservationsByStatus(final long ownerId, final long carId) {
        return reservationDao.countCarReservationsByStatus(ownerId, carId);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getCarTotalEarnings(final long ownerId, final long carId) {
        return schedulerService.getCarTotalEarnings(ownerId, carId);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getCarPendingEarnings(final long ownerId, final long carId) {
        return schedulerService.getCarPendingEarnings(ownerId, carId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getCarTotalDaysRented(final long ownerId, final long carId) {
        return schedulerService.getCarTotalDaysRented(ownerId, carId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getCarReservationsThisMonth(final long ownerId, final long carId) {
        return schedulerService.getCarReservationsThisMonth(ownerId, carId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OffsetDateTime> getCarNextReservationDate(final long ownerId, final long carId) {
        return schedulerService.getCarNextReservationDate(ownerId, carId);
    }

    // -----------------------------------------------------------------------------------------------------------
    // Sub-service-orchestrated operations on reservation rows (see contract Javadoc). Each method below is a thin
    // transactional pass-through to {@code ReservationDao} so that {@link ReservationQueryService},
    // {@link ReservationWorkflowService}, {@link ReservationPaymentService}, and
    // {@link ReservationLifecycleSchedulerService} can mutate / read reservation rows without injecting the DAO.
    // -----------------------------------------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveOverlapByCar(final long carId, final OffsetDateTime startDate, final OffsetDateTime endDate) {
        return reservationDao.hasActiveOverlapByCar(carId, startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveOverlapByCarExcluding(
            final long carId, final OffsetDateTime startDate, final OffsetDateTime endDate,
            final long excludingReservationId) {
        return reservationDao.hasActiveOverlapByCarExcluding(carId, startDate, endDate, excludingReservationId);
    }

    @Override
    @Transactional
    public Reservation createReservationForCar(
            final long riderId, final long carId,
            final OffsetDateTime startDate, final OffsetDateTime endDate,
            final Reservation.Status status, final BigDecimal totalPrice,
            final OffsetDateTime paymentProofDeadlineAt) {
        return reservationDao.createReservationForCar(
                riderId, carId, startDate, endDate, status, totalPrice, paymentProofDeadlineAt);
    }

    @Override
    @Transactional
    public int updateReservationStatus(final long reservationId, final String status) {
        return reservationDao.updateReservationStatus(reservationId, status);
    }

    @Override
    @Transactional
    public int updateRiderPendingReservationPeriod(
            final long reservationId, final long riderId,
            final OffsetDateTime newStartDate, final OffsetDateTime newEndDate,
            final BigDecimal newTotalPrice, final OffsetDateTime newPaymentProofDeadlineAt) {
        return reservationDao.updateRiderPendingReservationPeriod(
                reservationId, riderId, newStartDate, newEndDate, newTotalPrice, newPaymentProofDeadlineAt);
    }

    @Override
    @Transactional
    public int updateParticipantCancellationWithRefundMeta(
            final long reservationId, final String statusLower,
            final boolean paymentRefundRequired, final OffsetDateTime refundProofDeadlineAtOrNull) {
        return reservationDao.updateParticipantCancellationWithRefundMeta(
                reservationId, statusLower, paymentRefundRequired, refundProofDeadlineAtOrNull);
    }

    @Override
    @Transactional
    public int applyAdminCarPauseCancellation(
            final long reservationId,
            final boolean paymentRefundRequired,
            final OffsetDateTime refundProofDeadlineAtOrNull) {
        return reservationDao.applyAdminCarPauseCancellation(
                reservationId, paymentRefundRequired, refundProofDeadlineAtOrNull);
    }

    @Override
    @Transactional
    public int markCarReturned(final long reservationId, final long ownerUserId) {
        return reservationDao.markCarReturned(reservationId, ownerUserId);
    }

    @Override
    @Transactional
    public int attachPaymentReceiptAndAccept(
            final long reservationId, final long riderId, final long storedFileId) {
        return reservationDao.attachPaymentReceiptAndAccept(reservationId, riderId, storedFileId);
    }

    @Override
    @Transactional
    public int attachRefundReceipt(
            final long reservationId, final long ownerUserId, final long storedFileId) {
        return reservationDao.attachRefundReceipt(reservationId, ownerUserId, storedFileId);
    }

    @Override
    @Transactional
    public int cancelPendingMissingPaymentProofIfEligible(
            final long reservationId, final OffsetDateTime now) {
        return reservationDao.cancelPendingMissingPaymentProofIfEligible(reservationId, now);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reservation> findPendingPaymentPastDeadline(final OffsetDateTime now) {
        return reservationDao.findPendingPaymentPastDeadline(now);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findAcceptedReservationIdsWithStartOnOrBefore(final OffsetDateTime now) {
        return reservationDao.findAcceptedReservationIdsWithStartOnOrBefore(now);
    }

    @Override
    @Transactional
    public int transitionAcceptedToStartedIfDue(final long reservationId, final OffsetDateTime now) {
        return reservationDao.transitionAcceptedToStartedIfDue(reservationId, now);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reservation> findReservationsForReturnReminderEmail(
            final OffsetDateTime now, final int hoursBeforeCheckout) {
        return reservationDao.findReservationsForReturnReminderEmail(now, hoursBeforeCheckout);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reservation> findReservationsForReturnCheckoutEmail(final OffsetDateTime now) {
        return reservationDao.findReservationsForReturnCheckoutEmail(now);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reservation> findReservationsForRiderReviewInviteEmail(final OffsetDateTime now) {
        return reservationDao.findReservationsForRiderReviewInviteEmail(now);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reservation> findReservationsForRiderReviewAutoSkip(
            final OffsetDateTime now, final OffsetDateTime endDateCutoff) {
        return reservationDao.findReservationsForRiderReviewAutoSkip(now, endDateCutoff);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reservation> findReservationsForOwnerReviewAutoSkip(
            final OffsetDateTime now, final OffsetDateTime carReturnedAtCutoff) {
        return reservationDao.findReservationsForOwnerReviewAutoSkip(now, carReturnedAtCutoff);
    }

    @Override
    @Transactional
    public int claimReturnReminderEmailSent(final long reservationId) {
        return reservationDao.claimReturnReminderEmailSent(reservationId);
    }

    @Override
    @Transactional
    public int claimPickupReminderEmailSent(final long reservationId) {
        return reservationDao.claimPickupReminderEmailSent(reservationId);
    }

    @Override
    @Transactional
    public int claimReturnCheckoutEmailSent(final long reservationId) {
        return reservationDao.claimReturnCheckoutEmailSent(reservationId);
    }

    @Override
    @Transactional
    public int claimRiderReviewInviteEmailSent(final long reservationId) {
        return reservationDao.claimRiderReviewInviteEmailSent(reservationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reservation> findReservationsWithDuePendingPaymentProof(final OffsetDateTime now) {
        return reservationDao.findReservationsWithDuePendingPaymentProof(now);
    }

    @Override
    @Transactional
    public int claimPendingPaymentProofEmailSent(final long reservationId) {
        return reservationDao.claimPendingPaymentProofEmailSent(reservationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reservation> findReservationsWithDuePendingRefundProof(final OffsetDateTime now) {
        return reservationDao.findReservationsWithDuePendingRefundProof(now);
    }

    @Override
    @Transactional
    public int claimPendingRefundEmailSent(final long reservationId) {
        return reservationDao.claimPendingRefundEmailSent(reservationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reservation> findReservationsWithOverdueRefundProof(final OffsetDateTime now) {
        return reservationDao.findReservationsWithOverdueRefundProof(now);
    }

    @Override
    @Transactional(readOnly = true)
    public long countOverdueRefundProofsForOwner(final long ownerUserId, final OffsetDateTime now) {
        return reservationDao.countOverdueRefundProofsForOwner(ownerUserId, now);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reservation> findOverdueRefundProofReservationsForOwner(
            final long ownerUserId, final OffsetDateTime now) {
        return reservationDao.findOverdueRefundProofReservationsForOwner(ownerUserId, now);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reservation> findReservationsRequiringRefundProofForOwner(final long ownerUserId) {
        return reservationDao.findReservationsRequiringRefundProofForOwner(ownerUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal sumCarRevenueByStatuses(
            final long ownerId, final long carId, final Collection<String> statuses) {
        return reservationDao.sumCarRevenueByStatuses(ownerId, carId, statuses);
    }

    @Override
    @Transactional(readOnly = true)
    public long countCarReservationsCreatedBetween(
            final long ownerId, final long carId,
            final OffsetDateTime from, final OffsetDateTime until) {
        return reservationDao.countCarReservationsCreatedBetween(ownerId, carId, from, until);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OffsetDateTime> findCarNextActiveReservationDate(
            final long ownerId, final long carId, final OffsetDateTime after) {
        return reservationDao.findCarNextActiveReservationDate(ownerId, carId, after);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OffsetDateTime[]> findCarFinishedReservationBounds(final long ownerId, final long carId) {
        return reservationDao.findCarFinishedReservationBounds(ownerId, carId);
    }

    @Override
    @Transactional(readOnly = true)
    public long sumCarFinishedBillableDays(final long ownerId, final long carId) {
        return reservationDao.sumCarFinishedBillableDays(ownerId, carId);
    }
}
