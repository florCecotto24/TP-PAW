package ar.edu.itba.paw.webapp.support;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.stereotype.Component;

import ar.edu.itba.paw.webapp.form.common.OctetStreamBodyForm;

/** Reads and validates octet-stream bodies via Bean Validation. */
@Component
public final class BinaryPayloadSupport {

    private final FormValidationSupport formValidationSupport;

    public BinaryPayloadSupport(final FormValidationSupport formValidationSupport) {
        this.formValidationSupport = formValidationSupport;
    }

    public byte[] readValidatedBody(final InputStream body) throws IOException {
        final byte[] bytes = body == null ? new byte[0] : body.readAllBytes();
        formValidationSupport.validate(OctetStreamBodyForm.of(bytes));
        return bytes;
    }
}
