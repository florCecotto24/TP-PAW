package ar.edu.itba.paw.webapp.support;

import java.math.BigDecimal;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.services.review.ReviewService;
import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.dto.rest.UserDto;
import ar.edu.itba.paw.webapp.dto.rest.UserPrivateDto;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;

/**
 * Builds user GET/PATCH responses with vendor MIME negotiation (public vs private).
 */
@Component
public final class UserRepresentationSupport {

    private final ReviewService reviewService;
    private final UserResourceAccess userResourceAccess;

    public UserRepresentationSupport(
            final ReviewService reviewService,
            final UserResourceAccess userResourceAccess) {
        this.reviewService = reviewService;
        this.userResourceAccess = userResourceAccess;
    }

    public boolean acceptsPrivateRepresentation(final HttpHeaders headers) {
        if (headers == null) {
            return false;
        }
        final List<MediaType> acceptable = headers.getAcceptableMediaTypes();
        if (acceptable == null || acceptable.isEmpty()) {
            return false;
        }
        final MediaType privateType = MediaType.valueOf(VndMediaType.USER_PRIVATE_V1_JSON);
        for (final MediaType candidate : acceptable) {
            if (candidate.isCompatible(privateType)) {
                return true;
            }
        }
        return false;
    }

    public Response buildUserResponse(
            final User user,
            final javax.ws.rs.core.UriInfo uriInfo,
            final RydenUserDetails viewer,
            final HttpHeaders headers) {
        final BigDecimal ratingAsOwner = reviewService.getAverageRatingForCounterparty(user.getId(), true);
        final BigDecimal ratingAsRider = reviewService.getAverageRatingForCounterparty(user.getId(), false);

        if (acceptsPrivateRepresentation(headers)) {
            if (!userResourceAccess.canViewPrivate(user.getId(), viewer)) {
                throw new javax.ws.rs.ForbiddenException();
            }
            return Response.ok(UserPrivateDto.from(user, uriInfo, ratingAsOwner, ratingAsRider))
                    .type(VndMediaType.USER_PRIVATE_V1_JSON)
                    .build();
        }

        return Response.ok(UserDto.fromPublic(user, uriInfo, ratingAsOwner, ratingAsRider))
                .type(VndMediaType.USER_V1_JSON)
                .build();
    }

    public UserPrivateDto toPrivateDto(
            final User user, final javax.ws.rs.core.UriInfo uriInfo) {
        final BigDecimal ratingAsOwner = reviewService.getAverageRatingForCounterparty(user.getId(), true);
        final BigDecimal ratingAsRider = reviewService.getAverageRatingForCounterparty(user.getId(), false);
        return UserPrivateDto.from(user, uriInfo, ratingAsOwner, ratingAsRider);
    }
}
