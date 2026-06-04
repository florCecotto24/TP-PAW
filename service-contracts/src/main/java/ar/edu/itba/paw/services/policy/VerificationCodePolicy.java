package ar.edu.itba.paw.services.policy;

import java.util.regex.Pattern;

/**
 * Single source of truth for the length / shape of numeric verification codes (email verify, forgot password).
 * Length comes from {@code app.validation.verification-code-length}; the digits-only regex is derived from it
 * so JSP {@code maxlength}, the {@link Pattern} and the form validator never drift apart.
 */
public final class VerificationCodePolicy {

    private final int codeLength;
    private final Pattern codePattern;

    public static VerificationCodePolicy fromValidatedCodeLength(final int codeLength) {
        if (codeLength < 1) {
            throw new IllegalArgumentException(
                    "app.validation.verification-code-length must be >= 1, got " + codeLength);
        }
        return new VerificationCodePolicy(codeLength, Pattern.compile("[0-9]{" + codeLength + "}"));
    }

    private VerificationCodePolicy(final int codeLength, final Pattern codePattern) {
        this.codeLength = codeLength;
        this.codePattern = codePattern;
    }

    public int getCodeLength() {
        return codeLength;
    }

    public Pattern getCodePattern() {
        return codePattern;
    }

    public String getCodePatternString() {
        return codePattern.pattern();
    }
}
