package ar.edu.itba.paw.webapp.support;


import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;

/**
 * Encapsulates Spring Security's {@link SessionRegistry} traversal to expire every active session
 * belonging to a specific application user. Lives in {@code webapp} because {@link RydenUserDetails}
 * (the principal carried by registered sessions) and {@link SessionRegistry} are web-tier concerns.
 *
 * <p>Used by {@code AdminController#blockUser} after the database flag is flipped so the blocked
 * user is logged out on every browser they currently hold open.</p>
 */
@Component
public final class UserSessionService {

    private final SessionRegistry sessionRegistry;

    public UserSessionService(final SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    /**
     * Marks every active session principal whose {@link RydenUserDetails#getUserId()} matches
     * {@code userId} as expired. The actual logout happens on the user's next request, when the
     * concurrent-session filter consumes the expiration. Non-{@link RydenUserDetails} principals
     * (e.g. anonymous) are ignored.
     */
    public void invalidateSessionsForUser(final long userId) {
        for (final Object principal : sessionRegistry.getAllPrincipals()) {
            if (principal instanceof RydenUserDetails details && details.getUserId() == userId) {
                for (final SessionInformation session : sessionRegistry.getAllSessions(principal, false)) {
                    session.expireNow();
                }
            }
        }
    }
}
