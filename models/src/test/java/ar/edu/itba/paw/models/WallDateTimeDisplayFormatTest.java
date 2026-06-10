package ar.edu.itba.paw.models;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ar.edu.itba.paw.models.util.time.WallDateTimeDisplayFormat;

public class WallDateTimeDisplayFormatTest {

    @Test
    public void testFormatSpanishUsesDayMonthYearAndNoSecondsLiteral() {
        // 1. Arrange
        final LocalDateTime wall = LocalDateTime.of(2026, 4, 17, 9, 5);

        // 2. Act
        final String result = WallDateTimeDisplayFormat.formatWallLocalNoSeconds(wall, Locale.forLanguageTag("es-AR"));
        final int seconds = result.trim().split(":").length;

        // 3. Assert
        Assertions.assertEquals("17/04/2026 09:05", result);
        Assertions.assertEquals(2, seconds, "must not include seconds");
    }

    @Test
    public void testFormatUtcRespectsWallZone() {
        // 1. Arrange
        final OffsetDateTime utc = OffsetDateTime.of(2026, 4, 17, 12, 0, 0, 0, ZoneOffset.UTC);

        // 2. Act
        final String result = WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(utc, Locale.forLanguageTag("es"));

        // 3. Assert
        Assertions.assertEquals("17/04/2026 09:00", result);
    }

    @Test
    public void testFormatClientInputParsesIsoLocalWithoutSeconds() {
        // 1. Arrange
        final String raw = "2026-05-01T10:30";

        // 2. Act
        final String result = WallDateTimeDisplayFormat.formatClientWallDateTimeInputOrRaw(raw, Locale.forLanguageTag("es"));
        final int seconds = result.trim().split(":").length;

        // 3. Assert
        Assertions.assertEquals("01/05/2026 10:30", result);
        Assertions.assertEquals(2, seconds, "must not include seconds");
    }
}
