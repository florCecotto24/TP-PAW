package ar.edu.itba.paw.webapp.controller.reservation;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.models.domain.reservation.ReservationMessage;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.reservation.ReservationMessageDto;
import ar.edu.itba.paw.services.reservation.ReservationMessageService;
import ar.edu.itba.paw.services.user.AdminService;
import ar.edu.itba.paw.webapp.api.common.PaginationLinks;
import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.config.properties.AppPaginationProperties;
import ar.edu.itba.paw.webapp.dto.rest.MessageDto;
import ar.edu.itba.paw.webapp.form.reservation.ReservationMessageCreateForm;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;
import ar.edu.itba.paw.webapp.support.CurrentUserResolver;
import ar.edu.itba.paw.webapp.support.FormValidationSupport;
import ar.edu.itba.paw.webapp.support.ReservationResourceAccess;
import ar.edu.itba.paw.webapp.util.RestUriUtils;

/**
 * Reservation chat messages ({@code /reservations/{id}/messages}).
 */
@Path("/reservations/{id}/messages")
@Component
public final class ReservationMessageController {

    private final ReservationMessageService reservationMessageService;
    private final AdminService adminService;
    private final CurrentUserResolver currentUserResolver;
    private final ReservationResourceAccess reservationResourceAccess;
    private final AppPaginationProperties paginationProperties;
    private final FormValidationSupport formValidationSupport;

    @Context
    private UriInfo uriInfo;

    @Autowired
    public ReservationMessageController(
            final ReservationMessageService reservationMessageService,
            final AdminService adminService,
            final CurrentUserResolver currentUserResolver,
            final ReservationResourceAccess reservationResourceAccess,
            final AppPaginationProperties paginationProperties,
            final FormValidationSupport formValidationSupport) {
        this.reservationMessageService = reservationMessageService;
        this.adminService = adminService;
        this.currentUserResolver = currentUserResolver;
        this.reservationResourceAccess = reservationResourceAccess;
        this.paginationProperties = paginationProperties;
        this.formValidationSupport = formValidationSupport;
    }

    @GET
    @Produces(VndMediaType.MESSAGE_V1_JSON)
    public Response listMessages(
            @PathParam("id") final long reservationId,
            @QueryParam("page") @DefaultValue("1") final int page,
            @QueryParam("pageSize") final Integer pageSizeParam,
            @QueryParam("afterId") final Long afterId) {
        final RydenUserDetails viewer = currentUserResolver.currentPrincipalOrNull();
        reservationResourceAccess.requireViewReservation(reservationId, viewer);

        if (afterId != null) {
            return listMessagesAfter(reservationId, viewer, afterId);
        }

        final int safePage = Math.max(1, page);
        final int pageSize = pageSizeParam != null && pageSizeParam > 0
                ? pageSizeParam
                : paginationProperties.getAdminReservationChatPageSize();

        if (reservationResourceAccess.isAdmin()) {
            return listMessagesAdmin(reservationId, safePage, pageSize);
        }
        return listMessagesParticipant(reservationId, viewer, safePage, pageSize);
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(VndMediaType.MESSAGE_V1_JSON)
    public Response postMessage(
            @PathParam("id") final long reservationId,
            @FormDataParam("body") final String body,
            @FormDataParam("file") final FormDataBodyPart filePart) throws IOException {
        final RydenUserDetails viewer = currentUserResolver.requirePrincipal();
        reservationResourceAccess.requireViewReservation(reservationId, viewer);
        final byte[] fileBytes = readFileBytes(filePart);
        formValidationSupport.validate(new ReservationMessageCreateForm(body, fileBytes != null));
        final String fileName = filePart != null && filePart.getContentDisposition() != null
                ? filePart.getContentDisposition().getFileName()
                : null;
        final String contentType = filePart != null && filePart.getMediaType() != null
                ? filePart.getMediaType().toString()
                : null;
        final ReservationMessageDto saved = reservationMessageService.postMessageWithAttachment(
                viewer.getUserId(), reservationId, body, fileName, contentType, fileBytes);
        final MessageDto dto = MessageDto.fromDto(saved, uriInfo);
        return Response.created(
                        RestUriUtils.reservationMessageUri(uriInfo, reservationId, saved.getId()))
                .entity(dto)
                .build();
    }

    private Response listMessagesAdmin(final long reservationId, final int safePage, final int pageSize) {
        final int zeroBasedPage = safePage - 1;
        final int offset = zeroBasedPage * pageSize;
        final List<ReservationMessage> messages =
                adminService.getAdminChatMessages(reservationId, offset, pageSize);
        final long total = adminService.countReservationMessages(reservationId);
        if (total == 0L) {
            return Response.noContent().build();
        }
        final List<MessageDto> dtos = messages.stream()
                .map(message -> MessageDto.from(message, uriInfo))
                .collect(Collectors.toList());
        final Response.ResponseBuilder builder = Response.ok(new GenericEntity<List<MessageDto>>(dtos) {})
                .header("X-Total-Count", total);
        PaginationLinks.add(builder, uriInfo, safePage, pageSize, (int) total);
        return builder.build();
    }

    private Response listMessagesParticipant(
            final long reservationId,
            final RydenUserDetails viewer,
            final int safePage,
            final int pageSize) {
        final Page<ReservationMessageDto> page = reservationMessageService.getMessagesForParticipant(
                viewer.getUserId(), reservationId, safePage - 1, pageSize);
        if (page.getTotalItems() == 0L) {
            return Response.noContent().build();
        }
        final List<MessageDto> dtos = page.getContent().stream()
                .map(dto -> MessageDto.fromDto(dto, uriInfo))
                .collect(Collectors.toList());
        final Response.ResponseBuilder builder = Response.ok(new GenericEntity<List<MessageDto>>(dtos) {})
                .header("X-Total-Count", page.getTotalItems());
        PaginationLinks.add(builder, uriInfo, safePage, pageSize, (int) page.getTotalItems());
        return builder.build();
    }

    private Response listMessagesAfter(
            final long reservationId,
            final RydenUserDetails viewer,
            final long afterId) {
        final int limit = paginationProperties.getAdminReservationChatPageSize();
        if (reservationResourceAccess.isAdmin()) {
            final List<ReservationMessage> messages =
                    adminService.getChatMessagesAfter(reservationId, afterId, limit);
            if (messages.isEmpty()) {
                return Response.noContent().build();
            }
            final List<MessageDto> dtos = messages.stream()
                    .map(message -> MessageDto.from(message, uriInfo))
                    .collect(Collectors.toList());
            return Response.ok(new GenericEntity<List<MessageDto>>(dtos) {}).build();
        }
        final List<ReservationMessageDto> messages = reservationMessageService.pollMessagesForParticipant(
                viewer.getUserId(), reservationId, afterId);
        if (messages.isEmpty()) {
            return Response.noContent().build();
        }
        final List<MessageDto> dtos = messages.stream()
                .map(dto -> MessageDto.fromDto(dto, uriInfo))
                .collect(Collectors.toList());
        return Response.ok(new GenericEntity<List<MessageDto>>(dtos) {}).build();
    }

    private static byte[] readFileBytes(final FormDataBodyPart filePart) throws IOException {
        if (filePart == null) {
            return null;
        }
        final InputStream stream = filePart.getValueAs(InputStream.class);
        if (stream == null) {
            return null;
        }
        final byte[] bytes = stream.readAllBytes();
        return bytes.length == 0 ? null : bytes;
    }
}
