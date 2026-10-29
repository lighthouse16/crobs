package hk.edu.polyu.comp.comp2021.crobs.gui;

import hk.edu.polyu.comp.comp2021.crobs.model.Booking;
import hk.edu.polyu.comp.comp2021.crobs.model.CROBS;
import hk.edu.polyu.comp.comp2021.crobs.model.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.model.Room;
import hk.edu.polyu.comp.comp2021.crobs.model.RoomType;
import hk.edu.polyu.comp.comp2021.crobs.model.User;
import hk.edu.polyu.comp.comp2021.crobs.model.UserRole;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Controller and presentation facade connecting Swing views to the underlying CROBS model.
 * Serializes user actions to the REQ1 command logger and dispatches model operations.
 */
public class GuiController {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final CROBS model;
    private final GuiSessionLogger logger;
    private final List<GuiListener> listeners = new ArrayList<>();
    private final java.util.Map<String, User> knownUsers = new java.util.LinkedHashMap<>();

    /**
     * Listener interface for UI components to observe model and session mutations.
     */
    @FunctionalInterface
    public interface GuiListener {
        /**
         * Invoked whenever domain data or user session changes.
         */
        void onDataOrSessionChanged();
    }

    /**
     * Constructs the GUI controller.
     *
     * @param model  the domain model instance.
     * @param logger the session logger.
     */
    public GuiController(CROBS model, GuiSessionLogger logger) {
        if (model == null) {
            throw new IllegalArgumentException("Model cannot be null");
        }
        this.model = model;
        this.logger = (logger != null) ? logger : new GuiSessionLogger("", "");
        // Pre-register initial default superadmin (u1)
        knownUsers.put("u1", new User("u1", "Superadmin", UserRole.SUPERADMIN));
    }

    /**
     * Registers a listener to be notified on state changes.
     *
     * @param listener listener callback.
     */
    public void addListener(GuiListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    private void notifyListeners() {
        for (GuiListener listener : listeners) {
            listener.onDataOrSessionChanged();
        }
    }

    // ==========================================
    // Session & Authentication (REQ5, REQ6, REQ18)
    // ==========================================

    /**
     * Checks if a user is currently authenticated.
     *
     * @return true if logged in.
     */
    public boolean isLoggedIn() {
        return model.isLoggedIn();
    }

    /**
     * Gets the currently authenticated user.
     *
     * @return current User or null.
     */
    public User getCurrentUser() {
        return model.getCurrentUser();
    }

    /**
     * Checks if current user has administrator privileges (Admin or Superadmin).
     *
     * @return true if user is Admin or Superadmin.
     */
    public boolean isAdmin() {
        User u = getCurrentUser();
        return u != null && u.getRole() != UserRole.REGULAR;
    }

    /**
     * Checks if current user is the superadministrator.
     *
     * @return true if user is Superadmin.
     */
    public boolean isSuperadmin() {
        User u = getCurrentUser();
        return u != null && u.getRole() == UserRole.SUPERADMIN;
    }

    /**
     * Logs in as an existing user (REQ5).
     *
     * @param userId user ID.
     * @throws CrobsException if user does not exist.
     */
    public void login(String userId) throws CrobsException {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be empty");
        }
        String cleanId = userId.trim();
        logger.logCommand("login " + cleanId);
        model.login(cleanId);
        User current = model.getCurrentUser();
        if (current != null) {
            knownUsers.put(current.getId().toLowerCase(), current);
        }
        notifyListeners();
    }

    /**
     * Logs out the currently authenticated user (REQ6).
     *
     * @throws CrobsException if no user logged in.
     */
    public void logout() throws CrobsException {
        logger.logCommand("logout");
        model.logout();
        notifyListeners();
    }

    /**
     * Quietly logs out the currently authenticated user without emitting a CLI log.
     */
    public void logoutQuiet() {
        try {
            model.logout();
            notifyListeners();
        } catch (Exception ignored) {
        }
    }

    /**
     * Exits the GUI application cleanly (REQ18).
     */
    public void quit() {
        logger.logCommand("quit");
        logger.close();
        System.exit(0);
    }

    /**
     * Closes the session logger streams.
     */
    public void closeLogger() {
        logger.close();
    }

    /**
     * Returns the session logger instance.
     *
     * @return GuiSessionLogger.
     */
    public GuiSessionLogger getLogger() {
        return logger;
    }

    // ==========================================
    // User Management (REQ2, REQ3, REQ4)
    // ==========================================

    /**
     * Adds a new regular user (REQ2).
     *
     * @param name user name.
     * @return newly created User.
     * @throws CrobsException on authorization or validation failure.
     */
    public User addUser(String name) throws CrobsException {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("User name cannot be blank");
        }
        String sanitizedName = name.trim().replace(' ', '_');
        logger.logCommand("addUser " + sanitizedName);
        User user = model.addUser(sanitizedName);
        if (user != null) {
            knownUsers.put(user.getId().toLowerCase(), user);
        }
        notifyListeners();
        return user;
    }

    /**
     * Removes an existing regular user (REQ3).
     *
     * @param targetUserId ID of the regular user to remove.
     * @throws CrobsException on failure.
     */
    public void removeUser(String targetUserId) throws CrobsException {
        if (targetUserId == null || targetUserId.trim().isEmpty()) {
            throw new IllegalArgumentException("Target user ID cannot be blank");
        }
        String cleanId = targetUserId.trim();
        logger.logCommand("removeUser " + cleanId);
        model.removeUser(cleanId);
        knownUsers.remove(cleanId.toLowerCase());
        notifyListeners();
    }

    /**
     * Changes access role of another user (REQ4 - Superadmin only).
     *
     * @param targetUserId target user ID.
     * @param newRole      new role to assign.
     * @throws CrobsException on authorization or validation failure.
     */
    public void changeRole(String targetUserId, UserRole newRole) throws CrobsException {
        if (targetUserId == null || newRole == null) {
            throw new IllegalArgumentException("Arguments cannot be null");
        }
        String cleanId = targetUserId.trim();
        logger.logCommand("changeRole " + cleanId + " " + newRole.name());
        model.changeRole(cleanId, newRole);
        User u = knownUsers.get(cleanId.toLowerCase());
        if (u != null) {
            u.setRole(newRole);
        }
        notifyListeners();
    }

    /**
     * Returns all known users tracked in the session.
     *
     * @return list of User objects.
     */
    public List<User> getAllKnownUsers() {
        return new ArrayList<>(knownUsers.values());
    }

    // ==========================================
    // Room Management (REQ7, REQ8, REQ9, REQ15, REQ16)
    // ==========================================

    /**
     * Adds a new room (REQ7).
     *
     * @param roomType room type.
     * @param capacity maximum capacity.
     * @param features set of features.
     * @return newly created Room.
     * @throws CrobsException on failure.
     */
    public Room addRoom(RoomType roomType, int capacity, Set<String> features) throws CrobsException {
        String featStr = (features != null && !features.isEmpty()) ? String.join(",", features) : "";
        logger.logCommand("addRoom " + roomType.name() + " " + capacity + " " + featStr);
        Room room = model.addRoom(roomType, capacity, features);
        notifyListeners();
        return room;
    }

    /**
     * Modifies one field of an existing room (REQ8).
     *
     * @param roomId   target room ID.
     * @param field    field name ("type", "capacity", or "features").
     * @param newValue new value.
     * @throws CrobsException on validation failure.
     */
    public void modifyRoom(String roomId, String field, String newValue) throws CrobsException {
        String cleanId = roomId.trim();
        String cleanField = field.trim();
        String cleanValue = newValue.trim();
        logger.logCommand("modifyRoom " + cleanId + " " + cleanField + " " + cleanValue);
        model.modifyRoom(cleanId, cleanField, cleanValue);
        notifyListeners();
    }

    /**
     * Deletes a room and cancels all future bookings (REQ9).
     *
     * @param roomId room ID to delete.
     * @throws CrobsException on failure.
     */
    public void deleteRoom(String roomId) throws CrobsException {
        String cleanId = roomId.trim();
        logger.logCommand("deleteRoom " + cleanId);
        model.deleteRoom(cleanId);
        notifyListeners();
    }

    /**
     * Inspects a single room with logging (REQ15).
     *
     * @param roomId target room ID.
     * @return the Room instance.
     * @throws CrobsException if room does not exist.
     */
    public Room userListRoom(String roomId) throws CrobsException {
        String cleanId = roomId.trim();
        logger.logCommand("listRoom " + cleanId);
        return model.listRoom(cleanId);
    }

    /**
     * Internal quiet room lookup without generating a user CLI log record.
     *
     * @param roomId target room ID.
     * @return the Room instance or null if not found.
     */
    public Room getRoomQuiet(String roomId) {
        try {
            return model.listRoom(roomId);
        } catch (CrobsException e) {
            return null;
        }
    }

    /**
     * Returns all rooms with explicit user action logging (REQ16).
     *
     * @return list of all rooms.
     */
    public List<Room> userListAllRooms() {
        logger.logCommand("listAllRooms");
        return model.listAllRooms();
    }

    /**
     * Internal quiet query returning all rooms without emitting a CLI log.
     *
     * @return list of all rooms.
     */
    public List<Room> getAllRoomsQuiet() {
        return model.listAllRooms();
    }

    // ==========================================
    // Booking Management (REQ10, REQ11, REQ12, REQ17)
    // ==========================================

    /**
     * Creates a new booking for the authenticated user (REQ10).
     *
     * @param roomId    target room ID.
     * @param date      date of reservation.
     * @param startTime start time.
     * @param endTime   end time.
     * @param purpose   reservation purpose.
     * @return newly created Booking.
     * @throws CrobsException on conflict or validation error.
     */
    public Booking book(String roomId, LocalDate date, LocalTime startTime,
                        LocalTime endTime, String purpose) throws CrobsException {
        String cleanRoom = roomId.trim();
        String cleanPurpose = (purpose != null && !purpose.trim().isEmpty())
                ? purpose.trim().replace(' ', '_') : "None";
        String cmd = String.format("book %s %s %s %s %s",
                cleanRoom, date, startTime.format(TIME_FMT), endTime.format(TIME_FMT), cleanPurpose);
        logger.logCommand(cmd);
        Booking booking = model.book(cleanRoom, date, startTime, endTime, cleanPurpose);
        notifyListeners();
        return booking;
    }

    /**
     * Modifies an existing booking owned by the current user (REQ11).
     *
     * @param bookingId target booking ID.
     * @param roomId    new room ID.
     * @param date      new date.
     * @param startTime new start time.
     * @param endTime   new end time.
     * @param purpose   new purpose.
     * @throws CrobsException on conflict or authorization failure.
     */
    public void modifyBooking(String bookingId, String roomId, LocalDate date,
                              LocalTime startTime, LocalTime endTime, String purpose) throws CrobsException {
        String cleanId = bookingId.trim();
        String cleanRoom = roomId.trim();
        String cleanPurpose = (purpose != null && !purpose.trim().isEmpty())
                ? purpose.trim().replace(' ', '_') : "None";
        String cmd = String.format("modifyBooking %s %s %s %s %s %s",
                cleanId, cleanRoom, date, startTime.format(TIME_FMT), endTime.format(TIME_FMT), cleanPurpose);
        logger.logCommand(cmd);
        model.modifyBooking(cleanId, cleanRoom, date, startTime, endTime, cleanPurpose);
        notifyListeners();
    }

    /**
     * Cancels an existing booking owned by the current user (REQ12).
     *
     * @param bookingId booking ID to cancel.
     * @throws CrobsException on authorization or lookup failure.
     */
    public void deleteBooking(String bookingId) throws CrobsException {
        String cleanId = bookingId.trim();
        logger.logCommand("deleteBooking " + cleanId);
        model.deleteBooking(cleanId);
        notifyListeners();
    }

    /**
     * Lists all bookings owned by current user with explicit user logging (REQ17).
     *
     * @return list of bookings.
     * @throws CrobsException if not logged in.
     */
    public List<Booking> userListBookings() throws CrobsException {
        logger.logCommand("listBookings");
        return model.listBookings();
    }

    /**
     * Quietly fetches current user's bookings without emitting a CLI log.
     *
     * @return list of bookings or empty list.
     */
    public List<Booking> getBookingsQuiet() {
        if (!model.isLoggedIn()) {
            return Collections.emptyList();
        }
        try {
            return model.listBookings();
        } catch (CrobsException e) {
            return Collections.emptyList();
        }
    }

    // ==========================================
    // Search & Schedule (REQ13, REQ14)
    // ==========================================

    /**
     * Searches available rooms with explicit command logging (REQ13).
     *
     * @param date        target date.
     * @param startTime   target start time.
     * @param endTime     target end time.
     * @param minCapacity minimum capacity.
     * @param features    required features.
     * @return matching available rooms.
     */
    public List<Room> search(LocalDate date, LocalTime startTime, LocalTime endTime,
                             int minCapacity, Set<String> features) {
        String featStr = (features != null && !features.isEmpty()) ? String.join(",", features) : "";
        String cmd = String.format("search %s %s %s %d %s",
                date, startTime.format(TIME_FMT), endTime.format(TIME_FMT), minCapacity, featStr);
        logger.logCommand(cmd);
        return model.search(date, startTime, endTime, minCapacity, features);
    }

    /**
     * Quiet search query without emitting a CLI log.
     *
     * @param date        target date.
     * @param startTime   target start time.
     * @param endTime     target end time.
     * @param minCapacity minimum capacity.
     * @param features    required features.
     * @return matching available rooms.
     */
    public List<Room> searchQuiet(LocalDate date, LocalTime startTime, LocalTime endTime,
                                  int minCapacity, Set<String> features) {
        return model.search(date, startTime, endTime, minCapacity, features);
    }

    /**
     * Queries the 7-day schedule for a specified room (REQ14).
     *
     * @param roomId        target room ID.
     * @param weekStartDate starting date of 7-day period.
     * @return bookings in the 7-day range.
     * @throws CrobsException if room not found or not logged in.
     */
    public List<Booking> schedule(String roomId, LocalDate weekStartDate) throws CrobsException {
        String cleanId = roomId.trim();
        logger.logCommand("schedule " + cleanId + " " + weekStartDate);
        return model.schedule(cleanId, weekStartDate);
    }

    /**
     * Quietly queries the 7-day schedule without emitting an interactive CLI command log.
     *
     * @param roomId        target room ID.
     * @param weekStartDate starting date of 7-day period.
     * @return bookings in the 7-day range, or empty list if failed.
     */
    public List<Booking> scheduleQuiet(String roomId, LocalDate weekStartDate) {
        try {
            return model.schedule(roomId.trim(), weekStartDate);
        } catch (CrobsException e) {
            return Collections.emptyList();
        }
    }

    public void loginQuiet(String userId) throws CrobsException {
        model.login(userId.trim().toLowerCase());
        notifyListeners();
    }

    public Room addRoomQuiet(RoomType type, int capacity, Set<String> features) throws CrobsException {
        Room r = model.addRoom(type, capacity, features);
        notifyListeners();
        return r;
    }

    public User addUserQuiet(String name) throws CrobsException {
        User u = model.addUser(name.trim().replace(" ", "_"));
        knownUsers.put(u.getId().toLowerCase(), u);
        notifyListeners();
        return u;
    }

    public void changeRoleQuiet(String userId, UserRole newRole) throws CrobsException {
        model.changeRole(userId.trim().toLowerCase(), newRole);
        User u = knownUsers.get(userId.trim().toLowerCase());
        if (u != null) {
            u.setRole(newRole);
        }
        notifyListeners();
    }

    public Booking bookQuiet(String roomId, LocalDate date, LocalTime start, LocalTime end, String purpose) throws CrobsException {
        String cleanP = (purpose == null || purpose.trim().isEmpty()) ? "-" : purpose.trim().replace(" ", "_");
        Booking b = model.book(roomId.trim(), date, start, end, cleanP);
        notifyListeners();
        return b;
    }
}
