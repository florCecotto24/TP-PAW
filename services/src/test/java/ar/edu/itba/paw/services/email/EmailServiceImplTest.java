package ar.edu.itba.paw.services.email;


import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import ar.edu.itba.paw.models.email.reservation.ReservationMailPayload;
import ar.edu.itba.paw.mail.MailDispatchSupport;
import ar.edu.itba.paw.mail.MailPublicUrls;
import ar.edu.itba.paw.policy.ReservationTimingPolicy;

@ExtendWith(MockitoExtension.class)
public class EmailServiceImplTest {

    private record SentMail(String to, String subject) {
    }

    private static final String TEMPLATE_RIDER = "html/reservation-confirmation-rider";
    private static final String TEMPLATE_OWNER = "html/reservation-confirmation-owner";
    private static final String SUBJECT_KEY_RIDER = "mail.reservationRequestSent.subject";
    private static final String SUBJECT_KEY_OWNER = "mail.reservationRequestReceived.subject";

    @Mock
    private Environment environment;

    @Mock
    private MailPublicUrls mailPublicUrls;

    @Mock
    private JavaMailSender mailSender;

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

    // Use a real MailDispatchSupport wired with mock JavaMail/Environment/MessageSource to exercise
    // the full pipeline (template render -> sendEmail) while keeping the test focused on the
    // EmailServiceImpl orchestration logic.
    private MailDispatchSupport mailDispatch;

    private EmailServiceImpl emailService;

    @BeforeEach
    public void setUp() {
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

    private static Answer<Void> recordSentInto(final List<SentMail> out) {
        return invocation -> {
            final MimeMessage message = invocation.getArgument(0);
            try {
                out.add(new SentMail(
                        ((InternetAddress) message.getRecipients(Message.RecipientType.TO)[0]).getAddress(),
                        message.getSubject()));
            } catch (final MessagingException e) {
                throw new AssertionError(e);
            }
            return null;
        };
    }

    @Test
    public void testSendReservationConfirmationEmail() {
        // 1. Arrange
        Mockito.when(mailPublicUrls.absolutePath(Mockito.anyString()))
                .thenAnswer(invocation -> "http://localhost" + invocation.getArgument(0));
        final List<SentMail> sent = new ArrayList<>();
        Mockito.doAnswer(recordSentInto(sent)).when(mailSender).send(Mockito.any(MimeMessage.class));
        Mockito.when(mailSender.createMimeMessage())
                .thenAnswer(invocation -> new MimeMessage(Session.getDefaultInstance(new Properties())));

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
        Mockito.when(emailMessageSource.getMessage(Mockito.eq(SUBJECT_KEY_RIDER), Mockito.any(), Mockito.any(Locale.class)))
                .thenReturn(subjectRider);
        Mockito.when(emailMessageSource.getMessage(Mockito.eq(SUBJECT_KEY_OWNER), Mockito.any(), Mockito.any(Locale.class)))
                .thenReturn(subjectOwner);
        Mockito.when(htmlTemplateEngine.process(Mockito.eq(TEMPLATE_RIDER), Mockito.any(Context.class)))
                .thenReturn("<html>rider</html>");
        Mockito.when(htmlTemplateEngine.process(Mockito.eq(TEMPLATE_OWNER), Mockito.any(Context.class)))
                .thenReturn("<html>owner</html>");

        // 2. Execute
        emailService.sendReservationConfirmationEmail(payload);

        // 3. Assert
        Assertions.assertIterableEquals(
                List.of(new SentMail(riderEmail, subjectRider), new SentMail(ownerEmail, subjectOwner)),
                sent);
    }


}
