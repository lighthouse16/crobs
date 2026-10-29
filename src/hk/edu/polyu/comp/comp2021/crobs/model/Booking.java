package hk.edu.polyu.comp.comp2021.crobs.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Represents a room reservation made by a user in CROBS.
 * Contains time interval overlap logic and maintains reservation invariants.
 */
public class Booking {

    private final String id;
    private String roomId;
    private final String userId;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String purpose;

    /**
     * Constructs a new Booking.
     *
     * @param id        the unique booking ID.
     * @param roomId    the ID of the reserved room.
     * @param userId    the ID of the user who owns the booking.
     * @param date      the booking date.
     * @param startTime the start time of the reservation.
     * @param endTime   the end time of the reservation (must be after startTime).
     * @param purpose   the purpose of the reservation.
     */
    public Booking(String id, String roomId, String userId,
                   LocalDate date, LocalTime startTime, LocalTime endTime, String purpose) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Booking ID cannot be null or empty");
        }
        if (roomId == null || roomId.trim().isEmpty()) {
            throw new IllegalArgumentException("Room ID cannot be null or empty");
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Start time and end time cannot be null");
        }
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Start time must be strictly before end time");
        }
        this.id = id.trim();
        this.roomId = roomId.trim();
        this.userId = userId.trim();
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.purpose = (purpose != null) ? purpose.trim() : "";
    }

    /**
     * Gets the booking ID.
     *
     * @return booking ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the reserved room ID.
     *
     * @return room ID.
     */
    public String getRoomId() {
        return roomId;
    }

    /**
     * Sets the reserved room ID (REQ11).
     *
     * @param roomId new room ID.
     */
    public void setRoomId(String roomId) {
        if (roomId == null || roomId.trim().isEmpty()) {
            throw new IllegalArgumentException("Room ID cannot be null or empty");
        }
        this.roomId = roomId.trim();
    }

    /**
     * Gets the owner user ID.
     *
     * @return owner user ID.
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Gets the reservation date.
     *
     * @return date.
     */
    public LocalDate getDate() {
        return date;
    }

    /**
     * Sets the reservation date (REQ11).
     *
     * @param date new reservation date.
     */
    public void setDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }
        this.date = date;
    }

    /**
     * Gets the reservation start time.
     *
     * @return start time.
     */
    public LocalTime getStartTime() {
        return startTime;
    }

    /**
     * Gets the reservation end time.
     *
     * @return end time.
     */
    public LocalTime getEndTime() {
        return endTime;
    }

    /**
     * Sets new start and end times together to validate invariant (REQ11).
     *
     * @param startTime new start time.
     * @param endTime   new end time.
     */
    public void setTimeInterval(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Time values cannot be null");
        }
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Start time must be strictly before end time");
        }
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * Gets the booking purpose.
     *
     * @return purpose.
     */
    public String getPurpose() {
        return purpose;
    }

    /**
     * Sets the booking purpose (REQ11).
     *
     * @param purpose new purpose string.
     */
    public void setPurpose(String purpose) {
        this.purpose = (purpose != null) ? purpose.trim() : "";
    }

    /**
     * Checks if this booking conflicts with another room reservation.
     * Two bookings conflict if they refer to the same room on the same date and their time intervals overlap.
     *
     * @param otherRoomId   the target room ID.
     * @param otherDate     the target date.
     * @param otherStart    the target start time.
     * @param otherEnd      the target end time.
     * @return true if there is a conflict.
     */
    public boolean conflictsWith(String otherRoomId, LocalDate otherDate,
                                 LocalTime otherStart, LocalTime otherEnd) {
        if (!this.roomId.equalsIgnoreCase(otherRoomId) || !this.date.equals(otherDate)) {
            return false;
        }
        return this.endTime.isAfter(otherStart) && this.startTime.isBefore(otherEnd);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Booking)) {
            return false;
        }
        Booking booking = (Booking) o;
        return Objects.equals(this.id, booking.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    @Override
    public String toString() {
        return String.format("Booking ID: %s, Room: %s, User: %s, Date: %s, Time: %s-%s, Purpose: %s",
                id, roomId, userId, date, startTime, endTime, purpose);
    }
}
