package ar.edu.itba.paw.models;

public class User {
    private final long id;
    private final String email;
    private final String forename;
    private final String surname;

    public User(long id, String email, String forename, String surname){
        this.id = id;
        this.email = email;
        this.forename = forename;
        this.surname = surname;
    }

    public long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getForename() {
        return forename;
    }

    public String getSurname() {
        return surname;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", forename='" + forename + '\'' +
                ", surname='" + surname + '\'' +
                '}';
    }
}
