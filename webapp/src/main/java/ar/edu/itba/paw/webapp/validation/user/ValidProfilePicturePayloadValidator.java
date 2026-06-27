package ar.edu.itba.paw.webapp.validation.user;

import java.util.Locale;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.models.domain.file.Image;
import ar.edu.itba.paw.services.file.ImageService;
import ar.edu.itba.paw.webapp.form.user.ProfilePictureRestForm;
import ar.edu.itba.paw.webapp.validation.constraint.user.ValidProfilePicturePayload;

@Component
public final class ValidProfilePicturePayloadValidator
        implements ConstraintValidator<ValidProfilePicturePayload, ProfilePictureRestForm> {

    private final ImageService imageService;
    private final MessageSource messageSource;

    @Autowired
    public ValidProfilePicturePayloadValidator(
            final ImageService imageService,
            final MessageSource messageSource) {
        this.imageService = imageService;
        this.messageSource = messageSource;
    }

    @Override
    public boolean isValid(final ProfilePictureRestForm form, final ConstraintValidatorContext context) {
        if (form == null || form.getBytes() == null || form.getBytes().length == 0) {
            return false;
        }
        final String contentType = form.getContentType();
        if (contentType == null || !Image.isImageContentType(contentType)) {
            emit(context, "profile.picture.notImage");
            return false;
        }
        final long max = imageService.getMaxImageBytes();
        if (form.getBytes().length > max) {
            final long maxMb = imageService.getMaxImageMegabytesRoundedUp();
            context.disableDefaultConstraintViolation();
            final Locale locale = LocaleContextHolder.getLocale();
            final String message = messageSource.getMessage(
                    MessageKeys.IMAGE_FILE_TOO_LARGE,
                    new Object[] { maxMb },
                    MessageKeys.IMAGE_FILE_TOO_LARGE,
                    locale);
            context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
            return false;
        }
        return true;
    }

    private static void emit(final ConstraintValidatorContext context, final String key) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate("{" + key + "}").addConstraintViolation();
    }
}
