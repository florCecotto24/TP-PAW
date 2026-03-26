package ar.edu.itba.paw.models;

public class Car {
    public enum Type {
        SEDAN, HATCHBACK, SUV, COUPE, CONVERTIBLE, WAGON, VAN, PICKUP
    }

    public enum Powertrain {
        GASOLINE, DIESEL, ELECTRIC, HYBRID
    }

    public enum Trasnmission {
        MANUAL, AUTOMATIC, SEMI_AUTOMATIC
    }

    private final long id;
    private final long ownerId;
    private final String plate;
    private final String brand;
    private final String model;
    private final Type type;
    private final Powertrain powertrain;
    private final Trasnmission trasnmission;


    public Car(long id, long ownerId, String plate, String brand, String model, Type type, Powertrain powertrain, Trasnmission trasnmission) {
        this.id = id;
        this.ownerId = ownerId;
        this.plate = plate;
        this.brand = brand;
        this.model = model;
        this.type = type;
        this.powertrain = powertrain;
        this.trasnmission = trasnmission;
    }

    public long getId() {
        return id;
    }

    public long getOwnerId() {
        return ownerId;
    }

    public String getPlate() {
        return plate;
    }

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }

    public Type getType() {
        return type;
    }

    public Powertrain getPowertrain() {
        return powertrain;
    }

    public Trasnmission getTrasnmission() {
        return trasnmission;
    }

    @Override
    public String toString() {
        return "Car{" +
                "id=" + id +
                ", ownerId=" + ownerId +
                ", plate='" + plate + '\'' +
                ", brand='" + brand + '\'' +
                ", model='" + model + '\'' +
                ", type=" + type +
                ", powertrain=" + powertrain +
                ", trasnmission=" + trasnmission +
                '}';
    }
}
