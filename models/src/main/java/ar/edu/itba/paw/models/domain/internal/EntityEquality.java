package ar.edu.itba.paw.models.domain.internal;

/**
 * Helpers that consolidate the JPA-friendly {@code equals}/{@code hashCode} contract used by the
 * domain entities. Two entities of the same logical class are equal iff both have a non-zero
 * (i.e. persisted) primary key and that key matches; transient instances (id=0) are only equal to
 * themselves so that a {@code Set<Entity>} populated before a flush still keeps each new row
 * separate. {@code hashCode} is stable across the unmanaged → managed transition: it returns the
 * identity hash for transient rows so the bucket location does not change once the database
 * generates an id.
 */
public final class EntityEquality {

    private EntityEquality() {
    }

    /**
     * Equality predicate for entities keyed by a {@code long} primary key. The caller passes the
     * already-narrowed peer (via {@code instanceof}) so this method does not need to know about
     * Hibernate proxies; the cast is the caller's responsibility.
     */
    public static boolean equalsByLongId(final Object self, final long selfId, final long otherId) {
        if (selfId == 0L || otherId == 0L) {
            return false;
        }
        return selfId == otherId;
    }

    /**
     * Identity-stable hash for a {@code long}-keyed entity. Returns the JVM identity hash code while
     * the row is transient (id=0) so the entity does not change its bucket once the persist call
     * generates the database id.
     */
    public static int hashByLongId(final Object self, final long id) {
        return id == 0L ? System.identityHashCode(self) : Long.hashCode(id);
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
     * Identity-stable hash for entities keyed by an embedded composite id. Mirrors
     * {@link #hashByLongId(Object, long)} for the {@code @EmbeddedId} case.
     */
    public static int hashByEmbeddedId(final Object self, final Object id) {
        return id == null ? System.identityHashCode(self) : id.hashCode();
    }
}
