package ar.edu.itba.paw.policy;

import java.time.Duration;
import java.util.regex.Pattern;

/**
 * Single source of truth for the length / shape / lifetime of numeric verification codes
 * (email verify, forgot password). Length comes from {@code app.validation.verification-code-length};
 * TTL from {@code app.validation.otp-code-ttl-minutes}. The digits-only regex is derived from length
 * so JSP {@code maxlength}, the {@link Pattern} and the form validator never drift apart.
 */
public final class VerificationCodePolicy {

    private final int codeLength;
    private final Pattern codePattern;
    private final Duration codeTtl;

    public static VerificationCodePolicy fromValidatedConfiguration(final int codeLength, final int ttlMinutes) {
        if (codeLength < 1) {
            throw new IllegalArgumentException(
                    "app.validation.verification-code-length must be >= 1, got " + codeLength);
        }
        if (ttlMinutes < 1) {
            throw new IllegalArgumentException(
                    "app.validation.otp-code-ttl-minutes must be >= 1, got " + ttlMinutes);
        }
        return new VerificationCodePolicy(
                codeLength,
                Pattern.compile("[0-9]{" + codeLength + "}"),
                Duration.ofMinutes(ttlMinutes));
    }

    private VerificationCodePolicy(final int codeLength, final Pattern codePattern, final Duration codeTtl) {
        this.codeLength = codeLength;
        this.codePattern = codePattern;
        this.codeTtl = codeTtl;
    }

    public int getCodeLength() {
        return codeLength;
    }

    public Pattern getCodePattern() {
        return codePattern;
    }

    public Duration getCodeTtl() {
        return codeTtl;
    }

    public String getCodePatternString() {
        return codePattern.pattern();
    }
}
