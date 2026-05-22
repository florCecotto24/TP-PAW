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
import ar.edu.itba.paw.models.domain.StoredFile;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.ReservationMessageAttachmentDto;
import ar.edu.itba.paw.models.dto.ReservationMessageDto;
import ar.edu.itba.paw.models.email.ReservationChatMessageEmailPayload;
import ar.edu.itba.paw.models.util.ChatAttachmentContentTypes;
import ar.edu.itba.paw.models.util.ChatAttachmentKindResolver;
import ar.edu.itba.paw.persistence.ReservationMessageDao;
import ar.edu.itba.paw.services.mail.MailPublicUrls;
import ar.edu.itba.paw.services.policy.ChatAttachmentUploadPolicy;
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
    private final StoredFileService storedFileService;
    private final ReservationMessageValidationPolicy messageValidationPolicy;
    private final ReservationChatPolicy chatPolicy;
    private final ChatAttachmentUploadPolicy chatAttachmentUploadPolicy;

    @Autowired
    public ReservationMessageServiceImpl(
            final ReservationMessageDao reservationMessageDao,
            final ReservationService reservationService,
            final UserService userService,
            final ListingService listingService,
            final CarService carService,
            final EmailService emailService,
            final MailPublicUrls mailPublicUrls,
            final StoredFileService storedFileService,
            final ReservationMessageValidationPolicy messageValidationPolicy,
            final ReservationChatPolicy chatPolicy,
            final ChatAttachmentUploadPolicy chatAttachmentUploadPolicy) {
        this.reservationMessageDao = reservationMessageDao;
        this.reservationService = reservationService;
        this.userService = userService;
        this.listingService = listingService;
        this.carService = carService;
        this.emailService = emailService;
        this.mailPublicUrls = mailPublicUrls;
        this.storedFileService = storedFileService;
        this.messageValidationPolicy = messageValidationPolicy;
        this.chatPolicy = chatPolicy;
        this.chatAttachmentUploadPolicy = chatAttachmentUploadPolicy;
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
    public int getMaxChatAttachmentMegabytes() {
        return chatAttachmentUploadPolicy.getMaxMegabytesRoundedUp();
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
        validateBodyLength(trimmed);
        final Reservation reservation = requireChatOpenForParticipant(senderUserId, reservationId);
        final ReservationMessage saved = reservationMessageDao.create(reservationId, senderUserId, trimmed);
        final ReservationMessageDto dto = toDto(saved);
        enqueueCounterpartyNotification(senderUserId, reservation, trimmed, null);
        return dto;
    }

    @Override
    @Transactional
    public ReservationMessageDto postMessageWithAttachment(
            final long senderUserId,
            final long reservationId,
            final String body,
            final String fileName,
            final String contentType,
            final byte[] data) {
        final String trimmedBody = body == null ? "" : body.trim();
        if (data == null || data.length == 0) {
            if (trimmedBody.isEmpty()) {
                throw new ReservationMessageException(MessageKeys.RESERVATION_CHAT_ATTACHMENT_REQUIRED);
            }
            return postMessage(senderUserId, reservationId, trimmedBody);
        }
        if (!trimmedBody.isEmpty()) {
            validateBodyLength(trimmedBody);
        }
        validateAttachment(fileName, contentType, data);
        final Reservation reservation = requireChatOpenForParticipant(senderUserId, reservationId);
        final String safeFileName = sanitizeFileName(fileName);
        final StoredFile storedFile =
                storedFileService.create(senderUserId, safeFileName, normalizeContentType(contentType), data);
        final ReservationMessage saved = reservationMessageDao.create(
                reservationId, senderUserId, trimmedBody, storedFile.getId());
        final ReservationMessageDto dto = toDto(saved);
        enqueueCounterpartyNotification(senderUserId, reservation, trimmedBody, safeFileName);
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoredFile> findMessageAttachmentForParticipant(
            final long viewerUserId, final long reservationId, final long messageId) {
        final Reservation reservation = findParticipantReservation(viewerUserId, reservationId)
                .orElseThrow(() -> new ReservationMessageException(MessageKeys.RESERVATION_CHAT_NOT_PARTICIPANT));
        if (!isChatAvailable(reservation)) {
            throw new ReservationMessageException(MessageKeys.RESERVATION_CHAT_NOT_AVAILABLE);
        }
        final Optional<ReservationMessage> messageOpt =
                reservationMessageDao.findByIdAndReservationId(messageId, reservationId);
        if (messageOpt.isEmpty() || messageOpt.get().getAttachment() == null) {
            return Optional.empty();
        }
        return Optional.of(messageOpt.get().getAttachment());
    }

    private void validateBodyLength(final String trimmed) {
        final int maxLength = messageValidationPolicy.getBodyMaxLength();
        if (trimmed.length() > maxLength) {
            throw new ReservationMessageException(MessageKeys.RESERVATION_CHAT_BODY_TOO_LONG, maxLength);
        }
    }

    private void validateAttachment(final String fileName, final String contentType, final byte[] data) {
        if (!ChatAttachmentContentTypes.isAllowed(contentType, fileName)) {
            throw new ReservationMessageException(MessageKeys.RESERVATION_CHAT_ATTACHMENT_INVALID);
        }
        if (data.length > chatAttachmentUploadPolicy.getMaxBytes()) {
            throw new ReservationMessageException(
                    MessageKeys.RESERVATION_CHAT_ATTACHMENT_TOO_LARGE,
                    chatAttachmentUploadPolicy.getMaxMegabytesRoundedUp());
        }
    }

    private Reservation requireChatOpenForParticipant(final long senderUserId, final long reservationId) {
        final Reservation reservation = findParticipantReservation(senderUserId, reservationId)
                .orElseThrow(() -> new ReservationMessageException(MessageKeys.RESERVATION_CHAT_NOT_PARTICIPANT));
        if (!isChatAvailable(reservation)) {
            throw new ReservationMessageException(MessageKeys.RESERVATION_CHAT_NOT_AVAILABLE);
        }
        return reservation;
    }

    private Optional<Reservation> findParticipantReservation(final long viewerUserId, final long reservationId) {
        final Optional<Reservation> asRider = reservationService.getRiderReservationById(viewerUserId, reservationId);
        if (asRider.isPresent()) {
            return asRider;
        }
        return reservationService.getOwnerReservationById(viewerUserId, reservationId);
    }

    private ReservationMessageDto toDto(final ReservationMessage message) {
        final long senderUserId = message.getSenderUserId();
        final User sender = userService
                .getUserById(senderUserId)
                .orElseThrow(() -> new ReservationMessageException(MessageKeys.RESERVATION_CHAT_NOT_PARTICIPANT));
        final ReservationMessageAttachmentDto attachmentDto = toAttachmentDto(message);
        return new ReservationMessageDto(
                message.getId(),
                message.getReservationId(),
                senderUserId,
                formatDisplayName(sender),
                message.getBody(),
                message.getCreatedAt(),
                attachmentDto);
    }

    private ReservationMessageAttachmentDto toAttachmentDto(final ReservationMessage message) {
        final StoredFile attachment = message.getAttachment();
        if (attachment == null) {
            return null;
        }
        final long reservationId = message.getReservationId();
        final long messageId = message.getId();
        final String url = "/my-reservations/" + reservationId + "/messages/" + messageId + "/attachment";
        return new ReservationMessageAttachmentDto(
                attachment.getId(),
                attachment.getFileName(),
                attachment.getContentType(),
                attachment.getData().length,
                ChatAttachmentKindResolver.resolve(attachment.getContentType(), attachment.getFileName()),
                url);
    }

    private static String formatDisplayName(final User user) {
        return user.getForename() + " " + user.getSurname();
    }

    private void enqueueCounterpartyNotification(
            final long senderUserId,
            final Reservation reservation,
            final String messageBody,
            final String attachmentFileName) {
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
                    .messagePreview(truncateForEmailPreview(messageBody, attachmentFileName))
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

    private static String truncateForEmailPreview(final String body, final String attachmentFileName) {
        if (attachmentFileName != null && !attachmentFileName.isBlank()) {
            if (body == null || body.isBlank()) {
                return attachmentFileName;
            }
            final String combined = body + " [" + attachmentFileName + "]";
            if (combined.length() <= EMAIL_PREVIEW_MAX_LENGTH) {
                return combined;
            }
            return combined.substring(0, EMAIL_PREVIEW_MAX_LENGTH) + "…";
        }
        if (body == null || body.isBlank()) {
            return "";
        }
        if (body.length() <= EMAIL_PREVIEW_MAX_LENGTH) {
            return body;
        }
        return body.substring(0, EMAIL_PREVIEW_MAX_LENGTH) + "…";
    }

    private static String sanitizeFileName(final String raw) {
        if (raw == null || raw.isBlank()) {
            return "attachment";
        }
        final String trimmed = raw.trim();
        final int lastSep = Math.max(trimmed.lastIndexOf('/'), trimmed.lastIndexOf('\\'));
        final String base = lastSep >= 0 ? trimmed.substring(lastSep + 1) : trimmed;
        final String cleaned = base.replaceAll("[^a-zA-Z0-9._\\- ]", "_");
        if (cleaned.isBlank()) {
            return "attachment";
        }
        return cleaned.length() > 200 ? cleaned.substring(0, 200) : cleaned;
    }

    private static String normalizeContentType(final String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "application/octet-stream";
        }
        final String t = contentType.trim();
        return t.length() > 100 ? t.substring(0, 100) : t;
    }
}
