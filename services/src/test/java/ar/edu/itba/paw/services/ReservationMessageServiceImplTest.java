package ar.edu.itba.paw.services;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.env.Environment;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.reservation.ReservationMessageException;
import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.domain.ReservationMessage;
import ar.edu.itba.paw.models.domain.StoredFile;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.ReservationMessageDto;
import ar.edu.itba.paw.persistence.ReservationMessageDao;
import ar.edu.itba.paw.services.mail.MailPublicUrls;
import ar.edu.itba.paw.services.policy.ChatAttachmentUploadPolicy;
import ar.edu.itba.paw.services.policy.ReservationChatPolicy;
import ar.edu.itba.paw.services.policy.ReservationMessageValidationPolicy;
import ar.edu.itba.paw.services.util.UploadBinaryMegabyte;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReservationMessageServiceImplTest {

    private static final long OWNER_ID = 10L;
    private static final long RIDER_ID = 20L;
    private static final long RESERVATION_ID = 30L;
    private static final long CAR_ID = 40L;
    private static final long STORED_FILE_ID = 99L;

    @Mock
    private ReservationMessageDao reservationMessageDao;

    @Mock
    private ReservationService reservationService;

    @Mock
    private UserService userService;

    @Mock
    private CarService carService;

    @Mock
    private EmailService emailService;

    @Mock
    private MailPublicUrls mailPublicUrls;

    @Mock
    private StoredFileService storedFileService;

    @Mock
    private Environment environment;

    private ReservationMessageServiceImpl service;

    @BeforeEach
    void setUp() {
        Mockito.when(environment.getProperty(UploadBinaryMegabyte.PROPERTY_BYTES_PER_BINARY_MB, Integer.class))
                .thenReturn(1048576);
        Mockito.when(environment.getProperty(UploadBinaryMegabyte.PROPERTY_MAX_CHAT_ATTACHMENT_MB, Long.class))
                .thenReturn(25L);
        final ChatAttachmentUploadPolicy chatAttachmentUploadPolicy = new ChatAttachmentUploadPolicy(environment);
        service = new ReservationMessageServiceImpl(
                reservationMessageDao,
                reservationService,
                userService,
                carService,
                emailService,
                mailPublicUrls,
                storedFileService,
                ReservationMessageValidationPolicy.fromValidatedBodyMaxLength(1000),
                ReservationChatPolicy.fromValidatedConfiguration(7, 50),
                chatAttachmentUploadPolicy);
    }

    private static Reservation reservation(final Reservation.Status status, final boolean paymentApproved) {
        final Car carRef = Mockito.mock(Car.class);
        Mockito.when(carRef.getId()).thenReturn(CAR_ID);
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return Reservation.builder()
                .id(RESERVATION_ID)
                .rider(User.identities(RIDER_ID, "r@test.com", "R", "Rider"))
                .car(carRef)
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(2))
                .status(status)
                .createdAt(now.minusDays(5))
                .updatedAt(now)
                .totalPrice(new BigDecimal("100"))
                .paymentApproved(paymentApproved)
                .build();
    }

    private void stubParticipantAndSender() {
        final Reservation res = reservation(Reservation.Status.ACCEPTED, true);
        Mockito.when(reservationService.getRiderReservationById(RIDER_ID, RESERVATION_ID))
                .thenReturn(Optional.of(res));
        Mockito.when(userService.getUserById(RIDER_ID))
                .thenReturn(Optional.of(User.identities(RIDER_ID, "r@test.com", "R", "Rider")));
    }

    @Test
    void testIsChatAvailableWhenAcceptedAndPaymentApproved() {
        // 1.Arrange
        final Reservation res = reservation(Reservation.Status.ACCEPTED, true);

        // 2.Exercise / 3.Assert
        Assertions.assertTrue(service.isChatAvailable(res));
    }

    @Test
    void testIsChatAvailableWhenPending() {
        // 1.Arrange
        final Reservation res = reservation(Reservation.Status.PENDING, true);

        // 2.Exercise / 3.Assert
        Assertions.assertFalse(service.isChatAvailable(res));
    }

    @Test
    void testIsChatAvailableWhenFinishedBeyondGracePeriod() {
        // 1.Arrange
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        final Car carRef = Mockito.mock(Car.class);
        Mockito.when(carRef.getId()).thenReturn(CAR_ID);
        final Reservation res = Reservation.builder()
                .id(RESERVATION_ID)
                .rider(User.identities(RIDER_ID, "r@test.com", "R", "Rider"))
                .car(carRef)
                .startDate(now.minusDays(20))
                .endDate(now.minusDays(10))
                .status(Reservation.Status.FINISHED)
                .createdAt(now.minusDays(25))
                .updatedAt(now)
                .totalPrice(new BigDecimal("100"))
                .paymentApproved(true)
                .build();

        // 2.Exercise / 3.Assert
        Assertions.assertFalse(service.isChatAvailable(res));
    }

    @Test
    void testPostMessageRejectsEmptyBody() {
        // 1.Arrange
        final Reservation res = reservation(Reservation.Status.ACCEPTED, true);
        Mockito.when(reservationService.getRiderReservationById(RIDER_ID, RESERVATION_ID))
                .thenReturn(Optional.of(res));

        // 2.Exercise & 3.Assert
        final ReservationMessageException ex = Assertions.assertThrows(
                ReservationMessageException.class,
                () -> service.postMessage(RIDER_ID, RESERVATION_ID, "   "));
        Assertions.assertEquals(MessageKeys.RESERVATION_CHAT_BODY_EMPTY, ex.getMessageCode());
    }

    @Test
    void testPostMessageWithAttachmentRejectsEmptyBodyAndFile() {
        // 1.Arrange
        stubParticipantAndSender();

        // 2.Exercise & 3.Assert
        final ReservationMessageException ex = Assertions.assertThrows(
                ReservationMessageException.class,
                () -> service.postMessageWithAttachment(RIDER_ID, RESERVATION_ID, "  ", "a.pdf", "application/pdf", new byte[0]));
        Assertions.assertEquals(MessageKeys.RESERVATION_CHAT_ATTACHMENT_REQUIRED, ex.getMessageCode());
    }

    @Test
    void testPostMessageWithAttachmentRejectsInvalidType() {
        // 1.Arrange
        stubParticipantAndSender();

        // 2.Exercise & 3.Assert
        final ReservationMessageException ex = Assertions.assertThrows(
                ReservationMessageException.class,
                () -> service.postMessageWithAttachment(
                        RIDER_ID, RESERVATION_ID, "", "virus.exe", "application/x-msdownload", new byte[] {1}));
        Assertions.assertEquals(MessageKeys.RESERVATION_CHAT_ATTACHMENT_INVALID, ex.getMessageCode());
    }

    @Test
    void testPostMessageWithAttachmentRejectsOversizedFile() {
        // 1.Arrange
        stubParticipantAndSender();
        final byte[] tooLarge = new byte[26 * 1024 * 1024];

        // 2.Exercise & 3.Assert
        final ReservationMessageException ex = Assertions.assertThrows(
                ReservationMessageException.class,
                () -> service.postMessageWithAttachment(
                        RIDER_ID, RESERVATION_ID, "", "big.pdf", "application/pdf", tooLarge));
        Assertions.assertEquals(MessageKeys.RESERVATION_CHAT_ATTACHMENT_TOO_LARGE, ex.getMessageCode());
    }

    @Test
    void testPostMessageWithAttachmentPersistsMessageWithFile() {
        // 1.Arrange
        stubParticipantAndSender();
        final byte[] pdfBytes = new byte[] {1, 2, 3};
        final StoredFile storedFile = new StoredFile(
                STORED_FILE_ID,
                User.identities(RIDER_ID, "r@test.com", "R", "Rider"),
                "receipt.pdf",
                "application/pdf",
                pdfBytes,
                OffsetDateTime.now(ZoneOffset.UTC));
        Mockito.when(storedFileService.create(RIDER_ID, "receipt.pdf", "application/pdf", pdfBytes))
                .thenReturn(storedFile);
        final ReservationMessage saved = Mockito.mock(ReservationMessage.class);
        Mockito.when(saved.getId()).thenReturn(7L);
        Mockito.when(saved.getReservationId()).thenReturn(RESERVATION_ID);
        Mockito.when(saved.getSenderUserId()).thenReturn(RIDER_ID);
        Mockito.when(saved.getBody()).thenReturn("See attached");
        Mockito.when(saved.getCreatedAt()).thenReturn(OffsetDateTime.now(ZoneOffset.UTC));
        Mockito.when(saved.getAttachment()).thenReturn(storedFile);
        Mockito.when(reservationMessageDao.create(RESERVATION_ID, RIDER_ID, "See attached", STORED_FILE_ID))
                .thenReturn(saved);

        // 2.Exercise
        final ReservationMessageDto dto = service.postMessageWithAttachment(
                RIDER_ID, RESERVATION_ID, "See attached", "receipt.pdf", "application/pdf", pdfBytes);

        // 3.Assert
        Assertions.assertEquals(7L, dto.getId());
        Assertions.assertNotNull(dto.getAttachment());
        Assertions.assertEquals("receipt.pdf", dto.getAttachment().getFileName());
        Assertions.assertEquals("PDF", dto.getAttachment().getKind().name());
    }

    @Test
    void testPostMessageWithAttachmentAllowsEmptyBodyWhenFilePresent() {
        // 1.Arrange
        stubParticipantAndSender();
        final byte[] pngBytes = new byte[] {9};
        final StoredFile storedFile = new StoredFile(
                STORED_FILE_ID,
                User.identities(RIDER_ID, "r@test.com", "R", "Rider"),
                "shot.png",
                "image/png",
                pngBytes,
                OffsetDateTime.now(ZoneOffset.UTC));
        Mockito.when(storedFileService.create(RIDER_ID, "shot.png", "image/png", pngBytes))
                .thenReturn(storedFile);
        final ReservationMessage saved = Mockito.mock(ReservationMessage.class);
        Mockito.when(saved.getId()).thenReturn(8L);
        Mockito.when(saved.getReservationId()).thenReturn(RESERVATION_ID);
        Mockito.when(saved.getSenderUserId()).thenReturn(RIDER_ID);
        Mockito.when(saved.getBody()).thenReturn("");
        Mockito.when(saved.getCreatedAt()).thenReturn(OffsetDateTime.now(ZoneOffset.UTC));
        Mockito.when(saved.getAttachment()).thenReturn(storedFile);
        Mockito.when(reservationMessageDao.create(RESERVATION_ID, RIDER_ID, "", STORED_FILE_ID))
                .thenReturn(saved);

        // 2.Exercise
        final ReservationMessageDto dto =
                service.postMessageWithAttachment(RIDER_ID, RESERVATION_ID, "", "shot.png", "image/png", pngBytes);

        // 3.Assert
        Assertions.assertEquals("", dto.getBody());
        Assertions.assertNotNull(dto.getAttachment());
        Assertions.assertEquals("IMAGE", dto.getAttachment().getKind().name());
    }
}
