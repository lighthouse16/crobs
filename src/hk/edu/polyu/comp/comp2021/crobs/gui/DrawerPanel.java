package hk.edu.polyu.comp.comp2021.crobs.gui;

import hk.edu.polyu.comp.comp2021.crobs.model.Booking;
import hk.edu.polyu.comp.comp2021.crobs.model.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.model.Room;
import hk.edu.polyu.comp.comp2021.crobs.model.RoomType;
import hk.edu.polyu.comp.comp2021.crobs.model.User;
import hk.edu.polyu.comp.comp2021.crobs.model.UserRole;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Slide-out drawer component matching the CROBS V3 specification (Screen B and all action panels).
 * Docked on the right side of the main workspace with 420px width, clean header, dynamic body,
 * and footer action buttons.
 */
public class DrawerPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    public enum Mode {
        BOOK_SEARCH,
        BOOK_WEEK,
        EDIT_BOOKING,
        ADD_ROOM,
        EDIT_ROOM,
        ADD_USER,
        CHANGE_ROLE
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter READABLE_DATE = DateTimeFormatter.ofPattern("EEE, d MMM yyyy");

    private final GuiController controller;
    private final Consumer<String> toastCallback;

    private Mode currentMode = Mode.BOOK_SEARCH;

    // Header
    private JLabel headerTitleLabel;
    private JLabel headerSubtitleLabel;

    // Dynamic body container
    private JPanel bodyContainer;

    // Footer
    private JButton cancelBtn;
    private JButton primaryActionBtn;

    // Error label
    private JLabel errorLabel;

    // Active payload references
    private Room activeRoom;
    private Booking activeBooking;
    private User activeUser;

    // Shared Form Fields
    private static final String[] PRESET_DATES = {
            "2026-11-03", "2026-11-04", "2026-11-05", "2026-11-06", "2026-11-07", "2026-11-08", "2026-11-09"
    };

    private static final String[] PRESET_START_TIMES = {
            "08:00", "08:30", "09:00", "09:30", "10:00", "10:30",
            "11:00", "11:30", "12:00", "12:30", "13:00", "13:30",
            "14:00", "14:30", "15:00", "15:30", "16:00", "16:30",
            "17:00", "17:30", "18:00", "18:30", "19:00", "19:30",
            "20:00", "20:30", "21:00"
    };

    private static final String[] PRESET_END_TIMES = {
            "08:30", "09:00", "09:30", "10:00", "10:30",
            "11:00", "11:30", "12:00", "12:30", "13:00", "13:30",
            "14:00", "14:30", "15:00", "15:30", "16:00", "16:30",
            "17:00", "17:30", "18:00", "18:30", "19:00", "19:30",
            "20:00", "20:30", "21:00", "21:30"
    };

    private JComboBox<String> roomSelectCombo;
    private JComboBox<String> dateCombo;
    private JComboBox<String> startCombo;
    private JComboBox<String> endCombo;
    private JTextField dateField;
    private JTextField startField;
    private JTextField endField;
    private JTextArea purposeArea;

    private JComboBox<RoomType> roomTypeCombo;
    private JTextField roomCapField;
    private JTextField roomFeaturesField;

    private JTextField userNameField;
    private JComboBox<UserRole> roleCombo;

    public DrawerPanel(GuiController controller, Consumer<String> toastCallback) {
        this.controller = controller;
        this.toastCallback = toastCallback;

        initComponents();
        setVisible(false);
    }

    @Override
    public void setVisible(boolean aFlag) {
        super.setVisible(aFlag);
        if (getParent() != null) {
            getParent().revalidate();
            getParent().repaint();
        }
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(420, 0));
        Border leftBorder = BorderFactory.createMatteBorder(0, 1, 0, 0, GuiTheme.COLOR_BORDER);
        setBorder(leftBorder);

        // Header (padding: 31px 30px 19px)
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, GuiTheme.COLOR_BORDER),
                new EmptyBorder(26, 30, 18, 30)
        ));

        JPanel titleBox = new JPanel();
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.setOpaque(false);

        headerTitleLabel = new JLabel("Book room");
        headerTitleLabel.setFont(GuiTheme.FONT_H1);
        headerTitleLabel.setForeground(GuiTheme.COLOR_TEXT);

        headerSubtitleLabel = new JLabel("Confirm your reservation.");
        headerSubtitleLabel.setFont(GuiTheme.FONT_SMALL);
        headerSubtitleLabel.setForeground(GuiTheme.COLOR_MUTED);

        titleBox.add(headerTitleLabel);
        titleBox.add(Box.createVerticalStrut(4));
        titleBox.add(headerSubtitleLabel);
        header.add(titleBox, BorderLayout.WEST);

        // Close button: 31x31 square box with 1px border #dee3e8
        JButton closeBtn = new JButton("×") {
            private static final long serialVersionUID = 1L;
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? new Color(245, 247, 250) : Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(GuiTheme.COLOR_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        closeBtn.setFont(new Font("Arial", Font.PLAIN, 20));
        closeBtn.setForeground(new Color(111, 122, 135));
        closeBtn.setContentAreaFilled(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setPreferredSize(new Dimension(31, 31));
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> closeDrawer());

        JPanel closeWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        closeWrapper.setOpaque(false);
        closeWrapper.add(closeBtn);
        header.add(closeWrapper, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // Scrollable Body
        bodyContainer = new JPanel();
        bodyContainer.setLayout(new BoxLayout(bodyContainer, BoxLayout.Y_AXIS));
        bodyContainer.setBackground(Color.WHITE);
        bodyContainer.setBorder(new EmptyBorder(24, 30, 24, 30));

        JScrollPane bodyScroll = new JScrollPane(bodyContainer);
        bodyScroll.setBorder(BorderFactory.createEmptyBorder());
        bodyScroll.getViewport().setBackground(Color.WHITE);
        bodyScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(bodyScroll, BorderLayout.CENTER);

        // Footer (padding: 17px 30px, border-top 1px solid #dee3e8)
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setOpaque(false);
        footer.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, GuiTheme.COLOR_BORDER),
                new EmptyBorder(16, 30, 16, 30)
        ));

        cancelBtn = GuiTheme.createSecondaryButton("Cancel");
        cancelBtn.addActionListener(e -> closeDrawer());

        primaryActionBtn = GuiTheme.createPrimaryButton("Confirm booking");
        primaryActionBtn.addActionListener(e -> onPrimaryAction());

        footer.add(cancelBtn);
        footer.add(primaryActionBtn);
        add(footer, BorderLayout.SOUTH);
    }

    public void closeDrawer() {
        setVisible(false);
        activeRoom = null;
        activeBooking = null;
        activeUser = null;
    }

    // --- Mode Openers ---

    private JComboBox<String> createSelectorCombo(String[] presets, String selectedValue) {
        JComboBox<String> combo = new JComboBox<>(presets);
        combo.setEditable(true);
        combo.setFont(GuiTheme.FONT_BODY);
        combo.setPreferredSize(new Dimension(0, 39));
        if (selectedValue != null && !selectedValue.isEmpty()) {
            boolean found = false;
            for (String p : presets) {
                if (p.equals(selectedValue)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                combo.insertItemAt(selectedValue, 0);
            }
            combo.setSelectedItem(selectedValue);
        }
        return combo;
    }

    private String getSelectedValue(JComboBox<String> combo, JTextField fallback) {
        if (combo != null) {
            Object ed = combo.getEditor().getItem();
            if (ed != null && !ed.toString().trim().isEmpty()) {
                return ed.toString().trim();
            }
            Object sel = combo.getSelectedItem();
            if (sel != null && !sel.toString().trim().isEmpty()) {
                return sel.toString().trim();
            }
        }
        return (fallback != null) ? fallback.getText().trim() : "";
    }

    public void openBookFromSearch(Room room, LocalDate date, LocalTime start, LocalTime end) {
        this.currentMode = Mode.BOOK_SEARCH;
        this.activeRoom = room;

        headerTitleLabel.setText("Book room");
        headerSubtitleLabel.setText("Confirm your reservation.");
        primaryActionBtn.setText("Confirm booking");

        bodyContainer.removeAll();

        // Summary Box
        JPanel summaryBox = createSummaryBox(
                formatRoomType(room.getType()) + "  " + room.getId(),
                String.format("<html>%s · %s–%s<br>%d seats · %s</html>",
                        date.format(READABLE_DATE), start.format(TIME_FMT), end.format(TIME_FMT),
                        room.getCapacity(),
                        room.getFeatures().isEmpty() ? "No features" : String.join(", ", room.getFeatures()))
        );
        bodyContainer.add(summaryBox);
        bodyContainer.add(Box.createVerticalStrut(20));

        // Hidden date/time values saved in fields for submission
        dateField = new JTextField(date.format(DATE_FMT));
        startField = new JTextField(start.format(TIME_FMT));
        endField = new JTextField(end.format(TIME_FMT));
        dateCombo = null;
        startCombo = null;
        endCombo = null;

        // Purpose Field
        purposeArea = new JTextArea(4, 20);
        purposeArea.setLineWrap(true);
        purposeArea.setWrapStyleWord(true);
        purposeArea.setFont(GuiTheme.FONT_BODY);
        purposeArea.setBorder(new EmptyBorder(8, 10, 8, 10));

        JScrollPane pScroll = new JScrollPane(purposeArea);
        pScroll.setBorder(new LineBorder(GuiTheme.COLOR_BORDER, 1));
        pScroll.setMaximumSize(new Dimension(Short.MAX_VALUE, 92));
        pScroll.setAlignmentX(LEFT_ALIGNMENT);

        bodyContainer.add(createFieldBox("Purpose · optional", pScroll));

        errorLabel = createErrorLabel();
        bodyContainer.add(errorLabel);

        bodyContainer.revalidate();
        bodyContainer.repaint();
        setVisible(true);
    }

    public void openBookFromWeek(Room room, LocalDate date, LocalTime start, LocalTime end) {
        this.currentMode = Mode.BOOK_WEEK;
        this.activeRoom = room;

        headerTitleLabel.setText("Book room");
        headerSubtitleLabel.setText("Confirm your reservation.");
        primaryActionBtn.setText("Confirm booking");

        bodyContainer.removeAll();

        JPanel summaryBox = createSummaryBox(
                formatRoomType(room.getType()) + "  " + room.getId(),
                String.format("%d seats · %s",
                        room.getCapacity(),
                        room.getFeatures().isEmpty() ? "No features" : String.join(", ", room.getFeatures()))
        );
        bodyContainer.add(summaryBox);
        bodyContainer.add(Box.createVerticalStrut(18));

        // Date selector (convenient dropdown + editable)
        dateCombo = createSelectorCombo(PRESET_DATES, date.format(DATE_FMT));
        bodyContainer.add(createFieldBox("Date", dateCombo));
        bodyContainer.add(Box.createVerticalStrut(14));

        // Row of two: Start & End (convenient 30-min time slot dropdowns + editable)
        startCombo = createSelectorCombo(PRESET_START_TIMES, start.format(TIME_FMT));
        endCombo = createSelectorCombo(PRESET_END_TIMES, end.format(TIME_FMT));

        JPanel rowTwo = new JPanel(new GridLayout(1, 2, 12, 0));
        rowTwo.setOpaque(false);
        rowTwo.setAlignmentX(LEFT_ALIGNMENT);
        rowTwo.setMaximumSize(new Dimension(Short.MAX_VALUE, 65));
        rowTwo.add(createFieldBox("Start", startCombo));
        rowTwo.add(createFieldBox("End", endCombo));

        bodyContainer.add(rowTwo);
        bodyContainer.add(Box.createVerticalStrut(14));

        // Purpose Field
        purposeArea = new JTextArea(4, 20);
        purposeArea.setLineWrap(true);
        purposeArea.setWrapStyleWord(true);
        purposeArea.setFont(GuiTheme.FONT_BODY);
        purposeArea.setBorder(new EmptyBorder(8, 10, 8, 10));

        JScrollPane pScroll = new JScrollPane(purposeArea);
        pScroll.setBorder(new LineBorder(GuiTheme.COLOR_BORDER, 1));
        pScroll.setMaximumSize(new Dimension(Short.MAX_VALUE, 92));
        pScroll.setAlignmentX(LEFT_ALIGNMENT);

        bodyContainer.add(createFieldBox("Purpose · optional", pScroll));

        errorLabel = createErrorLabel();
        bodyContainer.add(errorLabel);

        bodyContainer.revalidate();
        bodyContainer.repaint();
        setVisible(true);
    }

    public void openEditBooking(Booking booking) {
        this.currentMode = Mode.EDIT_BOOKING;
        this.activeBooking = booking;

        headerTitleLabel.setText("Edit booking");
        headerSubtitleLabel.setText("Update the booking details below.");
        primaryActionBtn.setText("Save changes");

        bodyContainer.removeAll();

        // Room dropdown
        List<Room> allRooms = controller.getAllRoomsQuiet();
        roomSelectCombo = new JComboBox<>();
        roomSelectCombo.setFont(GuiTheme.FONT_BODY);
        roomSelectCombo.setPreferredSize(new Dimension(0, 39));
        int selectedIndex = 0;
        for (int i = 0; i < allRooms.size(); i++) {
            Room r = allRooms.get(i);
            roomSelectCombo.addItem(formatRoomType(r.getType()) + " (" + r.getId() + ")");
            if (r.getId().equalsIgnoreCase(booking.getRoomId())) {
                selectedIndex = i;
            }
        }
        if (!allRooms.isEmpty()) {
            roomSelectCombo.setSelectedIndex(selectedIndex);
        }
        bodyContainer.add(createFieldBox("Room", roomSelectCombo));
        bodyContainer.add(Box.createVerticalStrut(14));

        // Date selector (convenient dropdown + editable)
        dateCombo = createSelectorCombo(PRESET_DATES, booking.getDate().format(DATE_FMT));
        bodyContainer.add(createFieldBox("Date", dateCombo));
        bodyContainer.add(Box.createVerticalStrut(14));

        // Start & End (convenient 30-min time slot dropdowns + editable)
        startCombo = createSelectorCombo(PRESET_START_TIMES, booking.getStartTime().format(TIME_FMT));
        endCombo = createSelectorCombo(PRESET_END_TIMES, booking.getEndTime().format(TIME_FMT));

        JPanel rowTwo = new JPanel(new GridLayout(1, 2, 12, 0));
        rowTwo.setOpaque(false);
        rowTwo.setAlignmentX(LEFT_ALIGNMENT);
        rowTwo.setMaximumSize(new Dimension(Short.MAX_VALUE, 65));
        rowTwo.add(createFieldBox("Start", startCombo));
        rowTwo.add(createFieldBox("End", endCombo));

        bodyContainer.add(rowTwo);
        bodyContainer.add(Box.createVerticalStrut(14));

        // Purpose Field
        purposeArea = new JTextArea(4, 20);
        purposeArea.setLineWrap(true);
        purposeArea.setWrapStyleWord(true);
        purposeArea.setFont(GuiTheme.FONT_BODY);
        purposeArea.setText(booking.getPurpose().replace("_", " "));
        purposeArea.setBorder(new EmptyBorder(8, 10, 8, 10));

        JScrollPane pScroll = new JScrollPane(purposeArea);
        pScroll.setBorder(new LineBorder(GuiTheme.COLOR_BORDER, 1));
        pScroll.setMaximumSize(new Dimension(Short.MAX_VALUE, 92));
        pScroll.setAlignmentX(LEFT_ALIGNMENT);

        bodyContainer.add(createFieldBox("Purpose · optional", pScroll));

        errorLabel = createErrorLabel();
        bodyContainer.add(errorLabel);

        bodyContainer.revalidate();
        bodyContainer.repaint();
        setVisible(true);
    }

    public void openAddRoom() {
        this.currentMode = Mode.ADD_ROOM;
        this.activeRoom = null;

        headerTitleLabel.setText("Add room");
        headerSubtitleLabel.setText("Manage room details.");
        primaryActionBtn.setText("Add room");

        bodyContainer.removeAll();

        // Room Type
        roomTypeCombo = new JComboBox<>(RoomType.values());
        roomTypeCombo.setFont(GuiTheme.FONT_BODY);
        roomTypeCombo.setPreferredSize(new Dimension(0, 39));
        bodyContainer.add(createFieldBox("Room type", roomTypeCombo));
        bodyContainer.add(Box.createVerticalStrut(14));

        // Capacity
        roomCapField = GuiTheme.createTextField(8);
        roomCapField.setText("20");
        bodyContainer.add(createFieldBox("Capacity", roomCapField));
        bodyContainer.add(Box.createVerticalStrut(14));

        // Features
        roomFeaturesField = GuiTheme.createTextField(20);
        roomFeaturesField.setText("wifi");
        bodyContainer.add(createFieldBox("Features · comma-separated", roomFeaturesField));

        errorLabel = createErrorLabel();
        bodyContainer.add(errorLabel);

        bodyContainer.revalidate();
        bodyContainer.repaint();
        setVisible(true);
    }

    public void openEditRoom(Room room) {
        this.currentMode = Mode.EDIT_ROOM;
        this.activeRoom = room;

        headerTitleLabel.setText("Edit room");
        headerSubtitleLabel.setText("Manage room details.");
        primaryActionBtn.setText("Save changes");

        bodyContainer.removeAll();

        // Room Type
        roomTypeCombo = new JComboBox<>(RoomType.values());
        roomTypeCombo.setSelectedItem(room.getType());
        roomTypeCombo.setFont(GuiTheme.FONT_BODY);
        roomTypeCombo.setPreferredSize(new Dimension(0, 39));
        bodyContainer.add(createFieldBox("Room type", roomTypeCombo));
        bodyContainer.add(Box.createVerticalStrut(14));

        // Capacity
        roomCapField = GuiTheme.createTextField(8);
        roomCapField.setText(String.valueOf(room.getCapacity()));
        bodyContainer.add(createFieldBox("Capacity", roomCapField));
        bodyContainer.add(Box.createVerticalStrut(14));

        // Features
        roomFeaturesField = GuiTheme.createTextField(20);
        roomFeaturesField.setText(String.join(",", room.getFeatures()));
        bodyContainer.add(createFieldBox("Features · comma-separated", roomFeaturesField));

        errorLabel = createErrorLabel();
        bodyContainer.add(errorLabel);

        bodyContainer.revalidate();
        bodyContainer.repaint();
        setVisible(true);
    }

    public void openAddUser() {
        this.currentMode = Mode.ADD_USER;
        this.activeUser = null;

        headerTitleLabel.setText("Add user");
        headerSubtitleLabel.setText("Add a new regular user.");
        primaryActionBtn.setText("Create user");

        bodyContainer.removeAll();

        userNameField = GuiTheme.createTextField(20);
        bodyContainer.add(createFieldBox("Name", userNameField));
        bodyContainer.add(Box.createVerticalStrut(8));

        JLabel hint = new JLabel("New users are created with the Regular role.");
        hint.setFont(GuiTheme.FONT_SMALL);
        hint.setForeground(GuiTheme.COLOR_MUTED);
        hint.setAlignmentX(LEFT_ALIGNMENT);
        bodyContainer.add(hint);

        errorLabel = createErrorLabel();
        bodyContainer.add(errorLabel);

        bodyContainer.revalidate();
        bodyContainer.repaint();
        setVisible(true);
    }

    public void openChangeRole(User user) {
        this.currentMode = Mode.CHANGE_ROLE;
        this.activeUser = user;

        headerTitleLabel.setText("Change role");
        headerSubtitleLabel.setText("Update access privileges.");
        primaryActionBtn.setText("Update role");

        bodyContainer.removeAll();

        JPanel summaryBox = createSummaryBox(
                user.getName() + "  " + user.getId(),
                "Current role: " + user.getRole()
        );
        bodyContainer.add(summaryBox);
        bodyContainer.add(Box.createVerticalStrut(18));

        roleCombo = new JComboBox<>(new UserRole[]{UserRole.REGULAR, UserRole.ADMIN});
        roleCombo.setSelectedItem(user.getRole() == UserRole.ADMIN ? UserRole.ADMIN : UserRole.REGULAR);
        roleCombo.setFont(GuiTheme.FONT_BODY);
        roleCombo.setPreferredSize(new Dimension(0, 39));
        bodyContainer.add(createFieldBox("New role", roleCombo));

        errorLabel = createErrorLabel();
        bodyContainer.add(errorLabel);

        bodyContainer.revalidate();
        bodyContainer.repaint();
        setVisible(true);
    }

    // --- Action Handler ---

    private void onPrimaryAction() {
        errorLabel.setText(" ");
        try {
            switch (currentMode) {
                case BOOK_SEARCH:
                case BOOK_WEEK: {
                    if (activeRoom == null) return;
                    LocalDate d = LocalDate.parse(getSelectedValue(dateCombo, dateField), DATE_FMT);
                    LocalTime st = LocalTime.parse(getSelectedValue(startCombo, startField), TIME_FMT);
                    LocalTime en = LocalTime.parse(getSelectedValue(endCombo, endField), TIME_FMT);
                    if (!st.isBefore(en)) {
                        errorLabel.setText("Start time must be before end time.");
                        return;
                    }
                    String purpose = purposeArea.getText().trim();
                    Booking b = controller.book(activeRoom.getId(), d, st, en, purpose);
                    closeDrawer();
                    toastCallback.accept(b.getId() + " · Booking confirmed");
                    MainFrame top = (MainFrame) javax.swing.SwingUtilities.getWindowAncestor(this);
                    if (top != null) {
                        top.navigateTo(MainFrame.VIEW_MY_BOOKINGS);
                    }
                    break;
                }
                case EDIT_BOOKING: {
                    if (activeBooking == null) return;
                    List<Room> allRooms = controller.getAllRoomsQuiet();
                    int sel = roomSelectCombo.getSelectedIndex();
                    if (sel < 0 || sel >= allRooms.size()) return;
                    String roomId = allRooms.get(sel).getId();

                    LocalDate d = LocalDate.parse(getSelectedValue(dateCombo, dateField), DATE_FMT);
                    LocalTime st = LocalTime.parse(getSelectedValue(startCombo, startField), TIME_FMT);
                    LocalTime en = LocalTime.parse(getSelectedValue(endCombo, endField), TIME_FMT);
                    if (!st.isBefore(en)) {
                        errorLabel.setText("Start time must be before end time.");
                        return;
                    }
                    String purpose = purposeArea.getText().trim();
                    controller.modifyBooking(activeBooking.getId(), roomId, d, st, en, purpose);
                    closeDrawer();
                    toastCallback.accept(activeBooking.getId() + " · Booking updated");
                    MainFrame top = (MainFrame) javax.swing.SwingUtilities.getWindowAncestor(this);
                    if (top != null) {
                        top.navigateTo(MainFrame.VIEW_MY_BOOKINGS);
                    }
                    break;
                }
                case ADD_ROOM: {
                    RoomType type = (RoomType) roomTypeCombo.getSelectedItem();
                    int cap = Integer.parseInt(roomCapField.getText().trim());
                    String[] fArray = roomFeaturesField.getText().trim().split(",");
                    Set<String> features = new HashSet<>();
                    for (String f : fArray) {
                        String s = f.trim();
                        if (!s.isEmpty()) features.add(s);
                    }
                    Room r = controller.addRoom(type, cap, features);
                    closeDrawer();
                    toastCallback.accept("Room added · " + r.getId());
                    break;
                }
                case EDIT_ROOM: {
                    if (activeRoom == null) return;
                    RoomType type = (RoomType) roomTypeCombo.getSelectedItem();
                    int cap = Integer.parseInt(roomCapField.getText().trim());
                    String featStr = roomFeaturesField.getText().trim();

                    controller.modifyRoom(activeRoom.getId(), "type", type.name());
                    controller.modifyRoom(activeRoom.getId(), "capacity", String.valueOf(cap));
                    controller.modifyRoom(activeRoom.getId(), "features", featStr);

                    closeDrawer();
                    toastCallback.accept("Room updated · " + activeRoom.getId());
                    break;
                }
                case ADD_USER: {
                    String name = userNameField.getText().trim();
                    if (name.isEmpty()) {
                        errorLabel.setText("Enter a user name.");
                        return;
                    }
                    User u = controller.addUser(name);
                    closeDrawer();
                    toastCallback.accept("User added · " + u.getId());
                    break;
                }
                case CHANGE_ROLE: {
                    if (activeUser == null) return;
                    UserRole role = (UserRole) roleCombo.getSelectedItem();
                    controller.changeRole(activeUser.getId(), role);
                    closeDrawer();
                    toastCallback.accept("Role updated · " + activeUser.getId());
                    break;
                }
            }
        } catch (CrobsException ex) {
            errorLabel.setText(ex.getMessage());
        } catch (Exception ex) {
            errorLabel.setText("Please verify all fields: " + ex.getMessage());
        }
    }

    // --- Component Helpers ---

    private JPanel createFieldBox(String labelText, JComponent field) {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setOpaque(false);
        box.setAlignmentX(LEFT_ALIGNMENT);

        JLabel label = new JLabel(labelText);
        label.setFont(GuiTheme.FONT_SMALL_BOLD);
        label.setForeground(GuiTheme.COLOR_MUTED);
        label.setBorder(new EmptyBorder(0, 0, 6, 0));

        box.add(label);
        box.add(field);
        return box;
    }

    private JPanel createSummaryBox(String title, String descHtml) {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBackground(new Color(249, 250, 251));
        box.setBorder(new CompoundBorder(
                new LineBorder(GuiTheme.COLOR_BORDER, 1),
                new EmptyBorder(14, 16, 14, 16)
        ));
        box.setAlignmentX(LEFT_ALIGNMENT);
        box.setMaximumSize(new Dimension(Short.MAX_VALUE, 100));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(GuiTheme.FONT_SECTION);
        titleLabel.setForeground(GuiTheme.COLOR_TEXT);

        JLabel descLabel = new JLabel(descHtml.startsWith("<html>") ? descHtml : "<html>" + descHtml + "</html>");
        descLabel.setFont(GuiTheme.FONT_BODY);
        descLabel.setForeground(GuiTheme.COLOR_MUTED);
        descLabel.setBorder(new EmptyBorder(6, 0, 0, 0));

        box.add(titleLabel);
        box.add(descLabel);
        return box;
    }

    private JLabel createErrorLabel() {
        JLabel err = new JLabel(" ");
        err.setFont(GuiTheme.FONT_SMALL);
        err.setForeground(GuiTheme.COLOR_DANGER);
        err.setBorder(new EmptyBorder(8, 0, 8, 0));
        err.setAlignmentX(LEFT_ALIGNMENT);
        return err;
    }

    private static String formatRoomType(RoomType type) {
        if (type == null) return "Room";
        switch (type) {
            case lectureTheatre: return "Lecture theatre";
            case computerLab: return "Computer lab";
            case seminarRoom: return "Seminar room";
            case meetingRoom: return "Meeting room";
            default: return type.name();
        }
    }
}
