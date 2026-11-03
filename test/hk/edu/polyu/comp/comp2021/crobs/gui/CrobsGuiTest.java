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
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.GraphicsEnvironment;

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
        assertEquals(UserRole.ADMIN, controller.getAllKnownUsers().stream().filter(u -> u.getId().equals(user.getId())).findFirst().get().getRole());
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
        assertEquals(60, controller.getRoomQuiet(roomId).getCapacity());

        controller.modifyRoom(roomId, "type", "meetingRoom");
        assertEquals(RoomType.meetingRoom, controller.getRoomQuiet(roomId).getType());

        controller.modifyRoom(roomId, "features", "Whiteboard");
        assertTrue(controller.getRoomQuiet(roomId).getFeatures().contains("Whiteboard"));
        assertFalse(controller.getRoomQuiet(roomId).getFeatures().contains("Projector"));

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
        assertEquals(newEnd, controller.getBookingsQuiet().stream().filter(b -> b.getId().equals(booking.getId())).findFirst().get().getEndTime());
        assertEquals("Extended_Session", controller.getBookingsQuiet().stream().filter(b -> b.getId().equals(booking.getId())).findFirst().get().getPurpose());

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
        assertEquals("(hidden)", studentB.getUserId());
        assertEquals("Private_Superadmin_Meeting", studentB.getPurpose());
        assertFalse(controller.isAdmin());
    }

    @Test
    void testEmptyFeatureCommandsUseReplayableSentinel() throws IOException, CrobsException {
        controller.login("u1");
        Room room = controller.addRoom(RoomType.meetingRoom, 10, Collections.singleton("wifi"));
        controller.modifyRoom(room.getId(), "features", "");
        controller.addRoom(RoomType.meetingRoom, 10, Collections.emptySet());
        controller.search(LocalDate.now().plusDays(1), LocalTime.of(9, 0),
                LocalTime.of(10, 0), 1, Collections.emptySet());
        controller.closeLogger();

        String txtContent = new String(Files.readAllBytes(tempTxt.toPath()));
        assertTrue(txtContent.contains("modifyRoom " + room.getId() + " features -"));
        assertTrue(txtContent.contains("addRoom meetingRoom 10 -"));
        assertTrue(txtContent.contains("search "));
        assertTrue(txtContent.contains(" 1 -"));
        assertTrue(controller.getRoomQuiet(room.getId()).getFeatures().isEmpty());

        java.io.InputStream originalIn = System.in;
        java.io.PrintStream originalOut = System.out;
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        try {
            System.setIn(new java.io.ByteArrayInputStream(
                    txtContent.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            System.setOut(new java.io.PrintStream(output, true, "UTF-8"));
            hk.edu.polyu.comp.comp2021.crobs.Application replay =
                    new hk.edu.polyu.comp.comp2021.crobs.Application(null, null);
            replay.run();
            assertEquals(2, replay.getCrobs().listAllRooms().size());
            assertTrue(replay.getCrobs().listRoom(room.getId()).getFeatures().isEmpty());
            assertEquals(controller.getRoomQuiet(room.getId()).getCapacity(),
                    replay.getCrobs().listRoom(room.getId()).getCapacity());
            String replayOutput = output.toString("UTF-8");
            assertFalse(replayOutput.contains("Error:"));
            assertFalse(replayOutput.contains("Invalid Input:"));
            assertTrue(replayOutput.contains("Available Rooms (2):"));
        } finally {
            System.setIn(originalIn);
            System.setOut(originalOut);
        }
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

        assumeFalse(GraphicsEnvironment.isHeadless(), "Top-level window dialogs require a display");
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
        assumeFalse(GraphicsEnvironment.isHeadless(), "MainFrame requires a display");
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

    @Test
    void testMainFrameUndoRedo() throws CrobsException {
        assumeFalse(GraphicsEnvironment.isHeadless(), "MainFrame requires a display");
        controller.loginQuiet("u1");
        MainFrame mainFrame = new MainFrame(controller);
        assertNotNull(mainFrame);

        // Perform mutating action
        controller.addUserQuiet("BobUndo");
        assertTrue(controller.canUndo());

        // Perform Undo via MainFrame
        mainFrame.performUndo();
        assertFalse(controller.canUndo());
        assertTrue(controller.canRedo());

        // Perform Redo via MainFrame
        mainFrame.performRedo();
        assertTrue(controller.canUndo());
        assertFalse(controller.canRedo());

        mainFrame.dispose();
    }
}
