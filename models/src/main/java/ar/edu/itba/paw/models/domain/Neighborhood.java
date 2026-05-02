package ar.edu.itba.paw.models.domain;

/** City neighborhood row for listing location pickers. */
public final class Neighborhood {
    private final long id;
    private final String name;

    public Neighborhood(final long id, final String name) {
        this.id = id;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
