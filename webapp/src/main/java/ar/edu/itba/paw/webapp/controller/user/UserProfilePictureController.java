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

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.user.UserNotFoundException;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.dto.file.BinaryContent;
import ar.edu.itba.paw.services.file.ImageService;
import ar.edu.itba.paw.services.user.UserService;
import ar.edu.itba.paw.webapp.form.user.ProfilePictureRestForm;
import ar.edu.itba.paw.webapp.support.BinaryPayloadSupport;
import ar.edu.itba.paw.webapp.support.CurrentUserResolver;
import ar.edu.itba.paw.webapp.support.FormValidationSupport;
import ar.edu.itba.paw.webapp.support.UserResourceAccess;

/** Profile picture sub-resource ({@code /users/{id}/profile-picture}). */
@Path("/users/{id}/profile-picture")
@Component
public final class UserProfilePictureController {

    private final UserService userService;
    private final ImageService imageService;
    private final CurrentUserResolver currentUserResolver;
    private final UserResourceAccess userResourceAccess;
    private final BinaryPayloadSupport binaryPayloadSupport;
    private final FormValidationSupport formValidationSupport;

    @Autowired
    public UserProfilePictureController(
            final UserService userService,
            final ImageService imageService,
            final CurrentUserResolver currentUserResolver,
            final UserResourceAccess userResourceAccess,
            final BinaryPayloadSupport binaryPayloadSupport,
            final FormValidationSupport formValidationSupport) {
        this.userService = userService;
        this.imageService = imageService;
        this.currentUserResolver = currentUserResolver;
        this.userResourceAccess = userResourceAccess;
        this.binaryPayloadSupport = binaryPayloadSupport;
        this.formValidationSupport = formValidationSupport;
    }

    @GET
    public Response getProfilePicture(@PathParam("id") final long id) {
        final User user = userService.getUserById(id)
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        final Long imageId = user.getProfilePictureId().orElse(null);
        if (imageId == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return imageService.getImageContent(imageId)
                .map(this::binaryResponse)
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    @PUT
    @Consumes({MediaType.WILDCARD, "image/*", MediaType.MULTIPART_FORM_DATA})
    public Response uploadProfilePicture(
            @PathParam("id") final long id,
            @Context final HttpHeaders headers,
            final InputStream body,
            @FormDataParam("file") final InputStream multipartBody,
            @FormDataParam("file") final org.glassfish.jersey.media.multipart.FormDataContentDisposition fileMeta)
            throws IOException {
        userResourceAccess.requireSelf(id, currentUserResolver.currentPrincipalOrNull());
        userService.getUserById(id)
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));

        final InputStream source = multipartBody != null ? multipartBody : body;
        final byte[] bytes = binaryPayloadSupport.readValidatedBody(source);
        final String contentType = resolveContentType(headers, fileMeta);
        final ProfilePictureRestForm form = new ProfilePictureRestForm(bytes, contentType);
        formValidationSupport.validate(form);

        userService.updateProfilePicture(id, "profile-picture", contentType, bytes);
        return Response.noContent().build();
    }

    @DELETE
    public Response deleteProfilePicture(@PathParam("id") final long id) {
        userResourceAccess.requireSelf(id, currentUserResolver.currentPrincipalOrNull());
        userService.getUserById(id)
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        userService.clearProfilePicture(id);
        return Response.noContent().build();
    }

    private Response binaryResponse(final BinaryContent content) {
        return Response.ok(content.getBytes())
                .type(content.getContentType())
                .build();
    }

    private static String resolveContentType(
            final HttpHeaders headers,
            final org.glassfish.jersey.media.multipart.FormDataContentDisposition fileMeta) {
        final MediaType type = headers.getMediaType();
        return type != null ? type.toString() : MediaType.APPLICATION_OCTET_STREAM;
    }
}
