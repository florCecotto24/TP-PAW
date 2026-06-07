package ar.edu.itba.paw.dto;

import java.util.List;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.car.Car;

/**
 * Immutable command consumed by {@code CarPublishingService} when an owner submits the publish
 * form. Captures every field needed to: resolve brand/model in the catalog, persist the car row,
 * upload gallery pictures, and optionally attach an insurance document.
 *
 * Insurance is intentionally exposed as a {@code (filename, contentType, bytes)} triple
 * because the controller reads it from either a fresh {@code MultipartFile} or the session
 * stash before delegating; the service does not need to know about either source.
 */
public final class PublishCarRequest {

    private final String brand;
    private final String model;
    private final Car.Type type;
    private final String plate;
    private final int year;
    private final Car.Powertrain powertrain;
    private final Car.Transmission transmission;
    private final String description;
    private final List<GalleryMediaUpload> galleryUploads;
    private final String insuranceFilename;
    private final String insuranceContentType;
    private final byte[] insuranceBytes;

    private PublishCarRequest(final Builder b) {
        this.brand = b.brand;
        this.model = b.model;
        this.type = b.type;
        this.plate = b.plate;
        this.year = b.year;
        this.powertrain = b.powertrain;
        this.transmission = b.transmission;
        this.description = b.description;
        this.galleryUploads = List.copyOf(b.galleryUploads);
        this.insuranceFilename = b.insuranceFilename;
        this.insuranceContentType = b.insuranceContentType;
        this.insuranceBytes = b.insuranceBytes;
    }

    public String getBrand() { return brand; }
    public String getModel() { return model; }
    public Car.Type getType() { return type; }
    public String getPlate() { return plate; }
    public int getYear() { return year; }
    public Car.Powertrain getPowertrain() { return powertrain; }
    public Car.Transmission getTransmission() { return transmission; }
    public String getDescription() { return description; }
    public List<GalleryMediaUpload> getGalleryUploads() { return galleryUploads; }
    public Optional<String> getInsuranceFilename() { return Optional.ofNullable(insuranceFilename); }
    public Optional<String> getInsuranceContentType() { return Optional.ofNullable(insuranceContentType); }
    public byte[] getInsuranceBytes() { return insuranceBytes; }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String brand;
        private String model;
        private Car.Type type;
        private String plate;
        private int year;
        private Car.Powertrain powertrain;
        private Car.Transmission transmission;
        private String description;
        private List<GalleryMediaUpload> galleryUploads = List.of();
        private String insuranceFilename;
        private String insuranceContentType;
        private byte[] insuranceBytes;

        public Builder brand(final String v) { this.brand = v; return this; }
        public Builder model(final String v) { this.model = v; return this; }
        public Builder type(final Car.Type v) { this.type = v; return this; }
        public Builder plate(final String v) { this.plate = v; return this; }
        public Builder year(final int v) { this.year = v; return this; }
        public Builder powertrain(final Car.Powertrain v) { this.powertrain = v; return this; }
        public Builder transmission(final Car.Transmission v) { this.transmission = v; return this; }
        public Builder description(final String v) { this.description = v; return this; }
        public Builder galleryUploads(final List<GalleryMediaUpload> v) { this.galleryUploads = v; return this; }
        public Builder insurance(final String filename, final String contentType, final byte[] bytes) {
            this.insuranceFilename = filename;
            this.insuranceContentType = contentType;
            this.insuranceBytes = bytes;
            return this;
        }

        public PublishCarRequest build() {
            return new PublishCarRequest(this);
        }
    }
}
