package ar.edu.itba.paw.webapp.support;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.user.UserNotFoundException;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.services.file.ImageService;
import ar.edu.itba.paw.services.user.UserService;
import ar.edu.itba.paw.webapp.form.user.ProfilePictureRestForm;

/**
 * HTTP binding for {@code /users/{id}/profile-picture}: download, upload, clear.
 */
@Component
public final class UserProfilePictureHttpSupport {

    private final UserService userService;
    private final ImageService imageService;
    private final BinaryPayloadSupport binaryPayloadSupport;
    private final FormValidationSupport formValidationSupport;

    public UserProfilePictureHttpSupport(
            final UserService userService,
            final ImageService imageService,
            final BinaryPayloadSupport binaryPayloadSupport,
            final FormValidationSupport formValidationSupport) {
        this.userService = userService;
        this.imageService = imageService;
        this.binaryPayloadSupport = binaryPayloadSupport;
        this.formValidationSupport = formValidationSupport;
    }

    public Response download(final long userId, final Request request) {
        final User user = requireUser(userId);
        final Long imageId = user.getProfilePictureId()
                .orElseThrow(NotFoundException::new);
        return imageService.getImageContent(imageId)
                .map(content -> CacheableBinaryResponses.of(request, content))
                .orElseThrow(NotFoundException::new);
    }

    public Response upload(final long userId, final InputStream body, final HttpHeaders httpHeaders)
            throws IOException {
        requireUser(userId);
        final byte[] bytes = binaryPayloadSupport.readValidatedBody(body);
        final String contentType = httpHeaders.getMediaType() != null
                ? httpHeaders.getMediaType().toString()
                : MediaType.APPLICATION_OCTET_STREAM;
        formValidationSupport.validate(ProfilePictureRestForm.of(bytes, contentType));
        userService.updateProfilePicture(userId, "profile-picture", contentType, bytes);
        return Response.noContent().build();
    }

    public Response delete(final long userId) {
        requireUser(userId);
        userService.clearProfilePicture(userId);
        return Response.noContent().build();
    }

    private User requireUser(final long userId) {
        return userService.getUserById(userId)
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
    }
}
