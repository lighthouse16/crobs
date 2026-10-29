package hk.edu.polyu.comp.comp2021.crobs.model;

/**
 * Defines user roles and their associated system privileges in CROBS.
 */
public enum UserRole {

    /**
     * Superadmin with all privileges, including changing roles of other users.
     */
    SUPERADMIN {
        @Override
        public boolean canManageRooms() {
            return true;
        }

        @Override
        public boolean canManageUsers() {
            return true;
        }

        @Override
        public boolean canChangeRoles() {
            return true;
        }
    },

    /**
     * Administrator who can manage rooms and regular users.
     */
    ADMIN {
        @Override
        public boolean canManageRooms() {
            return true;
        }

        @Override
        public boolean canManageUsers() {
            return true;
        }

        @Override
        public boolean canChangeRoles() {
            return false;
        }
    },

    /**
     * Regular user who can only book rooms and view schedules.
     */
    REGULAR {
        @Override
        public boolean canManageRooms() {
            return false;
        }

        @Override
        public boolean canManageUsers() {
            return false;
        }

        @Override
        public boolean canChangeRoles() {
            return false;
        }
    };

    /**
     * Checks if this role can add, modify, or delete rooms.
     *
     * @return true if permitted, false otherwise.
     */
    public abstract boolean canManageRooms();

    /**
     * Checks if this role can add or remove regular users.
     *
     * @return true if permitted, false otherwise.
     */
    public abstract boolean canManageUsers();

    /**
     * Checks if this role can modify roles of other users.
     *
     * @return true if permitted, false otherwise.
     */
    public abstract boolean canChangeRoles();

    /**
     * Parses a string role name into a UserRole safely.
     *
     * @param roleStr string representation of role (e.g. "admin", "regular").
     * @return corresponding UserRole or null if invalid.
     */
    public static UserRole fromString(String roleStr) {
        if (roleStr == null) {
            return null;
        }
        for (UserRole r : values()) {
            if (r.name().equalsIgnoreCase(roleStr)) {
                return r;
            }
        }
        return null;
    }
}
