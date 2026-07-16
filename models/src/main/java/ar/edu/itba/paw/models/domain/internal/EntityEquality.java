package ar.edu.itba.paw.models.domain.internal;

/**
 * Helpers that consolidate the JPA-friendly {@code equals}/{@code hashCode} contract used by the
 * domain entities.
 *
 * Equality: two entities of the same logical class are equal iff both have a non-zero
 * (persisted) primary key and that key matches. Transient instances ({@code id == 0}) are only
 * equal to themselves ({@code ==}); callers must compare ids via getters so Hibernate proxies
 * initialize correctly.
 *
 * Hash code: stable for the lifetime of an instance across the unmanaged → managed transition.
 * It is based on the entity class (proxy subclasses unwrap to the mapped type), not on the id,
 * so assigning a generated PK does not move the object to another {@link java.util.HashSet} bucket.
 * Equal persisted entities of the same type therefore share the same hash regardless of instance
 * identity.
 */
public final class EntityEquality {

    private EntityEquality() {
    }

    /**
     * Equality predicate for entities keyed by a {@code long} primary key. The caller passes ids
     * obtained via getters (not field access) so Hibernate proxies are initialized.
     */
    public static boolean equalsByLongId(final Object self, final long selfId, final long otherId) {
        if (selfId == 0L || otherId == 0L) {
            return false;
        }
        return selfId == otherId;
    }

    /**
     * Class-stable hash for a {@code long}-keyed entity. Ignores {@code id} so the hash does not
     * change when a transient row receives a generated primary key.
     */
    public static int hashByLongId(final Object self, final long id) {
        return entityClass(self).hashCode();
    }

    /**
     * Equality predicate for entities keyed by an embedded composite id object. {@code null} keys
     * (transient rows) are never equal to anything other than themselves; the caller is expected to
     * have already short-circuited the {@code self == other} case.
     */
    public static boolean equalsByEmbeddedId(final Object selfId, final Object otherId) {
        if (selfId == null || otherId == null) {
            return false;
        }
        return selfId.equals(otherId);
    }

    /**
     * Class-stable hash for entities keyed by an embedded composite id. Mirrors
     * {@link #hashByLongId(Object, long)}.
     */
    public static int hashByEmbeddedId(final Object self, final Object id) {
        return entityClass(self).hashCode();
    }

    private static Class<?> entityClass(final Object self) {
        Class<?> type = self.getClass();
        // Hibernate / ByteBuddy / Javassist proxies subclass the mapped entity.
        while (type.getSuperclass() != null && type.getSuperclass() != Object.class) {
            final String name = type.getName();
            if (name.contains("HibernateProxy")
                    || name.contains("ByteBuddy")
                    || name.contains("javassist")
                    || name.contains("EnhancerBy")) {
                type = type.getSuperclass();
            } else {
                break;
            }
        }
        return type;
    }
}
