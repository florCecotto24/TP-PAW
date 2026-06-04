package ar.edu.itba.paw.webapp.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;

class WebAuthConfigSavedRequestMatcherTest {

    private RequestMatcher matcher;

    @BeforeEach
    void setUp() throws Exception {
        final Method method = WebAuthConfig.class.getDeclaredMethod("savedRequestMatcher");
        method.setAccessible(true);
        matcher = (RequestMatcher) method.invoke(null);
    }

    @Test
    void testChatPollWithJsonAcceptNotCachedForRedirect() {
        final MockHttpServletRequest request =
                new MockHttpServletRequest(HttpMethod.GET.name(), "/my-reservations/1/messages/poll");
        request.addHeader("Accept", "application/json");
        request.setQueryString("afterId=126");

        assertFalse(matcher.matches(request));
    }

    @Test
    void testReservationDetailHtmlNavigationCachedForRedirect() {
        final MockHttpServletRequest request =
                new MockHttpServletRequest(HttpMethod.GET.name(), "/my-reservations/1");
        request.addHeader("Accept", "text/html,application/xhtml+xml");

        assertTrue(matcher.matches(request));
    }

    @Test
    void testXmlHttpRequestNotCachedForRedirect() {
        final MockHttpServletRequest request =
                new MockHttpServletRequest(HttpMethod.GET.name(), "/my-reservations");
        request.addHeader("X-Requested-With", "XMLHttpRequest");

        assertFalse(matcher.matches(request));
    }
}
