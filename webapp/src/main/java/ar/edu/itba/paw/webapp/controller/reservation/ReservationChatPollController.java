package ar.edu.itba.paw.webapp.controller.reservation;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.dto.reservation.ReservationMessageDto;
import ar.edu.itba.paw.services.reservation.ReservationMessageService;
import ar.edu.itba.paw.webapp.support.CurrentUser;
import ar.edu.itba.paw.webapp.util.WebAuthUtils;

/** JSON polling API for new reservation chat messages. */
@RestController
public final class ReservationChatPollController {

    private final ReservationMessageService reservationMessageService;

    @Autowired
    public ReservationChatPollController(final ReservationMessageService reservationMessageService) {
        this.reservationMessageService = reservationMessageService;
    }

    @GetMapping(
            value = "/my-reservations/{reservationId}/messages/poll",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ReservationMessageDto>> pollMessages(
            @CurrentUser final User currentUser,
            @PathVariable("reservationId") final long reservationId,
            @RequestParam(name = "afterId", defaultValue = "0") final long afterId) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final List<ReservationMessageDto> messages =
                reservationMessageService.pollMessagesForParticipant(me.getId(), reservationId, afterId);
        return ResponseEntity.ok(messages);
    }
}
