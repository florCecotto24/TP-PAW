package ar.edu.itba.paw.services;

import java.util.List;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.domain.ReservationMessage;
import ar.edu.itba.paw.models.domain.StoredFile;
import ar.edu.itba.paw.models.dto.reservation.ReservationMessageDto;

public interface ReservationMessageService {

    boolean isChatAvailable(Reservation reservation);

    int getMessageBodyMaxLength();

    int getHistoryPageSize();

    int getMaxChatAttachmentMegabytes();

    List<ReservationMessageDto> getMessagesForParticipant(long viewerUserId, long reservationId, int page);

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

    boolean canParticipantAccessReservationChat(long viewerUserId, long reservationId);

    void dispatchChatDigestEmails();

    // -----------------------------------------------------------------------------------------------------------
    // Admin-orchestrated read of chat messages.
    //
    // Exists so that {@link AdminService} can audit reservation chats without bypassing the layering rule
    // "each service may only call its own DAO". Unlike {@link #getMessagesForParticipant}, no participant
    // check is performed: the caller (admin facade) decides who is allowed to read.
    // -----------------------------------------------------------------------------------------------------------

    /** Admin-only: raw page of chat messages for a reservation, no participant check applied. */
    List<ReservationMessage> getAdminChatMessages(long reservationId, int offset, int limit);

    /** Total number of chat messages for a reservation. */
    long countMessages(long reservationId);
}
