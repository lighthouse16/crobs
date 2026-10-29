package hk.edu.polyu.comp.comp2021.crobs.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * The core engine and Facade of the Campus Room Booking System (CROBS).
 * Manages in-memory data for users, rooms, and bookings.
 * Follows the MVC Model layer architecture.
 */
public class CROBS {

    private final Map<String, User> users = new HashMap<>();
    private final Map<String, Room> rooms = new HashMap<>();
    private final Map<String, Booking> bookings = new HashMap<>();

    private User currentUser = null;

    private int nextUserSeq = 1;
    private int nextRoomSeq = 1;
    private int nextBookingSeq = 1;

    /**
     * Initializes a new CROBS session.
     * Automatically registers the initial superadmin user as required by spec.
     */
    public CROBS() {
        // Automatically create a default superadmin user upon system startup
        String superadminId = generateUserId();
        User superadmin = new User(superadminId, "SuperAdmin", UserRole.SUPERADMIN);
        users.put(superadmin.getId().toLowerCase(), superadmin);
    }

    // ==========================================
    // ID Generator Helpers
    // ==========================================

    private String generateUserId() {
        return "u" + (nextUserSeq++);
    }

    private String generateRoomId() {
        return "r" + (nextRoomSeq++);
    }

    private String generateBookingId() {
        return "b" + (nextBookingSeq++);
    }

    // ==========================================
    // Authentication & State (REQ5, REQ6)
    // ==========================================

    /**
     * Returns the currently authenticated user in this session.
     *
     * @return current User or null if no user is logged in.
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Checks if a user is currently logged in.
     *
     * @return true if authenticated, false otherwise.
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * [REQ5] Logs in as an existing user.
     *
     * @param userId ID of the user trying to log in.
     * @throws CrobsException if user does not exist.
     */
    public void login(String userId) throws CrobsException {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be empty");
        }
        User user = users.get(userId.trim().toLowerCase());
        if (user == null) {
            throw new CrobsException("User ID not found: " + userId);
        }
        this.currentUser = user;
    }

    /**
     * [REQ6] Logs out the currently authenticated user.
     *
     * @throws CrobsException if no user is currently logged in.
     */
    public void logout() throws CrobsException {
        if (currentUser == null) {
            throw new CrobsException("No user is currently logged in.");
        }
        this.currentUser = null;
    }

    // ==========================================
    // User Management (REQ2, REQ3, REQ4)
    // ==========================================

    /**
     * [REQ2] Adds a new regular user. Only admin users can execute this.
     *
     * @param name name of the new user.
     * @return newly created User.
     * @throws CrobsException if current user lacks admin privileges or name is invalid.
     */
    public User addUser(String name) throws CrobsException {
        checkAdminAccess();
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("User name cannot be blank");
        }

        // Generate a unique user ID and create a new regular user
        String newId = generateUserId();
        User newUser = new User(newId, name.trim(), UserRole.REGULAR);
        users.put(newUser.getId().toLowerCase(), newUser);
        return newUser;
    }

    /**
     * [REQ3] Removes an existing regular user and cancels all bookings by that user.
     *
     * @param targetUserId ID of the regular user to remove.
     * @throws CrobsException if user lacks admin access or target user does not exist or target is not regular.
     */
    public void removeUser(String targetUserId) throws CrobsException {
        checkAdminAccess();
        if (targetUserId == null || targetUserId.trim().isEmpty()) {
            throw new IllegalArgumentException("Target user ID cannot be blank");
        }
        User target = users.get(targetUserId.trim().toLowerCase());
        if (target == null) {
            throw new CrobsException("Target user not found: " + targetUserId);
        }
        if (target.getRole() != UserRole.REGULAR) {
            throw new CrobsException("Cannot remove admin or superadmin user.");
        }

        users.remove(targetUserId.toLowerCase());
        List<Booking> userBookings = new ArrayList<>();
        for (Booking b : bookings.values()) {
            if (b.getUserId().equalsIgnoreCase(targetUserId)) {
                userBookings.add(b);
            }
        }
        for (Booking b : userBookings) {
            bookings.remove(b.getId());
        }
    }

    /**
     * [REQ4] Modifies the access privilege of another user. Only superadmin can call this.
     *
     * @param targetUserId ID of the target user.
     * @param newRole      the new role to assign.
     * @throws CrobsException if current user is not superadmin or target user not found.
     */
    public void changeRole(String targetUserId, UserRole newRole) throws CrobsException {
        if (currentUser == null || !currentUser.getRole().canChangeRoles()) {
            throw new CrobsException("Permission denied: Only superadmin can change user roles.");
        }
        if (targetUserId == null || newRole == null) {
            throw new IllegalArgumentException("Arguments cannot be null");
        }
        User target = users.get(targetUserId.trim().toLowerCase());
        if (target == null) {
            throw new CrobsException("User not found: " + targetUserId);
        }

        // Update target user's role
        target.setRole(newRole);
    }

    // ==========================================
    // Room Management (REQ7, REQ8, REQ9, REQ15, REQ16)
    // ==========================================

    /**
     * [REQ7] Adds a new room. Only admin users can execute this.
     *
     * @param roomType type of the room.
     * @param capacity room capacity.
     * @param features set of features.
     * @return newly created Room.
     * @throws CrobsException if current user is not admin.
     */
    public Room addRoom(RoomType roomType, int capacity, Set<String> features) throws CrobsException {
        checkAdminAccess();

        // Generate unique room ID and create new Room instance
        String roomId = generateRoomId();
        Room room = new Room(roomId, roomType, capacity, features);
        rooms.put(room.getId().toLowerCase(), room);
        return room;
    }

    /**
     * [REQ8] Modifies one field (roomType, capacity, or features) of a specified room.
     *
     * @param roomId   ID of target room.
     * @param field    field to modify ("type", "capacity", "features").
     * @param newValue new value as string.
     * @throws CrobsException if room not found or field/value is invalid.
     */
    public void modifyRoom(String roomId, String field, String newValue) throws CrobsException {
        checkAdminAccess();
        Room room = getRoomOrThrow(roomId);

        if ("type".equalsIgnoreCase(field)) {
            RoomType newType = RoomType.fromString(newValue);
            if (newType == null) {
                throw new CrobsException("Invalid room type: " + newValue);
            }
            room.setType(newType);
            return;
        }
        if ("capacity".equalsIgnoreCase(field)) {
            int newCap;
            try {
                newCap = Integer.parseInt(newValue);
            } catch (NumberFormatException e) {
                throw new CrobsException("Invalid capacity value: " + newValue);
            }
            if (newCap <= 0) {
                throw new CrobsException("Capacity must be greater than 0");
            }
            room.setCapacity(newCap);
            return;
        }
        if ("features".equalsIgnoreCase(field)) {
            String[] tokens = newValue.trim().split(",");
            Set<String> set = new HashSet<>();
            for (String feature : tokens) {
                if (!feature.trim().isEmpty()) {
                    set.add(feature.trim());
                }
            }
            room.setFeatures(set);
            return;
        }
        throw new CrobsException("Invalid field: " + field);
    }

    /**
     * [REQ9] Deletes a room and cancels all future bookings for that room.
     *
     * @param roomId ID of the room to delete.
     * @throws CrobsException if user is not admin or room not found.
     */
    public void deleteRoom(String roomId) throws CrobsException {
        checkAdminAccess();
        Room room = getRoomOrThrow(roomId);

        rooms.remove(room.getId());
        List<Booking> futureBookings = new ArrayList<>();
        for (Booking b : bookings.values()) {
            if (b.getRoomId().equalsIgnoreCase(room.getId()) && !b.getDate().isBefore(LocalDate.now())) {
                futureBookings.add(b);
            }
        }
        for (Booking b : futureBookings) {
            bookings.remove(b.getId());
        }
    }

    /**
     * [REQ15] Displays information about a single room.
     *
     * @param roomId room ID to inspect.
     * @return the Room instance.
     * @throws CrobsException if room does not exist.
     */
    public Room listRoom(String roomId) throws CrobsException {
        return getRoomOrThrow(roomId);
    }

    /**
     * [REQ16] Lists all rooms sorted in ascending order of room ID.
     *
     * @return sorted list of all rooms.
     */
    public List<Room> listAllRooms() {
        List<Room> roomList = new ArrayList<>(rooms.values());
        roomList.sort(Comparator.comparing(Room::getId));
        return Collections.unmodifiableList(roomList);
    }

    // ==========================================
    // Booking Management (REQ10, REQ11, REQ12, REQ17)
    // ==========================================

    /**
     * [REQ10] Creates a booking for the currently logged-in user.
     * Checks for timeslot conflict in the same room on the same date.
     *
     * @param roomId    target room ID.
     * @param date      date of reservation.
     * @param startTime start time.
     * @param endTime   end time.
     * @param purpose   reservation purpose.
     * @return newly created Booking.
     * @throws CrobsException if room not found, timeslot overlaps, or not logged in.
     */
    public Booking book(String roomId, LocalDate date, LocalTime startTime,
                        LocalTime endTime, String purpose) throws CrobsException {
        checkLoggedIn();
        getRoomOrThrow(roomId);

        for (Booking b : bookings.values()) {
            if (b.conflictsWith(roomId, date, startTime, endTime)) {
                throw new CrobsException("Time conflict with existing booking.");
            }
        }
        Booking newBooking = new Booking(generateBookingId(), roomId, currentUser.getId(), date, startTime, endTime, purpose);
        bookings.put(newBooking.getId(), newBooking);
        return newBooking;
    }

    /**
     * [REQ11] Modifies an existing booking owned by the currently logged-in user.
     *
     * @param bookingId booking to update.
     * @param roomId    new room ID.
     * @param date      new date.
     * @param startTime new start time.
     * @param endTime   new end time.
     * @param purpose   new purpose.
     * @throws CrobsException if booking not owned by user, or conflicts with others.
     */
    public void modifyBooking(String bookingId, String roomId, LocalDate date,
                              LocalTime startTime, LocalTime endTime, String purpose) throws CrobsException {
        checkLoggedIn();
        Booking booking = getBookingOrThrow(bookingId);

        if (!booking.getUserId().equalsIgnoreCase(currentUser.getId())) {
            throw new CrobsException("Permission denied: You can only modify your own bookings.");
        }
        getRoomOrThrow(roomId);

        for (Booking b : bookings.values()) {
            if (!b.getId().equalsIgnoreCase(bookingId) && b.conflictsWith(roomId, date, startTime, endTime)) {
                throw new CrobsException("Time conflict with existing booking.");
            }
        }
        booking.setRoomId(roomId);
        booking.setDate(date);
        booking.setTimeInterval(startTime, endTime);
        booking.setPurpose(purpose);
    }

    /**
     * [REQ12] Deletes or cancels a booking owned by the currently logged-in user.
     *
     * @param bookingId booking ID to cancel.
     * @throws CrobsException if booking not found or not owned by user.
     */
    public void deleteBooking(String bookingId) throws CrobsException {
        checkLoggedIn();
        Booking booking = getBookingOrThrow(bookingId);
        if (!booking.getUserId().equalsIgnoreCase(currentUser.getId())) {
            throw new CrobsException("Permission denied: You can only delete your own bookings.");
        }
        bookings.remove(booking.getId());
    }

    /**
     * [REQ17] Lists all bookings made by the current user, sorted by date and start time.
     *
     * @return sorted list of bookings owned by currentUser.
     * @throws CrobsException if not logged in.
     */
    public List<Booking> listBookings() throws CrobsException {
        checkLoggedIn();

        List<Booking> bookingList = new ArrayList<>();
        for (Booking b : bookings.values()) {
            if (b.getUserId().equalsIgnoreCase(currentUser.getId())) {
                bookingList.add(b);
            }
        }
        bookingList.sort(Comparator.comparing(Booking::getDate).thenComparing(Booking::getStartTime));
        return Collections.unmodifiableList(bookingList);
    }

    // ==========================================
    // Search & Schedule (REQ13, REQ14)
    // ==========================================

    /**
     * [REQ13] Searches for available rooms at a specific timeslot with desired features and minimum capacity.
     *
     * @param date        target reservation date.
     * @param startTime   target start time.
     * @param endTime     target end time.
     * @param minCapacity minimum room capacity.
     * @param features    set of required features.
     * @return list of matching available rooms.
     */
    public List<Room> search(LocalDate date, LocalTime startTime, LocalTime endTime,
                             int minCapacity, Set<String> features) {
        List<Room> result = new ArrayList<>();
        for (Room r : rooms.values()) {
            if (r.matchesCapacity(minCapacity) && r.hasAllFeatures(features)) {
                boolean hasConflict = false;
                for (Booking b : bookings.values()) {
                    if (b.conflictsWith(r.getId(), date, startTime, endTime)) {
                        hasConflict = true;
                        break;
                    }
                }
                if (!hasConflict) {
                    result.add(r);
                }
            }
        }
        result.sort(Comparator.comparing(Room::getId));
        return result;
    }

    /**
     * [REQ14] Lists all active bookings for a room in the 7-day period starting from weekStartDate.
     * Sorted by date and start time.
     *
     * @param roomId        room ID to query.
     * @param weekStartDate the start date of the 7-day period.
     * @return sorted list of active bookings in the 7-day range.
     * @throws CrobsException if room not found.
     */
    public List<Booking> schedule(String roomId, LocalDate weekStartDate) throws CrobsException {
        checkLoggedIn();
        Room room = getRoomOrThrow(roomId);
        LocalDate weekEndDate = weekStartDate.plusDays(7);

        List<Booking> result = new ArrayList<>();
        for (Booking b : bookings.values()) {
            if (b.getRoomId().equalsIgnoreCase(room.getId())
                    && b.getDate().isBefore(weekEndDate)
                    && !b.getDate().isBefore(weekStartDate)) {
                result.add(b);
            }
        }
        result.sort(Comparator.comparing(Booking::getDate).thenComparing(Booking::getStartTime));
        return result;
    }

    // ==========================================
    // Internal Helper Checks
    // ==========================================

    private void checkLoggedIn() throws CrobsException {
        if (currentUser == null) {
            throw new CrobsException("Operation rejected: No user is currently logged in.");
        }
    }

    private void checkAdminAccess() throws CrobsException {
        checkLoggedIn();
        if (!currentUser.getRole().canManageRooms() && !currentUser.getRole().canManageUsers()) {
            throw new CrobsException("Permission denied: Admin privileges required.");
        }
    }

    private Room getRoomOrThrow(String roomId) throws CrobsException {
        if (roomId == null) throw new IllegalArgumentException("Room ID cannot be null");
        Room room = rooms.get(roomId.trim().toLowerCase());
        if (room == null) {
            throw new CrobsException("Room not found: " + roomId);
        }
        return room;
    }

    private Booking getBookingOrThrow(String bookingId) throws CrobsException {
        if (bookingId == null) throw new IllegalArgumentException("Booking ID cannot be null");
        Booking booking = bookings.get(bookingId.trim().toLowerCase());
        if (booking == null) {
            throw new CrobsException("Booking not found: " + bookingId);
        }
        return booking;
    }
}
