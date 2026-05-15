package ar.edu.itba.paw.webapp.dto;

/** STOMP JSON body for posting a reservation chat message. */
public final class PostReservationMessageRequest {

    private String body;

    public PostReservationMessageRequest() {
        // For Jackson
    }

    public PostReservationMessageRequest(final String body) {
        this.body = body;
    }

    public String getBody() {
        return body;
    }

    public void setBody(final String body) {
        this.body = body;
    }
}
