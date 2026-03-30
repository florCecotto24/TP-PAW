package ar.edu.itba.paw.models;

import java.util.Arrays;

public class Image {
    private final long id;
    private final String contentType;
    private final byte[] data;

    public Image(final long id, final String contentType, final byte[] data) {
        this.id = id;
        this.contentType = contentType;
        this.data = data;
    }

    public String getContentType() {
        return contentType;
    }

    public long getId() {
        return id;
    }

    public byte[] getData() {
        return data;
    }

    
}
