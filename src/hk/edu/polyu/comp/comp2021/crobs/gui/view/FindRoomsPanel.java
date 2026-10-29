package hk.edu.polyu.comp.comp2021.crobs.gui.view;

import hk.edu.polyu.comp.comp2021.crobs.gui.GuiController;
import hk.edu.polyu.comp.comp2021.crobs.gui.GuiTheme;
import hk.edu.polyu.comp.comp2021.crobs.gui.MainFrame;
import hk.edu.polyu.comp.comp2021.crobs.gui.dialog.LoginDialog;
import hk.edu.polyu.comp.comp2021.crobs.model.Booking;
import hk.edu.polyu.comp.comp2021.crobs.model.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.model.Room;
import hk.edu.polyu.comp.comp2021.crobs.model.RoomType;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Screen A: Find Rooms workspace with 6-column search filter card, available rooms list,
 * and seamless integration with the V3 slide-out booking drawer.
 */
public class FindRoomsPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter READABLE_DATE = DateTimeFormatter.ofPattern("EEE, d MMM yyyy");

    private final GuiController controller;
    private final BiConsumer<String, LocalDate> scheduleNavCallback;
    private final Runnable myBookingsNavCallback;
    private final Runnable manageRoomsNavCallback;

    private JTextField dateField;
    private JTextField startField;
    private JTextField endField;
    private JTextField capacityField;
    private JButton equipDropdownBtn;
    private final Set<String> selectedFeatures = new LinkedHashSet<>();
    private final String[] allKnownEquipment = {
            "Wi-Fi", "Projector", "Whiteboard", "Computers", "Display", "Audio", "Sound System"
    };

    private JCheckBox showBookedCheck;
    private JLabel resultsTitleLabel;
    private JLabel resultsSubtitleLabel;

    private JPanel resultsCardContainer;
    private CardLayout resultsCardLayout;
    private static final String CARD_TABLE = "TABLE";
    private static final String CARD_EMPTY = "EMPTY";

    private JTable resultsTable;
    private RoomResultsModel tableModel;

    private Room activeBookingRoom;
    private final List<RoomDisplayItem> currentDisplayItems = new ArrayList<>();

    public static class RoomDisplayItem {
        public final Room room;
        public final boolean isAvailable;

        public RoomDisplayItem(Room room, boolean isAvailable) {
            this.room = room;
            this.isAvailable = isAvailable;
        }
    }

    public FindRoomsPanel(GuiController controller,
                          BiConsumer<String, LocalDate> scheduleNavCallback,
                          Runnable myBookingsNavCallback,
                          Runnable manageRoomsNavCallback) {
        this.controller = controller;
        this.scheduleNavCallback = scheduleNavCallback;
        this.myBookingsNavCallback = myBookingsNavCallback;
        this.manageRoomsNavCallback = manageRoomsNavCallback;

        initComponents();
        controller.addListener(this::onDomainDataChanged);
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(GuiTheme.COLOR_BG);
        // V3 Content margin: margin: 31px 30px 72px
        setBorder(new EmptyBorder(31, 30, 72, 30));

        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setOpaque(false);

        // Heading: Find a room (font 25px, letter-spacing -0.55px)
        JLabel pageHeading = new JLabel("Find a room");
        pageHeading.setFont(GuiTheme.FONT_H1);
        pageHeading.setForeground(GuiTheme.COLOR_TEXT);
        pageHeading.setAlignmentX(LEFT_ALIGNMENT);
        pageHeading.setBorder(new EmptyBorder(0, 0, 19, 0));
        mainContent.add(pageHeading);

        // Search Filter Panel Card (padding 17px 20px, border-radius 7px)
        JPanel filterCard = createFilterCard();
        filterCard.setAlignmentX(LEFT_ALIGNMENT);
        mainContent.add(filterCard);
        mainContent.add(Box.createVerticalStrut(26));

        // Results Heading Bar
        JPanel resultsHeadingBar = createResultsHeadingBar();
        resultsHeadingBar.setAlignmentX(LEFT_ALIGNMENT);
        mainContent.add(resultsHeadingBar);
        mainContent.add(Box.createVerticalStrut(12));

        // Results Container
        resultsCardLayout = new CardLayout();
        resultsCardContainer = new JPanel(resultsCardLayout);
        resultsCardContainer.setOpaque(false);
        resultsCardContainer.setAlignmentX(LEFT_ALIGNMENT);

        resultsCardContainer.add(createTableWrap(), CARD_TABLE);
        resultsCardContainer.add(createEmptyStateWrap(), CARD_EMPTY);

        mainContent.add(resultsCardContainer);
        add(mainContent, BorderLayout.CENTER);

        // Run initial search
        executeSearch(false);
    }

    private JPanel createFilterCard() {
        JPanel card = GuiTheme.createCardPanel();
        card.setLayout(new FlowLayout(FlowLayout.LEFT, 16, 17));
        card.setMaximumSize(new Dimension(Short.MAX_VALUE, 98));

        // 1. Date
        dateField = GuiTheme.createTextField(10);
        dateField.setText("2026-11-03");
        dateField.addActionListener(e -> executeSearch(true));
        card.add(createFieldBox("Date", dateField));

        // 2. Start
        startField = GuiTheme.createTextField(6);
        startField.setText("14:00");
        startField.addActionListener(e -> executeSearch(true));
        card.add(createFieldBox("Start", startField));

        // 3. End
        endField = GuiTheme.createTextField(6);
        endField.setText("15:00");
        endField.addActionListener(e -> executeSearch(true));
        card.add(createFieldBox("End", endField));

        // 4. People
        capacityField = GuiTheme.createTextField(5);
        capacityField.setText("10");
        capacityField.addActionListener(e -> executeSearch(true));
        card.add(createFieldBox("People", capacityField));

        // 5. Equipment Multi-Select (Preselect Wi-Fi matching CROBS_GUI_V3.html)
        selectedFeatures.add("wifi");
        equipDropdownBtn = GuiTheme.createSecondaryButton("1 selected ⌄");
        equipDropdownBtn.setFont(GuiTheme.FONT_BODY);
        equipDropdownBtn.setPreferredSize(new Dimension(175, 39));
        equipDropdownBtn.addActionListener(e -> showEquipmentPopup());
        card.add(createFieldBox("Equipment · optional", equipDropdownBtn));

        // 6. Find rooms button
        JButton searchBtn = GuiTheme.createPrimaryButton("Find rooms");
        searchBtn.setPreferredSize(new Dimension(120, 39));
        searchBtn.addActionListener(e -> executeSearch(true));

        JPanel btnBox = new JPanel();
        btnBox.setLayout(new BoxLayout(btnBox, BoxLayout.Y_AXIS));
        btnBox.setOpaque(false);
        JLabel spacer = new JLabel(" ");
        spacer.setFont(GuiTheme.FONT_SMALL_BOLD);
        btnBox.add(spacer);
        btnBox.add(Box.createVerticalStrut(4));
        btnBox.add(searchBtn);
        card.add(btnBox);

        return card;
    }

    private void showEquipmentPopup() {
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(Color.WHITE);
        menu.setBorder(BorderFactory.createLineBorder(GuiTheme.COLOR_BORDER, 1));

        for (String eq : allKnownEquipment) {
            String norm = eq.toLowerCase().replace(" ", "_").replace("-", "");
            boolean checked = selectedFeatures.contains(norm) || selectedFeatures.contains(eq.toLowerCase());
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(eq, checked);
            item.setFont(GuiTheme.FONT_BODY);
            item.addActionListener(e -> {
                if (item.isSelected()) {
                    selectedFeatures.add(norm);
                } else {
                    selectedFeatures.remove(norm);
                    selectedFeatures.remove(eq.toLowerCase());
                }
                updateEquipButtonLabel();
            });
            menu.add(item);
        }

        menu.show(equipDropdownBtn, 0, equipDropdownBtn.getHeight() + 2);
    }

    private void updateEquipButtonLabel() {
        if (selectedFeatures.isEmpty()) {
            equipDropdownBtn.setText("Any equipment ⌄");
        } else {
            equipDropdownBtn.setText(selectedFeatures.size() + " selected ⌄");
        }
    }

    private JPanel createFieldBox(String labelText, JComponent field) {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setOpaque(false);

        JLabel label = new JLabel(labelText);
        label.setFont(GuiTheme.FONT_SMALL_BOLD);
        label.setForeground(GuiTheme.COLOR_MUTED);
        label.setBorder(new EmptyBorder(0, 0, 8, 0));

        box.add(label);
        box.add(field);
        return box;
    }

    private JPanel createResultsHeadingBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setMaximumSize(new Dimension(Short.MAX_VALUE, 34));

        JPanel textGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        textGroup.setOpaque(false);

        resultsTitleLabel = new JLabel("Rooms available");
        resultsTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        resultsTitleLabel.setForeground(GuiTheme.COLOR_TEXT);

        resultsSubtitleLabel = new JLabel("");
        resultsSubtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        resultsSubtitleLabel.setForeground(new Color(124, 133, 145));

        textGroup.add(resultsTitleLabel);
        textGroup.add(resultsSubtitleLabel);
        bar.add(textGroup, BorderLayout.WEST);

        showBookedCheck = new JCheckBox("Show booked rooms");
        showBookedCheck.setFont(GuiTheme.FONT_SMALL);
        showBookedCheck.setForeground(GuiTheme.COLOR_MUTED);
        showBookedCheck.setOpaque(false);
        showBookedCheck.setCursor(new Cursor(Cursor.HAND_CURSOR));
        showBookedCheck.addActionListener(e -> executeSearch(false));
        bar.add(showBookedCheck, BorderLayout.EAST);

        return bar;
    }

    private JPanel createTableWrap() {
        JPanel wrap = GuiTheme.createCardPanel();
        wrap.setLayout(new BorderLayout());

        tableModel = new RoomResultsModel();
        resultsTable = new JTable(tableModel);
        GuiTheme.styleTable(resultsTable);
        resultsTable.setRowHeight(72);

        // Proportional column widths matching V3 grid
        resultsTable.getColumnModel().getColumn(0).setPreferredWidth(260); // Room
        resultsTable.getColumnModel().getColumn(1).setPreferredWidth(130); // Capacity
        resultsTable.getColumnModel().getColumn(2).setPreferredWidth(320); // Equipment
        resultsTable.getColumnModel().getColumn(3).setPreferredWidth(140); // Status
        resultsTable.getColumnModel().getColumn(4).setPreferredWidth(220); // Actions

        // Cell Renderers
        resultsTable.getColumnModel().getColumn(0).setCellRenderer(new RoomCellRenderer());
        resultsTable.getColumnModel().getColumn(1).setCellRenderer(new CapacityCellRenderer());
        resultsTable.getColumnModel().getColumn(2).setCellRenderer(new EquipmentCellRenderer());
        resultsTable.getColumnModel().getColumn(3).setCellRenderer(new StatusCellRenderer());
        resultsTable.getColumnModel().getColumn(4).setCellRenderer(new ActionButtonsRenderer());

        // Instant single-click action listener on column 4
        resultsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int row = resultsTable.rowAtPoint(e.getPoint());
                int col = resultsTable.columnAtPoint(e.getPoint());
                if (row >= 0 && col == 4 && row < currentDisplayItems.size()) {
                    java.awt.Rectangle rect = resultsTable.getCellRect(row, col, false);
                    int clickX = e.getX() - rect.x;
                    int rightX = rect.width;
                    RoomDisplayItem item = currentDisplayItems.get(row);
                    // Book room button: rightX - 104 to rightX - 8
                    if (clickX >= rightX - 104 && clickX <= rightX - 8) {
                        if (item.isAvailable) {
                            openBookingDrawer(item.room);
                        }
                    } else if (clickX >= rightX - 200 && clickX <= rightX - 112) {
                        onViewWeekClicked(item.room);
                    }
                }
            }
        });

        resultsTable.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = resultsTable.rowAtPoint(e.getPoint());
                int col = resultsTable.columnAtPoint(e.getPoint());
                if (row >= 0 && col == 4 && row < currentDisplayItems.size()) {
                    java.awt.Rectangle rect = resultsTable.getCellRect(row, col, false);
                    int clickX = e.getX() - rect.x;
                    int rightX = rect.width;
                    RoomDisplayItem item = currentDisplayItems.get(row);
                    boolean overBook = (clickX >= rightX - 104 && clickX <= rightX - 8 && item.isAvailable);
                    boolean overWeek = (clickX >= rightX - 200 && clickX <= rightX - 112);
                    if (overBook || overWeek) {
                        resultsTable.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        return;
                    }
                }
                resultsTable.setCursor(Cursor.getDefaultCursor());
            }
        });

        JScrollPane scroll = new JScrollPane(resultsTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        wrap.add(scroll, BorderLayout.CENTER);

        return wrap;
    }

    private JPanel createEmptyStateWrap() {
        JPanel wrap = GuiTheme.createCardPanel();
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.setBorder(new EmptyBorder(52, 20, 52, 20));

        JLabel title = new JLabel("No rooms match this time");
        title.setFont(new Font("Segoe UI", Font.BOLD, 17));
        title.setForeground(new Color(37, 42, 49));
        title.setAlignmentX(CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Try another time, reduce the capacity, or remove equipment filters.");
        sub.setFont(GuiTheme.FONT_BODY);
        sub.setForeground(new Color(109, 119, 132));
        sub.setAlignmentX(CENTER_ALIGNMENT);

        wrap.add(title);
        wrap.add(Box.createVerticalStrut(7));
        wrap.add(sub);
        return wrap;
    }

    public void openBookingDrawer(Room room) {
        this.activeBookingRoom = room;
        MainFrame top = (MainFrame) SwingUtilities.getWindowAncestor(this);
        if (top != null) {
            LocalDate d = LocalDate.now().plusDays(1);
            LocalTime st = LocalTime.of(14, 0);
            LocalTime en = LocalTime.of(15, 0);
            try {
                d = LocalDate.parse(dateField.getText().trim(), DATE_FMT);
                st = LocalTime.parse(startField.getText().trim(), TIME_FMT);
                en = LocalTime.parse(endField.getText().trim(), TIME_FMT);
            } catch (Exception ignored) {
            }
            top.openBookFromSearch(room, d, st, en);
        }
    }

    public void closeBookingDrawer() {
        this.activeBookingRoom = null;
        MainFrame top = (MainFrame) SwingUtilities.getWindowAncestor(this);
        if (top != null) {
            top.closeDrawer();
        }
    }

    private void onViewWeekClicked(Room room) {
        LocalDate date = LocalDate.now();
        try {
            date = LocalDate.parse(dateField.getText().trim(), DATE_FMT);
        } catch (Exception ignored) {
        }
        if (scheduleNavCallback != null) {
            scheduleNavCallback.accept(room.getId(), date);
        }
    }

    public void executeSearch(boolean logCommand) {
        LocalDate date;
        LocalTime start;
        LocalTime end;
        int capacity;

        try {
            date = LocalDate.parse(dateField.getText().trim(), DATE_FMT);
            start = LocalTime.parse(startField.getText().trim(), TIME_FMT);
            end = LocalTime.parse(endField.getText().trim(), TIME_FMT);
            capacity = Integer.parseInt(capacityField.getText().trim());
        } catch (Exception ex) {
            resultsTitleLabel.setText("Invalid search input");
            resultsSubtitleLabel.setText("Please check your date, time and capacity");
            currentDisplayItems.clear();
            tableModel.fireTableDataChanged();
            resultsCardLayout.show(resultsCardContainer, CARD_EMPTY);
            return;
        }

        List<Room> availableRooms;
        if (logCommand) {
            availableRooms = controller.search(date, start, end, capacity, selectedFeatures);
        } else {
            availableRooms = controller.searchQuiet(date, start, end, capacity, selectedFeatures);
        }

        currentDisplayItems.clear();
        boolean showBooked = showBookedCheck.isSelected();

        if (showBooked) {
            List<Room> allRooms = controller.getAllRoomsQuiet();
            for (Room r : allRooms) {
                if (r.matchesCapacity(capacity) && r.hasAllFeatures(selectedFeatures)) {
                    boolean isAvail = availableRooms.contains(r);
                    currentDisplayItems.add(new RoomDisplayItem(r, isAvail));
                }
            }
        } else {
            for (Room r : availableRooms) {
                currentDisplayItems.add(new RoomDisplayItem(r, true));
            }
        }

        resultsTitleLabel.setText(availableRooms.size() + (availableRooms.size() == 1 ? " room" : " rooms") + " available");
        resultsSubtitleLabel.setText(date.format(READABLE_DATE) + " · " + start + "–" + end);
        tableModel.fireTableDataChanged();

        if (currentDisplayItems.isEmpty()) {
            resultsCardLayout.show(resultsCardContainer, CARD_EMPTY);
        } else {
            resultsCardLayout.show(resultsCardContainer, CARD_TABLE);
        }
    }

    private void onDomainDataChanged() {
        executeSearch(false);
    }

    private static String formatRoomType(RoomType type) {
        switch (type) {
            case lectureTheatre:
                return "Lecture theatre";
            case computerLab:
                return "Computer lab";
            case seminarRoom:
                return "Seminar room";
            case meetingRoom:
                return "Meeting room";
            default:
                return type.name();
        }
    }

    // --- Table Model & Renderers ---

    private class RoomResultsModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private final String[] cols = {"Room", "Capacity", "Equipment", "Status", ""};

        @Override
        public int getRowCount() {
            return currentDisplayItems.size();
        }

        @Override
        public int getColumnCount() {
            return cols.length;
        }

        @Override
        public String getColumnName(int c) {
            return cols[c];
        }

        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }

        @Override
        public Object getValueAt(int r, int c) {
            return currentDisplayItems.get(r);
        }
    }

    private static class RoomCellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int col) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 22));
            p.setBackground(isSelected ? GuiTheme.COLOR_LIGHT : Color.WHITE);
            if (value instanceof RoomDisplayItem) {
                Room r = ((RoomDisplayItem) value).room;
                JLabel typeLabel = new JLabel(formatRoomType(r.getType()));
                typeLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
                typeLabel.setForeground(GuiTheme.COLOR_TEXT);

                JLabel idLabel = new JLabel(r.getId());
                idLabel.setFont(new Font("Consolas", Font.PLAIN, 12));
                idLabel.setForeground(new Color(133, 141, 153));

                p.add(typeLabel);
                p.add(idLabel);
            }
            return p;
        }
    }

    private static class CapacityCellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int col) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            label.setFont(GuiTheme.FONT_BODY);
            label.setForeground(new Color(56, 68, 83));
            if (value instanceof RoomDisplayItem) {
                label.setText(((RoomDisplayItem) value).room.getCapacity() + " seats");
            }
            label.setBorder(new EmptyBorder(0, 16, 0, 0));
            return label;
        }
    }

    private static class EquipmentCellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int col) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            label.setFont(GuiTheme.FONT_BODY);
            label.setForeground(new Color(87, 99, 112));
            if (value instanceof RoomDisplayItem) {
                Room r = ((RoomDisplayItem) value).room;
                label.setText(r.getFeatures().isEmpty() ? "None specified" : String.join(" · ", r.getFeatures()));
            }
            label.setBorder(new EmptyBorder(0, 16, 0, 0));
            return label;
        }
    }

    private static class StatusCellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int col) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 24)) {
                private static final long serialVersionUID = 1L;
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    if (value instanceof RoomDisplayItem) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        boolean avail = ((RoomDisplayItem) value).isAvailable;
                        g2.setColor(avail ? GuiTheme.COLOR_GREEN_DOT : new Color(170, 117, 103));
                        g2.fillOval(6, 31, 6, 6);
                        g2.dispose();
                    }
                }
            };
            p.setBackground(isSelected ? GuiTheme.COLOR_LIGHT : Color.WHITE);
            if (value instanceof RoomDisplayItem) {
                boolean avail = ((RoomDisplayItem) value).isAvailable;
                JLabel text = new JLabel(avail ? "      Available" : "      Booked");
                text.setFont(new Font("Segoe UI", Font.BOLD, 12));
                text.setForeground(avail ? new Color(70, 106, 85) : new Color(138, 102, 96));
                p.add(text);
            }
            return p;
        }
    }

    private class ActionButtonsRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int col) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 18));
            p.setBackground(isSelected ? GuiTheme.COLOR_LIGHT : Color.WHITE);
            if (value instanceof RoomDisplayItem) {
                boolean avail = ((RoomDisplayItem) value).isAvailable;

                JButton viewWeekBtn = GuiTheme.createTextButton("View week");
                viewWeekBtn.setPreferredSize(new Dimension(88, 32));

                JButton bookBtn = GuiTheme.createSmallPrimaryButton("Book room");
                bookBtn.setPreferredSize(new Dimension(96, 32));
                bookBtn.setEnabled(avail);

                p.add(viewWeekBtn);
                p.add(bookBtn);
            }
            return p;
        }
    }

    private class ActionButtonsEditor extends AbstractCellEditor implements TableCellEditor {
        private static final long serialVersionUID = 1L;
        private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 18));
        private final JButton viewWeekBtn = GuiTheme.createTextButton("View week");
        private final JButton bookBtn = GuiTheme.createSmallPrimaryButton("Book room");
        private RoomDisplayItem currentItem;

        ActionButtonsEditor() {
            viewWeekBtn.setPreferredSize(new Dimension(88, 32));
            bookBtn.setPreferredSize(new Dimension(96, 32));

            viewWeekBtn.addActionListener(e -> {
                fireEditingStopped();
                if (currentItem != null) {
                    onViewWeekClicked(currentItem.room);
                }
            });

            bookBtn.addActionListener(e -> {
                fireEditingStopped();
                if (currentItem != null && currentItem.isAvailable) {
                    openBookingDrawer(currentItem.room);
                }
            });

            panel.add(viewWeekBtn);
            panel.add(bookBtn);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int col) {
            panel.setBackground(GuiTheme.COLOR_LIGHT);
            if (value instanceof RoomDisplayItem) {
                currentItem = (RoomDisplayItem) value;
                bookBtn.setEnabled(currentItem.isAvailable);
            }
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return currentItem;
        }
    }
}
