package ar.edu.itba.paw.webapp.form;

public final class VerifyEmailForm {

    private String email = "";
    private String code = "";

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email != null ? email.trim() : "";
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code != null ? code.trim() : "";
    }
}
