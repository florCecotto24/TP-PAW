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

import ar.edu.itba.paw.webapp.support.UserDocumentHttpSupport;

/**
 * Profile documents ({@code /users/{id}/documents/{documentType}}). HTTP routing only.
 *
 * Authorization is fully declarative here ({@code @PreAuthorize}, backed by the
 * {@code userResourceAccess}/{@code currentUserResolver} beans referenced by name in the
 * expressions).
 */
@Path("/users/{id}/documents/{documentType}")
@Component
public class UserDocumentController {

    private final UserDocumentHttpSupport userDocumentHttpSupport;

    @Context
    private HttpHeaders httpHeaders;

    @Autowired
    public UserDocumentController(final UserDocumentHttpSupport userDocumentHttpSupport) {
        this.userDocumentHttpSupport = userDocumentHttpSupport;
    }

    @GET
    @PreAuthorize("@userResourceAccess.isSelfOrAdmin(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response downloadDocument(
            @P("id") @PathParam("id") final long id,
            @PathParam("documentType") final String documentType) {
        return userDocumentHttpSupport.download(id, documentType);
    }

    @PUT
    @Consumes({MediaType.APPLICATION_OCTET_STREAM, MediaType.WILDCARD, MediaType.MULTIPART_FORM_DATA})
    @PreAuthorize("@userResourceAccess.isSelf(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response uploadDocument(
            @P("id") @PathParam("id") final long id,
            @PathParam("documentType") final String documentType,
            final InputStream body) throws IOException {
        return userDocumentHttpSupport.upload(id, documentType, body, httpHeaders);
    }

    @DELETE
    @PreAuthorize("@userResourceAccess.isSelf(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response deleteDocument(
            @P("id") @PathParam("id") final long id,
            @PathParam("documentType") final String documentType) {
        return userDocumentHttpSupport.delete(id, documentType);
    }
}
