package hk.edu.polyu.comp.comp2021.crobs.gui;

import hk.edu.polyu.comp.comp2021.crobs.gui.dialog.ConfirmationDialog;
import hk.edu.polyu.comp.comp2021.crobs.gui.dialog.LoginDialog;
import hk.edu.polyu.comp.comp2021.crobs.gui.dialog.RoomEditorDialog;
import hk.edu.polyu.comp.comp2021.crobs.gui.view.FindRoomsPanel;
import hk.edu.polyu.comp.comp2021.crobs.gui.view.ManagePanel;
import hk.edu.polyu.comp.comp2021.crobs.gui.view.MyBookingsPanel;
import hk.edu.polyu.comp.comp2021.crobs.Application;
import hk.edu.polyu.comp.comp2021.crobs.gui.view.LoginPanel;
import hk.edu.polyu.comp.comp2021.crobs.gui.view.RoomSchedulePanel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
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
        assertEquals("Unit Test Session", booking.getPurpose());

        List<Booking> myBookings = controller.userListBookings();
        assertTrue(myBookings.stream().anyMatch(b -> b.getId().equals(booking.getId())));

        // Test modify booking
        LocalTime newEnd = LocalTime.of(12, 30);
        controller.modifyBooking(booking.getId(), roomId, date, start, newEnd, "Extended Session");
        assertEquals(newEnd, controller.getBookingsQuiet().stream().filter(b -> b.getId().equals(booking.getId())).findFirst().get().getEndTime());
        assertEquals("Extended Session", controller.getBookingsQuiet().stream().filter(b -> b.getId().equals(booking.getId())).findFirst().get().getPurpose());

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
        assertEquals("Private Superadmin Meeting", adminB.getPurpose());

        // Student Dave views schedule -> can query schedule through controller
        controller.login(student.getId());
        List<Booking> studentView = controller.schedule(roomId, date);
        assertFalse(studentView.isEmpty());
        Booking studentB = studentView.get(0);
        assertEquals(booking.getId(), studentB.getId());
        assertEquals("(hidden)", studentB.getUserId());
        assertEquals("Private Superadmin Meeting", studentB.getPurpose());
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

    @Test
    void testGuiMultiwordPreservationAndCliReplay() throws Exception {
        File tempTxt = File.createTempFile("crobs_gui_test_", ".txt");
        File tempHtml = File.createTempFile("crobs_gui_test_", ".html");
        tempTxt.deleteOnExit();
        tempHtml.deleteOnExit();

        GuiSessionLogger dedicatedLogger = new GuiSessionLogger(tempHtml.getAbsolutePath(), tempTxt.getAbsolutePath());
        GuiController dedicatedController = new GuiController(new CROBS(), dedicatedLogger);

        dedicatedController.login("u1");
        User user = dedicatedController.addUser("Dr. Alexander Lam & Team <Alpha>");
        assertEquals("Dr. Alexander Lam & Team <Alpha>", user.getName());
        assertEquals("Dr. Alexander Lam & Team <Alpha>", dedicatedController.getAllKnownUsers().stream()
                .filter(u -> u.getId().equals(user.getId())).findFirst().get().getName());

        Room room = dedicatedController.addRoom(RoomType.meetingRoom, 10, Collections.emptySet());
        LocalDate date = LocalDate.now().plusDays(2);
        Booking booking = dedicatedController.book(room.getId(), date, LocalTime.of(10, 0), LocalTime.of(11, 0), "Weekly project meeting & discussion");
        assertEquals("Weekly project meeting & discussion", booking.getPurpose());

        dedicatedController.modifyBooking(booking.getId(), room.getId(), date, LocalTime.of(10, 0), LocalTime.of(11, 30), "Extended project meeting & review");
        assertEquals("Extended project meeting & review", dedicatedController.getBookingsQuiet().stream()
                .filter(b -> b.getId().equals(booking.getId())).findFirst().get().getPurpose());

        dedicatedController.logout();

        // 1. Verify TXT log preserves exact multiword tokens
        String txtContent = new String(Files.readAllBytes(tempTxt.toPath()), StandardCharsets.UTF_8);
        assertTrue(txtContent.contains("addUser Dr. Alexander Lam & Team <Alpha>"));
        assertTrue(txtContent.contains("Weekly project meeting & discussion"));
        assertTrue(txtContent.contains("Extended project meeting & review"));

        // 2. Verify HTML log escapes HTML special characters properly
        String htmlContent = new String(Files.readAllBytes(tempHtml.toPath()), StandardCharsets.UTF_8);
        assertTrue(htmlContent.contains("&amp;"));
        assertTrue(htmlContent.contains("&lt;Alpha&gt;"));
        assertFalse(htmlContent.contains("<Alpha>"));

        // 3. Replay generated TXT log through actual CLI Application entry point
        String replayScript = txtContent + System.lineSeparator() + "quit" + System.lineSeparator();
        InputStream originalIn = System.in;
        PrintStream originalOut = System.out;
        try {
            System.setIn(new ByteArrayInputStream(replayScript.getBytes(StandardCharsets.UTF_8)));
            System.setOut(new PrintStream(new ByteArrayOutputStream(), true, "UTF-8"));
            Application app = new Application(null, null);
            app.run();

            CROBS replayedModel = app.getCrobs();
            assertEquals("Dr. Alexander Lam & Team <Alpha>", replayedModel.listAllUsers().stream()
                    .filter(u -> u.getId().equals(user.getId())).findFirst().get().getName());
            assertEquals(1, replayedModel.listAllRooms().size());

            replayedModel.login("u1");
            assertEquals("Extended project meeting & review", replayedModel.listBookings().get(0).getPurpose());
        } finally {
            System.setIn(originalIn);
            System.setOut(originalOut);
            tempTxt.delete();
            tempHtml.delete();
        }
    }

    @Test
    void testCleanGuiStartupMatchesSpecification() throws Exception {
        // 1. Normal GUI startup path (default, no --demo)
        GuiController cleanCtrl = CrobsGuiApplication.createController(new String[0]);
        assertFalse(cleanCtrl.isLoggedIn());
        assertNull(cleanCtrl.getCurrentUser());
        assertEquals(0, cleanCtrl.getAllRoomsQuiet().size());
        assertEquals(0, cleanCtrl.getBookingsQuiet().size());
        assertEquals(0, cleanCtrl.getModel().getUndoHistorySize());
        assertFalse(cleanCtrl.canUndo());
        assertEquals(1, cleanCtrl.getAllKnownUsers().size());
        assertEquals("u1", cleanCtrl.getAllKnownUsers().get(0).getId());

        // Verify clean login panel renders without error and only has default user
        LoginPanel cleanLogin = new LoginPanel(cleanCtrl, null);
        assertNotNull(cleanLogin);

        // 2. Explicit demo mode startup path (--demo)
        GuiController demoCtrl = CrobsGuiApplication.createController(new String[]{"--demo"});
        assertFalse(demoCtrl.isLoggedIn());
        assertNull(demoCtrl.getCurrentUser());
        assertEquals(5, demoCtrl.getAllRoomsQuiet().size());
        // Unauthenticated session has 0 personal bookings visible
        assertEquals(0, demoCtrl.getBookingsQuiet().size());
        // Logging in as demo users reveals their seeded bookings
        demoCtrl.loginQuiet("u2");
        assertEquals(4, demoCtrl.getBookingsQuiet().size());
        demoCtrl.loginQuiet("u3");
        assertEquals(3, demoCtrl.getBookingsQuiet().size());
        demoCtrl.loginQuiet("u1");
        assertEquals(1, demoCtrl.getBookingsQuiet().size());
        demoCtrl.logoutQuiet();

        assertEquals(0, demoCtrl.getModel().getUndoHistorySize());
        assertFalse(demoCtrl.canUndo());
        assertEquals(3, demoCtrl.getAllKnownUsers().size());
        assertTrue(demoCtrl.getAllKnownUsers().stream().anyMatch(u -> u.getId().equals("u1")));
        assertTrue(demoCtrl.getAllKnownUsers().stream().anyMatch(u -> u.getId().equals("u2")));
        assertTrue(demoCtrl.getAllKnownUsers().stream().anyMatch(u -> u.getId().equals("u3")));

        // Verify demo login panel renders with all demo users available
        LoginPanel demoLogin = new LoginPanel(demoCtrl, null);
        assertNotNull(demoLogin);
    }

    private static Object componentField(Object panel, String name) {
        try {
            java.lang.reflect.Field f = panel.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(panel);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }

    @Test
    void testRoomEditValidationBeforeMutating() throws Exception {
        controller.login("u1");
        Room room = controller.addRoom(RoomType.meetingRoom, 10, Collections.singleton("wifi"));
        int history = model.getUndoHistorySize();
        DrawerPanel drawer = new DrawerPanel(controller, message -> {});
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            drawer.openEditRoom(room);
            ((javax.swing.JComboBox<?>) componentField(drawer, "roomTypeCombo")).setSelectedItem(RoomType.computerLab);
            ((javax.swing.JTextField) componentField(drawer, "roomCapField")).setText("0");
            ((JButton) componentField(drawer, "primaryActionBtn")).doClick();
            assertTrue(((javax.swing.JLabel) componentField(drawer, "errorLabel")).getText().contains("positive"));
        });
        assertEquals(RoomType.meetingRoom, model.listRoom(room.getId()).getType());
        assertEquals(10, model.listRoom(room.getId()).getCapacity());
        assertEquals(history, model.getUndoHistorySize());
    }

    @Test
    void testStrictCalendarDateOnBookingDrawer() throws Exception {
        controller.login("u1");
        Room room = controller.addRoom(RoomType.meetingRoom, 10, Collections.emptySet());
        DrawerPanel drawer = new DrawerPanel(controller, message -> {});
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            drawer.openBookFromSearch(room, LocalDate.of(2026, 2, 28),
                    LocalTime.of(9, 0), LocalTime.of(10, 0));
            ((javax.swing.JTextField) componentField(drawer, "dateField")).setText("2026-02-30");
            ((javax.swing.JTextArea) componentField(drawer, "purposeArea")).setText("Project meeting");
            ((JButton) componentField(drawer, "primaryActionBtn")).doClick();
            assertFalse(((javax.swing.JLabel) componentField(drawer, "errorLabel")).getText().trim().isEmpty());
            ((javax.swing.JTextField) componentField(drawer, "dateField")).setText("2026-02-28");
            ((javax.swing.JTextField) componentField(drawer, "startField")).setText("24:00");
            ((JButton) componentField(drawer, "primaryActionBtn")).doClick();
            assertFalse(((javax.swing.JLabel) componentField(drawer, "errorLabel")).getText().trim().isEmpty());
        });
        assertTrue(controller.getBookingsQuiet().isEmpty());
    }

    @Test
    void testMultilinePurposeLogAndCliReplay() throws Exception {
        controller.login("u1");
        Room room = controller.addRoom(RoomType.meetingRoom, 10, Collections.emptySet());
        LocalDate date = LocalDate.of(2027, 1, 5);
        Booking booking = controller.book(room.getId(), date, LocalTime.of(9, 0),
                LocalTime.of(10, 0), "Project meeting\nDiscussion");
        assertEquals("Project meeting Discussion", booking.getPurpose());
        controller.modifyBooking(booking.getId(), room.getId(), date, LocalTime.of(9, 0),
                LocalTime.of(10, 30), "Follow-up\r\nDecisions");
        assertEquals("Follow-up Decisions", controller.getBookingsQuiet().get(0).getPurpose());
        controller.closeLogger();
        String log = new String(Files.readAllBytes(tempTxt.toPath()), StandardCharsets.UTF_8);
        assertEquals(4, log.trim().split("\\R").length);
        assertTrue(log.contains("Project meeting Discussion"));
        assertTrue(log.contains("Follow-up Decisions"));
        InputStream originalIn = System.in;
        PrintStream originalOut = System.out;
        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        try {
            System.setIn(new ByteArrayInputStream((log + "quit\n").getBytes(StandardCharsets.UTF_8)));
            System.setOut(new PrintStream(capture, true, "UTF-8"));
            Application app = new Application(null, null);
            app.run();
            assertEquals("Follow-up Decisions", app.getCrobs().listBookings().get(0).getPurpose());
            assertFalse(capture.toString("UTF-8").contains("Unknown command"));
        } finally {
            System.setIn(originalIn);
            System.setOut(originalOut);
        }
    }

    @Test
    void testInvalidSearchRangeShowsErrorAndInvalidDateRejected() throws Exception {
        FindRoomsPanel panel = new FindRoomsPanel(controller, (r, d) -> {}, () -> {}, () -> {});
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            ((javax.swing.JComboBox<?>) componentField(panel, "startCombo")).setSelectedItem("12:00");
            ((javax.swing.JComboBox<?>) componentField(panel, "endCombo")).setSelectedItem("10:00");
            panel.executeSearch(true);
            assertEquals("Invalid search interval",
                    ((javax.swing.JLabel) componentField(panel, "resultsTitleLabel")).getText());
            ((javax.swing.JComboBox<?>) componentField(panel, "dateCombo")).setSelectedItem("2026-02-30");
            panel.executeSearch(true);
            assertEquals("Invalid search input",
                    ((javax.swing.JLabel) componentField(panel, "resultsTitleLabel")).getText());
        });
    }

    @Test
    void testUserOpensScheduleLoggedButPassiveRefreshQuiet() throws Exception {
        controller.login("u1");
        Room room = controller.addRoom(RoomType.meetingRoom, 10, Collections.emptySet());
        RoomSchedulePanel panel = new RoomSchedulePanel(controller, () -> {});
        javax.swing.SwingUtilities.invokeAndWait(() ->
                panel.loadScheduleForRoom(room.getId(), LocalDate.of(2027, 1, 5)));
        String first = new String(Files.readAllBytes(tempTxt.toPath()), StandardCharsets.UTF_8);
        assertEquals(1, first.split("schedule " + room.getId() + " ", -1).length - 1);
        controller.addUser("Passive refresh trigger");
        String second = new String(Files.readAllBytes(tempTxt.toPath()), StandardCharsets.UTF_8);
        assertEquals(1, second.split("schedule " + room.getId() + " ", -1).length - 1);
    }


    @Test
    void testInvalidEditBookingDateKeepsOriginalReservation() throws Exception {
        controller.login("u1");
        Room room = controller.addRoom(RoomType.meetingRoom, 10, Collections.emptySet());
        LocalDate date = LocalDate.of(2027, 1, 5);
        Booking booking = controller.book(room.getId(), date,
                LocalTime.of(9, 0), LocalTime.of(10, 0), "Phase_1_review");
        DrawerPanel drawer = new DrawerPanel(controller, message -> {});
        int historyBefore = model.getUndoHistorySize();
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            drawer.openEditBooking(booking);
            assertEquals("Phase_1_review",
                    ((javax.swing.JTextArea) componentField(drawer, "purposeArea")).getText());
            ((javax.swing.JComboBox<?>) componentField(drawer, "dateCombo"))
                    .getEditor().setItem("2026-02-30");
            ((JButton) componentField(drawer, "primaryActionBtn")).doClick();
            assertFalse(((javax.swing.JLabel) componentField(drawer, "errorLabel")).getText().trim().isEmpty());
        });
        assertEquals(date, controller.getBookingsQuiet().get(0).getDate());
        assertEquals("Phase_1_review", controller.getBookingsQuiet().get(0).getPurpose());
        assertEquals(historyBefore, model.getUndoHistorySize());
    }

    @Test
    void testMultilinePurposeFromSwingDrawerIsNormalizedBeforeLogging() throws Exception {
        controller.login("u1");
        Room room = controller.addRoom(RoomType.meetingRoom, 10, Collections.emptySet());
        DrawerPanel drawer = new DrawerPanel(controller, message -> {});
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            drawer.openBookFromSearch(room, LocalDate.of(2027, 1, 5),
                    LocalTime.of(9, 0), LocalTime.of(10, 0));
            ((javax.swing.JTextArea) componentField(drawer, "purposeArea"))
                    .setText("Project meeting\nDiscussion");
            ((JButton) componentField(drawer, "primaryActionBtn")).doClick();
        });
        assertEquals("Project meeting Discussion", controller.getBookingsQuiet().get(0).getPurpose());
        String log = new String(Files.readAllBytes(tempTxt.toPath()), StandardCharsets.UTF_8);
        assertEquals(3, log.trim().split("\\R").length);
        assertTrue(log.contains("Project meeting Discussion"));
    }


    @Test
    void testUnicodeLineSeparatorsRemainReplayableThroughSwing() throws Exception {
        controller.login("u1");
        Room room = controller.addRoom(RoomType.meetingRoom, 10, Collections.emptySet());
        DrawerPanel drawer = new DrawerPanel(controller, message -> {});
        LocalDate date = LocalDate.of(2027, 1, 5);

        javax.swing.SwingUtilities.invokeAndWait(() -> {
            drawer.openBookFromSearch(room, date, LocalTime.of(9, 0), LocalTime.of(10, 0));
            ((javax.swing.JTextArea) componentField(drawer, "purposeArea"))
                    .setText("\u0085Project\u2028meeting\u2029Discussion\u0085");
            ((JButton) componentField(drawer, "primaryActionBtn")).doClick();
        });

        Booking booking = controller.getBookingsQuiet().get(0);
        assertEquals("Project meeting Discussion", booking.getPurpose());

        controller.modifyBooking(booking.getId(), room.getId(), date,
                LocalTime.of(9, 0), LocalTime.of(10, 30),
                "Decisions\u0085Tasks\u2028Owner\u2029Review");
        assertEquals("Decisions Tasks Owner Review", controller.getBookingsQuiet().get(0).getPurpose());

        controller.modifyBooking(booking.getId(), room.getId(), date,
                LocalTime.of(9, 0), LocalTime.of(10, 30), "\u0085\u2028\u2029");
        assertEquals("None", controller.getBookingsQuiet().get(0).getPurpose());

        controller.closeLogger();
        String txt = new String(Files.readAllBytes(tempTxt.toPath()), StandardCharsets.UTF_8);
        assertEquals(5, txt.trim().split("\\R").length);
        assertFalse(txt.contains("\u0085"));
        assertFalse(txt.contains("\u2028"));
        assertFalse(txt.contains("\u2029"));
        assertTrue(txt.contains("Project meeting Discussion"));
        assertTrue(txt.contains("Decisions Tasks Owner Review"));
        assertTrue(txt.contains("modifyBooking " + booking.getId()));

        InputStream originalIn = System.in;
        PrintStream originalOut = System.out;
        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        try {
            System.setIn(new ByteArrayInputStream((txt + "quit\n").getBytes(StandardCharsets.UTF_8)));
            System.setOut(new PrintStream(capture, true, "UTF-8"));
            Application replay = new Application(null, null);
            replay.run();
            assertEquals("None", replay.getCrobs().listBookings().get(0).getPurpose());
            assertFalse(capture.toString("UTF-8").contains("Unknown command"));
        } finally {
            System.setIn(originalIn);
            System.setOut(originalOut);
        }
    }


    @Test
    void testUnicodeUserNamesFromDrawerReplayAsSingleCommands() throws Exception {
        controller.login("u1");
        DrawerPanel drawer = new DrawerPanel(controller, message -> {});
        String[] inputs = {"Alex\u0085Chen", "Maya\u2028Lin", "Sam\u2029Lee"};
        String[] expected = {"Alex Chen", "Maya Lin", "Sam Lee"};

        for (String input : inputs) {
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                drawer.openAddUser();
                ((javax.swing.JTextField) componentField(drawer, "userNameField")).setText(input);
                ((JButton) componentField(drawer, "primaryActionBtn")).doClick();
            });
        }
        for (String name : expected) {
            assertTrue(model.listAllUsers().stream().anyMatch(u -> u.getName().equals(name)));
        }

        javax.swing.SwingUtilities.invokeAndWait(() -> {
            drawer.openAddUser();
            ((javax.swing.JTextField) componentField(drawer, "userNameField"))
                    .setText("\u0085\u2028\u2029");
            ((JButton) componentField(drawer, "primaryActionBtn")).doClick();
            assertTrue(((javax.swing.JLabel) componentField(drawer, "errorLabel"))
                    .getText().contains("User name cannot be blank"));
        });
        assertEquals(4, model.listAllUsers().size());

        controller.closeLogger();
        String log = new String(Files.readAllBytes(tempTxt.toPath()), StandardCharsets.UTF_8);
        assertEquals(4, log.trim().split("\\R").length);
        for (String name : expected) {
            assertTrue(log.contains("addUser " + name));
        }
        assertFalse(log.contains("\u0085"));
        assertFalse(log.contains("\u2028"));
        assertFalse(log.contains("\u2029"));

        InputStream oldIn = System.in;
        PrintStream oldOut = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            System.setIn(new ByteArrayInputStream((log + "quit\n").getBytes(StandardCharsets.UTF_8)));
            System.setOut(new PrintStream(output, true, "UTF-8"));
            Application replay = new Application(null, null);
            replay.run();
            for (String name : expected) {
                assertTrue(replay.getCrobs().listAllUsers().stream()
                        .anyMatch(u -> u.getName().equals(name)));
            }
            assertFalse(output.toString("UTF-8").contains("Unknown command"));
        } finally {
            System.setIn(oldIn);
            System.setOut(oldOut);
        }
    }

    @Test
    void testRoomFeatureLineBreakValidationAndCliReplay() throws Exception {
        controller.login("u1");
        DrawerPanel drawer = new DrawerPanel(controller, message -> {});
        int history = model.getUndoHistorySize();
        String[] invalid = {"wifi\u0085projector", "wifi\u2028projector", "wifi\u2029projector"};

        for (String feature : invalid) {
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                drawer.openAddRoom();
                ((javax.swing.JTextField) componentField(drawer, "roomFeaturesField"))
                        .setText(feature);
                ((JButton) componentField(drawer, "primaryActionBtn")).doClick();
                assertTrue(((javax.swing.JLabel) componentField(drawer, "errorLabel"))
                        .getText().contains("Features"));
            });
            assertTrue(model.listAllRooms().isEmpty());
            assertEquals(history, model.getUndoHistorySize());
        }

        javax.swing.SwingUtilities.invokeAndWait(() -> {
            drawer.openAddRoom();
            ((javax.swing.JComboBox<?>) componentField(drawer, "roomTypeCombo"))
                    .setSelectedItem(RoomType.meetingRoom);
            ((javax.swing.JTextField) componentField(drawer, "roomFeaturesField"))
                    .setText("wifi, projector");
            ((JButton) componentField(drawer, "primaryActionBtn")).doClick();
        });
        Room room = model.listAllRooms().get(0);
        assertTrue(room.getFeatures().contains("wifi"));
        assertTrue(room.getFeatures().contains("projector"));
        int beforeEdit = model.getUndoHistorySize();

        for (String feature : invalid) {
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                drawer.openEditRoom(room);
                ((javax.swing.JComboBox<?>) componentField(drawer, "roomTypeCombo"))
                        .setSelectedItem(RoomType.computerLab);
                ((javax.swing.JTextField) componentField(drawer, "roomFeaturesField"))
                        .setText(feature);
                ((JButton) componentField(drawer, "primaryActionBtn")).doClick();
                assertTrue(((javax.swing.JLabel) componentField(drawer, "errorLabel"))
                        .getText().contains("Features"));
            });
            assertEquals(RoomType.meetingRoom, model.listRoom(room.getId()).getType());
            assertEquals(beforeEdit, model.getUndoHistorySize());
        }

        assertThrows(IllegalArgumentException.class,
                () -> controller.addRoom(RoomType.meetingRoom, 10,
                        Collections.singleton("wifi,projector")));
        assertThrows(IllegalArgumentException.class,
                () -> controller.modifyRoom(room.getId(), "features", "wifi projector"));
        assertEquals(beforeEdit, model.getUndoHistorySize());

        javax.swing.SwingUtilities.invokeAndWait(() -> {
            drawer.openEditRoom(room);
            ((javax.swing.JComboBox<?>) componentField(drawer, "roomTypeCombo"))
                    .setSelectedItem(RoomType.computerLab);
            ((javax.swing.JTextField) componentField(drawer, "roomFeaturesField"))
                    .setText("wifi, monitor");
            ((JButton) componentField(drawer, "primaryActionBtn")).doClick();
        });
        Room updated = model.listRoom(room.getId());
        assertEquals(RoomType.computerLab, updated.getType());
        assertEquals(new HashSet<>(java.util.Arrays.asList("wifi", "monitor")), updated.getFeatures());

        controller.closeLogger();
        String log = new String(Files.readAllBytes(tempTxt.toPath()), StandardCharsets.UTF_8);
        assertEquals(5, log.trim().split("\\R").length);
        assertTrue(log.contains("addRoom meetingRoom 20 projector,wifi"));
        assertTrue(log.contains("modifyRoom " + room.getId() + " features wifi,monitor"));
        assertFalse(log.contains("\u0085"));
        assertFalse(log.contains("\u2028"));
        assertFalse(log.contains("\u2029"));

        InputStream oldIn = System.in;
        PrintStream oldOut = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            System.setIn(new ByteArrayInputStream((log + "quit\n").getBytes(StandardCharsets.UTF_8)));
            System.setOut(new PrintStream(output, true, "UTF-8"));
            Application replay = new Application(null, null);
            replay.run();
            Room replayed = replay.getCrobs().listRoom(room.getId());
            assertEquals(updated.getType(), replayed.getType());
            assertEquals(updated.getCapacity(), replayed.getCapacity());
            assertEquals(updated.getFeatures(), replayed.getFeatures());
            assertFalse(output.toString("UTF-8").contains("Unknown command"));
        } finally {
            System.setIn(oldIn);
            System.setOut(oldOut);
        }
    }

}
