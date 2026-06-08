package ar.edu.itba.paw.models.email.reservation;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ReservationMailPayloadTest {

    private static final OffsetDateTime START = OffsetDateTime.of(2026, 5, 10, 10, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime END = OffsetDateTime.of(2026, 5, 12, 10, 0, 0, 0, ZoneOffset.UTC);

    private static ReservationMailPayload.Builder fullyPopulated() {
        return ReservationMailPayload.builder()
                .recipientEmail("rider@example.com")
                .riderFullName("Ada Rider")
                .reservationId(42L)
                .carId(77L)
                .vehicleLabel("Fiat Cronos")
                .startDate(START)
                .endDate(END)
                .riderHandoverLocation("Public corner")
                .ownerHandoverLocation("Owner Street 123")
                .ownerFullName("Owen Owner")
                .ownerEmail("owner@example.com")
                .reservationTotal("$ 12.000,00")
                .riderMailLocale(new Locale("es"))
                .ownerMailLocale(Locale.ENGLISH)
                .ownerCbu("0".repeat(22));
    }

    @Test
    void testBuildSucceedsWhenAllRequiredFieldsAreSet() {
        // 1.Arrange
        final ReservationMailPayload.Builder builder = fullyPopulated();

        // 2.Act
        final ReservationMailPayload payload = builder.build();

        // 3.Assert
        Assertions.assertEquals("rider@example.com", payload.getRecipientEmail());
        Assertions.assertEquals("Ada Rider", payload.getRiderFullName());
        Assertions.assertEquals(42L, payload.getReservationId());
        Assertions.assertEquals(77L, payload.getCarId());
        Assertions.assertEquals("Fiat Cronos", payload.getVehicleLabel());
        Assertions.assertEquals(START, payload.getStartDate());
        Assertions.assertEquals(END, payload.getEndDate());
        Assertions.assertEquals("Public corner", payload.getRiderHandoverLocation());
        Assertions.assertEquals("Public corner", payload.getDeliveryLocation());
        Assertions.assertEquals("Owner Street 123", payload.getOwnerHandoverLocation());
        Assertions.assertEquals("Owen Owner", payload.getOwnerFullName());
        Assertions.assertEquals("owner@example.com", payload.getOwnerEmail());
        Assertions.assertEquals("$ 12.000,00", payload.getReservationTotal());
        Assertions.assertEquals(new Locale("es"), payload.getRiderMailLocale());
        Assertions.assertEquals(new Locale("es"), payload.getMessageLocale());
        Assertions.assertEquals(Locale.ENGLISH, payload.getOwnerMailLocale());
        Assertions.assertEquals("0".repeat(22), payload.getOwnerCbu());
    }

    @Test
    void testBuildAllowsNullableHandoverAndCbuFields() {
        // 1.Arrange
        final ReservationMailPayload.Builder builder = fullyPopulated()
                .riderHandoverLocation(null)
                .ownerHandoverLocation(null)
                .ownerFullName(null)
                .ownerEmail(null)
                .reservationTotal(null)
                .ownerCbu(null);

        // 2.Act
        final ReservationMailPayload payload = builder.build();

        // 3.Assert
        Assertions.assertNull(payload.getRiderHandoverLocation());
        Assertions.assertNull(payload.getOwnerHandoverLocation());
        Assertions.assertNull(payload.getOwnerFullName());
        Assertions.assertNull(payload.getOwnerEmail());
        Assertions.assertNull(payload.getReservationTotal());
        Assertions.assertNull(payload.getOwnerCbu());
    }

    @Test
    void testBuildRejectsNullRecipientEmail() {
        // 1.Arrange
        final ReservationMailPayload.Builder builder = fullyPopulated().recipientEmail(null);

        // 2.Act / 3.Assert
        Assertions.assertThrows(NullPointerException.class, builder::build);
    }

    @Test
    void testBuildRejectsNullRiderFullName() {
        // 1.Arrange
        final ReservationMailPayload.Builder builder = fullyPopulated().riderFullName(null);

        // 2.Act / 3.Assert
        Assertions.assertThrows(NullPointerException.class, builder::build);
    }

    @Test
    void testBuildRejectsMissingReservationId() {
        // 1.Arrange
        final ReservationMailPayload.Builder builder = ReservationMailPayload.builder()
                .recipientEmail("rider@example.com")
                .riderFullName("Ada Rider")
                .carId(77L)
                .vehicleLabel("Fiat Cronos")
                .startDate(START)
                .endDate(END)
                .riderMailLocale(Locale.ENGLISH)
                .ownerMailLocale(Locale.ENGLISH);

        // 2.Act / 3.Assert
        Assertions.assertThrows(NullPointerException.class, builder::build);
    }

    @Test
    void testBuildRejectsMissingCarId() {
        // 1.Arrange
        final ReservationMailPayload.Builder builder = ReservationMailPayload.builder()
                .recipientEmail("rider@example.com")
                .riderFullName("Ada Rider")
                .reservationId(42L)
                .vehicleLabel("Fiat Cronos")
                .startDate(START)
                .endDate(END)
                .riderMailLocale(Locale.ENGLISH)
                .ownerMailLocale(Locale.ENGLISH);

        // 2.Act / 3.Assert
        Assertions.assertThrows(NullPointerException.class, builder::build);
    }

    @Test
    void testBuildRejectsNullVehicleLabel() {
        // 1.Arrange
        final ReservationMailPayload.Builder builder = fullyPopulated().vehicleLabel(null);

        // 2.Act / 3.Assert
        Assertions.assertThrows(NullPointerException.class, builder::build);
    }

    @Test
    void testBuildRejectsNullStartDate() {
        // 1.Arrange
        final ReservationMailPayload.Builder builder = fullyPopulated().startDate(null);

        // 2.Act / 3.Assert
        Assertions.assertThrows(NullPointerException.class, builder::build);
    }

    @Test
    void testBuildRejectsNullEndDate() {
        // 1.Arrange
        final ReservationMailPayload.Builder builder = fullyPopulated().endDate(null);

        // 2.Act / 3.Assert
        Assertions.assertThrows(NullPointerException.class, builder::build);
    }

    @Test
    void testBuildRejectsNullRiderMailLocale() {
        // 1.Arrange
        final ReservationMailPayload.Builder builder = fullyPopulated().riderMailLocale(null);

        // 2.Act / 3.Assert
        Assertions.assertThrows(NullPointerException.class, builder::build);
    }

    @Test
    void testBuildRejectsNullOwnerMailLocale() {
        // 1.Arrange
        final ReservationMailPayload.Builder builder = fullyPopulated().ownerMailLocale(null);

        // 2.Act / 3.Assert
        Assertions.assertThrows(NullPointerException.class, builder::build);
    }
}
