package ar.edu.itba.paw.webapp.controller;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.reservation.ReservationMessageException;
import ar.edu.itba.paw.models.domain.StoredFile;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.reservation.ReservationMessageDto;
import ar.edu.itba.paw.services.ReservationMessageService;
import ar.edu.itba.paw.webapp.support.CurrentUser;
import ar.edu.itba.paw.webapp.util.DownloadFileNameSanitizer;
import ar.edu.itba.paw.webapp.util.WebAuthUtils;

@RestController
public final class ReservationMessageController {

    private static final Logger LOG = LoggerFactory.getLogger(ReservationMessageController.class);

    private final ReservationMessageService reservationMessageService;

    @Autowired
    public ReservationMessageController(final ReservationMessageService reservationMessageService) {
        this.reservationMessageService = reservationMessageService;
    }

    @GetMapping(value = "/my-reservations/{reservationId}/messages", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<ReservationMessageDto>> listMessages(
            @CurrentUser final User currentUser,
            @PathVariable("reservationId") final long reservationId,
            @RequestParam(required = false) final Integer page,
            @RequestParam(required = false) final Integer size) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final Page<ReservationMessageDto> messages =
                reservationMessageService.getMessagesForParticipant(me.getId(), reservationId, page, size);
        return ResponseEntity.ok(messages);
    }

    @PostMapping(
            value = "/my-reservations/{reservationId}/messages",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReservationMessageDto> postMessageWithAttachment(
            @CurrentUser final User currentUser,
            @PathVariable("reservationId") final long reservationId,
            @RequestParam(value = "body", required = false) final String body,
            @RequestParam(value = "file", required = false) final MultipartFile file)
            throws IOException {
        final User me = WebAuthUtils.requireUser(currentUser);
        final ReservationMessageDto dto;
        if (file == null || file.isEmpty()) {
            dto = reservationMessageService.postMessage(me.getId(), reservationId, body);
        } else {
            dto = reservationMessageService.postMessageWithAttachment(
                    me.getId(),
                    reservationId,
                    body,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getBytes());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/my-reservations/{reservationId}/messages/{messageId}/attachment/download")
    public ResponseEntity<byte[]> downloadMessageAttachment(
            @CurrentUser final User currentUser,
            @PathVariable("reservationId") final long reservationId,
            @PathVariable("messageId") final long messageId) {
        final User me = WebAuthUtils.requireUser(currentUser);
        final StoredFile sf = reservationMessageService
                .findMessageAttachmentForParticipant(me.getId(), reservationId, messageId)
                .orElseThrow(() -> new ReservationMessageException(MessageKeys.RESERVATION_CHAT_ATTACHMENT_NOT_FOUND));
        final HttpHeaders headers = new HttpHeaders();
        MediaType contentType = MediaType.APPLICATION_OCTET_STREAM;
        if (sf.getContentType() != null && !sf.getContentType().isBlank()) {
            try {
                contentType = MediaType.parseMediaType(sf.getContentType());
            } catch (final IllegalArgumentException e) {
                LOG.atDebug()
                        .setMessage("Invalid chat attachment Content-Type reservationId={} messageId={} [{}]")
                        .addArgument(reservationId)
                        .addArgument(messageId)
                        .addArgument(sf.getContentType())
                        .setCause(e)
                        .log();
                contentType = MediaType.APPLICATION_OCTET_STREAM;
            }
        }
        headers.setContentType(contentType);
        final String safeName = DownloadFileNameSanitizer.sanitize(sf.getFileName(), "attachment");
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + safeName + "\"");
        return new ResponseEntity<>(sf.getData(), headers, HttpStatus.OK);
    }
}
