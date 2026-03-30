package ar.edu.itba.paw.models;

import java.util.Arrays;

public class Image {
    private final long id;
    private final String name;
    private final String contentType;
    private final byte[] data;

    public Image(final long id, final String name, final String contentType, final byte[] data) {
        this.id = id;
        this.name = name;
        this.contentType = contentType;
        this.data = data;
    }

    public String getContentType() {
        return contentType;
    }

    public String getName() {
        return name;
    }

    public long getId() {
        return id;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public String toString() {
        return "Image{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", contentType='" + contentType + '\'' +
                ", data=" + Arrays.toString(data) +
                '}';
    }
}
