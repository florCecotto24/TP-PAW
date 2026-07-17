package ar.edu.itba.paw.webapp.support;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

import ar.edu.itba.paw.models.dto.file.BinaryContent;
import ar.edu.itba.paw.services.reservation.ReservationService;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;

/**
 * Shared HTTP binding for reservation payment / refund receipt download and upload.
 */
@Component
public final class ReservationReceiptHttpSupport {

    private final ReservationService reservationService;
    private final CurrentUserResolver currentUserResolver;
    private final BinaryPayloadSupport binaryPayloadSupport;
    private final ReservationResourceAccess reservationResourceAccess;

    public ReservationReceiptHttpSupport(
            final ReservationService reservationService,
            final CurrentUserResolver currentUserResolver,
            final BinaryPayloadSupport binaryPayloadSupport,
            final ReservationResourceAccess reservationResourceAccess) {
        this.reservationService = reservationService;
        this.currentUserResolver = currentUserResolver;
        this.binaryPayloadSupport = binaryPayloadSupport;
        this.reservationResourceAccess = reservationResourceAccess;
    }

    public Response downloadPayment(final long reservationId) {
        return download(
                reservationId,
                reservationService::findPaymentReceiptContentForAdmin,
                reservationService::findPaymentReceiptContentForParticipant);
    }

    public Response downloadRefund(final long reservationId) {
        return download(
                reservationId,
                reservationService::findRefundReceiptContentForAdmin,
                reservationService::findRefundReceiptContentForParticipant);
    }

    public Response uploadPayment(final long reservationId, final InputStream body, final HttpHeaders httpHeaders)
            throws IOException {
        return upload(reservationId, body, httpHeaders, "payment-receipt", reservationService::attachPaymentReceipt);
    }

    public Response uploadRefund(final long reservationId, final InputStream body, final HttpHeaders httpHeaders)
            throws IOException {
        return upload(
                reservationId, body, httpHeaders, "refund-receipt", reservationService::attachRefundReceiptByOwner);
    }

    private Response download(
            final long reservationId,
            final Function<Long, Optional<BinaryContent>> forAdmin,
            final BiFunction<Long, Long, Optional<BinaryContent>> forParticipant) {
        final Optional<BinaryContent> content;
        if (reservationResourceAccess.isAdmin()) {
            content = forAdmin.apply(reservationId);
        } else {
            final RydenUserDetails viewer = currentUserResolver.requirePrincipal();
            content = forParticipant.apply(viewer.getUserId(), reservationId);
        }
        return content.map(c -> CacheableBinaryResponses.sensitive(c, c.getFileName()))
                .orElseThrow(NotFoundException::new);
    }

    @FunctionalInterface
    private interface ReceiptAttach {
        void attach(long userId, long reservationId, String filename, String contentType, byte[] bytes);
    }

    private Response upload(
            final long reservationId,
            final InputStream body,
            final HttpHeaders httpHeaders,
            final String defaultFilename,
            final ReceiptAttach attach) throws IOException {
        final RydenUserDetails viewer = currentUserResolver.requirePrincipal();
        final byte[] bytes = binaryPayloadSupport.readValidatedBody(body);
        final String contentType = httpHeaders.getMediaType() != null
                ? httpHeaders.getMediaType().toString()
                : MediaType.APPLICATION_OCTET_STREAM;
        attach.attach(viewer.getUserId(), reservationId, defaultFilename, contentType, bytes);
        return Response.noContent().build();
    }
}
