package ar.edu.itba.paw.webapp.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ar.edu.itba.paw.models.dto.reservation.ReservationMessageDto;

class ReservationMessageDtoJsonSerializationTest {

    private final ObjectMapper objectMapper = new JacksonConfig().objectMapper();

    @Test
    void testCreatedAtSerializesWithUtcOffset() throws Exception {
        // 1.Arrange
        final OffsetDateTime createdAt = OffsetDateTime.of(2026, 6, 2, 15, 30, 0, 0, ZoneOffset.UTC);
        final ReservationMessageDto dto =
                new ReservationMessageDto(1L, 2L, 3L, "Alice", "hello", createdAt);

        // 2.Act
        final String json = objectMapper.writeValueAsString(dto);

        // 3.Assert
        assertTrue(json.contains("2026-06-02T15:30:00"));
        assertTrue(json.contains("Z") || json.contains("+00:00"));
    }

    @Test
    void testCreatedAtJsonFieldRoundTripMatchesInstant() throws Exception {
        // 1.Arrange
        final OffsetDateTime createdAt = OffsetDateTime.of(2026, 6, 2, 15, 30, 0, 0, ZoneOffset.UTC);
        final ReservationMessageDto dto =
                new ReservationMessageDto(1L, 2L, 3L, "Alice", "hello", createdAt);

        // 2.Act
        final JsonNode root = objectMapper.readTree(objectMapper.writeValueAsString(dto));
        final OffsetDateTime parsed = objectMapper.treeToValue(root.get("createdAt"), OffsetDateTime.class);

        // 3.Assert
        assertEquals(dto.getCreatedAt().toInstant(), parsed.toInstant());
    }

    @Test
    void testSeenSerializesWhenTrue() throws Exception {
        // 1.Arrange
        final OffsetDateTime createdAt = OffsetDateTime.of(2026, 6, 2, 15, 30, 0, 0, ZoneOffset.UTC);
        final ReservationMessageDto dto =
                new ReservationMessageDto(1L, 2L, 3L, "Alice", "hello", createdAt, null, true);

        // 2.Act
        final JsonNode root = objectMapper.readTree(objectMapper.writeValueAsString(dto));

        // 3.Assert
        assertTrue(root.get("seen").asBoolean());
    }
}
