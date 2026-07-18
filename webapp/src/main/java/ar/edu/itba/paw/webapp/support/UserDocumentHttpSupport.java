package ar.edu.itba.paw.webapp.support;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.user.UserNotFoundException;
import ar.edu.itba.paw.models.domain.user.UserDocumentType;
import ar.edu.itba.paw.models.dto.file.BinaryContent;
import ar.edu.itba.paw.services.user.UserService;

/**
 * HTTP binding for {@code /users/{id}/documents/{documentType}}: download, upload, clear.
 */
@Component
public final class UserDocumentHttpSupport {

    private final UserService userService;
    private final BinaryPayloadSupport binaryPayloadSupport;

    public UserDocumentHttpSupport(
            final UserService userService,
            final BinaryPayloadSupport binaryPayloadSupport) {
        this.userService = userService;
        this.binaryPayloadSupport = binaryPayloadSupport;
    }

    public Response download(final long userId, final String documentType) {
        requireUser(userId);
        final UserDocumentType type = parseDocumentType(documentType);
        return userService.findProfileDocumentContent(userId, type)
                .map(this::sensitiveBinary)
                .orElseThrow(NotFoundException::new);
    }

    public Response upload(final long userId, final String documentType, final InputStream body, final HttpHeaders httpHeaders)
            throws IOException {
        requireUser(userId);
        final UserDocumentType type = parseDocumentType(documentType);
        final byte[] bytes = binaryPayloadSupport.readValidatedBody(body);
        final String contentType = httpHeaders.getMediaType() != null
                ? httpHeaders.getMediaType().toString()
                : MediaType.APPLICATION_OCTET_STREAM;
        userService.uploadProfileDocument(
                userId, type, type.name().toLowerCase(Locale.ROOT), contentType, bytes);
        return Response.noContent().build();
    }

    public Response delete(final long userId, final String documentType) {
        requireUser(userId);
        userService.clearProfileDocument(userId, parseDocumentType(documentType));
        return Response.noContent().build();
    }

    private void requireUser(final long userId) {
        userService.getUserById(userId)
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
    }

    private Response sensitiveBinary(final BinaryContent content) {
        // KYC documents are sensitive: never cache, never sniff, always download (see helper).
        return CacheableBinaryResponses.sensitive(content, content.getFileName());
    }

    private static UserDocumentType parseDocumentType(final String raw) {
        if (raw == null) {
            throw new NotFoundException();
        }
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "license" -> UserDocumentType.LICENSE;
            case "identity" -> UserDocumentType.IDENTITY;
            default -> throw new NotFoundException();
        };
    }
}
