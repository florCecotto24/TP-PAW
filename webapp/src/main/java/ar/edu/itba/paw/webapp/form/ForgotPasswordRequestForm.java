package ar.edu.itba.paw.webapp.form;

public final class ForgotPasswordRequestForm {

    private String email = "";

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email != null ? email.trim() : "";
    }
}
