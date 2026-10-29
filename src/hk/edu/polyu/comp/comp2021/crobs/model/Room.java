package hk.edu.polyu.comp.comp2021.crobs.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a room available for booking in CROBS.
 * Encapsulates room properties and supported features.
 */
public class Room {

    private final String id;
    private RoomType type;
    private int capacity;
    private final Set<String> features = new HashSet<>();

    /**
     * Constructs a new Room.
     *
     * @param id       the unique room ID.
     * @param type     the type of the room.
     * @param capacity the maximum capacity of the room (must be > 0).
     * @param features the set of features supported by the room.
     */
    public Room(String id, RoomType type, int capacity, Set<String> features) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Room ID cannot be null or empty");
        }
        if (type == null) {
            throw new IllegalArgumentException("Room type cannot be null");
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("Room capacity must be greater than 0");
        }
        this.id = id.trim();
        this.type = type;
        this.capacity = capacity;
        if (features != null) {
            for (String f : features) {
                if (f != null && !f.trim().isEmpty()) {
                    this.features.add(f.trim());
                }
            }
        }
    }

    /**
     * Gets the unique room ID.
     *
     * @return room ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the room type.
     *
     * @return room type.
     */
    public RoomType getType() {
        return type;
    }

    /**
     * Updates the room type (REQ8).
     *
     * @param type new room type.
     */
    public void setType(RoomType type) {
        if (type == null) {
            throw new IllegalArgumentException("Room type cannot be null");
        }
        this.type = type;
    }

    /**
     * Gets the capacity of the room.
     *
     * @return capacity.
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Updates the room capacity (REQ8).
     *
     * @param capacity new capacity (> 0).
     */
    public void setCapacity(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Room capacity must be greater than 0");
        }
        this.capacity = capacity;
    }

    /**
     * Returns an unmodifiable view of room features to prevent external mutation.
     *
     * @return unmodifiable set of features.
     */
    public Set<String> getFeatures() {
        return Collections.unmodifiableSet(features);
    }

    /**
     * Updates the features set of the room (REQ8).
     *
     * @param newFeatures set of new features.
     */
    public void setFeatures(Set<String> newFeatures) {
        features.clear();
        if (newFeatures != null) {
            for (String f : newFeatures) {
                if (f != null && !f.trim().isEmpty()) {
                    features.add(f.trim());
                }
            }
        }
    }

    /**
     * Checks if this room satisfies a minimum capacity requirement (REQ13).
     *
     * @param minCapacity minimum required capacity.
     * @return true if capacity is greater than or equal to minCapacity.
     */
    public boolean matchesCapacity(int minCapacity) {
        return this.capacity >= minCapacity;
    }

    /**
     * Checks if this room contains all required features (REQ13).
     *
     * @param requiredFeatures set of features that must all be present.
     * @return true if all required features exist in this room.
     */
    public boolean hasAllFeatures(Set<String> requiredFeatures) {
        if (requiredFeatures == null || requiredFeatures.isEmpty()) {
            return true;
        }
        for (String f : requiredFeatures) {
            if (!features.contains(f)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Room)) {
            return false;
        }
        Room room = (Room) o;
        return Objects.equals(this.id, room.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    @Override
    public String toString() {
        return String.format("Room ID: %s, Type: %s, Capacity: %d, Features: %s",
                id, type, capacity, String.join(",", features));
    }
}
