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
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.webapp.support.UserProfilePictureHttpSupport;

/**
 * Profile picture sub-resource ({@code /users/{id}/profile-picture}). HTTP routing only.
 *
 * Mutations are gated declaratively ({@code @PreAuthorize}, backed by the
 * {@code userResourceAccess}/{@code currentUserResolver} beans referenced by name); GET is public.
 * PUT accepts a raw {@code image/*} body (see {@code openapi.yaml}), not multipart.
 */
@Path("/users/{id}/profile-picture")
@Component
public class UserProfilePictureController {

    private final UserProfilePictureHttpSupport userProfilePictureHttpSupport;

    @Context
    private Request request;

    @Context
    private HttpHeaders httpHeaders;

    @Autowired
    public UserProfilePictureController(final UserProfilePictureHttpSupport userProfilePictureHttpSupport) {
        this.userProfilePictureHttpSupport = userProfilePictureHttpSupport;
    }

    @GET
    public Response getProfilePicture(@PathParam("id") final long id) {
        return userProfilePictureHttpSupport.download(id, request);
    }

    @PUT
    @Consumes({"image/*", MediaType.APPLICATION_OCTET_STREAM, MediaType.WILDCARD})
    @PreAuthorize("@userResourceAccess.isSelf(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response uploadProfilePicture(
            @P("id") @PathParam("id") final long id,
            final InputStream body) throws IOException {
        return userProfilePictureHttpSupport.upload(id, body, httpHeaders);
    }

    @DELETE
    @PreAuthorize("@userResourceAccess.isSelf(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response deleteProfilePicture(@P("id") @PathParam("id") final long id) {
        return userProfilePictureHttpSupport.delete(id);
    }
}
