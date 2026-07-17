package ar.edu.itba.paw.webapp.dto.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.junit.jupiter.api.Test;

import ar.edu.itba.paw.models.domain.user.User;

class UserPrivateDtoTest {

    @Test
    void testBuildIncludesTypedDocumentLinks() {
        // 1.Arrange
        final User user = User.identities(42L, "user@example.com", "Ada", "Lovelace");
        final UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getBaseUriBuilder())
                .thenAnswer(invocation -> UriBuilder.fromUri("http://localhost/webapp/api/"));

        // 2.Act
        final UserPrivateDto dto = UserPrivateDto.builder(user, uriInfo).build();

        // 3.Assert
        assertEquals(
                "http://localhost/webapp/api/users/42/documents/identity",
                dto.getLinks().get("identityDocument"));
        assertEquals(
                "http://localhost/webapp/api/users/42/documents/license",
                dto.getLinks().get("licenseDocument"));
    }
}
