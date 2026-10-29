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
        assertEquals(UserRole.ADMIN, user1.getRole());

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
        assertEquals(120, room.getCapacity());

        crobs.modifyRoom(room.getId(), "type", "seminarRoom");
        assertEquals(RoomType.seminarRoom, room.getType());

        crobs.modifyRoom(room.getId(), "features", "whiteboard,wifi");
        assertTrue(room.getFeatures().contains("whiteboard"));
        assertFalse(room.getFeatures().contains("projector"));

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
        assertEquals(LocalTime.of(8, 0), booking.getStartTime());

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
    void testInvalidOperations() {
        assertThrows(IllegalArgumentException.class, () -> crobs.login(""));
        assertThrows(CrobsException.class, () -> crobs.login("non_existent_id"));
        assertThrows(IllegalArgumentException.class, () -> crobs.addUser("   "));
        assertThrows(CrobsException.class, () -> crobs.listRoom("invalid_room"));
    }

    /**
     * CLI runner for CROBSTest to verify test suites from terminal.
     *
     * @param args command line arguments.
     */
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
}
