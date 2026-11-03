package hk.edu.polyu.comp.comp2021.crobs.model;

import hk.edu.polyu.comp.comp2021.crobs.command.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * The core engine and Facade of the Campus Room Booking System (CROBS).
 * Manages in-memory data for users, rooms, and bookings.
 * Follows the MVC Model layer architecture and supports undo/redo via Command Pattern (BON2).
 */
public class CROBS {

    private final Map<String, User> users = new HashMap<>();
    private final Map<String, Room> rooms = new HashMap<>();
    private final Map<String, Booking> bookings = new HashMap<>();

    private final UndoRedoManager undoRedoManager = new UndoRedoManager();

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
        return snapshot(currentUser);
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
        recordCommand(new AddUserCommand(this, newUser), UndoRedoManager.Permission.ADMIN);
        return snapshot(newUser);
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
        String targetId = targetUserId.trim().toLowerCase();
        User target = users.get(targetId);
        if (target == null) {
            throw new CrobsException("Target user not found: " + targetUserId);
        }
        if (target.getRole() != UserRole.REGULAR) {
            throw new CrobsException("Cannot remove admin or superadmin user.");
        }

        users.remove(targetId);
        List<Booking> userBookings = new ArrayList<>();
        for (Booking b : bookings.values()) {
            if (b.getUserId().equalsIgnoreCase(targetId)) {
                userBookings.add(b);
            }
        }
        for (Booking b : userBookings) {
            bookings.remove(b.getId().toLowerCase());
        }
        recordCommand(new RemoveUserCommand(this, target, userBookings), UndoRedoManager.Permission.ADMIN);
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
        String targetId = targetUserId.trim().toLowerCase();
        User target = users.get(targetId);
        if (target == null) {
            throw new CrobsException("User not found: " + targetUserId);
        }

        UserRole oldRole = target.getRole();
        target.setRole(newRole);
        recordCommand(new ChangeRoleCommand(this, targetId, oldRole, newRole), UndoRedoManager.Permission.SUPERADMIN);
    }

    /**
     * Lists all users in ascending order of user ID.
     *
     * @return sorted list of all registered users.
     */
    public List<User> listAllUsers() {
        List<User> userList = new ArrayList<>();
        for (User user : users.values()) {
            userList.add(snapshot(user));
        }
        userList.sort(Comparator.comparing(User::getId));
        return Collections.unmodifiableList(userList);
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
        recordCommand(new AddRoomCommand(this, room), UndoRedoManager.Permission.ADMIN);
        return snapshot(room);
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

        String oldValue;
        if ("type".equalsIgnoreCase(field)) {
            oldValue = room.getType().name();
            RoomType newType = RoomType.fromString(newValue);
            if (newType == null) {
                throw new CrobsException("Invalid room type: " + newValue);
            }
            room.setType(newType);
        } else if ("capacity".equalsIgnoreCase(field)) {
            oldValue = String.valueOf(room.getCapacity());
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
        } else if ("features".equalsIgnoreCase(field)) {
            oldValue = String.join(",", room.getFeatures());
            String[] tokens = newValue.trim().split(",");
            Set<String> set = new HashSet<>();
            for (String feature : tokens) {
                if (!feature.trim().isEmpty()) {
                    set.add(feature.trim());
                }
            }
            room.setFeatures(set);
        } else {
            throw new CrobsException("Invalid field: " + field);
        }

        recordCommand(new ModifyRoomCommand(this, room.getId(), field, oldValue, newValue), UndoRedoManager.Permission.ADMIN);
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

        rooms.remove(room.getId().toLowerCase());
        List<Booking> futureBookings = new ArrayList<>();
        for (Booking b : bookings.values()) {
            if (b.getRoomId().equalsIgnoreCase(room.getId()) && !b.getDate().isBefore(LocalDate.now())) {
                futureBookings.add(b);
            }
        }
        for (Booking b : futureBookings) {
            bookings.remove(b.getId().toLowerCase());
        }
        recordCommand(new DeleteRoomCommand(this, room, futureBookings), UndoRedoManager.Permission.ADMIN);
    }

    /**
     * [REQ15] Displays information about a single room.
     *
     * @param roomId room ID to inspect.
     * @return the Room instance.
     * @throws CrobsException if room does not exist.
     */
    public Room listRoom(String roomId) throws CrobsException {
        return snapshot(getRoomOrThrow(roomId));
    }

    /**
     * [REQ16] Lists all rooms sorted in ascending order of room ID.
     *
     * @return sorted list of all rooms.
     */
    public List<Room> listAllRooms() {
        List<Room> roomList = new ArrayList<>();
        for (Room room : rooms.values()) {
            roomList.add(snapshot(room));
        }
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
        String normalizedRoomId = normalizeId(roomId, "Room ID");
        Room room = getRoomOrThrow(normalizedRoomId);
        validateBookingValues(date, startTime, endTime);

        for (Booking b : bookings.values()) {
            if (b.conflictsWith(room.getId(), date, startTime, endTime)) {
                throw new CrobsException("Time conflict with existing booking.");
            }
        }
        Booking newBooking = new Booking(generateBookingId(), room.getId(), currentUser.getId(),
                date, startTime, endTime, purpose);
        bookings.put(newBooking.getId().toLowerCase(), newBooking);
        recordCommand(new BookCommand(this, newBooking), UndoRedoManager.Permission.LOGGED_IN);
        return snapshot(newBooking);
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
        String normalizedRoomId = normalizeId(roomId, "Room ID");
        Room room = getRoomOrThrow(normalizedRoomId);
        validateBookingValues(date, startTime, endTime);

        for (Booking b : bookings.values()) {
            if (!b.getId().equalsIgnoreCase(bookingId)
                    && b.conflictsWith(room.getId(), date, startTime, endTime)) {
                throw new CrobsException("Time conflict with existing booking.");
            }
        }

        String oldRoomId = booking.getRoomId();
        LocalDate oldDate = booking.getDate();
        LocalTime oldStartTime = booking.getStartTime();
        LocalTime oldEndTime = booking.getEndTime();
        String oldPurpose = booking.getPurpose();

        booking.update(room.getId(), date, startTime, endTime, purpose);

        recordCommand(new ModifyBookingCommand(
                this, bookingId,
                oldRoomId, oldDate, oldStartTime, oldEndTime, oldPurpose,
                room.getId(), date, startTime, endTime, purpose),
                UndoRedoManager.Permission.LOGGED_IN);
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
        bookings.remove(booking.getId().toLowerCase());
        recordCommand(new DeleteBookingCommand(this, booking), UndoRedoManager.Permission.LOGGED_IN);
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
                bookingList.add(snapshot(b));
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
                    result.add(snapshot(r));
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
        if (weekStartDate == null) {
            throw new IllegalArgumentException("Week start date cannot be null");
        }
        LocalDate weekEndDate = weekStartDate.plusDays(7);
        boolean isAdmin = currentUser != null && currentUser.getRole() != UserRole.REGULAR;
        String currentId = currentUser != null ? currentUser.getId() : "";

        List<Booking> result = new ArrayList<>();
        for (Booking b : bookings.values()) {
            if (b.getRoomId().equalsIgnoreCase(room.getId())
                    && b.getDate().isBefore(weekEndDate)
                    && !b.getDate().isBefore(weekStartDate)) {
                String visibleUserId = (isAdmin || b.getUserId().equalsIgnoreCase(currentId))
                        ? b.getUserId() : "(hidden)";
                result.add(new Booking(b.getId(), b.getRoomId(), visibleUserId,
                        b.getDate(), b.getStartTime(), b.getEndTime(), b.getPurpose()));
            }
        }
        result.sort(Comparator.comparing(Booking::getDate).thenComparing(Booking::getStartTime));
        return Collections.unmodifiableList(result);
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

    private User getUserOrThrow(String userId) throws CrobsException {
        if (userId == null) throw new IllegalArgumentException("User ID cannot be null");
        User u = users.get(userId.trim().toLowerCase());
        if (u == null) {
            throw new CrobsException("User not found: " + userId);
        }
        return u;
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

    // ==========================================
    // Undo / Redo Restoration & Direct Execution (BON2)
    // ==========================================

    /**
     * Restores a previously removed user and any associated bookings (BON2).
     *
     * @param user               user to restore.
     * @param associatedBookings list of bookings to restore.
     */
    public void restoreUser(User user, List<Booking> associatedBookings) {
        if (user != null) {
            users.put(user.getId().toLowerCase(), user);
            if (associatedBookings != null) {
                for (Booking b : associatedBookings) {
                    bookings.put(b.getId().toLowerCase(), b);
                }
            }
        }
    }

    /**
     * Directly removes a user by ID without recording in undo/redo history (BON2).
     *
     * @param userId user ID to remove.
     */
    public void removeUserDirect(String userId) {
        if (userId != null) {
            users.remove(userId.toLowerCase());
        }
    }

    /**
     * Directly changes a user's role without recording in undo/redo history (BON2).
     *
     * @param userId  target user ID.
     * @param newRole target role.
     * @throws CrobsException if user not found.
     */
    public void changeRoleDirect(String userId, UserRole newRole) throws CrobsException {
        User u = getUserOrThrow(userId);
        u.setRole(newRole);
    }

    /**
     * Restores a previously deleted room and any associated bookings (BON2).
     *
     * @param room               room to restore.
     * @param associatedBookings bookings to restore.
     */
    public void restoreRoom(Room room, List<Booking> associatedBookings) {
        if (room != null) {
            rooms.put(room.getId().toLowerCase(), room);
            if (associatedBookings != null) {
                for (Booking b : associatedBookings) {
                    bookings.put(b.getId().toLowerCase(), b);
                }
            }
        }
    }

    /**
     * Directly removes a room by ID without recording in undo/redo history (BON2).
     *
     * @param roomId room ID to remove.
     */
    public void removeRoomDirect(String roomId) {
        if (roomId != null) {
            rooms.remove(roomId.toLowerCase());
        }
    }

    /**
     * Directly modifies a room field without recording in undo/redo history (BON2).
     *
     * @param roomId room ID.
     * @param field  field name.
     * @param value  new field value.
     * @throws CrobsException if room not found or invalid format.
     */
    public void modifyRoomDirect(String roomId, String field, String value) throws CrobsException {
        Room room = getRoomOrThrow(roomId);
        if ("type".equalsIgnoreCase(field)) {
            RoomType type = RoomType.fromString(value);
            if (type == null) {
                throw new CrobsException("Invalid room type: " + value);
            }
            room.setType(type);
        } else if ("capacity".equalsIgnoreCase(field)) {
            try {
                room.setCapacity(Integer.parseInt(value));
            } catch (NumberFormatException e) {
                throw new CrobsException("Invalid capacity: " + value);
            }
        } else if ("features".equalsIgnoreCase(field)) {
            Set<String> set = new HashSet<>();
            if (value != null && !value.trim().isEmpty()) {
                for (String f : value.split(",")) {
                    if (!f.trim().isEmpty()) {
                        set.add(f.trim());
                    }
                }
            }
            room.setFeatures(set);
        } else {
            throw new CrobsException("Invalid field: " + field);
        }
    }

    /**
     * Restores a previously deleted booking (BON2).
     *
     * @param booking booking instance to restore.
     */
    public void restoreBooking(Booking booking) {
        if (booking != null) {
            bookings.put(booking.getId().toLowerCase(), booking);
        }
    }

    /**
     * Directly removes a booking by ID without recording in undo/redo history (BON2).
     *
     * @param bookingId booking ID.
     */
    public void deleteBookingDirect(String bookingId) {
        if (bookingId != null) {
            bookings.remove(bookingId.toLowerCase());
        }
    }

    /**
     * Directly updates a booking's properties without recording in undo/redo history (BON2).
     *
     * @param bookingId booking ID.
     * @param roomId    target room ID.
     * @param date      date.
     * @param startTime start time.
     * @param endTime   end time.
     * @param purpose   purpose.
     * @throws CrobsException if booking not found.
     */
    public void modifyBookingDirect(String bookingId, String roomId, LocalDate date,
                                    LocalTime startTime, LocalTime endTime, String purpose) throws CrobsException {
        Booking booking = getBookingOrThrow(bookingId);
        String normalizedRoomId = normalizeId(roomId, "Room ID");
        Room room = getRoomOrThrow(normalizedRoomId);
        validateBookingValues(date, startTime, endTime);
        booking.update(room.getId(), date, startTime, endTime, purpose);
    }

    /**
     * Undoes the most recent mutating operation (BON2).
     *
     * @return true if an operation was undone, false if stack was empty.
     * @throws CrobsException if undo fails.
     */
    public boolean undo() throws CrobsException {
        checkLoggedIn();
        return undoRedoManager.undo(currentUser.getId(), currentUser.getRole());
    }

    /**
     * Redoes the most recently undone operation (BON2).
     *
     * @return true if an operation was redone, false if stack was empty.
     * @throws CrobsException if redo fails.
     */
    public boolean redo() throws CrobsException {
        checkLoggedIn();
        return undoRedoManager.redo(currentUser.getId(), currentUser.getRole());
    }

    /**
     * Checks if undo is available to the current authenticated user.
     *
     * @return true if the latest undo entry belongs to and is authorized for the current user.
     */
    public boolean canUndo() {
        return currentUser != null
                && undoRedoManager.canUndo(currentUser.getId(), currentUser.getRole());
    }

    /**
     * Checks if redo is available to the current authenticated user.
     *
     * @return true if the latest redo entry belongs to and is authorized for the current user.
     */
    public boolean canRedo() {
        return currentUser != null
                && undoRedoManager.canRedo(currentUser.getId(), currentUser.getRole());
    }

    private void recordCommand(CrobsCommand command, UndoRedoManager.Permission permission) {
        undoRedoManager.recordExecutedCommand(command, currentUser.getId(), permission);
    }

    private static String normalizeId(String id, String label) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " cannot be null or empty");
        }
        return id.trim().toLowerCase();
    }

    private static void validateBookingValues(LocalDate date, LocalTime startTime, LocalTime endTime) {
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Start time must be strictly before end time");
        }
    }

    private static User snapshot(User user) {
        return user == null ? null : new User(user.getId(), user.getName(), user.getRole());
    }

    private static Room snapshot(Room room) {
        return room == null ? null
                : new Room(room.getId(), room.getType(), room.getCapacity(), room.getFeatures());
    }

    private static Booking snapshot(Booking booking) {
        return booking == null ? null : new Booking(
                booking.getId(), booking.getRoomId(), booking.getUserId(), booking.getDate(),
                booking.getStartTime(), booking.getEndTime(), booking.getPurpose());
    }

    /**
     * Returns undo history for test diagnostics without exposing the manager.
     *
     * @return number of undo entries.
     */
    public int getUndoHistorySize() {
        return undoRedoManager.getUndoCount();
    }
}
