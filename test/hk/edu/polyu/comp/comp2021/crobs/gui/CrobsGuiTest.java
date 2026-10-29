package hk.edu.polyu.comp.comp2021.crobs.gui;

import hk.edu.polyu.comp.comp2021.crobs.gui.dialog.ConfirmationDialog;
import hk.edu.polyu.comp.comp2021.crobs.gui.dialog.LoginDialog;
import hk.edu.polyu.comp.comp2021.crobs.gui.dialog.RoomEditorDialog;
import hk.edu.polyu.comp.comp2021.crobs.gui.view.FindRoomsPanel;
import hk.edu.polyu.comp.comp2021.crobs.gui.view.ManagePanel;
import hk.edu.polyu.comp.comp2021.crobs.gui.view.MyBookingsPanel;
import hk.edu.polyu.comp.comp2021.crobs.gui.view.RoomSchedulePanel;
import hk.edu.polyu.comp.comp2021.crobs.model.Booking;
import hk.edu.polyu.comp.comp2021.crobs.model.CROBS;
import hk.edu.polyu.comp.comp2021.crobs.model.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.model.Room;
import hk.edu.polyu.comp.comp2021.crobs.model.RoomType;
import hk.edu.polyu.comp.comp2021.crobs.model.User;
import hk.edu.polyu.comp.comp2021.crobs.model.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JFrame;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test suite for the CROBS Java Swing GUI architecture (BON1).
 */
public class CrobsGuiTest {

    private CROBS model;
    private GuiSessionLogger logger;
    private GuiController controller;
    private File tempHtml;
    private File tempTxt;

    @BeforeEach
    void setUp() throws IOException {
        tempHtml = File.createTempFile("crobs_test_log_", ".html");
        tempTxt = File.createTempFile("crobs_test_log_", ".txt");

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
    void testThemeConstantsAndFactories() {
        assertNotNull(GuiTheme.COLOR_BG);
        assertNotNull(GuiTheme.COLOR_BLUE);
        assertNotNull(GuiTheme.COLOR_BORDER);
        assertNotNull(GuiTheme.FONT_H1);
        assertNotNull(GuiTheme.FONT_BODY);

        JButton primaryBtn = GuiTheme.createPrimaryButton("Action");
        assertNotNull(primaryBtn);
        assertEquals("Action", primaryBtn.getText());

        JButton secondaryBtn = GuiTheme.createSecondaryButton("Cancel");
        assertNotNull(secondaryBtn);

        JButton textBtn = GuiTheme.createTextButton("View");
        assertNotNull(textBtn);

        JButton dangerBtn = GuiTheme.createDangerTextButton("Delete");
        assertNotNull(dangerBtn);
    }

    @Test
    void testInitialSessionState() {
        assertFalse(controller.isLoggedIn());
        assertNull(controller.getCurrentUser());
    }

    @Test
    void testLoginAndLogoutLifecycle() throws CrobsException {
        AtomicBoolean listenerNotified = new AtomicBoolean(false);
        controller.addListener(() -> listenerNotified.set(true));

        controller.login("u1");
        assertTrue(controller.isLoggedIn());
        assertTrue(listenerNotified.get());
        assertEquals(UserRole.SUPERADMIN, controller.getCurrentUser().getRole());

        listenerNotified.set(false);
        controller.logout();
        assertFalse(controller.isLoggedIn());
        assertTrue(listenerNotified.get());
        assertNull(controller.getCurrentUser());
    }

    @Test
    void testAddUserAndRoleManagement() throws CrobsException {
        controller.login("u1");

        User user = controller.addUser("Carol");
        assertNotNull(user);
        assertEquals("Carol", user.getName());
        assertEquals(UserRole.REGULAR, user.getRole());

        controller.changeRole(user.getId(), UserRole.ADMIN);
        assertEquals(UserRole.ADMIN, user.getRole());
    }

    @Test
    void testRoomManagement() throws CrobsException {
        controller.login("u1");

        Set<String> equip = new HashSet<>();
        equip.add("Projector");
        equip.add("Wi-Fi");

        Room room = controller.addRoom(RoomType.seminarRoom, 50, equip);
        assertNotNull(room);
        String roomId = room.getId();
        assertEquals(50, room.getCapacity());

        controller.modifyRoom(roomId, "capacity", "60");
        assertEquals(60, room.getCapacity());

        controller.modifyRoom(roomId, "type", "meetingRoom");
        assertEquals(RoomType.meetingRoom, room.getType());

        controller.modifyRoom(roomId, "features", "Whiteboard");
        assertTrue(room.getFeatures().contains("Whiteboard"));
        assertFalse(room.getFeatures().contains("Projector"));

        List<Room> allRooms = controller.getAllRoomsQuiet();
        assertTrue(allRooms.stream().anyMatch(r -> r.getId().equalsIgnoreCase(roomId)));

        controller.deleteRoom(roomId);
        allRooms = controller.getAllRoomsQuiet();
        assertFalse(allRooms.stream().anyMatch(r -> r.getId().equalsIgnoreCase(roomId)));
    }

    @Test
    void testBookingLifecycle() throws CrobsException {
        controller.login("u1");

        Room room = controller.addRoom(RoomType.seminarRoom, 30, Collections.emptySet());
        String roomId = room.getId();

        LocalDate date = LocalDate.now().plusDays(2);
        LocalTime start = LocalTime.of(10, 0);
        LocalTime end = LocalTime.of(12, 0);

        List<Room> available = controller.search(date, start, end, 20, Collections.emptySet());
        assertTrue(available.stream().anyMatch(r -> r.getId().equalsIgnoreCase(roomId)));

        Booking booking = controller.book(roomId, date, start, end, "Unit Test Session");
        assertNotNull(booking);
        assertEquals(roomId, booking.getRoomId());
        assertEquals("Unit_Test_Session", booking.getPurpose());

        List<Booking> myBookings = controller.userListBookings();
        assertTrue(myBookings.stream().anyMatch(b -> b.getId().equals(booking.getId())));

        // Test modify booking
        LocalTime newEnd = LocalTime.of(12, 30);
        controller.modifyBooking(booking.getId(), roomId, date, start, newEnd, "Extended Session");
        assertEquals(newEnd, booking.getEndTime());
        assertEquals("Extended_Session", booking.getPurpose());

        // Test cancel booking
        controller.deleteBooking(booking.getId());
        myBookings = controller.userListBookings();
        assertFalse(myBookings.stream().anyMatch(b -> b.getId().equals(booking.getId())));
    }

    @Test
    void testRoomScheduleAndPrivacyMasking() throws CrobsException {
        controller.login("u1");
        Room room = controller.addRoom(RoomType.seminarRoom, 40, Collections.emptySet());
        String roomId = room.getId();
        User student = controller.addUser("Dave");

        LocalDate date = LocalDate.now().plusDays(3);
        Booking booking = controller.book(roomId, date, LocalTime.of(14, 0), LocalTime.of(16, 0), "Private Superadmin Meeting");

        // Superadmin views schedule -> can see purpose
        List<Booking> adminView = controller.schedule(roomId, date);
        assertFalse(adminView.isEmpty());
        Booking adminB = adminView.get(0);
        assertEquals("Private_Superadmin_Meeting", adminB.getPurpose());

        // Student Dave views schedule -> can query schedule through controller
        controller.login(student.getId());
        List<Booking> studentView = controller.schedule(roomId, date);
        assertFalse(studentView.isEmpty());
        Booking studentB = studentView.get(0);
        assertEquals(booking.getId(), studentB.getId());
        assertFalse(controller.isAdmin());
    }

    @Test
    void testSessionLoggerOutput() throws IOException, CrobsException {
        controller.login("u1");
        controller.addUser("Eve");
        controller.closeLogger();

        assertTrue(tempHtml.exists());
        assertTrue(tempTxt.exists());

        String htmlContent = new String(Files.readAllBytes(tempHtml.toPath()));
        String txtContent = new String(Files.readAllBytes(tempTxt.toPath()));

        assertTrue(htmlContent.contains("CROBS Session Log"));
        assertTrue(htmlContent.contains("addUser"));
        assertTrue(htmlContent.contains("Eve"));

        assertTrue(txtContent.contains("addUser"));
        assertTrue(txtContent.contains("Eve"));
    }

    @Test
    void testGuiViewPanelsInstantiation() {
        FindRoomsPanel findPanel = new FindRoomsPanel(controller, (r, d) -> {}, () -> {}, () -> {});
        assertNotNull(findPanel);

        MyBookingsPanel bookingsPanel = new MyBookingsPanel(controller, () -> {});
        assertNotNull(bookingsPanel);

        RoomSchedulePanel schedulePanel = new RoomSchedulePanel(controller, () -> {});
        assertNotNull(schedulePanel);

        ManagePanel managePanel = new ManagePanel(controller);
        assertNotNull(managePanel);

        JFrame frame = new JFrame();
        LoginDialog loginDialog = new LoginDialog(frame, controller);
        assertNotNull(loginDialog);

        RoomEditorDialog roomDialog = new RoomEditorDialog(frame, controller, null);
        assertNotNull(roomDialog);

        ConfirmationDialog confDialog = new ConfirmationDialog(frame, "Test Confirm", "Are you sure?", "Delete");
        assertNotNull(confDialog);
    }

    @Test
    void testMainFrameAndDrawerInteraction() throws CrobsException {
        controller.loginQuiet("u1");
        MainFrame mainFrame = new MainFrame(controller);
        assertNotNull(mainFrame);
        assertNotNull(mainFrame.getDrawerPanel());
        assertFalse(mainFrame.getDrawerPanel().isVisible());

        // Test Drawer Open/Close
        controller.addRoomQuiet(RoomType.lectureTheatre, 100, Collections.singleton("wifi"));
        Room r = controller.getAllRoomsQuiet().get(0);

        mainFrame.openBookFromSearch(r, LocalDate.of(2026, 11, 3), LocalTime.of(14, 0), LocalTime.of(15, 0));
        assertTrue(mainFrame.getDrawerPanel().isVisible());

        mainFrame.closeDrawer();
        assertFalse(mainFrame.getDrawerPanel().isVisible());

        // Test Navigation & User Switch
        User u2 = controller.addUserQuiet("Alice");
        controller.login(u2.getId());
        assertEquals("Alice", controller.getCurrentUser().getName());
        assertFalse(controller.isAdmin());

        mainFrame.navigateTo(MainFrame.VIEW_FIND);
        mainFrame.dispose();
    }
}
