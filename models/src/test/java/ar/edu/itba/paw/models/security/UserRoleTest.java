package ar.edu.itba.paw.models.security;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class UserRoleTest {

    @Test
    void fromPersistenceNameAcceptsTrimAndCase() {
        Assertions.assertEquals(Optional.of(UserRole.USER), UserRole.fromPersistenceName("user"));
        Assertions.assertEquals(Optional.of(UserRole.USER), UserRole.fromPersistenceName(" USER "));
    }

    @Test
    void fromPersistenceNameRejectsUnknown() {
        Assertions.assertEquals(Optional.empty(), UserRole.fromPersistenceName("ADMIN"));
        Assertions.assertEquals(Optional.empty(), UserRole.fromPersistenceName(""));
        Assertions.assertEquals(Optional.empty(), UserRole.fromPersistenceName(null));
    }

    @Test
    void springAuthorityNameUsesRolePrefix() {
        Assertions.assertEquals("ROLE_USER", UserRole.USER.springAuthorityName());
    }

    @Test
    void persistenceNameMatchesEnumName() {
        Assertions.assertEquals("USER", UserRole.USER.persistenceName());
    }
}
