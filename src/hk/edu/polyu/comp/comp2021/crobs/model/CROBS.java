package hk.edu.polyu.comp.comp2021.crobs.model;

import hk.edu.polyu.comp.comp2021.crobs.command.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * The core engine and Facade of the Campus Room Booking System (CROBS).
 * Manages in-memory data for users, rooms, and bookings.
 * Follows the MVC Model layer architecture and supports undo/redo via Command Pattern (BON2).
 */
public class CROBS {

    /**
     * Internal mutation capability passed only to command instances created by CROBS.
     * External callers cannot obtain this capability from CROBS.
     */
    public interface MutationAccess {
        /**
         * Restores a previously removed user and associated bookings.
         *
         * @param user               the user to restore.
         * @param associatedBookings list of associated bookings to restore.
         */
        void restoreUser(User user, List<Booking> associatedBookings);

        /**
         * Directly removes a user by ID during undo/redo operations.
         *
         * @param userId the user ID.
         */
        void removeUser(String userId);

        /**
         * Changes a user's role during undo/redo operations.
         *
         * @param userId  the user ID.
         * @param newRole the target role.
         * @throws CrobsException if operation fails validation.
         */
        void changeRole(String userId, UserRole newRole) throws CrobsException;

        /**
         * Restores a previously deleted room and associated bookings.
         *
         * @param room               the room to restore.
         * @param associatedBookings list of associated bookings to restore.
         */
        void restoreRoom(Room room, List<Booking> associatedBookings);

        /**
         * Directly removes a room by ID during undo/redo operations.
         *
         * @param roomId the room ID.
         */
        void removeRoom(String roomId);

        /**
         * Modifies a room's field directly during undo/redo operations.
         *
         * @param roomId the room ID.
         * @param field  the field name.
         * @param value  the field value.
         * @throws CrobsException if operation fails validation.
         */
        void modifyRoom(String roomId, String field, String value) throws CrobsException;

        /**
         * Restores a previously deleted booking during undo/redo operations.
         *
         * @param booking the booking to restore.
         */
        void restoreBooking(Booking booking);

        /**
         * Directly deletes a booking by ID during undo/redo operations.
         *
         * @param bookingId the booking ID.
         */
        void deleteBooking(String bookingId);

        /**
         * Directly modifies a booking's parameters during undo/redo operations.
         *
         * @param bookingId the booking ID.
         * @param roomId    the target room ID.
         * @param date      the booking date.
         * @param startTime the start time.
         * @param endTime   the end time.
         * @param purpose   the meeting purpose.
         * @throws CrobsException if operation fails validation.
         */
        void modifyBooking(String bookingId, String roomId, LocalDate date,
                           LocalTime startTime, LocalTime endTime, String purpose) throws CrobsException;
    }

    private final Map<String, User> users = new HashMap<>();
    private final Map<String, Room> rooms = new HashMap<>();
    private final Map<String, Booking> bookings = new HashMap<>();

    private final UndoRedoManager undoRedoManager = new UndoRedoManager();
    private final MutationAccess mutationAccess = new InternalMutationAccess();

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
     * Returns a snapshot of the currently authenticated user in this session.
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
    @SuppressWarnings("AssignmentToNull")
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
     * @return newly created User snapshot.
     * @throws CrobsException if current user lacks admin privileges or name is invalid.
     */
    public User addUser(String name) throws CrobsException {
        checkAdminAccess();
        String cleanName = (name != null) ? name.replaceAll("(?U)\\s+", " ").trim() : "";
        if (cleanName.isEmpty()) {
            throw new IllegalArgumentException("User name cannot be blank");
        }

        // Generate a unique user ID and create a new regular user
        String newId = generateUserId();
        User newUser = new User(newId, cleanName, UserRole.REGULAR);
        users.put(newUser.getId().toLowerCase(), newUser);
        recordCommand(new AddUserCommand(mutationAccess, newUser), UndoRedoManager.Permission.ADMIN);
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
        recordCommand(new RemoveUserCommand(mutationAccess, target, userBookings), UndoRedoManager.Permission.ADMIN);
    }

    /**
     * [REQ4] Modifies the access privilege of another user. Only superadmin can call this.
     * Enforces the unique superadmin invariant.
     *
     * @param targetUserId ID of the target user.
     * @param newRole      the new role to assign.
     * @throws CrobsException if current user is not superadmin, target not found, or role breaks unique superadmin invariant.
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
        if (target.getId().equalsIgnoreCase(currentUser.getId()) || target.getRole() == UserRole.SUPERADMIN) {
            throw new CrobsException("Cannot change the role of the superadmin user.");
        }
        if (newRole == UserRole.SUPERADMIN) {
            throw new CrobsException("Cannot assign SUPERADMIN role: superadmin must remain unique.");
        }

        UserRole oldRole = target.getRole();
        target.setRole(newRole);
        recordCommand(new ChangeRoleCommand(mutationAccess, targetId, oldRole, newRole), UndoRedoManager.Permission.SUPERADMIN);
    }

    /**
     * Lists all users in ascending order of user ID.
     *
     * @return sorted list of all registered user snapshots.
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
     * @return newly created Room snapshot.
     * @throws CrobsException if current user is not admin.
     */
    public Room addRoom(RoomType roomType, int capacity, Set<String> features) throws CrobsException {
        checkAdminAccess();

        // Generate unique room ID and create new Room instance
        String roomId = generateRoomId();
        Room room = new Room(roomId, roomType, capacity, parseFeatureSet(features));
        rooms.put(room.getId().toLowerCase(), room);
        recordCommand(new AddRoomCommand(mutationAccess, room), UndoRedoManager.Permission.ADMIN);
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
        String oldValue = captureRoomFieldValue(room, field);
        String recordedNewValue = applyRoomModification(room, field, newValue);
        recordCommand(new ModifyRoomCommand(mutationAccess, room.getId(), field, oldValue, recordedNewValue),
                UndoRedoManager.Permission.ADMIN);
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

        LocalDateTime now = LocalDateTime.now();
        rooms.remove(room.getId().toLowerCase());
        List<Booking> futureBookings = new ArrayList<>();
        for (Booking b : bookings.values()) {
            if (b.getRoomId().equalsIgnoreCase(room.getId())
                    && LocalDateTime.of(b.getDate(), b.getEndTime()).isAfter(now)) {
                futureBookings.add(b);
            }
        }
        for (Booking b : futureBookings) {
            bookings.remove(b.getId().toLowerCase());
        }
        recordCommand(new DeleteRoomCommand(mutationAccess, room, futureBookings), UndoRedoManager.Permission.ADMIN);
    }

    /**
     * [REQ15] Displays information about a single room.
     *
     * @param roomId room ID to inspect.
     * @return the Room snapshot.
     * @throws CrobsException if room does not exist.
     */
    public Room listRoom(String roomId) throws CrobsException {
        return snapshot(getRoomOrThrow(roomId));
    }

    /**
     * [REQ16] Lists all rooms sorted in ascending order of room ID.
     *
     * @return sorted list of all room snapshots.
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
     *
     * @param roomId    target room ID.
     * @param date      date of reservation.
     * @param startTime start time.
     * @param endTime   end time.
     * @param purpose   reservation purpose.
     * @return newly created Booking snapshot.
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
        recordCommand(new BookCommand(mutationAccess, newBooking), UndoRedoManager.Permission.LOGGED_IN);
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
            if (!b.getId().equalsIgnoreCase(booking.getId())
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
                mutationAccess, booking.getId(),
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
        recordCommand(new DeleteBookingCommand(mutationAccess, booking), UndoRedoManager.Permission.LOGGED_IN);
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
        validateBookingValues(date, startTime, endTime);
        Set<String> requiredFeatures = parseFeatureSet(features);

        List<Room> result = new ArrayList<>();
        for (Room r : rooms.values()) {
            if (r.matchesCapacity(minCapacity) && r.hasAllFeatures(requiredFeatures)) {
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
     *
     * @param roomId        room ID to query.
     * @param weekStartDate the start date of the 7-day period.
     * @return sorted list of active bookings in the 7-day range.
     * @throws CrobsException if room not found.
     */
    public List<Booking> schedule(String roomId, LocalDate weekStartDate) throws CrobsException {
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

    /**
     * Returns undo history count for diagnostics without exposing the manager.
     *
     * @return number of undo entries.
     */
    public int getUndoHistorySize() {
        return undoRedoManager.getUndoCount();
    }

    /**
     * Clears the command history for undo and redo.
     */
    public void clearUndoHistory() {
        undoRedoManager.clear();
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
        String key = normalizeId(userId, "User ID");
        User u = users.get(key);
        if (u == null) {
            throw new CrobsException("User not found: " + userId);
        }
        return u;
    }

    private Room getRoomOrThrow(String roomId) throws CrobsException {
        String key = normalizeId(roomId, "Room ID");
        Room room = rooms.get(key);
        if (room == null) {
            throw new CrobsException("Room not found: " + roomId);
        }
        return room;
    }

    private Booking getBookingOrThrow(String bookingId) throws CrobsException {
        String key = normalizeId(bookingId, "Booking ID");
        Booking booking = bookings.get(key);
        if (booking == null) {
            throw new CrobsException("Booking not found: " + bookingId);
        }
        return booking;
    }

    private static String captureRoomFieldValue(Room room, String field) throws CrobsException {
        if ("type".equalsIgnoreCase(field) || "roomType".equalsIgnoreCase(field)) {
            return room.getType().name();
        }
        if ("capacity".equalsIgnoreCase(field)) {
            return String.valueOf(room.getCapacity());
        }
        if ("features".equalsIgnoreCase(field)) {
            return room.getFeatures().isEmpty() ? "-" : String.join(",", room.getFeatures());
        }
        throw new CrobsException("Invalid field: " + field);
    }

    private static String applyRoomModification(Room room, String field, String value) throws CrobsException {
        if ("type".equalsIgnoreCase(field) || "roomType".equalsIgnoreCase(field)) {
            RoomType newType = RoomType.fromString(value);
            if (newType == null) {
                throw new CrobsException("Invalid room type: " + value);
            }
            room.setType(newType);
            return newType.name();
        }
        if ("capacity".equalsIgnoreCase(field)) {
            int newCap;
            try {
                newCap = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new CrobsException("Invalid capacity value: " + value);
            }
            if (newCap <= 0) {
                throw new CrobsException("Capacity must be greater than 0");
            }
            room.setCapacity(newCap);
            return String.valueOf(newCap);
        }
        if ("features".equalsIgnoreCase(field)) {
            Set<String> set = parseFeatureString(value);
            room.setFeatures(set);
            return set.isEmpty() ? "-" : String.join(",", set);
        }
        throw new CrobsException("Invalid field: " + field);
    }

    private static Set<String> parseFeatureString(String value) {
        Set<String> set = new HashSet<>();
        if (value != null && !value.trim().isEmpty() && !"-".equals(value.trim())) {
            for (String feature : value.trim().split(",")) {
                String clean = feature.trim();
                if (!clean.isEmpty() && !"-".equals(clean)) {
                    set.add(clean);
                }
            }
        }
        return set;
    }

    private static Set<String> parseFeatureSet(Set<String> features) {
        Set<String> set = new HashSet<>();
        if (features != null) {
            for (String feature : features) {
                if (feature != null) {
                    String clean = feature.trim();
                    if (!clean.isEmpty() && !"-".equals(clean)) {
                        set.add(clean);
                    }
                }
            }
        }
        return set;
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

    // ==========================================
    // Undo / Redo Restoration & Direct Execution (BON2)
    // ==========================================

    private final class InternalMutationAccess implements MutationAccess {

        /**
         * Restores a previously removed user and any associated bookings (BON2).
         *
         * @param user               user to restore.
         * @param associatedBookings list of bookings to restore.
         */
        @Override
        public void restoreUser(User user, List<Booking> associatedBookings) {
            if (user != null) {
                User copy = snapshot(user);
                users.put(copy.getId().toLowerCase(), copy);
                if (associatedBookings != null) {
                    for (Booking b : associatedBookings) {
                        Booking bookingCopy = snapshot(b);
                        bookings.put(bookingCopy.getId().toLowerCase(), bookingCopy);
                    }
                }
            }
        }

        /**
         * Directly removes a user by ID without recording in undo/redo history (BON2).
         *
         * @param userId user ID to remove.
         */
        @Override
        public void removeUser(String userId) {
            if (userId != null) {
                users.remove(userId.trim().toLowerCase());
            }
        }

        /**
         * Directly changes a user's role without recording in undo/redo history (BON2).
         *
         * @param userId  target user ID.
         * @param newRole target role.
         * @throws CrobsException if user not found.
         */
        @Override
        public void changeRole(String userId, UserRole newRole) throws CrobsException {
            User u = getUserOrThrow(userId);
            u.setRole(newRole);
        }

        /**
         * Restores a previously deleted room and any associated bookings (BON2).
         *
         * @param room               room to restore.
         * @param associatedBookings bookings to restore.
         */
        @Override
        public void restoreRoom(Room room, List<Booking> associatedBookings) {
            if (room != null) {
                Room copy = snapshot(room);
                rooms.put(copy.getId().toLowerCase(), copy);
                if (associatedBookings != null) {
                    for (Booking b : associatedBookings) {
                        Booking bookingCopy = snapshot(b);
                        bookings.put(bookingCopy.getId().toLowerCase(), bookingCopy);
                    }
                }
            }
        }

        /**
         * Directly removes a room by ID without recording in undo/redo history (BON2).
         *
         * @param roomId room ID to remove.
         */
        @Override
        public void removeRoom(String roomId) {
            if (roomId != null) {
                rooms.remove(roomId.trim().toLowerCase());
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
        @Override
        public void modifyRoom(String roomId, String field, String value) throws CrobsException {
            Room room = getRoomOrThrow(roomId);
            applyRoomModification(room, field, value);
        }

        /**
         * Restores a previously deleted booking (BON2).
         *
         * @param booking booking instance to restore.
         */
        @Override
        public void restoreBooking(Booking booking) {
            if (booking != null) {
                Booking copy = snapshot(booking);
                bookings.put(copy.getId().toLowerCase(), copy);
            }
        }

        /**
         * Directly removes a booking by ID without recording in undo/redo history (BON2).
         *
         * @param bookingId booking ID.
         */
        @Override
        public void deleteBooking(String bookingId) {
            if (bookingId != null) {
                bookings.remove(bookingId.trim().toLowerCase());
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
        @Override
        public void modifyBooking(String bookingId, String roomId, LocalDate date,
                                  LocalTime startTime, LocalTime endTime, String purpose) throws CrobsException {
            Booking booking = getBookingOrThrow(bookingId);
            String normalizedRoomId = normalizeId(roomId, "Room ID");
            Room room = getRoomOrThrow(normalizedRoomId);
            validateBookingValues(date, startTime, endTime);
            for (Booking b : bookings.values()) {
                if (!b.getId().equalsIgnoreCase(booking.getId())
                        && b.conflictsWith(room.getId(), date, startTime, endTime)) {
                    throw new CrobsException("Time conflict with existing booking.");
                }
            }
            booking.update(room.getId(), date, startTime, endTime, purpose);
        }
    }
}
