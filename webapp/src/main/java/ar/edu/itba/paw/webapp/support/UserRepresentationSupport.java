package ar.edu.itba.paw.webapp.support;

import java.math.BigDecimal;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.services.reservation.ReservationService;
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
    private final ReservationService reservationService;
    private final UserResourceAccess userResourceAccess;

    public UserRepresentationSupport(
            final ReviewService reviewService,
            final ReservationService reservationService,
            final UserResourceAccess userResourceAccess) {
        this.reviewService = reviewService;
        this.reservationService = reservationService;
        this.userResourceAccess = userResourceAccess;
    }

    /**
     * Deep-link target for the blocked-account banner: when exactly one reservation has an overdue
     * refund proof, its id (so the CTA can go straight to the upload button); {@code null} for zero
     * (race with auto-unblock) or many, where the caller should fall back to a listing link instead.
     */
    private Long resolveBlockedOverdueReservationId(final User user) {
        if (!user.isBlocked()) {
            return null;
        }
        final List<Long> overdueIds = reservationService.findOverdueRefundProofReservationIdsForOwner(user.getId());
        return overdueIds.size() == 1 ? overdueIds.get(0) : null;
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
            if (!userResourceAccess.isSelfOrAdmin(user.getId(), viewer)) {
                throw new AccessDeniedException("You do not have permission to perform this action.");
            }
            return Response.ok(UserPrivateDto.builder(user, uriInfo)
                            .ratings(ratingAsOwner, ratingAsRider)
                            .blockedOverdueReservationId(resolveBlockedOverdueReservationId(user))
                            .build())
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
        return UserPrivateDto.builder(user, uriInfo)
                .ratings(ratingAsOwner, ratingAsRider)
                .blockedOverdueReservationId(resolveBlockedOverdueReservationId(user))
                .build();
    }
}
