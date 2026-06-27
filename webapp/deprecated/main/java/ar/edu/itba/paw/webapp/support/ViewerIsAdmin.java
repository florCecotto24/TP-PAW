package ar.edu.itba.paw.webapp.support;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects a {@code boolean} flag indicating whether the current session principal
 * carries the {@code ROLE_ADMIN} authority. The flag is read from
 * {@link org.springframework.security.core.context.SecurityContextHolder} by the
 * argument resolver, so controllers no longer need to inject
 * {@link org.springframework.security.core.Authentication} or inspect
 * {@link org.springframework.security.core.GrantedAuthority} entries by hand to
 * translate the session into a business-level "viewer is admin" input.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ViewerIsAdmin {
}
