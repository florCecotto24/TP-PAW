package ar.edu.itba.paw.webapp.dto.rest;

import javax.ws.rs.core.UriInfo;

import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.webapp.util.RestUriUtils;

/** Shared hypermedia links for user representations. */
final class UserLinks {

    private UserLinks() {
    }

    /**
     * Full affordances for {@code user.private} (self/admin). Includes private
     * collections and the favorites membership URI template.
     */
    static LinksDto build(final User user, final UriInfo uriInfo) {
        LinksDto links = LinksDto.ofSelf(RestUriUtils.userUri(uriInfo, user.getId()).toString())
                .withRelated(
                        "profilePicture",
                        RestUriUtils.userProfilePictureUri(uriInfo, user.getId()).toString())
                .withRelated("cars", RestUriUtils.userCarsUri(uriInfo, user.getId()).toString())
                .withRelated("reservations", RestUriUtils.userReservationsUri(uriInfo, user.getId()).toString())
                .withRelated("owned-reservations",
                        RestUriUtils.userOwnedReservationsUri(uriInfo, user.getId()).toString())
                .withRelated("favorites", RestUriUtils.userFavoritesUri(uriInfo, user.getId()).toString())
                .withRelated(
                        "favorites-item-template",
                        RestUriUtils.userFavoriteMembershipTemplate(uriInfo, user.getId()))
                .withRelated("reviews", RestUriUtils.userReviewsUri(uriInfo, user.getId()).toString());
        return withCredentialsIfUnverified(links, user, uriInfo);
    }

    /**
     * Public {@code user.v1} links: only affordances a third-party viewer may follow.
     * Private collections (favorites, reservations, …) stay on {@link #build}.
     */
    static LinksDto buildPublic(final User user, final UriInfo uriInfo) {
        LinksDto links = LinksDto.ofSelf(RestUriUtils.userUri(uriInfo, user.getId()).toString())
                .withRelated(
                        "profilePicture",
                        RestUriUtils.userProfilePictureUri(uriInfo, user.getId()).toString())
                .withRelated("cars", RestUriUtils.userCarsUri(uriInfo, user.getId()).toString())
                .withRelated("reviews", RestUriUtils.userReviewsUri(uriInfo, user.getId()).toString());
        // Registration 201 uses public MIME; announce credentials while email is unverified.
        return withCredentialsIfUnverified(links, user, uriInfo);
    }

    /** Minimal links for counterparty contact (no private user affordances). */
    static LinksDto buildCounterparty(final User user, final UriInfo uriInfo) {
        return LinksDto.ofSelf(RestUriUtils.userUri(uriInfo, user.getId()).toString())
                .withRelated(
                        "profilePicture",
                        RestUriUtils.userProfilePictureUri(uriInfo, user.getId()).toString());
    }

    private static LinksDto withCredentialsIfUnverified(
            final LinksDto links, final User user, final UriInfo uriInfo) {
        if (!Boolean.TRUE.equals(user.getEmailValidated().orElse(Boolean.FALSE))) {
            return links.withRelated(
                    "credentials",
                    RestUriUtils.userCredentialsUri(uriInfo, user.getId()).toString());
        }
        return links;
    }
}
