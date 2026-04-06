package ar.edu.itba.paw.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;


public final class JdbcDateTimeUtils {

    private JdbcDateTimeUtils() {
    }

    public static OffsetDateTime toOffsetDateTime(final Timestamp ts) {
        if (ts == null) {
            return null;
        }
        return ts.toInstant().atOffset(ZoneOffset.UTC);
    }

   
    public static OffsetDateTime readOffsetDateTime(final ResultSet rs, final String column) throws SQLException {
        final Object o = rs.getObject(column);
        if (o == null) {
            return null;
        }
        if (o instanceof OffsetDateTime) {
            return ((OffsetDateTime) o).withOffsetSameInstant(ZoneOffset.UTC);
        }
        if (o instanceof Timestamp) {
            return toOffsetDateTime((Timestamp) o);
        }
        return OffsetDateTime.parse(o.toString());
    }

    public static Timestamp toTimestamp(final OffsetDateTime odt) {
        if (odt == null) {
            return null;
        }
        return Timestamp.from(odt.toInstant());
    }

    public static Timestamp nowTimestamp() {
        return new Timestamp(System.currentTimeMillis());
    }

    public static LocalDate readLocalDate(final ResultSet rs, final String column) throws SQLException {
        final java.sql.Date d = rs.getDate(column);
        return d == null ? null : d.toLocalDate();
    }

    public static java.sql.Date toSqlDate(final LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        return java.sql.Date.valueOf(localDate);
    }

    public static LocalTime readLocalTime(final ResultSet rs, final String column) throws SQLException {
        final Object o = rs.getObject(column);
        if (o == null) {
            return null;
        }
        if (o instanceof LocalTime) {
            return (LocalTime) o;
        }
        if (o instanceof java.sql.Time) {
            return ((java.sql.Time) o).toLocalTime();
        }
        return LocalTime.parse(o.toString());
    }
}
