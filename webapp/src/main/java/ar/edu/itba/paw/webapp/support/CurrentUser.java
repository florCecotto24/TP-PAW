package ar.edu.itba.paw.webapp.support;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import ar.edu.itba.paw.models.domain.User;

/**
 * Injects the session {@link User} from {@link org.springframework.security.core.context.SecurityContextHolder}
 * without Spring MVC constructing a {@link User} from the model (which fails for guests when the model omits or holds {@code null}).
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUser {
}
