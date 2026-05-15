package ar.edu.itba.paw.services;

import java.util.List;

import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.dto.ReservationMessageDto;

public interface ReservationMessageService {

    boolean isChatAvailable(Reservation reservation);

    int getMessageBodyMaxLength();

    int getHistoryPageSize();

    List<ReservationMessageDto> getMessagesForParticipant(long viewerUserId, long reservationId, int page);

    ReservationMessageDto postMessage(long senderUserId, long reservationId, String body);

    boolean canParticipantAccessReservationChat(long viewerUserId, long reservationId);
}
