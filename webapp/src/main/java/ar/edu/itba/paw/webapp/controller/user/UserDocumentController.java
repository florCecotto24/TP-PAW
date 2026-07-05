package ar.edu.itba.paw.webapp.controller.user;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.user.UserNotFoundException;
import ar.edu.itba.paw.models.domain.user.UserDocumentType;
import ar.edu.itba.paw.models.dto.file.BinaryContent;
import ar.edu.itba.paw.services.user.UserService;
import ar.edu.itba.paw.webapp.support.BinaryPayloadSupport;

/**
 * Profile documents ({@code /users/{id}/documents/{documentType}}).
 *
 * Authorization is fully declarative here ({@code @PreAuthorize}, backed by the
 * {@code userResourceAccess}/{@code currentUserResolver} beans referenced by name in the
 * expressions) — this controller has no imperative checks left, so it no longer needs those
 * beans injected as fields.
 */
@Path("/users/{id}/documents/{documentType}")
@Component
public class UserDocumentController {

    private final UserService userService;
    private final BinaryPayloadSupport binaryPayloadSupport;

    @Context
    private HttpHeaders httpHeaders;

    @Autowired
    public UserDocumentController(
            final UserService userService,
            final BinaryPayloadSupport binaryPayloadSupport) {
        this.userService = userService;
        this.binaryPayloadSupport = binaryPayloadSupport;
    }

    @GET
    @PreAuthorize("@userResourceAccess.isSelfOrAdmin(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response downloadDocument(
            @P("id") @PathParam("id") final long id,
            @PathParam("documentType") final String documentType) {
        userService.getUserById(id)
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        final UserDocumentType type = parseDocumentType(documentType);
        return userService.findProfileDocumentContent(id, type)
                .map(this::binaryResponse)
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    @PUT
    @Consumes({MediaType.APPLICATION_OCTET_STREAM, MediaType.WILDCARD, MediaType.MULTIPART_FORM_DATA})
    @PreAuthorize("@userResourceAccess.isSelf(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response uploadDocument(
            @P("id") @PathParam("id") final long id,
            @PathParam("documentType") final String documentType,
            final InputStream body) throws IOException {
        userService.getUserById(id)
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        final UserDocumentType type = parseDocumentType(documentType);
        final byte[] bytes = binaryPayloadSupport.readValidatedBody(body);
        final String contentType = httpHeaders.getMediaType() != null
                ? httpHeaders.getMediaType().toString()
                : MediaType.APPLICATION_OCTET_STREAM;
        userService.uploadValidatedProfileDocument(
                id, type, type.name().toLowerCase(), contentType, bytes);
        return Response.noContent().build();
    }

    @DELETE
    @PreAuthorize("@userResourceAccess.isSelf(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response deleteDocument(
            @P("id") @PathParam("id") final long id,
            @PathParam("documentType") final String documentType) {
        userService.getUserById(id)
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        userService.clearProfileDocument(id, parseDocumentType(documentType));
        return Response.noContent().build();
    }

    private Response binaryResponse(final BinaryContent content) {
        return Response.ok(content.getBytes())
                .type(content.getContentType())
                .build();
    }

    private static UserDocumentType parseDocumentType(final String raw) {
        if (raw == null) {
            throw new javax.ws.rs.NotFoundException();
        }
        return switch (raw.toLowerCase()) {
            case "license" -> UserDocumentType.LICENSE;
            case "identity" -> UserDocumentType.IDENTITY;
            default -> throw new javax.ws.rs.NotFoundException();
        };
    }
}
