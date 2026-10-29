package hk.edu.polyu.comp.comp2021.crobs.model;

import java.util.Objects;

/**
 * Represents a user in the CROBS system.
 * Implements identity comparison and hashing based on unique user ID.
 */
public class User {

    private final String id;
    private final String name;
    private UserRole role;

    /**
     * Constructs a new User instance.
     *
     * @param id   the unique user ID.
     * @param name the user's display name.
     * @param role the user's role and access level.
     */
    public User(String id, String name, UserRole role) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("User name cannot be null or empty");
        }
        this.id = id.trim();
        this.name = name.trim();
        this.role = (role != null) ? role : UserRole.REGULAR;
    }

    /**
     * Returns the unique identifier of the user.
     *
     * @return user ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the name of the user.
     *
     * @return user name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the role of the user.
     *
     * @return user role.
     */
    public UserRole getRole() {
        return role;
    }

    /**
     * Updates the user's access role (REQ4: changeRole).
     *
     * @param role the new role to assign.
     */
    public void setRole(UserRole role) {
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }
        this.role = role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof User)) {
            return false;
        }
        User user = (User) o;
        return Objects.equals(this.id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    @Override
    public String toString() {
        return String.format("User{id='%s', name='%s', role=%s}", id, name, role);
    }
}
