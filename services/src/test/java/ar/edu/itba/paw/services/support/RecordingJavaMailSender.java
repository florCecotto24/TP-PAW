package ar.edu.itba.paw.services.support;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.MessagingException;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;

/**
 * State-based test double for {@link JavaMailSender}. Each {@code send(MimeMessage)} invocation
 * appends the message to {@link #sent()}; {@link #createMimeMessage()} builds plain
 * {@link MimeMessage} instances backed by a default {@link Session} (matches the testing
 * recipe Spring's javadoc recommends for {@code JavaMailSender}). Replaces the previous
 * {@code Mockito.doAnswer(recordSentInto(list)).when(mailSender).send(...)} captor banned by
 * AGENTS.md rule TEST-8.
 */
public class RecordingJavaMailSender implements JavaMailSender {

    private final Session session = Session.getDefaultInstance(new Properties());
    private final List<MimeMessage> sent = new ArrayList<>();

    public List<MimeMessage> sent() { return sent; }

    @Override
    public MimeMessage createMimeMessage() {
        return new MimeMessage(session);
    }

    @Override
    public MimeMessage createMimeMessage(final InputStream contentStream) {
        try {
            return new MimeMessage(session, contentStream);
        } catch (final MessagingException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void send(final MimeMessage mimeMessage) {
        sent.add(mimeMessage);
    }

    @Override
    public void send(final MimeMessage... mimeMessages) {
        for (final MimeMessage mimeMessage : mimeMessages) {
            sent.add(mimeMessage);
        }
    }

    @Override
    public void send(final MimeMessagePreparator mimeMessagePreparator) {
        try {
            final MimeMessage message = createMimeMessage();
            mimeMessagePreparator.prepare(message);
            sent.add(message);
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void send(final MimeMessagePreparator... mimeMessagePreparators) {
        for (final MimeMessagePreparator preparator : mimeMessagePreparators) {
            send(preparator);
        }
    }

    @Override
    public void send(final SimpleMailMessage simpleMessage) {
        throw new UnsupportedOperationException("SimpleMailMessage not used in EmailServiceImpl");
    }

    @Override
    public void send(final SimpleMailMessage... simpleMessages) {
        throw new UnsupportedOperationException("SimpleMailMessage not used in EmailServiceImpl");
    }
}
