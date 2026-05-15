package ar.edu.itba.paw.services;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.reservation.ReservationMessageException;
import ar.edu.itba.paw.models.domain.Listing;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.domain.ReservationMessage;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.ReservationMessageDto;
import ar.edu.itba.paw.models.email.ReservationChatMessageEmailPayload;
import ar.edu.itba.paw.persistence.ReservationMessageDao;
import ar.edu.itba.paw.services.mail.MailPublicUrls;
import ar.edu.itba.paw.services.policy.ReservationChatPolicy;
import ar.edu.itba.paw.services.policy.ReservationMessageValidationPolicy;

@Service
public final class ReservationMessageServiceImpl implements ReservationMessageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationMessageServiceImpl.class);
    private static final int EMAIL_PREVIEW_MAX_LENGTH = 200;

    private final ReservationMessageDao reservationMessageDao;
    private final ReservationService reservationService;
    private final UserService userService;
    private final ListingService listingService;
    private final CarService carService;
    private final EmailService emailService;
    private final MailPublicUrls mailPublicUrls;
    private final ReservationMessageValidationPolicy messageValidationPolicy;
    private final ReservationChatPolicy chatPolicy;

    @Autowired
    public ReservationMessageServiceImpl(
            final ReservationMessageDao reservationMessageDao,
            final ReservationService reservationService,
            final UserService userService,
            final ListingService listingService,
            final CarService carService,
            final EmailService emailService,
            final MailPublicUrls mailPublicUrls,
            final ReservationMessageValidationPolicy messageValidationPolicy,
            final ReservationChatPolicy chatPolicy) {
        this.reservationMessageDao = reservationMessageDao;
        this.reservationService = reservationService;
        this.userService = userService;
        this.listingService = listingService;
        this.carService = carService;
        this.emailService = emailService;
        this.mailPublicUrls = mailPublicUrls;
        this.messageValidationPolicy = messageValidationPolicy;
        this.chatPolicy = chatPolicy;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isChatAvailable(final Reservation reservation) {
        return chatPolicy.isChatAvailable(reservation, OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Override
    @Transactional(readOnly = true)
    public int getMessageBodyMaxLength() {
        return messageValidationPolicy.getBodyMaxLength();
    }

    @Override
    @Transactional(readOnly = true)
    public int getHistoryPageSize() {
        return chatPolicy.getHistoryPageSize();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canParticipantAccessReservationChat(final long viewerUserId, final long reservationId) {
        return findParticipantReservation(viewerUserId, reservationId)
                .filter(this::isChatAvailable)
                .isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationMessageDto> getMessagesForParticipant(
            final long viewerUserId, final long reservationId, final int page) {
        final Reservation reservation = findParticipantReservation(viewerUserId, reservationId)
                .orElseThrow(() -> new ReservationMessageException(MessageKeys.RESERVATION_CHAT_NOT_PARTICIPANT));
        if (!isChatAvailable(reservation)) {
            throw new ReservationMessageException(MessageKeys.RESERVATION_CHAT_NOT_AVAILABLE);
        }
        final int pageSize = chatPolicy.getHistoryPageSize();
        final int safePage = Math.max(0, page);
        final int offset = safePage * pageSize;
        return reservationMessageDao
                .findByReservationIdOrderByCreatedAtAsc(reservationId, offset, pageSize)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ReservationMessageDto postMessage(final long senderUserId, final long reservationId, final String body) {
        final String trimmed = body == null ? "" : body.trim();
        if (trimmed.isEmpty()) {
            throw new ReservationMessageException(MessageKeys.RESERVATION_CHAT_BODY_EMPTY);
        }
        final int maxLength = messageValidationPolicy.getBodyMaxLength();
        if (trimmed.length() > maxLength) {
            throw new ReservationMessageException(MessageKeys.RESERVATION_CHAT_BODY_TOO_LONG, maxLength);
        }

        final Reservation reservation = findParticipantReservation(senderUserId, reservationId)
                .orElseThrow(() -> new ReservationMessageException(MessageKeys.RESERVATION_CHAT_NOT_PARTICIPANT));
        if (!isChatAvailable(reservation)) {
            throw new ReservationMessageException(MessageKeys.RESERVATION_CHAT_NOT_AVAILABLE);
        }

        final ReservationMessage saved = reservationMessageDao.create(reservationId, senderUserId, trimmed);
        final ReservationMessageDto dto = toDto(saved);
        enqueueCounterpartyNotification(senderUserId, reservation, trimmed);
        return dto;
    }

    private Optional<Reservation> findParticipantReservation(final long viewerUserId, final long reservationId) {
        final Optional<Reservation> asRider = reservationService.getRiderReservationById(viewerUserId, reservationId);
        if (asRider.isPresent()) {
            return asRider;
        }
        return reservationService.getOwnerReservationById(viewerUserId, reservationId);
    }

    private ReservationMessageDto toDto(final ReservationMessage message) {
        final User sender = message.getSender();
        return new ReservationMessageDto(
                message.getId(),
                message.getReservationId(),
                message.getSenderUserId(),
                formatDisplayName(sender),
                message.getBody(),
                message.getCreatedAt());
    }

    private static String formatDisplayName(final User user) {
        return user.getForename() + " " + user.getSurname();
    }

    private void enqueueCounterpartyNotification(
            final long senderUserId, final Reservation reservation, final String messageBody) {
        try {
            final long counterpartyId = resolveCounterpartyUserId(senderUserId, reservation);
            final Optional<User> counterpartyOpt = userService.getUserById(counterpartyId);
            final Optional<User> senderOpt = userService.getUserById(senderUserId);
            if (counterpartyOpt.isEmpty() || senderOpt.isEmpty()) {
                return;
            }
            final User counterparty = counterpartyOpt.get();
            final User sender = senderOpt.get();
            final String vehicleLabel = resolveVehicleLabel(reservation.getListingId());
            final boolean recipientIsOwner = counterpartyId != reservation.getRiderId();
            final String roleParam = recipientIsOwner ? "owner" : "rider";
            final String detailPath =
                    "/my-reservations/" + reservation.getId() + "/chat?role=" + roleParam;
            final Locale mailLocale = userService.resolveMailLocale(counterpartyId);
            final ReservationChatMessageEmailPayload payload = ReservationChatMessageEmailPayload.builder()
                    .messageLocale(mailLocale)
                    .recipientEmail(counterparty.getEmail())
                    .recipientFullName(formatDisplayName(counterparty))
                    .senderFullName(formatDisplayName(sender))
                    .messagePreview(truncateForEmailPreview(messageBody))
                    .vehicleLabel(vehicleLabel)
                    .reservationId(reservation.getId())
                    .detailUrl(mailPublicUrls.absolutePath(detailPath))
                    .build();
            emailService.sendReservationChatMessageNotification(payload);
        } catch (final RuntimeException e) {
            LOGGER.atWarn()
                    .setCause(e)
                    .addArgument(reservation.getId())
                    .log("Failed to enqueue reservation chat notification email (reservation id={})");
        }
    }

    private long resolveCounterpartyUserId(final long senderUserId, final Reservation reservation) {
        if (senderUserId == reservation.getRiderId()) {
            return userService
                    .getListingOwner(reservation.getListingId())
                    .map(User::getId)
                    .orElseThrow(() -> new ReservationMessageException(MessageKeys.RESERVATION_CHAT_NOT_PARTICIPANT));
        }
        return reservation.getRiderId();
    }

    private String resolveVehicleLabel(final long listingId) {
        final Optional<Listing> listingOpt = listingService.getListingById(listingId);
        if (listingOpt.isEmpty()) {
            return "";
        }
        final Listing listing = listingOpt.get();
        return carService
                .getCarById(listing.getCarId())
                .map(car -> car.getBrand() + " " + car.getModel())
                .orElse("");
    }

    private static String truncateForEmailPreview(final String body) {
        if (body.length() <= EMAIL_PREVIEW_MAX_LENGTH) {
            return body;
        }
        return body.substring(0, EMAIL_PREVIEW_MAX_LENGTH) + "…";
    }
}
