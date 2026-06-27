package ar.edu.itba.paw.webapp.validation.user;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ar.edu.itba.paw.webapp.form.user.UserPatchForm;
import ar.edu.itba.paw.webapp.validation.constraint.user.UserPatchHasField;

public final class UserPatchHasFieldValidator implements ConstraintValidator<UserPatchHasField, UserPatchForm> {

    @Override
    public boolean isValid(final UserPatchForm form, final ConstraintValidatorContext context) {
        if (form == null) {
            return false;
        }
        return form.getPassword() != null
                || form.getForename() != null
                || form.getSurname() != null
                || form.getPhoneNumber() != null
                || form.getBirthDate() != null
                || form.getAbout() != null
                || form.getCbu() != null
                || form.getLatestLocale() != null
                || form.getRole() != null
                || form.getBlocked() != null
                || form.getIdentityValidated() != null
                || form.getLicenseValidated() != null;
    }
}
