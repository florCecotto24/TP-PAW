package ar.edu.itba.paw.webapp.support;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import ar.edu.itba.paw.webapp.util.RestUriUtils;

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
    private String resolveBlockedOverdueReservationUri(
            final User user,
            final List<Long> overdueIds,
            final javax.ws.rs.core.UriInfo uriInfo) {
        if (!user.isBlocked()) {
            return null;
        }
        if (overdueIds == null || overdueIds.size() != 1) {
            return null;
        }
        return RestUriUtils.reservationUri(uriInfo, overdueIds.get(0)).toString();
    }

    private List<Long> loadOverdueIdsForUser(final User user) {
        return reservationService.findOverdueRefundProofReservationIdsForOwner(user.getId());
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
                            .blockedOverdueReservationUri(resolveBlockedOverdueReservationUri(
                                    user, loadOverdueIdsForUser(user), uriInfo))
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
                .blockedOverdueReservationUri(resolveBlockedOverdueReservationUri(
                        user, loadOverdueIdsForUser(user), uriInfo))
                .build();
    }

    public List<UserPrivateDto> toPrivateDtos(
            final List<User> users, final javax.ws.rs.core.UriInfo uriInfo) {
        if (users.isEmpty()) {
            return List.of();
        }
        final List<Long> userIds = users.stream().map(User::getId).toList();
        final Map<Long, BigDecimal> ratingAsOwnerByUser =
                reviewService.getAverageRatingsAsOwnerForUserIds(userIds);
        final Map<Long, BigDecimal> ratingAsRiderByUser =
                reviewService.getAverageRatingsAsRiderForUserIds(userIds);
        final Map<Long, List<Long>> overdueIdsByOwner =
                reservationService.findOverdueRefundProofReservationIdsByOwnerIds(userIds);
        return users.stream()
                .map(user -> UserPrivateDto.builder(user, uriInfo)
                        .ratings(
                                ratingAsOwnerByUser.get(user.getId()),
                                ratingAsRiderByUser.get(user.getId()))
                        .blockedOverdueReservationUri(resolveBlockedOverdueReservationUri(
                                user,
                                overdueIdsByOwner.getOrDefault(user.getId(), List.of()),
                                uriInfo))
                        .build())
                .collect(Collectors.toList());
    }
}
