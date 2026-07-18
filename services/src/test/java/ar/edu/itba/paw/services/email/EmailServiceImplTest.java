package ar.edu.itba.paw.services.email;


import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import ar.edu.itba.paw.models.email.reservation.ReservationMailPayload;
import ar.edu.itba.paw.mail.MailDispatchSupport;
import ar.edu.itba.paw.mail.MailPublicUrls;
import ar.edu.itba.paw.policy.ReservationTimingPolicy;
import ar.edu.itba.paw.services.support.RecordingJavaMailSender;

@ExtendWith(MockitoExtension.class)
public class EmailServiceImplTest {

    private record SentMail(String to, String subject) { }

    private static final String TEMPLATE_RIDER = "html/reservation-confirmation-rider";
    private static final String TEMPLATE_OWNER = "html/reservation-confirmation-owner";
    private static final String SUBJECT_KEY_RIDER = "mail.reservationRequestSent.subject";
    private static final String SUBJECT_KEY_OWNER = "mail.reservationRequestReceived.subject";

    @Mock
    private Environment environment;

    @Mock
    private MailPublicUrls mailPublicUrls;

    @Mock
    private MessageSource emailMessageSource;

    @Mock
    private TemplateEngine htmlTemplateEngine;

    @Mock
    private ReservationTimingPolicy reservationTimingPolicy;

    @Mock
    private UserAccountEmailService userAccountEmailService;

    @Mock
    private OwnerListingEmailService ownerListingEmailService;

    // State-based fake mail sender per AGENTS.md rule TEST-8: replaces the previous Mockito mock
    // + doAnswer(recordSentInto(...)) captor over the JavaMailSender. The fake also produces real
    // MimeMessage instances so the production pipeline runs end-to-end against test inputs.
    private RecordingJavaMailSender mailSender;
    private MailDispatchSupport mailDispatch;
    private EmailServiceImpl emailService;

    @BeforeEach
    public void setUp() {
        mailSender = new RecordingJavaMailSender();
        mailDispatch = new MailDispatchSupport(environment, mailSender, emailMessageSource, mailPublicUrls);
        emailService = new EmailServiceImpl(
                mailDispatch,
                mailPublicUrls,
                emailMessageSource,
                htmlTemplateEngine,
                reservationTimingPolicy,
                userAccountEmailService,
                ownerListingEmailService);
    }

    private static SentMail toSentMail(final MimeMessage message) {
        try {
            return new SentMail(
                    ((InternetAddress) message.getRecipients(Message.RecipientType.TO)[0]).getAddress(),
                    message.getSubject());
        } catch (final MessagingException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void testSendReservationConfirmationEmail() {
        Mockito.when(mailPublicUrls.absolutePath(Mockito.eq("/")))
                .thenReturn("http://localhost/");
        Mockito.when(mailPublicUrls.absolutePathWithSelf(
                        Mockito.eq("/my-reservations/1"), Mockito.eq("reservations"), Mockito.eq(1L)))
                .thenReturn("http://localhost/my-reservations/1?self=%2Freservations%2F1");
        Mockito.when(mailPublicUrls.absolutePathWithSelf(
                        Mockito.eq("/my-cars/car/10"), Mockito.eq("cars"), Mockito.eq(10L)))
                .thenReturn("http://localhost/my-cars/car/10?self=%2Fcars%2F10");

        final Locale locale = Locale.ENGLISH;
        final String riderEmail = "rider@test.com";
        final String riderName = "Rider Name";
        final long reservationId = 1L;
        final long carId = 10L;
        final String vehicleLabel = "brandTest modelTest";
        final OffsetDateTime startDate = OffsetDateTime.parse("2026-05-01T10:00:00Z");
        final OffsetDateTime endDate = OffsetDateTime.parse("2026-05-03T18:00:00Z");
        final String riderHandoverLocation = "pickupStreetTest";
        final String ownerName = "Owner Name";
        final String ownerEmail = "owner@test.com";
        final String subjectRider = "Subject rider brandTest modelTest";
        final String subjectOwner = "Subject owner brandTest modelTest";
        final String reservationTotal = "100.00";
        final String ownerCbu = "1234567890123456789012";
        final ReservationMailPayload payload = ReservationMailPayload.builder()
                .recipientEmail(riderEmail)
                .riderFullName(riderName)
                .reservationId(reservationId)
                .carId(carId)
                .vehicleLabel(vehicleLabel)
                .startDate(startDate)
                .endDate(endDate)
                .riderHandoverLocation(riderHandoverLocation)
                .ownerHandoverLocation(riderHandoverLocation + " (owner full)")
                .ownerFullName(ownerName)
                .ownerEmail(ownerEmail)
                .reservationTotal(reservationTotal)
                .riderMailLocale(locale)
                .ownerMailLocale(locale)
                .ownerCbu(ownerCbu)
                .build();

        Mockito.when(environment.getProperty("mail.server.username", "noreply@localhost"))
                .thenReturn("noreply@localhost");
        Mockito.when(environment.getProperty("mail.from.address", "noreply@localhost"))
                .thenReturn("noreply@app.test");
        Mockito.when(emailMessageSource.getMessage(
                        Mockito.eq(SUBJECT_KEY_RIDER), Mockito.eq(new Object[] { vehicleLabel }), Mockito.eq(locale)))
                .thenReturn(subjectRider);
        Mockito.when(emailMessageSource.getMessage(
                        Mockito.eq(SUBJECT_KEY_OWNER), Mockito.eq(new Object[] { vehicleLabel }), Mockito.eq(locale)))
                .thenReturn(subjectOwner);
        Mockito.when(htmlTemplateEngine.process(Mockito.eq(TEMPLATE_RIDER), Mockito.isA(Context.class)))
                .thenReturn("<html>rider</html>");
        Mockito.when(htmlTemplateEngine.process(Mockito.eq(TEMPLATE_OWNER), Mockito.isA(Context.class)))
                .thenReturn("<html>owner</html>");

        emailService.sendReservationConfirmationEmail(payload);

        final List<SentMail> sent = mailSender.sent().stream().map(EmailServiceImplTest::toSentMail).toList();
        Assertions.assertIterableEquals(
                List.of(new SentMail(riderEmail, subjectRider), new SentMail(ownerEmail, subjectOwner)),
                sent);
    }
}
