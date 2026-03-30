package ar.edu.itba.paw.services;

import java.io.IOException;
import java.util.Locale;

import javax.mail.MessagingException;

public interface EmailService {

    void sendTextMail(String recipientName, String recipientEmail, Locale locale) throws MessagingException;

    void sendSimpleMail(String recipientName, String recipientEmail, Locale locale) throws MessagingException;

    void sendMailWithAttachment(String recipientName, String recipientEmail, String attachmentFileName,
            byte[] attachmentBytes, String attachmentContentType, Locale locale) throws MessagingException;

    void sendMailWithInline(String recipientName, String recipientEmail, String imageResourceName,
            byte[] imageBytes, String imageContentType, Locale locale) throws MessagingException;

    String getEditableMailTemplate() throws IOException;

    void sendEditableMail(String recipientName, String recipientEmail, String htmlContent, Locale locale)
            throws MessagingException;

}
