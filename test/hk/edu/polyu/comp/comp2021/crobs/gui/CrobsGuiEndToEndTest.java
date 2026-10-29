package hk.edu.polyu.comp.comp2021.crobs.gui;

import hk.edu.polyu.comp.comp2021.crobs.model.Booking;
import hk.edu.polyu.comp.comp2021.crobs.model.CROBS;
import hk.edu.polyu.comp.comp2021.crobs.model.Room;
import hk.edu.polyu.comp.comp2021.crobs.model.RoomType;
import hk.edu.polyu.comp.comp2021.crobs.model.User;
import hk.edu.polyu.comp.comp2021.crobs.model.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration test verifying complete multi-user workflow across all requirements.
 */
public class CrobsGuiEndToEndTest {

    private CROBS model;
    private GuiSessionLogger logger;
    private GuiController controller;
    private File tempHtml;
    private File tempTxt;

    @BeforeEach
    void setUp() throws IOException {
        tempHtml = File.createTempFile("crobs_e2e_log_", ".html");
        tempTxt = File.createTempFile("crobs_e2e_log_", ".txt");

        model = new CROBS();
        logger = new GuiSessionLogger(tempHtml.getAbsolutePath(), tempTxt.getAbsolutePath());
        controller = new GuiController(model, logger);
    }

    @AfterEach
    void tearDown() {
        if (controller != null) {
            controller.closeLogger();
        }
        if (tempHtml != null && tempHtml.exists()) {
            tempHtml.delete();
        }
        if (tempTxt != null && tempTxt.exists()) {
            tempTxt.delete();
        }
    }

    @Test
    void testFullTwentyStepEndToEndScenario() throws Exception {
        // Step 1: Login as initial superadmin u1
        controller.login("u1");
        assertTrue(controller.isLoggedIn());
        assertEquals(UserRole.SUPERADMIN, controller.getCurrentUser().getRole());

        // Step 2: Superadmin adds user Alice
        User alice = controller.addUser("Alice");
        assertNotNull(alice);
        assertEquals("Alice", alice.getName());

        // Step 3: Superadmin adds user Bob
        User bob = controller.addUser("Bob");
        assertNotNull(bob);
        assertEquals("Bob", bob.getName());

        // Step 4: Promote Alice to ADMIN
        controller.changeRole(alice.getId(), UserRole.ADMIN);
        assertEquals(UserRole.ADMIN, alice.getRole());

        // Step 5: Add Room R1 (computerLab, 40 seats, Projector, Wi-Fi)
        Room r1 = controller.addRoom(RoomType.computerLab, 40, new HashSet<>(Arrays.asList("Projector", "Wi-Fi")));
        assertNotNull(r1);
        String r1Id = r1.getId();

        // Step 6: Add Room R2 (meetingRoom, 15 seats, Wi-Fi)
        Room r2 = controller.addRoom(RoomType.meetingRoom, 15, Collections.singleton("Wi-Fi"));
        assertNotNull(r2);
        String r2Id = r2.getId();

        // Step 7: Add Room R3 (lectureTheatre, 120 seats, Projector)
        Room r3 = controller.addRoom(RoomType.lectureTheatre, 120, Collections.singleton("Projector"));
        assertNotNull(r3);
        String r3Id = r3.getId();

        // Step 8: Modify Room R1 capacity from 40 to 45
        controller.modifyRoom(r1Id, "capacity", "45");
        assertEquals(45, r1.getCapacity());

        // Step 9: Search available rooms for tomorrow 09:00 - 11:00 (REQ13)
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalTime t0900 = LocalTime.of(9, 0);
        LocalTime t1100 = LocalTime.of(11, 0);
        List<Room> availableStep9 = controller.search(tomorrow, t0900, t1100, 30, Collections.singleton("Wi-Fi"));
        assertEquals(1, availableStep9.size());
        assertEquals(r1Id, availableStep9.get(0).getId());

        // Step 10: Book Room R1 for tomorrow 09:00 - 11:00 (Purpose: COMP2021 Lecture)
        Booking b1 = controller.book(r1Id, tomorrow, t0900, t1100, "COMP2021_Lecture");
        assertNotNull(b1);
        assertEquals(r1Id, b1.getRoomId());

        // Step 11: Search available rooms again, confirm R1 is now occupied
        List<Room> availableStep11 = controller.search(tomorrow, t0900, t1100, 30, Collections.singleton("Wi-Fi"));
        assertTrue(availableStep11.isEmpty());

        // Step 12: Switch user to Alice (Admin)
        controller.login(alice.getId());
        assertEquals(alice.getId(), controller.getCurrentUser().getId());
        assertEquals(UserRole.ADMIN, controller.getCurrentUser().getRole());

        // Step 13: Alice books R2 for tomorrow 10:00 - 12:00
        LocalTime t1000 = LocalTime.of(10, 0);
        LocalTime t1200 = LocalTime.of(12, 0);
        Booking b2 = controller.book(r2Id, tomorrow, t1000, t1200, "Admin_Project_Meeting");
        assertNotNull(b2);

        // Step 14: Alice views her bookings
        List<Booking> aliceBookings = controller.userListBookings();
        assertEquals(1, aliceBookings.size());
        assertEquals(b2.getId(), aliceBookings.get(0).getId());

        // Step 15: Alice (Admin) views schedule of R1 -> Admin can see purpose
        List<Booking> r1SchedAlice = controller.schedule(r1Id, tomorrow);
        assertEquals(1, r1SchedAlice.size());
        assertEquals("COMP2021_Lecture", r1SchedAlice.get(0).getPurpose());

        // Step 16: Switch user to Bob (Regular User)
        controller.login(bob.getId());
        assertEquals(bob.getId(), controller.getCurrentUser().getId());
        assertEquals(UserRole.REGULAR, controller.getCurrentUser().getRole());

        // Step 17: Bob views schedule of R1
        List<Booking> r1SchedBob = controller.schedule(r1Id, tomorrow);
        assertEquals(1, r1SchedBob.size());
        assertEquals(b1.getId(), r1SchedBob.get(0).getId());

        // Step 18: Switch back to u1 (Superadmin)
        controller.login("u1");
        assertEquals(UserRole.SUPERADMIN, controller.getCurrentUser().getRole());

        // Step 19: Modify booking b1 end time from 11:00 to 11:30 (REQ11)
        LocalTime t1130 = LocalTime.of(11, 30);
        controller.modifyBooking(b1.getId(), r1Id, tomorrow, t0900, t1130, "Extended_COMP2021_Lecture");
        assertEquals(t1130, b1.getEndTime());
        assertEquals("Extended_COMP2021_Lecture", b1.getPurpose());

        // Step 20: Cancel booking b2 as Alice or Admin
        controller.login(alice.getId());
        controller.deleteBooking(b2.getId());

        // Verify R2 is now available again for 10:00 - 12:00
        List<Room> availR2 = controller.search(tomorrow, t1000, t1200, 10, Collections.singleton("Wi-Fi"));
        assertTrue(availR2.stream().anyMatch(r -> r.getId().equalsIgnoreCase(r2Id)));

        // Verify Log files
        controller.closeLogger();
        assertTrue(tempHtml.exists());
        assertTrue(tempTxt.exists());

        String txtContent = new String(Files.readAllBytes(tempTxt.toPath()));
        assertTrue(txtContent.contains("addUser"));
        assertTrue(txtContent.contains("Alice"));
        assertTrue(txtContent.contains("addRoom"));
        assertTrue(txtContent.contains("book"));
        assertTrue(txtContent.contains("deleteBooking"));

        String htmlContent = new String(Files.readAllBytes(tempHtml.toPath()));
        assertTrue(htmlContent.contains("<table"));
        assertTrue(htmlContent.contains("COMP2021_Lecture"));
    }
}
