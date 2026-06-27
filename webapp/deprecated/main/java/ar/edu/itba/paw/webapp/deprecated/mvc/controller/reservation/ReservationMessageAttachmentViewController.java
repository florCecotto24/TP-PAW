package ar.edu.itba.paw.webapp.deprecated.mvc.controller.reservation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.reservation.ReservationMessageException;
import ar.edu.itba.paw.models.domain.file.StoredFile;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.services.reservation.ReservationMessageService;
import ar.edu.itba.paw.webapp.deprecated.mvc.support.CurrentUser;
import ar.edu.itba.paw.webapp.util.WebAuthUtils;

/** HTML shell for opening chat attachments in a new tab (Ryden favicon, blob iframe). */
@Controller
public final class ReservationMessageAttachmentViewController {

    private final ReservationMessageService reservationMessageService;

    @Autowired
    public ReservationMessageAttachmentViewController(final ReservationMessageService reservationMessageService) {
        this.reservationMessageService = reservationMessageService;
    }

    @GetMapping("/my-reservations/{reservationId}/messages/{messageId}/attachment/view")
    public ModelAndView viewMessageAttachment(
            @CurrentUser final User currentUser,
            @PathVariable("reservationId") final long reservationId,
            @PathVariable("messageId") final long messageId) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final StoredFile sf = reservationMessageService
                .findMessageAttachmentForParticipant(me.getId(), reservationId, messageId)
                .orElseThrow(() -> new ReservationMessageException(MessageKeys.RESERVATION_CHAT_ATTACHMENT_NOT_FOUND));
        final ModelAndView view = new ModelAndView("reservation/chat-attachment-view");
        view.addObject("reservationId", reservationId);
        view.addObject("messageId", messageId);
        view.addObject("attachmentFileName", sf.getFileName());
        return view;
    }
}
