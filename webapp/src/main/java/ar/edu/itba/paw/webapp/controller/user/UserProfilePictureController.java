package ar.edu.itba.paw.webapp.controller.user;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
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

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.user.UserNotFoundException;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.services.file.ImageService;
import ar.edu.itba.paw.services.user.UserService;
import ar.edu.itba.paw.webapp.form.user.ProfilePictureRestForm;
import ar.edu.itba.paw.webapp.support.BinaryPayloadSupport;
import ar.edu.itba.paw.webapp.support.CacheableBinaryResponses;
import ar.edu.itba.paw.webapp.support.FormValidationSupport;

/**
 * Profile picture sub-resource ({@code /users/{id}/profile-picture}).
 *
 * Mutations are gated declaratively ({@code @PreAuthorize}, backed by the
 * {@code userResourceAccess}/{@code currentUserResolver} beans referenced by name); GET is public.
 * PUT accepts a raw {@code image/*} body (see {@code openapi.yaml}), not multipart.
 */
@Path("/users/{id}/profile-picture")
@Component
public class UserProfilePictureController {

    private final UserService userService;
    private final ImageService imageService;
    private final BinaryPayloadSupport binaryPayloadSupport;
    private final FormValidationSupport formValidationSupport;

    @Context
    private Request request;

    @Context
    private HttpHeaders httpHeaders;

    @Autowired
    public UserProfilePictureController(
            final UserService userService,
            final ImageService imageService,
            final BinaryPayloadSupport binaryPayloadSupport,
            final FormValidationSupport formValidationSupport) {
        this.userService = userService;
        this.imageService = imageService;
        this.binaryPayloadSupport = binaryPayloadSupport;
        this.formValidationSupport = formValidationSupport;
    }

    @GET
    public Response getProfilePicture(@PathParam("id") final long id) {
        final User user = userService.getUserById(id)
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        final Long imageId = user.getProfilePictureId()
                .orElseThrow(NotFoundException::new);
        return imageService.getImageContent(imageId)
                .map(content -> CacheableBinaryResponses.of(request, content))
                .orElseThrow(NotFoundException::new);
    }

    @PUT
    @Consumes({"image/*", MediaType.APPLICATION_OCTET_STREAM, MediaType.WILDCARD})
    @PreAuthorize("@userResourceAccess.isSelf(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response uploadProfilePicture(
            @P("id") @PathParam("id") final long id,
            final InputStream body) throws IOException {
        userService.getUserById(id)
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));

        final byte[] bytes = binaryPayloadSupport.readValidatedBody(body);
        final String contentType = httpHeaders.getMediaType() != null
                ? httpHeaders.getMediaType().toString()
                : MediaType.APPLICATION_OCTET_STREAM;
        final ProfilePictureRestForm form = ProfilePictureRestForm.of(bytes, contentType);
        formValidationSupport.validate(form);

        userService.updateProfilePicture(id, "profile-picture", contentType, bytes);
        return Response.noContent().build();
    }

    @DELETE
    @PreAuthorize("@userResourceAccess.isSelf(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response deleteProfilePicture(@P("id") @PathParam("id") final long id) {
        userService.getUserById(id)
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        userService.clearProfilePicture(id);
        return Response.noContent().build();
    }
}
