package ar.edu.itba.paw.models.security;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class UserRoleTest {

    @Test
    void testFromPersistenceNameAcceptsTrimAndCase() {
        Assertions.assertEquals(Optional.of(UserRole.USER), UserRole.fromPersistenceName("user"));
        Assertions.assertEquals(Optional.of(UserRole.USER), UserRole.fromPersistenceName(" USER "));
    }

    @Test
    void testFromPersistenceNameRejectsUnknown() {
        Assertions.assertEquals(Optional.empty(), UserRole.fromPersistenceName("SUPERUSER"));
        Assertions.assertEquals(Optional.empty(), UserRole.fromPersistenceName(""));
        Assertions.assertEquals(Optional.empty(), UserRole.fromPersistenceName(null));
    }

    @Test
    void testFromPersistenceNameAcceptsAdmin() {
        Assertions.assertEquals(Optional.of(UserRole.ADMIN), UserRole.fromPersistenceName("ADMIN"));
        Assertions.assertEquals(Optional.of(UserRole.ADMIN), UserRole.fromPersistenceName("admin"));
    }

    @Test
    void testSpringAuthorityNameUsesRolePrefix() {
        Assertions.assertEquals("ROLE_USER", UserRole.USER.springAuthorityName());
        Assertions.assertEquals("ROLE_ADMIN", UserRole.ADMIN.springAuthorityName());
    }

    @Test
    void testPersistenceNameMatchesEnumName() {
        Assertions.assertEquals("USER", UserRole.USER.persistenceName());
        Assertions.assertEquals("ADMIN", UserRole.ADMIN.persistenceName());
    }
}
