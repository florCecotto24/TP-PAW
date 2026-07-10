package ar.edu.itba.paw.webapp.form.common;

import javax.validation.constraints.NotNull;

import ar.edu.itba.paw.webapp.validation.constraint.file.NotEmptyPayload;

/** Octet-stream request body wrapper for Bean Validation. */
@NotEmptyPayload
public final class OctetStreamBodyForm {

    @NotNull
    private byte[] bytes;

    public OctetStreamBodyForm() {
    }

    public static OctetStreamBodyForm of(final byte[] bytes) {
        final OctetStreamBodyForm form = new OctetStreamBodyForm();
        form.bytes = bytes;
        return form;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(final byte[] bytes) {
        this.bytes = bytes;
    }
}
