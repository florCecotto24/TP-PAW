package ar.edu.itba.paw.webapp.dto;

import java.io.Serializable;

/**
 * Pictures from the publish car form saved in session when the POST fails,
 * so that a retry without choosing files again keeps the images.
 */
public record PublishCarRetainedImage(String filename, String contentType, byte[] data) implements Serializable {

    private static final long serialVersionUID = 1L;

    public PublishCarRetainedImage {
        data = data != null ? data : new byte[0];
    }
}
