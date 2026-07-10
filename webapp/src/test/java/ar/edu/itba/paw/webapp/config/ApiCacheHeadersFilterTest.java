package ar.edu.itba.paw.webapp.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import ar.edu.itba.paw.webapp.api.common.VndMediaType;

@ExtendWith(MockitoExtension.class)
class ApiCacheHeadersFilterTest {

    private final ApiCacheHeadersFilter filter = new ApiCacheHeadersFilter();

    @Mock
    private ContainerRequestContext requestContext;

    @Mock
    private ContainerResponseContext responseContext;

    @BeforeEach
    void setUp() {
        lenient().when(requestContext.getMethod()).thenReturn("GET");
        lenient().when(responseContext.getMediaType()).thenReturn(MediaType.valueOf(VndMediaType.CAR_SUMMARY_V1_JSON));
        when(responseContext.getHeaders()).thenReturn(new MultivaluedHashMap<>());
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testAnonymousJsonGetUsesNoStoreAndVaryAccept() {
        // 1.Arrange
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken(
                        "anon",
                        "anonymousUser",
                        java.util.List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        // 2.Act
        filter.filter(requestContext, responseContext);

        // 3.Assert
        assertEquals("no-store", responseContext.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL));
        assertEquals("Accept", responseContext.getHeaders().getFirst(HttpHeaders.VARY));
    }

    @Test
    void testAuthenticatedJsonGetUsesNoCache() {
        // 1.Arrange
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("user@example.com", "secret", "ROLE_USER"));

        // 2.Act
        filter.filter(requestContext, responseContext);

        // 3.Assert
        assertEquals("no-cache", responseContext.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL));
        assertEquals("Accept", responseContext.getHeaders().getFirst(HttpHeaders.VARY));
    }

    @Test
    void testSkipsWhenCacheControlAlreadySet() {
        // 1.Arrange
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(HttpHeaders.CACHE_CONTROL, "no-cache, must-revalidate");
        when(responseContext.getHeaders()).thenReturn(headers);

        // 2.Act
        filter.filter(requestContext, responseContext);

        // 3.Assert
        assertEquals("no-cache, must-revalidate", headers.getFirst(HttpHeaders.CACHE_CONTROL));
        assertNull(headers.getFirst(HttpHeaders.VARY));
    }

    @Test
    void testSkipsWhenEtagAlreadySet() {
        // 1.Arrange
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(HttpHeaders.ETAG, "\"abc\"");
        when(responseContext.getHeaders()).thenReturn(headers);

        // 2.Act
        filter.filter(requestContext, responseContext);

        // 3.Assert
        assertNull(headers.getFirst(HttpHeaders.CACHE_CONTROL));
        assertNull(headers.getFirst(HttpHeaders.VARY));
    }

    @Test
    void testSkipsNonGetRequests() {
        // 1.Arrange
        when(requestContext.getMethod()).thenReturn("POST");

        // 2.Act
        filter.filter(requestContext, responseContext);

        // 3.Assert
        assertNull(responseContext.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL));
    }

    @Test
    void testSkipsNonJsonResponses() {
        // 1.Arrange
        when(responseContext.getMediaType()).thenReturn(MediaType.APPLICATION_OCTET_STREAM_TYPE);

        // 2.Act
        filter.filter(requestContext, responseContext);

        // 3.Assert
        assertNull(responseContext.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL));
    }

    @Test
    void testAppendsAcceptToExistingVary() {
        // 1.Arrange
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(HttpHeaders.VARY, "Authorization");
        when(responseContext.getHeaders()).thenReturn(headers);
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken(
                        "anon",
                        "anonymousUser",
                        java.util.List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        // 2.Act
        filter.filter(requestContext, responseContext);

        // 3.Assert
        assertEquals("Authorization, Accept", headers.getFirst(HttpHeaders.VARY));
    }
}
