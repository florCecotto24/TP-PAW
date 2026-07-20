package ar.edu.itba.paw.services.reservation;

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
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.reservation.ReservationMessageException;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.models.domain.reservation.ReservationMessage;
import ar.edu.itba.paw.models.domain.file.StoredFile;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.file.BinaryContent;
import ar.edu.itba.paw.models.dto.reservation.ReservationMessageAttachmentDto;
import ar.edu.itba.paw.models.dto.reservation.ReservationMessageDto;
import ar.edu.itba.paw.models.dto.reservation.ReservationMessageProjection;
import ar.edu.itba.paw.models.util.format.TextTruncationLimits;
import ar.edu.itba.paw.models.pagination.UiPaging;
import ar.edu.itba.paw.models.email.reservation.chat.ReservationChatDigestConversationEntry;
import ar.edu.itba.paw.models.email.reservation.chat.ReservationChatDigestEmailPayload;
import ar.edu.itba.paw.models.email.reservation.chat.ReservationChatDigestMessageEntry;
import ar.edu.itba.paw.models.util.media.BinaryMagicBytes;
import ar.edu.itba.paw.models.util.media.ChatAttachmentContentTypes;
import ar.edu.itba.paw.models.util.media.ChatAttachmentKindResolver;
import ar.edu.itba.paw.models.util.time.WallDateTimeDisplayFormat;
import ar.edu.itba.paw.persistence.reservation.ReservationMessageDao;
import ar.edu.itba.paw.mail.MailPublicUrls;
import ar.edu.itba.paw.policy.ChatAttachmentUploadPolicy;
import ar.edu.itba.paw.policy.ReservationChatPolicy;
import ar.edu.itba.paw.policy.ReservationMessageValidationPolicy;

import ar.edu.itba.paw.services.email.EmailService;
import ar.edu.itba.paw.services.file.StoredFileService;
import ar.edu.itba.paw.services.user.UserLocaleService;
import ar.edu.itba.paw.services.user.UserService;

@Service
public class ReservationMessageServiceImpl implements ReservationMessageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationMessageServiceImpl.class);
    private static final int EMAIL_PREVIEW_MAX_LENGTH = TextTruncationLimits.RESERVATION_ATTACHMENT_FILENAME;

    private final ReservationMessageDao reservationMessageDao;
    private final ReservationService reservationService;
    private final UserService userService;
    private final UserLocaleService userLocaleService;
    private final EmailService emailService;
    private final MailPublicUrls mailPublicUrls;
    private final StoredFileService storedFileService;
    private final ReservationMessageValidationPolicy messageValidationPolicy;
    private final ReservationChatPolicy chatPolicy;
    private final ChatAttachmentUploadPolicy chatAttachmentUploadPolicy;
    private final ReservationLifecycleRowProcessor lifecycleRowProcessor;

    @Autowired
    public ReservationMessageServiceImpl(
            final ReservationMessageDao reservationMessageDao,
            final ReservationService reservationService,
            final UserService userService,
            final UserLocaleService userLocaleService,
            final EmailService emailService,
            final MailPublicUrls mailPublicUrls,
            final StoredFileService storedFileService,
            final ReservationMessageValidationPolicy messageValidationPolicy,
            final ReservationChatPolicy chatPolicy,
            final ChatAttachmentUploadPolicy chatAttachmentUploadPolicy,
            @Lazy final ReservationLifecycleRowProcessor lifecycleRowProcessor) {
        this.reservationMessageDao = reservationMessageDao;
        this.reservationService = reservationService;
        this.userService = userService;
        this.userLocaleService = userLocaleService;
        this.emailService = emailService;
        this.mailPublicUrls = mailPublicUrls;
        this.storedFileService = storedFileService;
        this.messageValidationPolicy = messageValidationPolicy;
        this.chatPolicy = chatPolicy;
        this.chatAttachmentUploadPolicy = chatAttachmentUploadPolicy;
        this.lifecycleRowProcessor = lifecycleRowProcessor;
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
    public Page<ReservationMessageDto> getMessagesForParticipant(
            final long viewerUserId, final long reservationId, final Integer page, final Integer size) {
        final Reservation reservation = findParticipantReservation(viewerUserId, reservationId)
                .orElseThrow(() -> new ReservationMessageException(MessageKeys.RESERVATION_CHAT_NOT_PARTICIPANT));
        if (!isChatAvailable(reservation)) {
            throw new ReservationMessageException(MessageKeys.RESERVATION_CHAT_NOT_AVAILABLE);
        }
        final int maxPageSize = chatPolicy.getHistoryPageSize();
        final int safeSize = resolveHistoryPageSize(size, maxPageSize);
        final long totalItems = reservationMessageDao.countByReservationId(reservationId);
        final int requestedPage = page == null ? Integer.MAX_VALUE : page;
        final int safePage = UiPaging.clampZeroBasedPage(requestedPage, totalItems, safeSize);
        final int offset = safePage * safeSize;
        final List<ReservationMessageDto> content = reservationMessageDao
                .findProjectedByReservationIdOrderByCreatedAtAsc(reservationId, offset, safeSize)
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
    @Transactional(readOnly = true)
    public List<ReservationMessageDto> pollMessagesForParticipant(
            final long viewerUserId, final long reservationId, final long afterMessageId) {
        final Reservation reservation = findParticipantReservation(viewerUserId, reservationId)
                .orElseThrow(() -> new ReservationMessageException(MessageKeys.RESERVATION_CHAT_NOT_PARTICIPANT));
        if (!isChatAvailable(reservation)) {
            throw new ReservationMessageException(MessageKeys.RESERVATION_CHAT_NOT_AVAILABLE);
        }
        final long safeAfterId = Math.max(0L, afterMessageId);
        final int limit = chatPolicy.getHistoryPageSize();
        return reservationMessageDao
                .findProjectedByReservationIdAfterIdOrderByCreatedAtAsc(reservationId, safeAfterId, limit)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public int markMessagesSeenForParticipant(final long viewerUserId, final long reservationId) {
        final Reservation reservation = findParticipantReservation(viewerUserId, reservationId)
                .orElseThrow(() -> new ReservationMessageException(MessageKeys.RESERVATION_CHAT_NOT_PARTICIPANT));
        if (!isChatAvailable(reservation)) {
            throw new ReservationMessageException(MessageKeys.RESERVATION_CHAT_NOT_AVAILABLE);
        }
        return reservationMessageDao.markSeenByRecipient(reservationId, viewerUserId);
    }

    @Override
    @Transactional
    public ReservationMessageDto postMessage(final long senderUserId, final long reservationId, final String body) {
        final String trimmed = body == null ? "" : body.trim();
        if (trimmed.isEmpty()) {
            throw new ReservationMessageException(MessageKeys.RESERVATION_CHAT_BODY_EMPTY);
        }
        validateBodyLength(trimmed);
        requireChatOpenForParticipant(senderUserId, reservationId);
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
        requireChatOpenForParticipant(senderUserId, reservationId);
        final String safeFileName = sanitizeFileName(fileName);
        final StoredFile storedFile =
                storedFileService.create(senderUserId, safeFileName, normalizeContentType(contentType), data);
        final ReservationMessage saved = reservationMessageDao.create(
                reservationId, senderUserId, trimmedBody, storedFile.getId());
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationMessageDto> getAdminChatMessages(
            final long reservationId, final int offset, final int limit) {
        return reservationMessageDao.findProjectedByReservationIdOrderByCreatedAtAsc(reservationId, offset, limit)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationMessageDto> findMessagesAfter(
            final long reservationId, final long afterMessageId, final int limit) {
        final int safeLimit = limit > 0 ? limit : chatPolicy.getHistoryPageSize();
        return reservationMessageDao
                .findProjectedByReservationIdAfterIdOrderByCreatedAtAsc(
                        reservationId, Math.max(0L, afterMessageId), safeLimit)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long countMessages(final long reservationId) {
        return reservationMessageDao.countByReservationId(reservationId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReservationMessageDto> getMessageForParticipant(
            final long viewerUserId, final long reservationId, final long messageId) {
        final Reservation reservation = findParticipantReservation(viewerUserId, reservationId)
                .orElseThrow(() -> new ReservationMessageException(MessageKeys.RESERVATION_CHAT_NOT_PARTICIPANT));
        if (!isChatAvailable(reservation)) {
            throw new ReservationMessageException(MessageKeys.RESERVATION_CHAT_NOT_AVAILABLE);
        }
        return reservationMessageDao.findProjectedByIdAndReservationId(messageId, reservationId).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReservationMessage> findMessageForAdmin(
            final long reservationId, final long messageId) {
        return reservationMessageDao.findByIdAndReservationId(messageId, reservationId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReservationMessageDto> findMessageDtoForAdmin(
            final long reservationId, final long messageId) {
        return reservationMessageDao.findProjectedByIdAndReservationId(messageId, reservationId).map(this::toDto);
    }

    /**
     * Deliberately NOT {@code @Transactional}: each recipient claim commits in {@code REQUIRES_NEW}
     * before mail is queued, so a failed send cannot roll back the claim (or leave TX rollback-only).
     */
    @Override
    public int dispatchChatDigestEmails() {
        final int digestBatchSize = 200;
        final List<ReservationMessage> pending =
                reservationMessageDao.findPendingEmailNotification(digestBatchSize);
        if (pending.isEmpty()) {
            return 0;
        }
        // findPendingEmailNotification JOIN FETCHes reservation.car.owner + catalog, so
        // counterparty and vehicle labels are read from the already-hydrated graph.
        final Map<Long, List<ReservationMessage>> byRecipient = new LinkedHashMap<>();
        for (final ReservationMessage message : pending) {
            if (message.isSeen()) {
                continue;
            }
            final long recipientId = resolveCounterpartyUserId(
                    message.getSenderUserId(), message.getReservation());
            byRecipient.computeIfAbsent(recipientId, ignored -> new ArrayList<>()).add(message);
        }
        // One WHERE id IN query for every distinct recipient — no per-recipient getUserById.
        final Map<Long, User> recipientsById = userService.getUsersByIds(byRecipient.keySet()).stream()
                .collect(Collectors.toMap(User::getId, recipient -> recipient));
        int queued = 0;
        for (final Map.Entry<Long, List<ReservationMessage>> entry : byRecipient.entrySet()) {
            if (dispatchDigestForRecipient(entry.getKey(), recipientsById.get(entry.getKey()), entry.getValue())) {
                queued++;
            }
        }
        return queued;
    }

    @Override
    @Transactional
    public int markEmailNotified(final java.util.Collection<Long> messageIds) {
        return reservationMessageDao.markEmailNotified(messageIds);
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

    @Override
    @Transactional(readOnly = true)
    public Optional<BinaryContent> findMessageAttachmentContentForParticipant(
            final long viewerUserId, final long reservationId, final long messageId) {
        // Map to a detached value object inside this transaction so the controller doesn't
        // depend on the JPA entity (issue #16).
        return findMessageAttachmentForParticipant(viewerUserId, reservationId, messageId)
                .map(sf -> new BinaryContent(sf.getData(), sf.getContentType(), sf.getFileName()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BinaryContent> findMessageAttachmentContentForAdmin(
            final long reservationId, final long messageId) {
        return findMessageForAdmin(reservationId, messageId)
                .map(ReservationMessage::getAttachment)
                .filter(attachment -> attachment != null)
                .map(sf -> new BinaryContent(sf.getData(), sf.getContentType(), sf.getFileName()));
    }

    /** {@code recipient} comes from the batch lookup; {@code null} when the account no longer exists. */
    /** Returns {@code true} when a digest email was queued for the recipient. */
    private boolean dispatchDigestForRecipient(
            final long recipientId,
            final User recipient,
            final List<ReservationMessage> messages) {
        try {
            final List<ReservationMessage> unseenMessages = messages.stream()
                    .filter(message -> !message.isSeen())
                    .collect(Collectors.toList());
            if (unseenMessages.isEmpty()) {
                return false;
            }
            final List<Long> messageIds =
                    unseenMessages.stream().map(ReservationMessage::getId).collect(Collectors.toList());
            if (!lifecycleRowProcessor.markChatDigestNotified(messageIds)) {
                return false;
            }
            if (recipient == null) {
                return false;
            }
            // resolveMailLocaleFor reuses the already-loaded user instead of re-querying users
            // by id, which previously cost one extra SELECT per recipient in the digest loop.
            final Locale mailLocale = userLocaleService.resolveMailLocaleFor(recipient);
            final Optional<ReservationChatDigestEmailPayload> payloadOpt =
                    buildDigestPayload(recipient, mailLocale, unseenMessages);
            if (payloadOpt.isEmpty()) {
                return false;
            }
            emailService.sendReservationChatDigestEmail(payloadOpt.get());
            return true;
        } catch (final RuntimeException e) {
            LOGGER.atWarn()
                    .setCause(e)
                    .addArgument(recipientId)
                    .log("Failed to dispatch reservation chat digest email (recipient id={})");
            return false;
        }
    }

    private Optional<ReservationChatDigestEmailPayload> buildDigestPayload(
            final User recipient,
            final Locale mailLocale,
            final List<ReservationMessage> messages) {
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
            final String vehicleLabel = resolveVehicleLabel(reservation);
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
        // Avoid navigating m.attachment (would load StoredFile LOB). For body-less messages, a
        // generic placeholder is enough for the digest preview.
        final String body = message.getBody();
        final String attachmentFileName =
                (body == null || body.isBlank()) && message.getAttachmentFileId() != null
                        ? "attachment"
                        : null;
        return new ReservationChatDigestMessageEntry(
                formatDisplayName(sender),
                truncateForEmailPreview(body, attachmentFileName),
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
        // Raster images + PDF: require magic-bytes match (same policy as KYC / receipts).
        // Office/zip/video remain MIME+extension only (no sniffer in BinaryMagicBytes).
        if (requiresMagicBytes(contentType) && !BinaryMagicBytes.matchesDeclared(contentType, data)) {
            throw new ReservationMessageException(MessageKeys.RESERVATION_CHAT_ATTACHMENT_INVALID);
        }
    }

    private static boolean requiresMagicBytes(final String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return false;
        }
        final String t = contentType.trim().toLowerCase(java.util.Locale.ROOT);
        final int semi = t.indexOf(';');
        final String normalized = semi < 0 ? t : t.substring(0, semi).trim();
        return normalized.startsWith("image/") || "application/pdf".equals(normalized);
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
        final User sender = message.getSender();
        if (sender == null) {
            throw new ReservationMessageException(MessageKeys.RESERVATION_CHAT_NOT_PARTICIPANT);
        }
        final ReservationMessageAttachmentDto attachmentDto = toAttachmentDto(message);
        return new ReservationMessageDto(
                message.getId(),
                message.getReservationId(),
                sender.getId(),
                sender.getForename(),
                sender.getSurname(),
                message.getBody(),
                message.getCreatedAt(),
                attachmentDto,
                message.isSeen());
    }

    private ReservationMessageDto toDto(final ReservationMessageProjection message) {
        final ReservationMessageAttachmentDto attachmentDto = toAttachmentDto(message);
        return new ReservationMessageDto(
                message.getId(),
                message.getReservationId(),
                message.getSenderUserId(),
                message.getSenderForename(),
                message.getSenderSurname(),
                message.getBody(),
                message.getCreatedAt(),
                attachmentDto,
                message.isSeen());
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
                attachment.getSizeBytes(),
                ChatAttachmentKindResolver.resolve(attachment.getContentType(), attachment.getFileName()),
                url);
    }

    private ReservationMessageAttachmentDto toAttachmentDto(final ReservationMessageProjection message) {
        if (message.getAttachmentFileId() == null) {
            return null;
        }
        final long reservationId = message.getReservationId();
        final long messageId = message.getId();
        final String url =
                "/my-reservations/" + reservationId + "/messages/" + messageId + "/attachment/download";
        return new ReservationMessageAttachmentDto(
                message.getAttachmentFileId(),
                message.getAttachmentFileName(),
                message.getAttachmentContentType(),
                message.getAttachmentSizeBytes(),
                ChatAttachmentKindResolver.resolve(
                        message.getAttachmentContentType(), message.getAttachmentFileName()),
                url);
    }

    private static String formatDisplayName(final User user) {
        return formatDisplayName(user.getForename(), user.getSurname());
    }

    private static String formatDisplayName(final String forename, final String surname) {
        return forename + " " + surname;
    }

    private long resolveCounterpartyUserId(final long senderUserId, final Reservation reservation) {
        if (senderUserId == reservation.getRiderId()) {
            return reservation.getCar().getOwner().getId();
        }
        return reservation.getRiderId();
    }

    private String resolveVehicleLabel(final Reservation reservation) {
        final Car car = reservation.getCar();
        if (car == null) {
            return "";
        }
        return car.getBrand() + " " + car.getModel();
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
        return cleaned.length() > TextTruncationLimits.RESERVATION_ATTACHMENT_FILENAME
                ? cleaned.substring(0, TextTruncationLimits.RESERVATION_ATTACHMENT_FILENAME)
                : cleaned;
    }

    private static String normalizeContentType(final String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "application/octet-stream";
        }
        final String t = contentType.trim();
        return t.length() > TextTruncationLimits.CONTENT_TYPE_HEADER_MAX
                ? t.substring(0, TextTruncationLimits.CONTENT_TYPE_HEADER_MAX)
                : t;
    }
}
