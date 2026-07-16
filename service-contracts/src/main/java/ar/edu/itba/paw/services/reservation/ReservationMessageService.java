package ar.edu.itba.paw.services.reservation;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.models.domain.reservation.ReservationMessage;
import ar.edu.itba.paw.models.domain.file.StoredFile;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.file.BinaryContent;
import ar.edu.itba.paw.models.dto.reservation.ReservationMessageDto;

public interface ReservationMessageService {

    boolean isChatAvailable(Reservation reservation);

    int getMessageBodyMaxLength();

    int getHistoryPageSize();

    int getMaxChatAttachmentMegabytes();

    Page<ReservationMessageDto> getMessagesForParticipant(
            long viewerUserId, long reservationId, Integer page, Integer size);

    List<ReservationMessageDto> pollMessagesForParticipant(
            long viewerUserId, long reservationId, long afterMessageId);

    ReservationMessageDto postMessage(long senderUserId, long reservationId, String body);

    ReservationMessageDto postMessageWithAttachment(
            long senderUserId,
            long reservationId,
            String body,
            String fileName,
            String contentType,
            byte[] data);

    Optional<StoredFile> findMessageAttachmentForParticipant(
            long viewerUserId, long reservationId, long messageId);

    /**
     * Same participant scoping as {@link #findMessageAttachmentForParticipant} but returns a
     * detached {@link BinaryContent} value object so download endpoints don't leak the JPA
     * entity (issue #16).
     */
    Optional<BinaryContent> findMessageAttachmentContentForParticipant(
            long viewerUserId, long reservationId, long messageId);

    /**
     * Admin download of a chat attachment without participant scoping (caller enforces admin ACL).
     */
    Optional<BinaryContent> findMessageAttachmentContentForAdmin(long reservationId, long messageId);

    boolean canParticipantAccessReservationChat(long viewerUserId, long reservationId);

    void dispatchChatDigestEmails();

    /**
     * Atomically marks unseen messages as email-notified. Returns how many rows were updated.
     * Used by the digest job after grouping by recipient.
     */
    int markEmailNotified(Collection<Long> messageIds);

    // -----------------------------------------------------------------------------------------------------------
    // Admin-orchestrated read of chat messages.
    //
    // Exists so that {@link AdminService} can audit reservation chats without bypassing the layering rule
    // "each service may only call its own DAO". Unlike {@link #getMessagesForParticipant}, no participant
    // check is performed: the caller (admin facade) decides who is allowed to read.
    // -----------------------------------------------------------------------------------------------------------

    /** Admin-only: raw page of chat messages for a reservation, no participant check applied. */
    List<ReservationMessage> getAdminChatMessages(long reservationId, int offset, int limit);

    /** Admin-only: messages with id greater than {@code afterMessageId}, ascending, capped at {@code limit}. */
    List<ReservationMessage> findMessagesAfter(long reservationId, long afterMessageId, int limit);

    /** Total number of chat messages for a reservation. */
    long countMessages(long reservationId);

    /** Participant-scoped message fetch for {@code GET .../messages/{messageId}}. */
    Optional<ReservationMessageDto> getMessageForParticipant(
            long viewerUserId, long reservationId, long messageId);

    /** Admin-scoped message fetch (no participant gate). */
    Optional<ReservationMessage> findMessageForAdmin(long reservationId, long messageId);
}
