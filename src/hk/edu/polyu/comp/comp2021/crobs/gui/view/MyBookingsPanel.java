package hk.edu.polyu.comp.comp2021.crobs.gui.view;

import hk.edu.polyu.comp.comp2021.crobs.gui.GuiController;
import hk.edu.polyu.comp.comp2021.crobs.gui.GuiTheme;
import hk.edu.polyu.comp.comp2021.crobs.gui.MainFrame;
import hk.edu.polyu.comp.comp2021.crobs.gui.dialog.ConfirmationDialog;
import hk.edu.polyu.comp.comp2021.crobs.gui.dialog.ModifyBookingDialog;
import hk.edu.polyu.comp.comp2021.crobs.model.Booking;
import hk.edu.polyu.comp.comp2021.crobs.model.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.model.Room;
import hk.edu.polyu.comp.comp2021.crobs.model.RoomType;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
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
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Screen C: My Bookings workspace displaying reservations owned by the current user
 * with inline Edit (via V3 slide-out drawer) and Cancel (via V3 confirmation modal).
 */
public class MyBookingsPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEE, d MMM");

    private final GuiController controller;
    private final Runnable findRoomsNavCallback;

    private final List<Booking> currentBookings = new ArrayList<>();
    private BookingsTableModel tableModel;
    private JTable bookingsTable;

    private JPanel cardContainer;
    private CardLayout cardLayout;

    private static final String CARD_TABLE = "TABLE";
    private static final String CARD_EMPTY = "EMPTY";

    public MyBookingsPanel(GuiController controller, Runnable findRoomsNavCallback) {
        this.controller = controller;
        this.findRoomsNavCallback = findRoomsNavCallback;

        initComponents();
        controller.addListener(this::onDomainChanged);
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(GuiTheme.COLOR_BG);
        // V3 Margin: margin: 31px 30px 72px
        setBorder(new EmptyBorder(31, 30, 72, 30));

        // Header Section
        JPanel headerBar = new JPanel(new BorderLayout());
        headerBar.setOpaque(false);
        headerBar.setBorder(new EmptyBorder(0, 0, 23, 0));

        JPanel textGroup = new JPanel();
        textGroup.setLayout(new BoxLayout(textGroup, BoxLayout.Y_AXIS));
        textGroup.setOpaque(false);

        JLabel titleLabel = new JLabel("My bookings");
        titleLabel.setFont(GuiTheme.FONT_H1);
        titleLabel.setForeground(GuiTheme.COLOR_TEXT);

        JLabel subtitleLabel = new JLabel("View, edit, or cancel your reservations.");
        subtitleLabel.setFont(GuiTheme.FONT_BODY);
        subtitleLabel.setForeground(GuiTheme.COLOR_MUTED);

        textGroup.add(titleLabel);
        textGroup.add(Box.createVerticalStrut(4));
        textGroup.add(subtitleLabel);
        headerBar.add(textGroup, BorderLayout.WEST);

        JButton findRoomBtn = GuiTheme.createPrimaryButton("Find a room");
        findRoomBtn.addActionListener(e -> {
            if (findRoomsNavCallback != null) {
                findRoomsNavCallback.run();
            }
        });
        headerBar.add(findRoomBtn, BorderLayout.EAST);

        add(headerBar, BorderLayout.NORTH);

        // Center: CardLayout (Table vs Empty state)
        cardLayout = new CardLayout();
        cardContainer = new JPanel(cardLayout);
        cardContainer.setOpaque(false);

        cardContainer.add(createTableCard(), CARD_TABLE);
        cardContainer.add(createEmptyStateCard(), CARD_EMPTY);

        add(cardContainer, BorderLayout.CENTER);

        refreshBookings(false);
    }

    private JPanel createTableCard() {
        JPanel card = GuiTheme.createCardPanel();
        card.setLayout(new BorderLayout());

        tableModel = new BookingsTableModel();
        bookingsTable = new JTable(tableModel);
        GuiTheme.styleTable(bookingsTable);
        bookingsTable.setRowHeight(64);

        // Column widths
        bookingsTable.getColumnModel().getColumn(0).setPreferredWidth(220); // Date & time
        bookingsTable.getColumnModel().getColumn(1).setPreferredWidth(240); // Room
        bookingsTable.getColumnModel().getColumn(2).setPreferredWidth(280); // Purpose
        bookingsTable.getColumnModel().getColumn(3).setPreferredWidth(120); // Booking ID
        bookingsTable.getColumnModel().getColumn(4).setPreferredWidth(180); // Actions

        bookingsTable.getColumnModel().getColumn(0).setCellRenderer(new DateTimeCellRenderer());
        bookingsTable.getColumnModel().getColumn(1).setCellRenderer(new RoomInfoCellRenderer());
        bookingsTable.getColumnModel().getColumn(2).setCellRenderer(new PurposeCellRenderer());
        bookingsTable.getColumnModel().getColumn(3).setCellRenderer(new CodeCellRenderer());
        bookingsTable.getColumnModel().getColumn(4).setCellRenderer(new ActionsCellRenderer());

        // Instant single-click action listener on column 4
        bookingsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int row = bookingsTable.rowAtPoint(e.getPoint());
                int col = bookingsTable.columnAtPoint(e.getPoint());
                if (row >= 0 && col == 4 && row < currentBookings.size()) {
                    java.awt.Rectangle rect = bookingsTable.getCellRect(row, col, false);
                    int clickX = e.getX() - rect.x;
                    int rightX = rect.width;
                    Booking b = currentBookings.get(row);
                    // Cancel button: rightX - 84 to rightX - 16
                    if (clickX >= rightX - 84 && clickX <= rightX - 16) {
                        onCancelBooking(b);
                    } else if (clickX >= rightX - 144 && clickX <= rightX - 90) {
                        // Edit button: rightX - 144 to rightX - 90
                        onEditBooking(b);
                    }
                }
            }
        });

        bookingsTable.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = bookingsTable.rowAtPoint(e.getPoint());
                int col = bookingsTable.columnAtPoint(e.getPoint());
                if (row >= 0 && col == 4 && row < currentBookings.size()) {
                    java.awt.Rectangle rect = bookingsTable.getCellRect(row, col, false);
                    int clickX = e.getX() - rect.x;
                    int rightX = rect.width;
                    boolean overCancel = (clickX >= rightX - 84 && clickX <= rightX - 16);
                    boolean overEdit = (clickX >= rightX - 144 && clickX <= rightX - 90);
                    if (overCancel || overEdit) {
                        bookingsTable.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        return;
                    }
                }
                bookingsTable.setCursor(Cursor.getDefaultCursor());
            }
        });

        JScrollPane scroll = new JScrollPane(bookingsTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    private JPanel createEmptyStateCard() {
        JPanel card = GuiTheme.createCardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(52, 20, 52, 20));

        JLabel title = new JLabel("No bookings yet");
        title.setFont(new Font("Segoe UI", Font.BOLD, 17));
        title.setForeground(new Color(37, 42, 49));
        title.setAlignmentX(CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Available rooms are just one search away.");
        sub.setFont(GuiTheme.FONT_BODY);
        sub.setForeground(new Color(109, 119, 132));
        sub.setAlignmentX(CENTER_ALIGNMENT);

        JButton findBtn = GuiTheme.createPrimaryButton("Find rooms");
        findBtn.setAlignmentX(CENTER_ALIGNMENT);
        findBtn.addActionListener(e -> {
            if (findRoomsNavCallback != null) {
                findRoomsNavCallback.run();
            }
        });

        card.add(title);
        card.add(Box.createVerticalStrut(7));
        card.add(sub);
        card.add(Box.createVerticalStrut(17));
        card.add(findBtn);

        return card;
    }

    public void refreshBookings(boolean logCommand) {
        currentBookings.clear();
        if (controller.isLoggedIn()) {
            if (logCommand) {
                try {
                    currentBookings.addAll(controller.userListBookings());
                } catch (CrobsException e) {
                    // Handled
                }
            } else {
                currentBookings.addAll(controller.getBookingsQuiet());
            }
        }

        tableModel.fireTableDataChanged();
        if (currentBookings.isEmpty()) {
            cardLayout.show(cardContainer, CARD_EMPTY);
        } else {
            cardLayout.show(cardContainer, CARD_TABLE);
        }
    }

    private void onDomainChanged() {
        refreshBookings(false);
    }

    private void onEditBooking(Booking booking) {
        MainFrame top = (MainFrame) SwingUtilities.getWindowAncestor(this);
        if (top != null) {
            top.openEditBooking(booking);
        } else {
            JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
            ModifyBookingDialog dialog = new ModifyBookingDialog(parent, controller, booking);
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                refreshBookings(false);
            }
        }
    }

    private void onCancelBooking(Booking booking) {
        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        boolean confirmed = ConfirmationDialog.confirm(
                parent,
                "Cancel booking?",
                booking.getId() + " · " + formatRoomType(getRoomType(booking.getRoomId())) + " · " +
                        booking.getDate() + ", " + booking.getStartTime() + "–" + booking.getEndTime() + ". This cannot be undone.",
                "Cancel booking"
        );

        if (confirmed) {
            try {
                controller.deleteBooking(booking.getId());
                refreshBookings(false);
                MainFrame top = (MainFrame) SwingUtilities.getWindowAncestor(this);
                if (top != null) {
                    top.showToast("Change saved");
                }
            } catch (CrobsException e) {
                JOptionPane.showMessageDialog(parent, "Could not cancel booking: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private RoomType getRoomType(String roomId) {
        Room r = controller.getRoomQuiet(roomId);
        return (r != null) ? r.getType() : RoomType.seminarRoom;
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

    // --- Table Model & Renderers ---

    private class BookingsTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private final String[] cols = {"Date & time", "Room", "Purpose", "Booking ID", ""};

        @Override
        public int getRowCount() {
            return currentBookings.size();
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
            return currentBookings.get(r);
        }
    }

    private static class DateTimeCellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int col) {
            JPanel p = new JPanel(new GridLayout(2, 1, 0, 2));
            p.setBackground(isSelected ? GuiTheme.COLOR_LIGHT : Color.WHITE);
            p.setBorder(new EmptyBorder(12, 22, 12, 10));
            if (value instanceof Booking) {
                Booking b = (Booking) value;
                JLabel dLabel = new JLabel(b.getDate().format(DATE_FMT));
                dLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
                dLabel.setForeground(GuiTheme.COLOR_TEXT);

                JLabel tLabel = new JLabel(b.getStartTime() + "–" + b.getEndTime());
                tLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                tLabel.setForeground(new Color(140, 149, 160));

                p.add(dLabel);
                p.add(tLabel);
            }
            return p;
        }
    }

    private class RoomInfoCellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int col) {
            JPanel p = new JPanel(new GridLayout(2, 1, 0, 2));
            p.setBackground(isSelected ? GuiTheme.COLOR_LIGHT : Color.WHITE);
            p.setBorder(new EmptyBorder(12, 10, 12, 10));
            if (value instanceof Booking) {
                Booking b = (Booking) value;
                Room r = controller.getRoomQuiet(b.getRoomId());
                String typeName = r != null ? formatRoomType(r.getType()) : "Room";

                JLabel typeLabel = new JLabel(typeName);
                typeLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
                typeLabel.setForeground(GuiTheme.COLOR_TEXT);

                JLabel idLabel = new JLabel(b.getRoomId());
                idLabel.setFont(new Font("Consolas", Font.PLAIN, 11));
                idLabel.setForeground(new Color(140, 149, 160));

                p.add(typeLabel);
                p.add(idLabel);
            }
            return p;
        }
    }

    private static class PurposeCellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int col) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            label.setFont(GuiTheme.FONT_BODY);
            label.setForeground(new Color(69, 80, 93));
            if (value instanceof Booking) {
                Booking b = (Booking) value;
                String p = b.getPurpose();
                label.setText(p.isEmpty() ? "—" : p.replace("_", " "));
            }
            label.setBorder(new EmptyBorder(0, 10, 0, 10));
            return label;
        }
    }

    private static class CodeCellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int col) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            label.setFont(new Font("Consolas", Font.PLAIN, 12));
            label.setForeground(new Color(120, 130, 142));
            if (value instanceof Booking) {
                label.setText(((Booking) value).getId());
            }
            label.setBorder(new EmptyBorder(0, 10, 0, 10));
            return label;
        }
    }

    private class ActionsCellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int col) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 16));
            p.setBackground(isSelected ? GuiTheme.COLOR_LIGHT : Color.WHITE);
            p.setBorder(new EmptyBorder(0, 0, 0, 16));

            JButton editBtn = GuiTheme.createTextButton("Edit");
            editBtn.setPreferredSize(new Dimension(54, 30));

            JButton cancelBtn = GuiTheme.createDangerTextButton("Cancel");
            cancelBtn.setPreferredSize(new Dimension(68, 30));

            p.add(editBtn);
            p.add(cancelBtn);
            return p;
        }
    }

    private class ActionsCellEditor extends AbstractCellEditor implements TableCellEditor {
        private static final long serialVersionUID = 1L;
        private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 16));
        private final JButton editBtn = GuiTheme.createTextButton("Edit");
        private final JButton cancelBtn = GuiTheme.createDangerTextButton("Cancel");
        private Booking currentBooking;

        ActionsCellEditor() {
            editBtn.setPreferredSize(new Dimension(54, 30));
            cancelBtn.setPreferredSize(new Dimension(68, 30));

            editBtn.addActionListener(e -> {
                fireEditingStopped();
                if (currentBooking != null) {
                    onEditBooking(currentBooking);
                }
            });

            cancelBtn.addActionListener(e -> {
                fireEditingStopped();
                if (currentBooking != null) {
                    onCancelBooking(currentBooking);
                }
            });

            panel.add(editBtn);
            panel.add(cancelBtn);
            panel.setBorder(new EmptyBorder(0, 0, 0, 16));
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int col) {
            panel.setBackground(GuiTheme.COLOR_LIGHT);
            if (value instanceof Booking) {
                currentBooking = (Booking) value;
            }
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return currentBooking;
        }
    }
}
