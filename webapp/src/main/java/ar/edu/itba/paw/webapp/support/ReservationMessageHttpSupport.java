package ar.edu.itba.paw.webapp.support;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.springframework.stereotype.Component;

import java.util.Optional;

import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.file.BinaryContent;
import ar.edu.itba.paw.models.dto.reservation.ReservationMessageDto;
import ar.edu.itba.paw.services.reservation.ReservationMessageService;
import ar.edu.itba.paw.services.user.AdminService;
import ar.edu.itba.paw.webapp.api.common.PaginationLinks;
import ar.edu.itba.paw.webapp.dto.rest.MessageDto;
import ar.edu.itba.paw.webapp.form.reservation.ReservationMessageCreateForm;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;
import ar.edu.itba.paw.webapp.util.RestUriUtils;

/**
 * HTTP orchestration for reservation chat (list/poll admin vs participant, multipart post,
 * attachment download, read receipts).
 */
@Component
public final class ReservationMessageHttpSupport {

    private final ReservationMessageService reservationMessageService;
    private final AdminService adminService;
    private final ReservationResourceAccess reservationResourceAccess;
    private final CurrentUserResolver currentUserResolver;
    private final PaginationSupport paginationSupport;
    private final FormValidationSupport formValidationSupport;
    private final BinaryPayloadSupport binaryPayloadSupport;

    public ReservationMessageHttpSupport(
            final ReservationMessageService reservationMessageService,
            final AdminService adminService,
            final ReservationResourceAccess reservationResourceAccess,
            final CurrentUserResolver currentUserResolver,
            final PaginationSupport paginationSupport,
            final FormValidationSupport formValidationSupport,
            final BinaryPayloadSupport binaryPayloadSupport) {
        this.reservationMessageService = reservationMessageService;
        this.adminService = adminService;
        this.reservationResourceAccess = reservationResourceAccess;
        this.currentUserResolver = currentUserResolver;
        this.paginationSupport = paginationSupport;
        this.formValidationSupport = formValidationSupport;
        this.binaryPayloadSupport = binaryPayloadSupport;
    }

    public Response list(
            final long reservationId,
            final PaginationParams paging,
            final Long afterId,
            final RydenUserDetails viewer,
            final UriInfo uriInfo) {
        if (afterId != null) {
            return listAfter(reservationId, viewer, afterId, uriInfo);
        }
        if (reservationResourceAccess.isAdmin()) {
            return listAdmin(reservationId, paging, uriInfo);
        }
        return listParticipant(reservationId, viewer, paging, uriInfo);
    }

    public Response get(
            final long reservationId,
            final long messageId,
            final RydenUserDetails viewer,
            final UriInfo uriInfo) {
        if (reservationResourceAccess.isAdmin()) {
            return reservationMessageService.findMessageDtoForAdmin(reservationId, messageId)
                    .map(message -> Response.ok(MessageDto.fromDto(message, uriInfo)).build())
                    .orElseThrow(NotFoundException::new);
        }
        return reservationMessageService.getMessageForParticipant(viewer.getUserId(), reservationId, messageId)
                .map(dto -> Response.ok(MessageDto.fromDto(dto, uriInfo)).build())
                .orElseThrow(NotFoundException::new);
    }

    public Response post(
            final long reservationId,
            final String body,
            final FormDataBodyPart filePart,
            final RydenUserDetails viewer,
            final UriInfo uriInfo) throws IOException {
        final byte[] fileBytes = readFileBytes(filePart);
        formValidationSupport.validate(ReservationMessageCreateForm.of(body, fileBytes != null));
        final String fileName = filePart != null && filePart.getContentDisposition() != null
                ? filePart.getContentDisposition().getFileName()
                : null;
        final String contentType = filePart != null && filePart.getMediaType() != null
                ? filePart.getMediaType().toString()
                : null;
        final ReservationMessageDto saved = reservationMessageService.postMessageWithAttachment(
                viewer.getUserId(), reservationId, body, fileName, contentType, fileBytes);
        return Response.created(
                        RestUriUtils.reservationMessageUri(uriInfo, reservationId, saved.getId()))
                .entity(MessageDto.fromDto(saved, uriInfo))
                .build();
    }

    public Response downloadAttachment(final long reservationId, final long messageId) {
        final Optional<BinaryContent> content;
        if (reservationResourceAccess.isAdmin()) {
            content = reservationMessageService.findMessageAttachmentContentForAdmin(
                    reservationId, messageId);
        } else {
            final RydenUserDetails viewer = currentUserResolver.requirePrincipal();
            content = reservationMessageService.findMessageAttachmentContentForParticipant(
                    viewer.getUserId(), reservationId, messageId);
        }
        return content
                .map(c -> CacheableBinaryResponses.sensitive(c, c.getFileName()))
                .orElseThrow(NotFoundException::new);
    }

    /** Marks counterparty messages as seen for the authenticated participant ({@code 204}). */
    public Response createReceipt(final long reservationId, final RydenUserDetails viewer) {
        reservationMessageService.markMessagesSeenForParticipant(viewer.getUserId(), reservationId);
        return Response.noContent().build();
    }

    private Response listAdmin(
            final long reservationId,
            final PaginationParams paging,
            final UriInfo uriInfo) {
        final int offset = paging.getZeroBasedPage() * paging.getPageSize();
        final List<ReservationMessageDto> messages =
                adminService.getAdminChatMessages(reservationId, offset, paging.getPageSize());
        final long total = adminService.countReservationMessages(reservationId);
        if (total == 0L) {
            return Response.noContent().build();
        }
        return pagedMessages(messages, paging, (int) total, uriInfo);
    }

    private Response listParticipant(
            final long reservationId,
            final RydenUserDetails viewer,
            final PaginationParams paging,
            final UriInfo uriInfo) {
        final Page<ReservationMessageDto> page = reservationMessageService.getMessagesForParticipant(
                viewer.getUserId(), reservationId, paging.getZeroBasedPage(), paging.getPageSize());
        if (page.getTotalItems() == 0L) {
            return Response.noContent().build();
        }
        return pagedMessages(page.getContent(), paging, (int) page.getTotalItems(), uriInfo);
    }

    private Response listAfter(
            final long reservationId,
            final RydenUserDetails viewer,
            final long afterId,
            final UriInfo uriInfo) {
        final int limit = paginationSupport.forMessages(1, null).getPageSize();
        final List<ReservationMessageDto> messages;
        if (reservationResourceAccess.isAdmin()) {
            messages = adminService.getChatMessagesAfter(reservationId, afterId, limit);
        } else {
            messages = reservationMessageService.pollMessagesForParticipant(
                    viewer.getUserId(), reservationId, afterId);
        }
        if (messages.isEmpty()) {
            return Response.noContent().build();
        }
        final List<MessageDto> dtos = messages.stream()
                .map(message -> MessageDto.fromDto(message, uriInfo))
                .collect(Collectors.toList());
        return Response.ok(new GenericEntity<List<MessageDto>>(dtos) {}).build();
    }

    private static Response pagedMessages(
            final List<ReservationMessageDto> messages,
            final PaginationParams paging,
            final int total,
            final UriInfo uriInfo) {
        final List<MessageDto> dtos = messages.stream()
                .map(message -> MessageDto.fromDto(message, uriInfo))
                .collect(Collectors.toList());
        final Response.ResponseBuilder builder = Response.ok(new GenericEntity<List<MessageDto>>(dtos) {})
                .header("X-Total-Count", total);
        PaginationLinks.add(builder, uriInfo, paging.getPage(), paging.getPageSize(), total);
        return builder.build();
    }

    private byte[] readFileBytes(final FormDataBodyPart filePart) throws IOException {
        if (filePart == null) {
            return null;
        }
        final InputStream stream = filePart.getValueAs(InputStream.class);
        if (stream == null) {
            return null;
        }
        final byte[] bytes = binaryPayloadSupport.readBounded(stream);
        return bytes.length == 0 ? null : bytes;
    }
}
