package ar.edu.itba.paw.services;

import java.util.Locale;

public interface EmailVerificationService {


    void issueNewCode(long userId, String email, Locale locale);


    long verifyEmailAndConsumeCode(String email, String code);
}
