package ar.edu.itba.paw.persistence.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class JdbcDateTimeUtilsTest {

    @Test
    public void testToOffsetDateTimeHandlesNull() {


        // Exercise & Assert
        Assertions.assertNull(JdbcDateTimeUtils.toOffsetDateTime(null));
    }

    @Test
    public void testToTimestampAndToOffsetDateTimeRoundTrip() {
        // Arrange
        final OffsetDateTime source = OffsetDateTime.of(2026, 4, 6, 12, 0, 0, 0, ZoneOffset.ofHours(-3));

        // Exercise
        final Timestamp ts = JdbcDateTimeUtils.toTimestamp(source);
        final OffsetDateTime converted = JdbcDateTimeUtils.toOffsetDateTime(ts);

        // Assert
        Assertions.assertEquals(source.toInstant(), converted.toInstant());
        Assertions.assertEquals(ZoneOffset.UTC, converted.getOffset());
    }

    @Test
    public void testReadOffsetDateTimeFromTimestamp() throws Exception {
        // Arrange
        final Timestamp ts = Timestamp.from(OffsetDateTime.parse("2026-04-06T15:00:00Z").toInstant());
        final ResultSet rs = fakeResultSet(ts);

        // Exercise
        final OffsetDateTime value = JdbcDateTimeUtils.readOffsetDateTime(rs, "col");

        // Assert
        Assertions.assertEquals(OffsetDateTime.parse("2026-04-06T15:00:00Z"), value);
    }

    @Test
    public void testReadOffsetDateTimeFromString() throws Exception {
        // Arrange
        final ResultSet rs = fakeResultSet("2026-04-06T15:00:00Z");

        // Exercise
        final OffsetDateTime value = JdbcDateTimeUtils.readOffsetDateTime(rs, "col");

        // Assert
        Assertions.assertEquals(OffsetDateTime.parse("2026-04-06T15:00:00Z"), value);
    }

    @Test
    public void testReadOffsetDateTimeFromOffsetDateTimeNormalizesToUtc() throws Exception {
        // Arrange
        final ResultSet rs = fakeResultSet(OffsetDateTime.parse("2026-04-06T12:00:00-03:00"));

        // Exercise
        final OffsetDateTime value = JdbcDateTimeUtils.readOffsetDateTime(rs, "col");

        // Assert
        Assertions.assertEquals(OffsetDateTime.parse("2026-04-06T15:00:00Z"), value);
    }

    @Test
    public void testReadOffsetDateTimeReturnsNullWhenColumnIsNull() throws Exception {
        // Arrange
        final ResultSet rs = fakeResultSet(null);

        // Exercise
        final OffsetDateTime value = JdbcDateTimeUtils.readOffsetDateTime(rs, "col");

        // Assert
        Assertions.assertNull(value);
    }

    @Test
    public void testNowTimestampIsCloseToCurrentTime() {
        // Arrange
        final long before = System.currentTimeMillis();

        // Exercise
        final Timestamp now = JdbcDateTimeUtils.nowTimestamp();
        final long after = System.currentTimeMillis();

        // Assert
        Assertions.assertTrue(now.getTime() >= before);
        Assertions.assertTrue(now.getTime() <= after);
    }

    private static ResultSet fakeResultSet(final Object value) {
        return (ResultSet) Proxy.newProxyInstance(
                JdbcDateTimeUtilsTest.class.getClassLoader(),
                new Class<?>[] {ResultSet.class},
                (proxy, method, args) -> {
                    if ("getObject".equals(method.getName())) {
                        return value;
                    }
                    if (method.getReturnType().equals(boolean.class)) {
                        return false;
                    }
                    if (method.getReturnType().isPrimitive()) {
                        return 0;
                    }
                    return null;
                });
    }
}
