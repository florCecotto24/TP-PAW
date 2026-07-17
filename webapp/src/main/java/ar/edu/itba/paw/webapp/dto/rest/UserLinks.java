package ar.edu.itba.paw.webapp.dto.rest;

import javax.ws.rs.core.UriInfo;

import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.webapp.util.RestUriUtils;

/** Shared hypermedia links for user representations. */
final class UserLinks {

    private UserLinks() {
    }

    static LinksDto build(final User user, final UriInfo uriInfo) {
        return LinksDto.ofSelf(RestUriUtils.userUri(uriInfo, user.getId()).toString())
                .withRelated("profilePicture",
                        user.getProfilePictureId()
                                .map(id -> RestUriUtils.userProfilePictureUri(uriInfo, user.getId()).toString())
                                .orElse(null))
                .withRelated("cars", RestUriUtils.userCarsUri(uriInfo, user.getId()).toString())
                .withRelated("reservations", RestUriUtils.userReservationsUri(uriInfo, user.getId()).toString())
                .withRelated("owned-reservations",
                        RestUriUtils.userOwnedReservationsUri(uriInfo, user.getId()).toString())
                .withRelated("favorites", RestUriUtils.userFavoritesUri(uriInfo, user.getId()).toString())
                .withRelated("reviews", RestUriUtils.userReviewsUri(uriInfo, user.getId()).toString());
    }
}
