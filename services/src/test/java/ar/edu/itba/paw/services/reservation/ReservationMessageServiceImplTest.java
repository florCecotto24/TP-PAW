package ar.edu.itba.paw.services.reservation;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.reservation.ReservationMessageDto;
import ar.edu.itba.paw.models.email.ReservationChatDigestEmailPayload;
import ar.edu.itba.paw.persistence.ReservationMessageDao;
import ar.edu.itba.paw.mail.MailPublicUrls;
import ar.edu.itba.paw.policy.ChatAttachmentUploadPolicy;
import ar.edu.itba.paw.policy.ChatAttachmentUploadPolicyImpl;
import ar.edu.itba.paw.policy.ReservationChatPolicy;
import ar.edu.itba.paw.policy.ReservationMessageValidationPolicy;
import ar.edu.itba.paw.util.UploadBinaryMegabyte;

import ar.edu.itba.paw.services.car.CarService;
import ar.edu.itba.paw.services.email.EmailService;
import ar.edu.itba.paw.services.file.StoredFileService;
import ar.edu.itba.paw.services.user.UserService;
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

    private final List<ReservationChatDigestEmailPayload> sentDigests = new ArrayList<>();

    private ReservationMessageServiceImpl service;

    @BeforeEach
    void setUp() {
        sentDigests.clear();
        Mockito.doAnswer(invocation -> {
                    sentDigests.add(invocation.getArgument(0));
                    return null;
                })
                .when(emailService)
                .sendReservationChatDigestEmail(Mockito.any(ReservationChatDigestEmailPayload.class));
        Mockito.when(environment.getProperty(UploadBinaryMegabyte.PROPERTY_BYTES_PER_BINARY_MB, Integer.class))
                .thenReturn(1048576);
        Mockito.when(environment.getProperty(UploadBinaryMegabyte.PROPERTY_MAX_CHAT_ATTACHMENT_MB, Long.class))
                .thenReturn(25L);
        final ChatAttachmentUploadPolicy chatAttachmentUploadPolicy = new ChatAttachmentUploadPolicyImpl(environment);
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

    private static Reservation reservation(final Reservation.Status status) {
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
                .build();
    }

    private void stubParticipantAndSender() {
        final Reservation res = reservation(Reservation.Status.ACCEPTED);
        Mockito.when(reservationService.getRiderReservationById(RIDER_ID, RESERVATION_ID))
                .thenReturn(Optional.of(res));
        Mockito.when(userService.getUserById(RIDER_ID))
                .thenReturn(Optional.of(User.identities(RIDER_ID, "r@test.com", "R", "Rider")));
    }

    @Test
    void testIsChatAvailableWhenAccepted() {
        // 1.Arrange
        final Reservation res = reservation(Reservation.Status.ACCEPTED);

        // 2.Exercise / 3.Assert
        Assertions.assertTrue(service.isChatAvailable(res));
    }

    @Test
    void testIsChatAvailableWhenPending() {
        // 1.Arrange
        final Reservation res = reservation(Reservation.Status.PENDING);

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
                .build();

        // 2.Exercise / 3.Assert
        Assertions.assertFalse(service.isChatAvailable(res));
    }

    @Test
    void testGetMessagesForParticipantReturnsPageWithTotalAndClampedPage() {
        // 1.Arrange
        stubParticipantAndSender();
        Mockito.when(reservationMessageDao.countByReservationId(RESERVATION_ID)).thenReturn(120L);
        Mockito.when(reservationMessageDao.findByReservationIdOrderByCreatedAtAsc(RESERVATION_ID, 0, 50))
                .thenReturn(List.of());
        Mockito.when(reservationMessageDao.findByReservationIdOrderByCreatedAtAsc(RESERVATION_ID, 100, 50))
                .thenReturn(List.of());

        // 2.Exercise
        final Page<ReservationMessageDto> firstPage =
                service.getMessagesForParticipant(RIDER_ID, RESERVATION_ID, 0, 50);
        final Page<ReservationMessageDto> lastPage =
                service.getMessagesForParticipant(RIDER_ID, RESERVATION_ID, null, null);

        // 3.Assert
        Assertions.assertEquals(120L, firstPage.getTotalItems());
        Assertions.assertEquals(0, firstPage.getCurrentPage());
        Assertions.assertEquals(50, firstPage.getPageSize());
        Assertions.assertTrue(firstPage.isHasNext());
        Assertions.assertFalse(firstPage.isHasPrevious());
        Assertions.assertEquals(2, lastPage.getCurrentPage());
        Assertions.assertTrue(lastPage.isHasPrevious());
        Assertions.assertFalse(lastPage.isHasNext());
    }

    @Test
    void testPostMessageRejectsEmptyBody() {
        // 1.Arrange
        final Reservation res = reservation(Reservation.Status.ACCEPTED);
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

    @Test
    void testPostMessageDoesNotSendImmediateDigestEmail() {
        // 1.Arrange
        stubParticipantAndSender();
        final ReservationMessage saved = Mockito.mock(ReservationMessage.class);
        Mockito.when(saved.getId()).thenReturn(1L);
        Mockito.when(saved.getReservationId()).thenReturn(RESERVATION_ID);
        Mockito.when(saved.getSenderUserId()).thenReturn(RIDER_ID);
        Mockito.when(saved.getBody()).thenReturn("Hello");
        Mockito.when(saved.getCreatedAt()).thenReturn(OffsetDateTime.now(ZoneOffset.UTC));
        Mockito.when(saved.getAttachment()).thenReturn(null);
        Mockito.when(reservationMessageDao.create(RESERVATION_ID, RIDER_ID, "Hello")).thenReturn(saved);

        // 2.Exercise
        service.postMessage(RIDER_ID, RESERVATION_ID, "Hello");

        // 3.Assert
        Assertions.assertTrue(sentDigests.isEmpty());
    }

    @Test
    void testDispatchChatDigestEmailsWhenNoPendingMessagesDoesNothing() {
        // 1.Arrange
        Mockito.when(reservationMessageDao.findPendingEmailNotification()).thenReturn(List.of());

        // 2.Exercise
        service.dispatchChatDigestEmails();

        // 3.Assert
        Assertions.assertTrue(sentDigests.isEmpty());
    }

    @Test
    void testDispatchChatDigestEmailsSkipsSeenMessages() {
        // 1.Arrange
        final Reservation reservation = reservation(Reservation.Status.ACCEPTED);
        final User rider = User.identities(RIDER_ID, "r@test.com", "R", "Rider");
        final ReservationMessage message = Mockito.mock(ReservationMessage.class);
        Mockito.when(message.getId()).thenReturn(55L);
        Mockito.when(message.getReservation()).thenReturn(reservation);
        Mockito.when(message.getReservationId()).thenReturn(RESERVATION_ID);
        Mockito.when(message.getSenderUserId()).thenReturn(RIDER_ID);
        Mockito.when(message.getSender()).thenReturn(rider);
        Mockito.when(message.isSeen()).thenReturn(true);

        Mockito.when(reservationMessageDao.findPendingEmailNotification()).thenReturn(List.of(message));

        // 2.Exercise
        service.dispatchChatDigestEmails();

        // 3.Assert
        Assertions.assertTrue(sentDigests.isEmpty());
    }

    @Test
    void testDispatchChatDigestEmailsSendsOneDigestForRecipient() {
        // 1.Arrange
        final Reservation reservation = reservation(Reservation.Status.ACCEPTED);
        final User owner = User.identities(OWNER_ID, "o@test.com", "O", "Owner");
        final User rider = User.identities(RIDER_ID, "r@test.com", "R", "Rider");
        final Car car = Mockito.mock(Car.class);
        Mockito.when(car.getId()).thenReturn(CAR_ID);
        Mockito.when(car.getBrand()).thenReturn("Toyota");
        Mockito.when(car.getModel()).thenReturn("Corolla");
        Mockito.when(car.getOwner()).thenReturn(owner);
        Mockito.when(carService.getCarById(CAR_ID)).thenReturn(Optional.of(car));

        final ReservationMessage message = Mockito.mock(ReservationMessage.class);
        Mockito.when(message.getId()).thenReturn(55L);
        Mockito.when(message.getReservation()).thenReturn(reservation);
        Mockito.when(message.getReservationId()).thenReturn(RESERVATION_ID);
        Mockito.when(message.getSenderUserId()).thenReturn(RIDER_ID);
        Mockito.when(message.getSender()).thenReturn(rider);
        Mockito.when(message.getBody()).thenReturn("When can I pick up?");
        Mockito.when(message.getCreatedAt()).thenReturn(OffsetDateTime.now(ZoneOffset.UTC));
        Mockito.when(message.getAttachment()).thenReturn(null);
        Mockito.when(message.isSeen()).thenReturn(false);

        Mockito.when(reservationMessageDao.findPendingEmailNotification()).thenReturn(List.of(message));
        Mockito.when(reservationMessageDao.markEmailNotified(List.of(55L))).thenReturn(1);
        Mockito.when(userService.getUserById(OWNER_ID)).thenReturn(Optional.of(owner));
        Mockito.when(userService.resolveMailLocaleFor(owner)).thenReturn(Locale.ENGLISH);
        Mockito.when(mailPublicUrls.absolutePath("/my-reservations/30/chat?role=owner"))
                .thenReturn("https://example.com/my-reservations/30/chat?role=owner");

        // 2.Exercise
        service.dispatchChatDigestEmails();

        // 3.Assert
        Assertions.assertEquals(1, sentDigests.size());
        final ReservationChatDigestEmailPayload payload = sentDigests.get(0);
        Assertions.assertEquals("o@test.com", payload.getRecipientEmail());
        Assertions.assertEquals(1, payload.getTotalMessageCount());
        Assertions.assertEquals(1, payload.getConversations().size());
        Assertions.assertEquals("Toyota Corolla", payload.getConversations().get(0).getVehicleLabel());
        Assertions.assertEquals(1, payload.getConversations().get(0).getMessages().size());
        Assertions.assertEquals("When can I pick up?", payload.getConversations().get(0).getMessages().get(0).getMessagePreview());
    }
}
