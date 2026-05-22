package ar.edu.itba.paw.services;

import java.util.List;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.domain.StoredFile;
import ar.edu.itba.paw.models.dto.ReservationMessageDto;

public interface ReservationMessageService {

    boolean isChatAvailable(Reservation reservation);

    int getMessageBodyMaxLength();

    int getHistoryPageSize();

    int getMaxChatAttachmentMegabytes();

    List<ReservationMessageDto> getMessagesForParticipant(long viewerUserId, long reservationId, int page);

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
}
