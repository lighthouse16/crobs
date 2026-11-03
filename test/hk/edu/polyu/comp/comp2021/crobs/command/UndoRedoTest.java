package hk.edu.polyu.comp.comp2021.crobs.command;

import hk.edu.polyu.comp.comp2021.crobs.gui.GuiController;
import hk.edu.polyu.comp.comp2021.crobs.gui.GuiSessionLogger;
import hk.edu.polyu.comp.comp2021.crobs.model.Booking;
import hk.edu.polyu.comp.comp2021.crobs.model.CROBS;
import hk.edu.polyu.comp.comp2021.crobs.model.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.model.Room;
import hk.edu.polyu.comp.comp2021.crobs.model.RoomType;
import hk.edu.polyu.comp.comp2021.crobs.model.User;
import hk.edu.polyu.comp.comp2021.crobs.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for Bonus Feature 2 (BON2): Command Pattern Undo & Redo.
 * Verifies all 9 mutating operations across Model, Controller, and GUI levels.
 */
public class UndoRedoTest {

    private CROBS crobs;

    @BeforeEach
    void setUp() throws Exception {
        crobs = new CROBS();
        crobs.login("u1"); // Logged in as initial Superadmin
    }

    @Test
    void testUndoRedoRequireSameLoggedInActor() throws CrobsException {
        crobs.addUser("Alice");
        crobs.logout();

        assertThrows(CrobsException.class, () -> crobs.undo());
        assertThrows(CrobsException.class, () -> crobs.redo());
        assertDoesNotThrow(() -> crobs.login("u2"));

        assertThrows(CrobsException.class, () -> crobs.undo());
        assertFalse(crobs.redo());

        crobs.login("u1");
        assertTrue(crobs.undo());
        assertThrows(CrobsException.class, () -> crobs.login("u2"));
    }

    @Test
    void testRoleChangeHistoryCannotBeUsedByTargetUser() throws CrobsException {
        User user = crobs.addUser("Target");
        crobs.changeRole(user.getId(), UserRole.ADMIN);
        crobs.login(user.getId());

        assertThrows(CrobsException.class, () -> crobs.undo());
        assertEquals(UserRole.ADMIN, crobs.getCurrentUser().getRole());

        crobs.login("u1");
        assertTrue(crobs.undo());
        assertEquals(UserRole.REGULAR,
                crobs.listAllUsers().stream()
                        .filter(u -> u.getId().equals(user.getId()))
                        .findFirst().get().getRole());
    }

    @Test
    void testEmptyUndoRedo() throws CrobsException {
        assertFalse(crobs.canUndo());
        assertFalse(crobs.canRedo());
        assertFalse(crobs.undo());
        assertFalse(crobs.redo());
    }

    @Test
    void testAddUserUndoRedo() throws CrobsException {
        User u2 = crobs.addUser("Alice");
        assertEquals("u2", u2.getId());
        assertTrue(crobs.canUndo());
        assertFalse(crobs.canRedo());

        // Undo -> u2 should be removed
        boolean undone = crobs.undo();
        assertTrue(undone);
        assertEquals(1, crobs.listAllUsers().size());
        assertThrows(CrobsException.class, () -> crobs.login("u2"));
        assertTrue(crobs.canRedo());

        // Redo -> u2 should be restored with exact same ID
        boolean redone = crobs.redo();
        assertTrue(redone);
        assertEquals(2, crobs.listAllUsers().size());
        assertDoesNotThrow(() -> crobs.login("u2"));
    }

    @Test
    void testRemoveUserWithCascadingBookingsUndoRedo() throws CrobsException {
        User u2 = crobs.addUser("Bob");
        Room r1 = crobs.addRoom(RoomType.lectureTheatre, 100, Collections.emptySet());

        // Bob logs in and creates a booking
        crobs.login("u2");
        LocalDate date = LocalDate.now().plusDays(2);
        Booking b1 = crobs.book(r1.getId(), date, LocalTime.of(10, 0), LocalTime.of(12, 0), "Seminar");

        // Superadmin logs in and removes Bob
        crobs.login("u1");
        crobs.removeUser("u2");

        // Verify Bob and booking are gone
        assertThrows(CrobsException.class, () -> crobs.login("u2"));
        assertEquals(0, crobs.schedule(r1.getId(), date).size());

        // Undo removeUser -> Bob and his booking should be restored!
        assertTrue(crobs.undo());
        assertDoesNotThrow(() -> crobs.login("u2"));
        crobs.login("u1");
        assertEquals(1, crobs.schedule(r1.getId(), date).size());

        // Redo removeUser -> Bob and his booking removed again
        assertTrue(crobs.redo());
        assertThrows(CrobsException.class, () -> crobs.login("u2"));
        assertEquals(0, crobs.schedule(r1.getId(), date).size());
    }

    @Test
    void testChangeRoleUndoRedo() throws CrobsException {
        User u2 = crobs.addUser("Charlie");
        assertEquals(UserRole.REGULAR, crobs.listAllUsers().stream().filter(u -> u.getId().equals(u2.getId())).findFirst().get().getRole());

        crobs.changeRole("u2", UserRole.ADMIN);
        assertEquals(UserRole.ADMIN, crobs.listAllUsers().stream().filter(u -> u.getId().equals(u2.getId())).findFirst().get().getRole());

        // Undo -> back to REGULAR
        assertTrue(crobs.undo());
        assertEquals(UserRole.REGULAR, crobs.listAllUsers().stream().filter(u -> u.getId().equals(u2.getId())).findFirst().get().getRole());

        // Redo -> back to ADMIN
        assertTrue(crobs.redo());
        assertEquals(UserRole.ADMIN, crobs.listAllUsers().stream().filter(u -> u.getId().equals(u2.getId())).findFirst().get().getRole());
    }

    @Test
    void testAddRoomUndoRedo() throws CrobsException {
        Set<String> features = new HashSet<>();
        features.add("Projector");
        Room r1 = crobs.addRoom(RoomType.seminarRoom, 40, features);
        assertEquals("r1", r1.getId());
        assertEquals(1, crobs.listAllRooms().size());

        // Undo -> room removed
        assertTrue(crobs.undo());
        assertEquals(0, crobs.listAllRooms().size());
        assertThrows(CrobsException.class, () -> crobs.listRoom("r1"));

        // Redo -> room restored with same ID and features
        assertTrue(crobs.redo());
        assertEquals(1, crobs.listAllRooms().size());
        Room restored = crobs.listRoom("r1");
        assertEquals(40, restored.getCapacity());
        assertTrue(restored.getFeatures().contains("Projector"));
    }

    @Test
    void testModifyRoomUndoRedo() throws CrobsException {
        Room r1 = crobs.addRoom(RoomType.seminarRoom, 40, Collections.emptySet());

        // Modify capacity
        crobs.modifyRoom(r1.getId(), "capacity", "80");
        assertEquals(80, crobs.listRoom(r1.getId()).getCapacity());

        assertTrue(crobs.undo());
        assertEquals(40, crobs.listRoom(r1.getId()).getCapacity());

        assertTrue(crobs.redo());
        assertEquals(80, crobs.listRoom(r1.getId()).getCapacity());

        // Modify type
        crobs.modifyRoom(r1.getId(), "type", "computerLab");
        assertEquals(RoomType.computerLab, crobs.listRoom(r1.getId()).getType());

        assertTrue(crobs.undo());
        assertEquals(RoomType.seminarRoom, crobs.listRoom(r1.getId()).getType());

        assertTrue(crobs.redo());
        assertEquals(RoomType.computerLab, crobs.listRoom(r1.getId()).getType());

        // Modify features
        crobs.modifyRoom(r1.getId(), "features", "Whiteboard,WiFi");
        assertEquals(2, crobs.listRoom(r1.getId()).getFeatures().size());

        assertTrue(crobs.undo());
        assertEquals(0, crobs.listRoom(r1.getId()).getFeatures().size());

        assertTrue(crobs.redo());
        assertEquals(2, crobs.listRoom(r1.getId()).getFeatures().size());
    }

    @Test
    void testDeleteRoomWithCascadingBookingsUndoRedo() throws CrobsException {
        Room r1 = crobs.addRoom(RoomType.meetingRoom, 12, Collections.emptySet());
        LocalDate date = LocalDate.now().plusDays(3);
        Booking b1 = crobs.book(r1.getId(), date, LocalTime.of(9, 0), LocalTime.of(11, 0), "TeamSync");

        // Delete room
        crobs.deleteRoom(r1.getId());
        assertEquals(0, crobs.listAllRooms().size());

        // Undo deleteRoom -> room and future booking restored
        assertTrue(crobs.undo());
        assertEquals(1, crobs.listAllRooms().size());
        assertEquals(1, crobs.schedule(r1.getId(), date).size());

        // Redo deleteRoom -> room and future booking removed again
        assertTrue(crobs.redo());
        assertEquals(0, crobs.listAllRooms().size());
    }

    @Test
    void testBookUndoRedo() throws CrobsException {
        Room r1 = crobs.addRoom(RoomType.seminarRoom, 50, Collections.emptySet());
        LocalDate date = LocalDate.now().plusDays(1);
        Booking b1 = crobs.book(r1.getId(), date, LocalTime.of(14, 0), LocalTime.of(16, 0), "Lecture");
        assertEquals("b1", b1.getId());
        assertEquals(1, crobs.listBookings().size());

        // Undo -> booking cancelled
        assertTrue(crobs.undo());
        assertEquals(0, crobs.listBookings().size());

        // Redo -> booking restored
        assertTrue(crobs.redo());
        assertEquals(1, crobs.listBookings().size());
        assertEquals("b1", crobs.listBookings().get(0).getId());
    }

    @Test
    void testModifyBookingUndoRedo() throws CrobsException {
        Room r1 = crobs.addRoom(RoomType.seminarRoom, 50, Collections.emptySet());
        LocalDate d1 = LocalDate.now().plusDays(1);
        LocalDate d2 = LocalDate.now().plusDays(2);

        Booking b1 = crobs.book(r1.getId(), d1, LocalTime.of(10, 0), LocalTime.of(11, 0), "Purpose1");

        // Modify booking to d2 and 14:00-15:00
        crobs.modifyBooking(b1.getId(), r1.getId(), d2, LocalTime.of(14, 0), LocalTime.of(15, 0), "Purpose2");
        Booking current = crobs.listBookings().get(0);
        assertEquals(d2, current.getDate());
        assertEquals(LocalTime.of(14, 0), current.getStartTime());
        assertEquals("Purpose2", current.getPurpose());

        // Undo modifyBooking -> returns to original values
        assertTrue(crobs.undo());
        Booking reverted = crobs.listBookings().get(0);
        assertEquals(d1, reverted.getDate());
        assertEquals(LocalTime.of(10, 0), reverted.getStartTime());
        assertEquals("Purpose1", reverted.getPurpose());

        // Redo modifyBooking -> returns to updated values
        assertTrue(crobs.redo());
        Booking reupdated = crobs.listBookings().get(0);
        assertEquals(d2, reupdated.getDate());
        assertEquals(LocalTime.of(14, 0), reupdated.getStartTime());
        assertEquals("Purpose2", reupdated.getPurpose());
    }

    @Test
    void testDeleteBookingUndoRedo() throws CrobsException {
        Room r1 = crobs.addRoom(RoomType.seminarRoom, 50, Collections.emptySet());
        LocalDate d1 = LocalDate.now().plusDays(1);
        Booking b1 = crobs.book(r1.getId(), d1, LocalTime.of(10, 0), LocalTime.of(11, 0), "Study");

        crobs.deleteBooking(b1.getId());
        assertEquals(0, crobs.listBookings().size());

        // Undo -> booking restored
        assertTrue(crobs.undo());
        assertEquals(1, crobs.listBookings().size());

        // Redo -> booking deleted again
        assertTrue(crobs.redo());
        assertEquals(0, crobs.listBookings().size());
    }

    @Test
    void testNewOperationClearsRedoStack() throws CrobsException {
        crobs.addUser("User1");
        crobs.addUser("User2");
        assertEquals(3, crobs.listAllUsers().size());

        // Undo User2
        crobs.undo();
        assertEquals(2, crobs.listAllUsers().size());
        assertTrue(crobs.canRedo());

        // Perform new operation -> should discard redo history
        crobs.addUser("User3");
        assertFalse(crobs.canRedo());

        // Undo should undo User3, NOT User2
        crobs.undo();
        assertEquals(2, crobs.listAllUsers().size());
        assertEquals("User1", crobs.listAllUsers().get(1).getName());
    }

    @Test
    void testNonMutatingOperationsDoNotAffectStacks() throws CrobsException {
        Room r1 = crobs.addRoom(RoomType.seminarRoom, 30, Collections.emptySet());
        assertTrue(crobs.canUndo());
        assertFalse(crobs.canRedo());

        crobs.undo();
        assertTrue(crobs.canRedo());

        // Execute non-mutating operations
        crobs.search(LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(10, 0), 10, Collections.emptySet());
        crobs.listAllRooms();
        crobs.logout();
        crobs.login("u1");

        // Stacks must remain intact
        assertTrue(crobs.canRedo());
        crobs.redo();
        assertEquals(1, crobs.listAllRooms().size());
    }

    @Test
    void testGuiControllerUndoRedo() throws CrobsException {
        GuiSessionLogger logger = new GuiSessionLogger(null, null);
        GuiController controller = new GuiController(crobs, logger);

        AtomicInteger changeCount = new AtomicInteger(0);
        controller.addListener(changeCount::incrementAndGet);

        assertFalse(controller.canUndo());
        assertFalse(controller.canRedo());

        // Mutating action via controller
        controller.addUser("Eva");
        int countAfterAdd = changeCount.get();
        assertTrue(countAfterAdd > 0);
        assertTrue(controller.canUndo());

        // Controller Undo
        boolean undone = controller.undo();
        assertTrue(undone);
        assertTrue(changeCount.get() > countAfterAdd);
        assertTrue(controller.canRedo());

        // Controller Redo
        int countBeforeRedo = changeCount.get();
        boolean redone = controller.redo();
        assertTrue(redone);
        assertTrue(changeCount.get() > countBeforeRedo);
    }

    @Test
    void testApplicationCliUndoRedo() throws Exception {
        java.io.File tempTxt = java.io.File.createTempFile("crobs_cli_log", ".txt");
        java.io.File tempHtml = java.io.File.createTempFile("crobs_cli_log", ".html");
        tempTxt.deleteOnExit();
        tempHtml.deleteOnExit();

        String simulatedInput = String.join(System.lineSeparator(),
                "login u1",
                "addUser CliAlice",
                "undo",
                "redo",
                "undo",
                "quit"
        );

        java.io.InputStream originalIn = System.in;
        java.io.PrintStream originalOut = System.out;
        try {
            System.setIn(new java.io.ByteArrayInputStream(simulatedInput.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            java.io.ByteArrayOutputStream outCaptor = new java.io.ByteArrayOutputStream();
            System.setOut(new java.io.PrintStream(outCaptor, true, "UTF-8"));

            hk.edu.polyu.comp.comp2021.crobs.Application app =
                    new hk.edu.polyu.comp.comp2021.crobs.Application(tempHtml.getAbsolutePath(), tempTxt.getAbsolutePath());
            app.run();

            String output = outCaptor.toString(java.nio.charset.StandardCharsets.UTF_8.name());
            assertTrue(output.contains("Undo successful."));
            assertTrue(output.contains("Redo successful."));

            // Check log file content
            String txtContent = new String(java.nio.file.Files.readAllBytes(tempTxt.toPath()), java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(txtContent.contains("undo"));
            assertTrue(txtContent.contains("redo"));
        } finally {
            System.setIn(originalIn);
            System.setOut(originalOut);
        }
    }

    @Test
    void testApplicationCliAcceptsEmptyFeatureSentinel() throws Exception {
        java.io.File tempTxt = java.io.File.createTempFile("crobs_empty_features", ".txt");
        java.io.File tempHtml = java.io.File.createTempFile("crobs_empty_features", ".html");
        tempTxt.deleteOnExit();
        tempHtml.deleteOnExit();

        String simulatedInput = String.join(System.lineSeparator(),
                "login u1",
                "addRoom meetingRoom 10 -",
                "search 2026-11-10 09:00 10:00 1 -",
                "quit");

        java.io.InputStream originalIn = System.in;
        java.io.PrintStream originalOut = System.out;
        try {
            System.setIn(new java.io.ByteArrayInputStream(
                    simulatedInput.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            java.io.ByteArrayOutputStream outCaptor = new java.io.ByteArrayOutputStream();
            System.setOut(new java.io.PrintStream(outCaptor, true, "UTF-8"));

            hk.edu.polyu.comp.comp2021.crobs.Application app =
                    new hk.edu.polyu.comp.comp2021.crobs.Application(
                            tempHtml.getAbsolutePath(), tempTxt.getAbsolutePath());
            app.run();

            String output = outCaptor.toString(java.nio.charset.StandardCharsets.UTF_8.name());
            assertTrue(output.contains("Room created: r1"));
            assertTrue(output.contains("Available Rooms (1):"));
            assertTrue(app.getCrobs().listRoom("r1").getFeatures().isEmpty());
        } finally {
            System.setIn(originalIn);
            System.setOut(originalOut);
        }
    }

    @Test
    void testUndoRedoHistoryDiagnostics() throws CrobsException {
        assertEquals(0, crobs.getUndoHistorySize());
        crobs.addUser("TestUser");
        assertEquals(1, crobs.getUndoHistorySize());

        crobs.undo();
        assertEquals(0, crobs.getUndoHistorySize());
        assertTrue(crobs.canRedo());
    }
}
