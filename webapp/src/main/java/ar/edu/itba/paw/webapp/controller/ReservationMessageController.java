package ar.edu.itba.paw.webapp.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.ReservationMessageDto;
import ar.edu.itba.paw.services.ReservationMessageService;
import ar.edu.itba.paw.webapp.support.CurrentUser;
import ar.edu.itba.paw.webapp.util.WebAuthUtils;

@RestController
public final class ReservationMessageController {

    private final ReservationMessageService reservationMessageService;

    @Autowired
    public ReservationMessageController(final ReservationMessageService reservationMessageService) {
        this.reservationMessageService = reservationMessageService;
    }

    @GetMapping(value = "/my-reservations/{reservationId}/messages", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ReservationMessageDto>> listMessages(
            @CurrentUser final User currentUser,
            @PathVariable("reservationId") final long reservationId,
            @RequestParam(defaultValue = "0") final int page) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final List<ReservationMessageDto> messages =
                reservationMessageService.getMessagesForParticipant(me.getId(), reservationId, page);
        return ResponseEntity.ok(messages);
    }
}
