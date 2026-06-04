package ar.edu.itba.paw.services;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.reservation.ReservationMessageException;
import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.domain.ReservationMessage;
import ar.edu.itba.paw.models.domain.StoredFile;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.reservation.ReservationMessageAttachmentDto;
import ar.edu.itba.paw.models.dto.reservation.ReservationMessageDto;
import ar.edu.itba.paw.models.pagination.UiPaging;
import ar.edu.itba.paw.models.email.ReservationChatDigestConversationEntry;
import ar.edu.itba.paw.models.email.ReservationChatDigestEmailPayload;
import ar.edu.itba.paw.models.email.ReservationChatDigestMessageEntry;
import ar.edu.itba.paw.models.util.media.ChatAttachmentContentTypes;
import ar.edu.itba.paw.models.util.media.ChatAttachmentKindResolver;
import ar.edu.itba.paw.models.util.time.WallDateTimeDisplayFormat;
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
    @Transactional
    public Page<ReservationMessageDto> getMessagesForParticipant(
            final long viewerUserId, final long reservationId, final Integer page, final Integer size) {
        final Reservation reservation = findParticipantReservation(viewerUserId, reservationId)
                .orElseThrow(() -> new ReservationMessageException(MessageKeys.RESERVATION_CHAT_NOT_PARTICIPANT));
        if (!isChatAvailable(reservation)) {
            throw new ReservationMessageException(MessageKeys.RESERVATION_CHAT_NOT_AVAILABLE);
        }
        reservationMessageDao.markSeenByRecipient(reservationId, viewerUserId);
        final int maxPageSize = chatPolicy.getHistoryPageSize();
        final int safeSize = resolveHistoryPageSize(size, maxPageSize);
        final long totalItems = reservationMessageDao.countByReservationId(reservationId);
        final int requestedPage = page == null ? Integer.MAX_VALUE : page;
        final int safePage = UiPaging.clampZeroBasedPage(requestedPage, totalItems, safeSize);
        final int offset = safePage * safeSize;
        final List<ReservationMessageDto> content = reservationMessageDao
                .findByReservationIdOrderByCreatedAtAsc(reservationId, offset, safeSize)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return new Page<>(content, safePage, safeSize, totalItems);
    }

    private static int resolveHistoryPageSize(final Integer size, final int maxPageSize) {
        if (size == null || size < 1) {
            return maxPageSize;
        }
        return Math.min(size, maxPageSize);
    }

    @Override
    @Transactional
    public List<ReservationMessageDto> pollMessagesForParticipant(
            final long viewerUserId, final long reservationId, final long afterMessageId) {
        final Reservation reservation = findParticipantReservation(viewerUserId, reservationId)
                .orElseThrow(() -> new ReservationMessageException(MessageKeys.RESERVATION_CHAT_NOT_PARTICIPANT));
        if (!isChatAvailable(reservation)) {
            throw new ReservationMessageException(MessageKeys.RESERVATION_CHAT_NOT_AVAILABLE);
        }
        reservationMessageDao.markSeenByRecipient(reservationId, viewerUserId);
        final long safeAfterId = Math.max(0L, afterMessageId);
        final int limit = chatPolicy.getHistoryPageSize();
        return reservationMessageDao
                .findByReservationIdAfterIdOrderByCreatedAtAsc(reservationId, safeAfterId, limit)
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
        return toDto(saved);
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
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationMessage> getAdminChatMessages(final long reservationId, final int offset, final int limit) {
        return reservationMessageDao.findByReservationIdOrderByCreatedAtAsc(reservationId, offset, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public long countMessages(final long reservationId) {
        return reservationMessageDao.countByReservationId(reservationId);
    }

    @Override
    @Transactional
    public void dispatchChatDigestEmails() {
        final List<ReservationMessage> pending = reservationMessageDao.findPendingEmailNotification();
        if (pending.isEmpty()) {
            return;
        }
        final Map<Long, List<ReservationMessage>> byRecipient = new LinkedHashMap<>();
        for (final ReservationMessage message : pending) {
            if (message.isSeen()) {
                continue;
            }
            final long recipientId = resolveCounterpartyUserId(message.getSenderUserId(), message.getReservation());
            byRecipient.computeIfAbsent(recipientId, ignored -> new ArrayList<>()).add(message);
        }
        for (final Map.Entry<Long, List<ReservationMessage>> entry : byRecipient.entrySet()) {
            dispatchDigestForRecipient(entry.getKey(), entry.getValue());
        }
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

    private void dispatchDigestForRecipient(final long recipientId, final List<ReservationMessage> messages) {
        try {
            final List<ReservationMessage> unseenMessages = messages.stream()
                    .filter(message -> !message.isSeen())
                    .collect(Collectors.toList());
            if (unseenMessages.isEmpty()) {
                return;
            }
            final List<Long> messageIds =
                    unseenMessages.stream().map(ReservationMessage::getId).collect(Collectors.toList());
            if (reservationMessageDao.markEmailNotified(messageIds) == 0) {
                return;
            }
            final Optional<User> recipientOpt = userService.getUserById(recipientId);
            if (recipientOpt.isEmpty()) {
                return;
            }
            final User recipient = recipientOpt.get();
            final Locale mailLocale = userService.resolveMailLocale(recipientId);
            final Optional<ReservationChatDigestEmailPayload> payloadOpt =
                    buildDigestPayload(recipient, mailLocale, unseenMessages);
            if (payloadOpt.isEmpty()) {
                return;
            }
            emailService.sendReservationChatDigestEmail(payloadOpt.get());
        } catch (final RuntimeException e) {
            LOGGER.atWarn()
                    .setCause(e)
                    .addArgument(recipientId)
                    .log("Failed to dispatch reservation chat digest email (recipient id={})");
        }
    }

    private Optional<ReservationChatDigestEmailPayload> buildDigestPayload(
            final User recipient, final Locale mailLocale, final List<ReservationMessage> messages) {
        final Map<Long, List<ReservationMessage>> byReservation = new LinkedHashMap<>();
        for (final ReservationMessage message : messages) {
            byReservation
                    .computeIfAbsent(message.getReservationId(), ignored -> new ArrayList<>())
                    .add(message);
        }
        final List<ReservationChatDigestConversationEntry> conversations = new ArrayList<>();
        for (final Map.Entry<Long, List<ReservationMessage>> reservationEntry : byReservation.entrySet()) {
            final List<ReservationMessage> reservationMessages = reservationEntry.getValue();
            if (reservationMessages.isEmpty()) {
                continue;
            }
            final Reservation reservation = reservationMessages.get(0).getReservation();
            final long reservationId = reservation.getId();
            final String vehicleLabel = resolveVehicleLabel(reservation.getCarId());
            final boolean recipientIsOwner = recipient.getId() != reservation.getRiderId();
            final String roleParam = recipientIsOwner ? "owner" : "rider";
            final String detailPath = "/my-reservations/" + reservationId + "/chat?role=" + roleParam;
            final List<ReservationChatDigestMessageEntry> messageEntries = reservationMessages.stream()
                    .map(message -> toDigestMessageEntry(message, mailLocale))
                    .collect(Collectors.toList());
            conversations.add(new ReservationChatDigestConversationEntry(
                    vehicleLabel,
                    reservationId,
                    mailPublicUrls.absolutePath(detailPath),
                    messageEntries));
        }
        if (conversations.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(ReservationChatDigestEmailPayload.builder()
                .messageLocale(mailLocale)
                .recipientEmail(recipient.getEmail())
                .recipientFullName(formatDisplayName(recipient))
                .totalMessageCount(messages.size())
                .conversations(conversations)
                .build());
    }

    private ReservationChatDigestMessageEntry toDigestMessageEntry(
            final ReservationMessage message, final Locale mailLocale) {
        final User sender = message.getSender();
        final String attachmentFileName =
                message.getAttachment() == null ? null : message.getAttachment().getFileName();
        return new ReservationChatDigestMessageEntry(
                formatDisplayName(sender),
                truncateForEmailPreview(message.getBody(), attachmentFileName),
                WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(message.getCreatedAt(), mailLocale));
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
        final String url =
                "/my-reservations/" + reservationId + "/messages/" + messageId + "/attachment/download";
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

    private long resolveCounterpartyUserId(final long senderUserId, final Reservation reservation) {
        if (senderUserId == reservation.getRiderId()) {
            return carService
                    .getCarById(reservation.getCarId())
                    .map(Car::getOwner)
                    .map(User::getId)
                    .orElseThrow(() -> new ReservationMessageException(MessageKeys.RESERVATION_CHAT_NOT_PARTICIPANT));
        }
        return reservation.getRiderId();
    }

    private String resolveVehicleLabel(final long carId) {
        return carService
                .getCarById(carId)
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
