package hk.edu.polyu.comp.comp2021.crobs.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test suite for the CROBS model package.
 * Targets >= 90% Line Coverage as required by Section 5 of the project spec.
 */
class CROBSTest {

    private CROBS crobs;

    @BeforeEach
    void setUp() throws Exception {
        crobs = new CROBS();
        // Login as default superadmin (u1) created at startup
        crobs.login("u1");
    }

    @Test
    void testInitialSuperadminState() {
        assertNotNull(crobs.getCurrentUser());
        assertEquals(UserRole.SUPERADMIN, crobs.getCurrentUser().getRole());
        assertTrue(crobs.isLoggedIn());
    }

    @Test
    void testUserLifecycleAndRoleChange() throws Exception {
        // Add regular user
        User user1 = crobs.addUser("Alice");
        assertNotNull(user1);
        assertEquals("Alice", user1.getName());
        assertEquals(UserRole.REGULAR, user1.getRole());

        // Change role to Admin
        crobs.changeRole(user1.getId(), UserRole.ADMIN);
        assertEquals(UserRole.ADMIN, crobs.listAllUsers().stream().filter(u -> u.getId().equals(user1.getId())).findFirst().get().getRole());

        // Logout and login as Alice
        crobs.logout();
        assertFalse(crobs.isLoggedIn());
        crobs.login(user1.getId());
        assertEquals(user1.getId(), crobs.getCurrentUser().getId());

        // Alice (Admin) adds Bob (Regular)
        User user2 = crobs.addUser("Bob");
        assertEquals("Bob", user2.getName());

        // Alice (Admin) cannot change roles (only Superadmin can)
        assertThrows(CrobsException.class, () -> crobs.changeRole(user2.getId(), UserRole.ADMIN));

        // Alice removes Bob
        crobs.removeUser(user2.getId());
    }

    @Test
    void testRoomManagement() throws Exception {
        Set<String> features = new HashSet<>();
        features.add("projector");
        features.add("wifi");

        Room room = crobs.addRoom(RoomType.lectureTheatre, 100, features);
        assertNotNull(room);
        assertEquals(RoomType.lectureTheatre, room.getType());
        assertEquals(100, room.getCapacity());
        assertTrue(room.getFeatures().contains("projector"));

        // Modify room
        crobs.modifyRoom(room.getId(), "capacity", "120");
        assertEquals(120, crobs.listRoom(room.getId()).getCapacity());

        crobs.modifyRoom(room.getId(), "type", "seminarRoom");
        assertEquals(RoomType.seminarRoom, crobs.listRoom(room.getId()).getType());

        crobs.modifyRoom(room.getId(), "features", "whiteboard,wifi");
        assertTrue(crobs.listRoom(room.getId()).getFeatures().contains("whiteboard"));
        assertFalse(crobs.listRoom(room.getId()).getFeatures().contains("projector"));

        // List room
        Room fetched = crobs.listRoom(room.getId());
        assertEquals(room.getId(), fetched.getId());

        // List all rooms
        List<Room> all = crobs.listAllRooms();
        assertFalse(all.isEmpty());

        // Delete room
        crobs.deleteRoom(room.getId());
        assertThrows(CrobsException.class, () -> crobs.listRoom(room.getId()));
    }

    @Test
    void testBookingAndConflictDetection() throws Exception {
        Room room = crobs.addRoom(RoomType.meetingRoom, 20, Collections.singleton("wifi"));
        LocalDate date = LocalDate.now().plusDays(1);
        LocalTime start = LocalTime.of(10, 0);
        LocalTime end = LocalTime.of(12, 0);

        // Make booking
        Booking booking = crobs.book(room.getId(), date, start, end, "Project discussion");
        assertNotNull(booking);
        assertEquals(room.getId(), booking.getRoomId());

        // Conflicting booking (same room, same date, overlapping time)
        assertThrows(CrobsException.class, () ->
                crobs.book(room.getId(), date, LocalTime.of(11, 0), LocalTime.of(13, 0), "Conflict meeting")
        );

        // Non-conflicting booking (different time)
        Booking booking2 = crobs.book(room.getId(), date, LocalTime.of(12, 0), LocalTime.of(14, 0), "After meeting");
        assertNotNull(booking2);

        // List bookings
        List<Booking> myBookings = crobs.listBookings();
        assertEquals(2, myBookings.size());

        // Modify booking
        crobs.modifyBooking(booking.getId(), room.getId(), date, LocalTime.of(8, 0), LocalTime.of(10, 0), "Moved earlier");
        assertEquals(LocalTime.of(8, 0), crobs.listBookings().stream().filter(b -> b.getId().equals(booking.getId())).findFirst().get().getStartTime());

        // Delete booking
        crobs.deleteBooking(booking.getId());
        assertEquals(1, crobs.listBookings().size());
    }

    @Test
    void testSearchAndSchedule() throws Exception {
        Set<String> f1 = new HashSet<>();
        f1.add("projector");
        f1.add("wifi");
        Room r1 = crobs.addRoom(RoomType.lectureTheatre, 200, f1);

        LocalDate date = LocalDate.now().plusDays(2);
        LocalTime start = LocalTime.of(14, 0);
        LocalTime end = LocalTime.of(16, 0);

        // Search: should find r1
        List<Room> found = crobs.search(date, start, end, 150, Collections.singleton("projector"));
        assertTrue(found.contains(r1));

        // Book r1 for that slot
        crobs.book(r1.getId(), date, start, end, "Lecture");

        // Search again: r1 should now be unavailable
        List<Room> foundAfter = crobs.search(date, start, end, 150, Collections.singleton("projector"));
        assertFalse(foundAfter.contains(r1));

        // Weekly schedule
        List<Booking> weekSchedule = crobs.schedule(r1.getId(), date.minusDays(1));
        assertFalse(weekSchedule.isEmpty());
    }

    @Test
    void testEntityEqualsAndHashCode() {
        User uA = new User("u10", "User A", UserRole.REGULAR);
        User uB = new User("u10", "User B", UserRole.ADMIN);
        assertEquals(uA, uB);
        assertEquals(uA.hashCode(), uB.hashCode());

        Room rA = new Room("r10", RoomType.computerLab, 30, Collections.emptySet());
        Room rB = new Room("r10", RoomType.meetingRoom, 50, Collections.emptySet());
        assertEquals(rA, rB);
        assertEquals(rA.hashCode(), rB.hashCode());

        LocalDate d = LocalDate.now();
        LocalTime t1 = LocalTime.of(9, 0);
        LocalTime t2 = LocalTime.of(10, 0);
        Booking bA = new Booking("b10", "r1", "u1", d, t1, t2, "A");
        Booking bB = new Booking("b10", "r2", "u2", d, t1, t2, "B");
        assertEquals(bA, bB);
        assertEquals(bA.hashCode(), bB.hashCode());
    }

    @Test
    void testModifyBookingFailureIsAtomic() throws Exception {
        Room originalRoom = crobs.addRoom(RoomType.seminarRoom, 30, Collections.emptySet());
        Room otherRoom = crobs.addRoom(RoomType.meetingRoom, 10, Collections.emptySet());
        LocalDate originalDate = LocalDate.now().plusDays(1);
        Booking booking = crobs.book(originalRoom.getId(), originalDate,
                LocalTime.of(10, 0), LocalTime.of(11, 0), "Original");
        boolean redoAvailableBefore = crobs.canRedo();

        assertThrows(IllegalArgumentException.class, () -> crobs.modifyBooking(
                booking.getId(), otherRoom.getId(), originalDate.plusDays(1),
                LocalTime.of(12, 0), LocalTime.of(11, 0), "Invalid"));

        Booking unchanged = crobs.listBookings().get(0);
        assertEquals(originalRoom.getId(), unchanged.getRoomId());
        assertEquals(originalDate, unchanged.getDate());
        assertEquals(LocalTime.of(10, 0), unchanged.getStartTime());
        assertEquals(LocalTime.of(11, 0), unchanged.getEndTime());
        assertEquals("Original", unchanged.getPurpose());
        assertEquals(redoAvailableBefore, crobs.canRedo());
    }

    @Test
    void testRoomIdNormalizationPreventsDuplicateBookings() throws Exception {
        Room room = crobs.addRoom(RoomType.seminarRoom, 30, Collections.emptySet());
        LocalDate date = LocalDate.now().plusDays(1);
        crobs.book(room.getId(), date, LocalTime.of(10, 0), LocalTime.of(11, 0), "First");

        assertThrows(CrobsException.class, () -> crobs.book(
                "  " + room.getId().toUpperCase() + "  ", date,
                LocalTime.of(10, 30), LocalTime.of(11, 30), "Overlap"));
        assertEquals(room.getId(), crobs.listBookings().get(0).getRoomId());
    }

    @Test
    void testReturnedModelObjectsCannotMutateInternalState() throws Exception {
        User user = crobs.addUser("ReadOnlyUser");
        Room room = crobs.addRoom(RoomType.seminarRoom, 30, Collections.singleton("wifi"));
        Booking booking = crobs.book(room.getId(), LocalDate.now().plusDays(1),
                LocalTime.of(10, 0), LocalTime.of(11, 0), "ReadOnlyBooking");

        User currentUserSnapshot = crobs.getCurrentUser();
        currentUserSnapshot.setRole(UserRole.REGULAR);
        User listedUserSnapshot = crobs.listAllUsers().get(1);
        listedUserSnapshot.setRole(UserRole.ADMIN);
        Room roomSnapshot = crobs.listRoom(room.getId());
        roomSnapshot.setCapacity(999);
        Booking bookingSnapshot = crobs.listBookings().get(0);
        bookingSnapshot.update(room.getId(), LocalDate.now().plusDays(2),
                LocalTime.of(10, 0), LocalTime.of(11, 0), "tampered");

        assertEquals(UserRole.SUPERADMIN, crobs.getCurrentUser().getRole());
        assertEquals(UserRole.REGULAR, crobs.listAllUsers().stream()
                .filter(u -> u.getId().equals(user.getId())).findFirst().get().getRole());
        assertEquals(30, crobs.listRoom(room.getId()).getCapacity());
        assertEquals("ReadOnlyBooking", crobs.listBookings().get(0).getPurpose());
    }

    @Test
    void testUniqueSuperadminInvariant() throws Exception {
        User alice = crobs.addUser("Alice");
        assertThrows(CrobsException.class, () -> crobs.changeRole(alice.getId(), UserRole.SUPERADMIN));
        assertThrows(CrobsException.class, () -> crobs.changeRole("u1", UserRole.REGULAR));
        assertThrows(CrobsException.class, () -> crobs.changeRole("u1", UserRole.ADMIN));
        assertEquals(UserRole.SUPERADMIN, crobs.getCurrentUser().getRole());
        assertEquals(UserRole.REGULAR, crobs.listAllUsers().stream()
                .filter(u -> u.getId().equals(alice.getId())).findFirst().get().getRole());
    }

    @Test
    void testSearchRejectsInvalidTimeInterval() throws Exception {
        crobs.addRoom(RoomType.meetingRoom, 10, Collections.emptySet());
        LocalDate date = LocalDate.now().plusDays(1);
        assertThrows(IllegalArgumentException.class, () ->
                crobs.search(date, LocalTime.of(12, 0), LocalTime.of(10, 0), 1, Collections.emptySet()));
        assertThrows(IllegalArgumentException.class, () ->
                crobs.search(date, LocalTime.of(10, 0), LocalTime.of(10, 0), 1, Collections.emptySet()));
        assertThrows(IllegalArgumentException.class, () ->
                crobs.search(null, LocalTime.of(10, 0), LocalTime.of(11, 0), 1, Collections.emptySet()));
        assertThrows(IllegalArgumentException.class, () ->
                crobs.search(date, null, LocalTime.of(11, 0), 1, Collections.emptySet()));
        assertThrows(IllegalArgumentException.class, () ->
                crobs.search(date, LocalTime.of(10, 0), null, 1, Collections.emptySet()));
        CROBS empty = new CROBS();
        assertThrows(IllegalArgumentException.class, () -> empty.search(
                date, LocalTime.of(12, 0), LocalTime.of(10, 0), 1, Collections.emptySet()));
    }

    @Test
    void testModifyRoomEmptyFeaturesSentinelAndUndoRedo() throws Exception {
        Room room = crobs.addRoom(RoomType.seminarRoom, 20,
                new HashSet<>(java.util.Arrays.asList("wifi", "projector")));
        crobs.modifyRoom(room.getId(), "features", "-");
        assertTrue(crobs.listRoom(room.getId()).getFeatures().isEmpty());

        assertTrue(crobs.undo());
        assertEquals(2, crobs.listRoom(room.getId()).getFeatures().size());

        assertTrue(crobs.redo());
        assertTrue(crobs.listRoom(room.getId()).getFeatures().isEmpty());
    }

    @Test
    void testModelDoesNotExposeDirectOrRestoreMethods() {
        for (java.lang.reflect.Method method : CROBS.class.getMethods()) {
            String name = method.getName();
            assertFalse(name.endsWith("Direct") || name.startsWith("restore"),
                    "Unexpected public bypass method: " + name);
        }
        for (java.lang.reflect.Method method : CROBS.class.getMethods()) {
            assertFalse(CROBS.MutationAccess.class.isAssignableFrom(method.getReturnType()),
                    "Mutation capability must not escape through a public getter");
            assertFalse(hk.edu.polyu.comp.comp2021.crobs.command.CrobsCommand.class
                    .isAssignableFrom(method.getReturnType()), "Live commands must not escape");
        }
        for (java.lang.reflect.Field field : CROBS.class.getDeclaredFields()) {
            if (CROBS.MutationAccess.class.isAssignableFrom(field.getType())) {
                assertTrue(java.lang.reflect.Modifier.isPrivate(field.getModifiers()));
                assertTrue(java.lang.reflect.Modifier.isFinal(field.getModifiers()));
            }
        }
    }

    @Test
    void testInvalidOperations() {
        assertThrows(IllegalArgumentException.class, () -> crobs.login(""));
        assertThrows(CrobsException.class, () -> crobs.login("non_existent_id"));
        assertThrows(IllegalArgumentException.class, () -> crobs.addUser("   "));
        assertThrows(CrobsException.class, () -> crobs.listRoom("invalid_room"));
    }

    @Test
    void testUserAndRoleValidation() throws Exception {
        // User validation & equality branches
        User defaultRoleUser = new User("u99", "DefaultRole", null);
        assertEquals(UserRole.REGULAR, defaultRoleUser.getRole());
        assertEquals(defaultRoleUser, defaultRoleUser);
        assertNotEquals(defaultRoleUser, "not-a-user");
        assertTrue(defaultRoleUser.toString().contains("u99"));
        assertThrows(IllegalArgumentException.class, () -> new User(null, "Name", UserRole.REGULAR));
        assertThrows(IllegalArgumentException.class, () -> new User("u1", " ", UserRole.REGULAR));
        assertThrows(IllegalArgumentException.class, () -> new User("u1", "  \u0085  ", UserRole.REGULAR));
        assertThrows(IllegalArgumentException.class, () -> crobs.addUser("  \u0085  "));
        assertThrows(IllegalArgumentException.class, () -> defaultRoleUser.setRole(null));

        // UserRole and RoomType parsing branches
        assertNull(UserRole.fromString(null));
        assertNull(UserRole.fromString("unknownRole"));
        assertEquals(UserRole.ADMIN, UserRole.fromString("admin"));
        assertTrue(UserRole.SUPERADMIN.canManageRooms());
        assertTrue(UserRole.SUPERADMIN.canManageUsers());
        assertTrue(UserRole.SUPERADMIN.canChangeRoles());
        assertTrue(UserRole.ADMIN.canManageRooms());
        assertTrue(UserRole.ADMIN.canManageUsers());
        assertFalse(UserRole.ADMIN.canChangeRoles());
        assertFalse(UserRole.REGULAR.canManageRooms());
        assertFalse(UserRole.REGULAR.canManageUsers());
        assertFalse(UserRole.REGULAR.canChangeRoles());

        assertNull(RoomType.fromString(null));
        assertNull(RoomType.fromString("unknownType"));
        assertEquals(RoomType.computerLab, RoomType.fromString("COMPUTERLAB"));

    }

    @Test
    void testRoomValidationAndFeatureCopies() {
        // Room validation & feature branches
        Set<String> rawFeatures = new HashSet<>(java.util.Arrays.asList("wifi", " ", null));
        Room roomEntity = new Room("r99", RoomType.meetingRoom, 10, rawFeatures);
        assertEquals(roomEntity, roomEntity);
        assertNotEquals(roomEntity, "not-a-room");
        assertTrue(roomEntity.hasAllFeatures(null));
        assertTrue(roomEntity.hasAllFeatures(Collections.emptySet()));
        assertFalse(roomEntity.hasAllFeatures(Collections.singleton("projector")));
        assertTrue(roomEntity.toString().contains("r99"));
        roomEntity.setFeatures(null);
        assertTrue(roomEntity.getFeatures().isEmpty());
        roomEntity.setFeatures(new HashSet<>(java.util.Arrays.asList("audio", "", null)));
        assertEquals(1, roomEntity.getFeatures().size());
        assertThrows(IllegalArgumentException.class, () -> new Room("", RoomType.meetingRoom, 10, null));
        assertThrows(IllegalArgumentException.class, () -> new Room("r1", null, 10, null));
        assertThrows(IllegalArgumentException.class, () -> new Room("r1", RoomType.meetingRoom, 0, null));
        assertThrows(IllegalArgumentException.class, () -> roomEntity.setType(null));
        assertThrows(IllegalArgumentException.class, () -> roomEntity.setCapacity(0));

    }

    @Test
    void testBookingValidationIsAtomic() {
        LocalDate date = LocalDate.now().plusDays(1);
        LocalTime t09 = LocalTime.of(9, 0);
        LocalTime t10 = LocalTime.of(10, 0);
        // Booking validation & equality branches
        Booking bookingEntity = new Booking("b99", "r99", "u99", date, t09, t10, null);
        assertEquals("", bookingEntity.getPurpose());
        assertEquals(bookingEntity, bookingEntity);
        assertNotEquals(bookingEntity, "not-a-booking");
        assertFalse(bookingEntity.conflictsWith("otherRoom", date, t09, t10));
        assertFalse(bookingEntity.conflictsWith("r99", date.plusDays(1), t09, t10));
        assertTrue(bookingEntity.toString().contains("b99"));
        assertThrows(IllegalArgumentException.class, () -> new Booking("", "r1", "u1", date, t09, t10, "p"));
        assertThrows(IllegalArgumentException.class, () -> new Booking("b1", "", "u1", date, t09, t10, "p"));
        assertThrows(IllegalArgumentException.class, () -> new Booking("b1", "r1", "", date, t09, t10, "p"));
        assertThrows(IllegalArgumentException.class, () -> new Booking("b1", "r1", "u1", null, t09, t10, "p"));
        assertThrows(IllegalArgumentException.class, () -> new Booking("b1", "r1", "u1", date, null, t10, "p"));
        assertThrows(IllegalArgumentException.class, () -> new Booking("b1", "r1", "u1", date, t10, t09, "p"));

        assertThrows(IllegalArgumentException.class, () -> bookingEntity.update(
                "r2", date.plusDays(1), t10, t09, "invalid"));
        assertEquals("r99", bookingEntity.getRoomId());
        assertEquals(date, bookingEntity.getDate());
        assertEquals(t09, bookingEntity.getStartTime());
        assertEquals(t10, bookingEntity.getEndTime());
        assertEquals("", bookingEntity.getPurpose());
    }

    @Test
    void testBookingSettersValidateBeforeMutation() {
        LocalDate date = LocalDate.now().plusDays(1);
        LocalTime start = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(10, 0);
        Booking booking = new Booking("b1", "r1", "u1", date, start, end, "Original");
        assertThrows(IllegalArgumentException.class, () -> booking.setRoomId(" "));
        assertThrows(IllegalArgumentException.class, () -> booking.setDate(null));
        assertThrows(IllegalArgumentException.class, () -> booking.setTimeInterval(null, end));
        assertThrows(IllegalArgumentException.class, () -> booking.setTimeInterval(end, start));
        assertEquals("r1", booking.getRoomId());
        assertEquals(date, booking.getDate());
        assertEquals(start, booking.getStartTime());
        assertEquals(end, booking.getEndTime());
        booking.setRoomId(" r2 ");
        booking.setDate(date.plusDays(1));
        booking.setTimeInterval(start.plusHours(1), end.plusHours(1));
        booking.setPurpose(" Changed ");
        assertEquals("r2", booking.getRoomId());
        assertEquals(date.plusDays(1), booking.getDate());
        assertEquals(start.plusHours(1), booking.getStartTime());
        assertEquals(end.plusHours(1), booking.getEndTime());
        assertEquals("Changed", booking.getPurpose());
        booking.setPurpose(null);
        assertEquals("", booking.getPurpose());
    }

    @Test
    void testModelPermissionAndValidationBoundaries() throws Exception {
        LocalDate date = LocalDate.now().plusDays(1);
        LocalTime t09 = LocalTime.of(9, 0);
        LocalTime t10 = LocalTime.of(10, 0);
        // CROBS negative & boundary branches
        User reg1 = crobs.addUser("RegOne");
        Room r1 = crobs.addRoom(RoomType.seminarRoom, 25, Collections.singleton("wifi"));
        Booking b1 = crobs.book(r1.getId(), date, t09, t10, "Slot1");
        Booking b2 = crobs.book(r1.getId(), date, LocalTime.of(11, 0), LocalTime.of(12, 0), "Slot2");

        assertThrows(IllegalArgumentException.class, () -> crobs.removeUser(" "));
        assertThrows(CrobsException.class, () -> crobs.removeUser("u999"));
        assertThrows(CrobsException.class, () -> crobs.removeUser("u1"));
        assertThrows(IllegalArgumentException.class, () -> crobs.changeRole(null, UserRole.ADMIN));
        assertThrows(CrobsException.class, () -> crobs.changeRole("u999", UserRole.ADMIN));

        assertThrows(CrobsException.class, () -> crobs.modifyRoom(r1.getId(), "type", "badType"));
        assertThrows(CrobsException.class, () -> crobs.modifyRoom(r1.getId(), "capacity", "notANumber"));
        assertThrows(CrobsException.class, () -> crobs.modifyRoom(r1.getId(), "capacity", "0"));
        assertThrows(CrobsException.class, () -> crobs.modifyRoom(r1.getId(), "unknownField", "val"));
        assertThrows(IllegalArgumentException.class, () -> crobs.listRoom(" "));
        assertThrows(IllegalArgumentException.class, () -> crobs.schedule(r1.getId(), null));
        assertEquals(1, crobs.search(date.plusDays(2), t09, t10, 0, Collections.emptySet()).size());

        // Modify booking conflict against another booking
        assertThrows(CrobsException.class, () -> crobs.modifyBooking(
                b2.getId(), r1.getId(), date, LocalTime.of(9, 30), LocalTime.of(10, 30), "Overlap"));

        // Regular user permission boundaries
        crobs.login(reg1.getId());
        Booking regBooking = crobs.book(r1.getId(), date.plusDays(1), t09, t10, "RegSlot");
        assertEquals(reg1.getId(), crobs.schedule(r1.getId(), date).stream()
                .filter(b -> b.getId().equals(regBooking.getId())).findFirst().get().getUserId());
        assertThrows(CrobsException.class, () -> crobs.addUser("Denied"));
        assertThrows(CrobsException.class, () -> crobs.modifyBooking(b1.getId(), r1.getId(), date, t09, t10, "Denied"));
        assertThrows(CrobsException.class, () -> crobs.deleteBooking(b1.getId()));
        assertThrows(CrobsException.class, () -> crobs.deleteBooking("b999"));

        // Logged-out boundaries
        crobs.logout();
        assertThrows(CrobsException.class, () -> crobs.logout());
        assertThrows(CrobsException.class, () -> crobs.listBookings());
    }

    @Test
    void testRejectedRoleChangesPreserveHistoryAndUniqueSuperadmin() throws Exception {
        User user = crobs.addUser("Alice");
        crobs.changeRole(user.getId(), UserRole.ADMIN);
        crobs.undo();
        int undoCount = crobs.getUndoHistorySize();
        for (String id : new String[]{"u1", " U1 "}) {
            for (UserRole role : UserRole.values()) {
                assertThrows(CrobsException.class, () -> crobs.changeRole(id, role));
            }
        }
        assertThrows(CrobsException.class, () -> crobs.changeRole(user.getId(), UserRole.SUPERADMIN));
        assertEquals(undoCount, crobs.getUndoHistorySize());
        assertTrue(crobs.canRedo());
        crobs.redo();
        assertEquals(1L, crobs.listAllUsers().stream()
                .filter(u -> u.getRole() == UserRole.SUPERADMIN).count());
        assertEquals(UserRole.SUPERADMIN, crobs.getCurrentUser().getRole());
    }

    @Test
    void testRestoredRoomAndBookingSnapshotsSurviveCompleteReplay() throws Exception {
        Room room = crobs.addRoom(RoomType.seminarRoom, 20, Collections.singleton("wifi"));
        LocalDate date = LocalDate.now().plusDays(1);
        Booking booking = crobs.book(room.getId(), date, LocalTime.of(9, 0), LocalTime.of(10, 0), "Original");
        crobs.modifyRoom(room.getId(), "capacity", "40");
        crobs.modifyBooking(booking.getId(), room.getId(), date,
                LocalTime.of(11, 0), LocalTime.of(12, 0), "Changed");
        crobs.deleteRoom(room.getId());
        crobs.undo();
        Room restored = crobs.listRoom(room.getId());
        restored.setCapacity(999);
        crobs.listBookings().get(0).update(room.getId(), date,
                LocalTime.of(13, 0), LocalTime.of(14, 0), "Tampered");
        assertEquals(40, crobs.listRoom(room.getId()).getCapacity());
        assertEquals("Changed", crobs.listBookings().get(0).getPurpose());
        for (int i = 0; i < 4; i++) assertTrue(crobs.undo());
        assertTrue(crobs.listAllRooms().isEmpty());
        for (int i = 0; i < 5; i++) assertTrue(crobs.redo());
        assertTrue(crobs.listAllRooms().isEmpty());
        assertTrue(crobs.listBookings().isEmpty());
        assertTrue(crobs.undo());
        assertEquals(40, crobs.listRoom(room.getId()).getCapacity());
        assertEquals("Changed", crobs.listBookings().get(0).getPurpose());
    }

    /**
     * CLI runner for CROBSTest to verify test suites from terminal.
     *
     */
    @Test
    void testRoomFeaturesSelfAliasingAndDefensiveCopies() {
        Set<String> initFeat = new HashSet<>();
        initFeat.add("wifi");
        initFeat.add("projector");
        Room room = new Room("r1", RoomType.lectureTheatre, 100, initFeat);
        assertEquals(2, room.getFeatures().size());
        assertTrue(room.getFeatures().contains("wifi"));
        assertTrue(room.getFeatures().contains("projector"));

        // Self-aliasing check: setFeatures(room.getFeatures()) must not wipe features
        room.setFeatures(room.getFeatures());
        assertEquals(2, room.getFeatures().size());
        assertTrue(room.getFeatures().contains("wifi"));
        assertTrue(room.getFeatures().contains("projector"));

        // Null and empty string handling
        Set<String> mixed = new HashSet<>();
        mixed.add("whiteboard");
        mixed.add("   ");
        mixed.add(null);
        room.setFeatures(mixed);
        assertEquals(1, room.getFeatures().size());
        assertTrue(room.getFeatures().contains("whiteboard"));

        // Setting null clears features safely
        room.setFeatures(null);
        assertTrue(room.getFeatures().isEmpty());
    }

    @Test
    void testModifyRoomRoomTypeAliasAndUndoRedo() throws CrobsException {
        Room room = crobs.addRoom(RoomType.lectureTheatre, 80, Collections.emptySet());
        assertEquals(RoomType.lectureTheatre, room.getType());

        // Alias: roomType (case-insensitive)
        crobs.modifyRoom(room.getId(), "roomType", "seminarRoom");
        assertEquals(RoomType.seminarRoom, crobs.listRoom(room.getId()).getType());

        // Undo restores lectureTheatre
        assertTrue(crobs.undo());
        assertEquals(RoomType.lectureTheatre, crobs.listRoom(room.getId()).getType());

        // Redo restores seminarRoom
        assertTrue(crobs.redo());
        assertEquals(RoomType.seminarRoom, crobs.listRoom(room.getId()).getType());

        // Mixed case alias: RoOmTyPe
        crobs.modifyRoom(room.getId(), "RoOmTyPe", "computerLab");
        assertEquals(RoomType.computerLab, crobs.listRoom(room.getId()).getType());

        // Invalid roomType value throws CrobsException
        assertThrows(CrobsException.class, () -> crobs.modifyRoom(room.getId(), "roomType", "invalidType"));
    }

    @Test
    void testScheduleGuestAndRolePrivacy() throws CrobsException {
        Room room = crobs.addRoom(RoomType.seminarRoom, 30, Collections.emptySet());
        User u2 = crobs.addUser("Bob Dylan");
        LocalDate date = LocalDate.of(2026, 12, 1);
        crobs.book(room.getId(), date, LocalTime.of(9, 0), LocalTime.of(10, 0), "Admin Keynote");

        crobs.login(u2.getId());
        crobs.book(room.getId(), date, LocalTime.of(11, 0), LocalTime.of(12, 0), "Bob Study Group");

        // 1. Regular user (u2) view: sees own ID, u1 masked, both purposes visible
        List<Booking> u2Sched = crobs.schedule(room.getId(), date);
        assertEquals(2, u2Sched.size());
        assertEquals("(hidden)", u2Sched.get(0).getUserId());
        assertEquals("Admin Keynote", u2Sched.get(0).getPurpose());
        assertEquals(u2.getId(), u2Sched.get(1).getUserId());
        assertEquals("Bob Study Group", u2Sched.get(1).getPurpose());

        // 2. Unauthenticated Guest view (logged out): both IDs masked, both purposes visible
        crobs.logout();
        assertFalse(crobs.isLoggedIn());
        List<Booking> guestSched = crobs.schedule(room.getId(), date);
        assertEquals(2, guestSched.size());
        assertEquals("(hidden)", guestSched.get(0).getUserId());
        assertEquals("Admin Keynote", guestSched.get(0).getPurpose());
        assertEquals("(hidden)", guestSched.get(1).getUserId());
        assertEquals("Bob Study Group", guestSched.get(1).getPurpose());

        // 3. Admin user view (u1): sees all IDs, both purposes visible
        crobs.login("u1");
        List<Booking> adminSched = crobs.schedule(room.getId(), date);
        assertEquals(2, adminSched.size());
        assertEquals("u1", adminSched.get(0).getUserId());
        assertEquals(u2.getId(), adminSched.get(1).getUserId());
    }

    public static void main(String[] args) {
        CROBSTest suite = new CROBSTest();
        int passed = 0;
        int failed = 0;
        for (java.lang.reflect.Method m : CROBSTest.class.getDeclaredMethods()) {
            if (m.isAnnotationPresent(Test.class)) {
                try {
                    suite.setUp();
                    m.invoke(suite);
                    System.out.println("[PASS] " + m.getName());
                    passed++;
                } catch (Throwable t) {
                    System.err.println("[FAIL] " + m.getName() + " -> " + t.getCause());
                    failed++;
                }
            }
        }
        System.out.println("\nAll Tests Executed: " + passed + " passed, " + failed + " failed.");
        if (failed > 0) System.exit(1);
    }

    @Test
    void testDeleteRoomPreservesCompletedSameDayBooking() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(
                LocalTime.now().isAfter(LocalTime.of(0, 2))
                        && LocalTime.now().isBefore(LocalTime.of(23, 45)),
                "Requires a completed early booking and an upcoming late booking");

        Room room = crobs.addRoom(RoomType.meetingRoom, 10, Collections.emptySet());
        LocalDate today = LocalDate.now();
        Booking completed = crobs.book(room.getId(), today,
                LocalTime.of(0, 0), LocalTime.of(0, 1), "Completed meeting");
        Booking upcoming = crobs.book(room.getId(), today,
                LocalTime.of(23, 50), LocalTime.of(23, 59), "Upcoming meeting");

        crobs.deleteRoom(room.getId());
        assertTrue(crobs.listBookings().stream().anyMatch(b -> b.getId().equals(completed.getId())));
        assertFalse(crobs.listBookings().stream().anyMatch(b -> b.getId().equals(upcoming.getId())));

        assertTrue(crobs.undo());
        assertTrue(crobs.listBookings().stream().anyMatch(b -> b.getId().equals(upcoming.getId())));
    }

}
