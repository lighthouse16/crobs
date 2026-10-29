package hk.edu.polyu.comp.comp2021.crobs.gui.view;

import hk.edu.polyu.comp.comp2021.crobs.gui.GuiController;
import hk.edu.polyu.comp.comp2021.crobs.gui.GuiTheme;
import hk.edu.polyu.comp.comp2021.crobs.gui.MainFrame;
import hk.edu.polyu.comp.comp2021.crobs.gui.dialog.ConfirmationDialog;
import hk.edu.polyu.comp.comp2021.crobs.gui.dialog.RoomEditorDialog;
import hk.edu.polyu.comp.comp2021.crobs.model.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.model.Room;
import hk.edu.polyu.comp.comp2021.crobs.model.RoomType;
import hk.edu.polyu.comp.comp2021.crobs.model.User;
import hk.edu.polyu.comp.comp2021.crobs.model.UserRole;

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
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
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
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List;

/**
 * Screen E: Administration panel for managing rooms (REQ7, REQ8, REQ9, REQ16)
 * and managing users (REQ2, REQ3, REQ4) matching the CROBS V3 specification.
 * Connects all creation and editing actions directly to the V3 slide-out drawer.
 */
public class ManagePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final String SUBTAB_ROOMS = "ROOMS";
    private static final String SUBTAB_USERS = "USERS";

    private final GuiController controller;

    private CardLayout subtabCardLayout;
    private JPanel subtabContainer;

    private JButton roomsSubtabBtn;
    private JButton usersSubtabBtn;

    // Rooms
    private final List<Room> roomsList = new ArrayList<>();
    private RoomsTableModel roomsTableModel;
    private JTable roomsTable;

    // Users
    private final List<User> usersList = new ArrayList<>();
    private UsersTableModel usersTableModel;
    private JTable usersTable;

    public ManagePanel(GuiController controller) {
        this.controller = controller;

        initComponents();
        controller.addListener(this::onDomainChanged);
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(GuiTheme.COLOR_BG);
        // V3 Margin: margin: 31px 30px 72px
        setBorder(new EmptyBorder(31, 30, 72, 30));

        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setOpaque(false);

        // Heading: Manage (font 25px bold)
        JLabel pageHeading = new JLabel("Manage");
        pageHeading.setFont(GuiTheme.FONT_H1);
        pageHeading.setForeground(GuiTheme.COLOR_TEXT);
        pageHeading.setAlignmentX(LEFT_ALIGNMENT);
        mainContent.add(pageHeading);
        mainContent.add(Box.createVerticalStrut(14));

        // Subtabs Bar
        JPanel subtabsBar = createSubtabsBar();
        subtabsBar.setAlignmentX(LEFT_ALIGNMENT);
        mainContent.add(subtabsBar);
        mainContent.add(Box.createVerticalStrut(18));

        // Container with CardLayout for Rooms / Users
        subtabCardLayout = new CardLayout();
        subtabContainer = new JPanel(subtabCardLayout);
        subtabContainer.setOpaque(false);
        subtabContainer.setAlignmentX(LEFT_ALIGNMENT);

        subtabContainer.add(createRoomsView(), SUBTAB_ROOMS);
        subtabContainer.add(createUsersView(), SUBTAB_USERS);

        mainContent.add(subtabContainer);
        add(mainContent, BorderLayout.CENTER);

        refreshData();
    }

    private JPanel createSubtabsBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 22, 0));
        bar.setOpaque(false);
        Border bottomBorder = BorderFactory.createMatteBorder(0, 0, 1, 0, GuiTheme.COLOR_BORDER);
        bar.setBorder(bottomBorder);
        bar.setMaximumSize(new Dimension(Short.MAX_VALUE, 40));

        roomsSubtabBtn = createSubtabButton("Rooms", true, () -> switchSubtab(SUBTAB_ROOMS));
        usersSubtabBtn = createSubtabButton("Users", false, () -> switchSubtab(SUBTAB_USERS));

        bar.add(roomsSubtabBtn);
        bar.add(usersSubtabBtn);

        return bar;
    }

    private JButton createSubtabButton(String text, boolean active, Runnable action) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(active ? new Color(38, 50, 63) : new Color(119, 129, 140));
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        Border activeBorder = BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(41, 79, 133));
        Border emptyBorder = BorderFactory.createEmptyBorder(0, 4, 13, 4);
        btn.setBorder(active ? new CompoundBorder(activeBorder, emptyBorder) : emptyBorder);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> action.run());
        return btn;
    }

    private void switchSubtab(String key) {
        subtabCardLayout.show(subtabContainer, key);
        boolean isRooms = SUBTAB_ROOMS.equals(key);

        Border activeB = new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(41, 79, 133)),
                BorderFactory.createEmptyBorder(0, 4, 13, 4)
        );
        Border inactiveB = BorderFactory.createEmptyBorder(0, 4, 13, 4);

        roomsSubtabBtn.setForeground(isRooms ? new Color(38, 50, 63) : new Color(119, 129, 140));
        roomsSubtabBtn.setBorder(isRooms ? activeB : inactiveB);

        usersSubtabBtn.setForeground(!isRooms ? new Color(38, 50, 63) : new Color(119, 129, 140));
        usersSubtabBtn.setBorder(!isRooms ? activeB : inactiveB);
    }

    private JPanel createRoomsView() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        // Toolbar: h2 All rooms + Add room button
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        toolbar.setMaximumSize(new Dimension(Short.MAX_VALUE, 44));
        toolbar.setBorder(new EmptyBorder(0, 0, 16, 0));

        JLabel title = new JLabel("All rooms");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(GuiTheme.COLOR_TEXT);
        toolbar.add(title, BorderLayout.WEST);

        JButton addRoomBtn = GuiTheme.createPrimaryButton("Add room");
        addRoomBtn.addActionListener(e -> onAddRoom());
        toolbar.add(addRoomBtn, BorderLayout.EAST);

        panel.add(toolbar);

        // Table Card
        JPanel card = GuiTheme.createCardPanel();
        card.setLayout(new BorderLayout());

        roomsTableModel = new RoomsTableModel();
        roomsTable = new JTable(roomsTableModel);
        GuiTheme.styleTable(roomsTable);
        roomsTable.setRowHeight(56);

        roomsTable.getColumnModel().getColumn(0).setPreferredWidth(120); // ID
        roomsTable.getColumnModel().getColumn(1).setPreferredWidth(220); // Type
        roomsTable.getColumnModel().getColumn(2).setPreferredWidth(120); // Seats
        roomsTable.getColumnModel().getColumn(3).setPreferredWidth(340); // Equipment
        roomsTable.getColumnModel().getColumn(4).setPreferredWidth(180); // Actions

        roomsTable.getColumnModel().getColumn(0).setCellRenderer(new BoldCellRenderer());
        roomsTable.getColumnModel().getColumn(1).setCellRenderer(new RoomTypeCellRenderer());
        roomsTable.getColumnModel().getColumn(2).setCellRenderer(new PlainCellRenderer());
        roomsTable.getColumnModel().getColumn(3).setCellRenderer(new PlainCellRenderer());
        roomsTable.getColumnModel().getColumn(4).setCellRenderer(new RoomActionsCellRenderer());

        // Instant single-click action listener on column 4
        roomsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int row = roomsTable.rowAtPoint(e.getPoint());
                int col = roomsTable.columnAtPoint(e.getPoint());
                if (row >= 0 && col == 4 && row < roomsList.size()) {
                    java.awt.Rectangle rect = roomsTable.getCellRect(row, col, false);
                    int clickX = e.getX() - rect.x;
                    int rightX = rect.width;
                    Room r = roomsList.get(row);
                    // Delete button: rightX - 84 to rightX - 16
                    if (clickX >= rightX - 84 && clickX <= rightX - 16) {
                        onDeleteRoom(r);
                    } else if (clickX >= rightX - 144 && clickX <= rightX - 90) {
                        // Edit button: rightX - 144 to rightX - 90
                        onEditRoom(r);
                    }
                }
            }
        });

        roomsTable.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = roomsTable.rowAtPoint(e.getPoint());
                int col = roomsTable.columnAtPoint(e.getPoint());
                if (row >= 0 && col == 4 && row < roomsList.size()) {
                    java.awt.Rectangle rect = roomsTable.getCellRect(row, col, false);
                    int clickX = e.getX() - rect.x;
                    int rightX = rect.width;
                    boolean overDel = (clickX >= rightX - 84 && clickX <= rightX - 16);
                    boolean overEdit = (clickX >= rightX - 144 && clickX <= rightX - 90);
                    if (overDel || overEdit) {
                        roomsTable.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        return;
                    }
                }
                roomsTable.setCursor(Cursor.getDefaultCursor());
            }
        });

        JScrollPane scroll = new JScrollPane(roomsTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        card.add(scroll, BorderLayout.CENTER);

        panel.add(card);
        return panel;
    }

    private JPanel createUsersView() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        // Toolbar: h2 Users + Add user button
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        toolbar.setMaximumSize(new Dimension(Short.MAX_VALUE, 44));
        toolbar.setBorder(new EmptyBorder(0, 0, 16, 0));

        JLabel title = new JLabel("Users");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(GuiTheme.COLOR_TEXT);
        toolbar.add(title, BorderLayout.WEST);

        JButton addUserBtn = GuiTheme.createPrimaryButton("Add user");
        addUserBtn.addActionListener(e -> onAddUser());
        toolbar.add(addUserBtn, BorderLayout.EAST);

        panel.add(toolbar);

        // Table Card
        JPanel card = GuiTheme.createCardPanel();
        card.setLayout(new BorderLayout());

        usersTableModel = new UsersTableModel();
        usersTable = new JTable(usersTableModel);
        GuiTheme.styleTable(usersTable);
        usersTable.setRowHeight(56);

        usersTable.getColumnModel().getColumn(0).setPreferredWidth(120); // ID
        usersTable.getColumnModel().getColumn(1).setPreferredWidth(260); // Name
        usersTable.getColumnModel().getColumn(2).setPreferredWidth(180); // Role
        usersTable.getColumnModel().getColumn(3).setPreferredWidth(220); // Actions

        usersTable.getColumnModel().getColumn(0).setCellRenderer(new BoldCellRenderer());
        usersTable.getColumnModel().getColumn(1).setCellRenderer(new PlainCellRenderer());
        usersTable.getColumnModel().getColumn(2).setCellRenderer(new PlainCellRenderer());
        usersTable.getColumnModel().getColumn(3).setCellRenderer(new UserActionsCellRenderer());

        // Instant single-click action listener on column 3
        usersTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int row = usersTable.rowAtPoint(e.getPoint());
                int col = usersTable.columnAtPoint(e.getPoint());
                if (row >= 0 && col == 3 && row < usersList.size()) {
                    java.awt.Rectangle rect = usersTable.getCellRect(row, col, false);
                    int clickX = e.getX() - rect.x;
                    int rightX = rect.width;
                    User u = usersList.get(row);
                    boolean hasRole = controller.isSuperadmin() && !u.getId().equalsIgnoreCase("u1");
                    boolean hasRemove = (u.getRole() == UserRole.REGULAR);

                    if (hasRole && hasRemove) {
                        if (clickX >= rightX - 88 && clickX <= rightX - 16) {
                            onRemoveUser(u);
                        } else if (clickX >= rightX - 194 && clickX <= rightX - 94) {
                            onChangeRole(u);
                        }
                    } else if (hasRole) {
                        if (clickX >= rightX - 116 && clickX <= rightX - 16) {
                            onChangeRole(u);
                        }
                    } else if (hasRemove) {
                        if (clickX >= rightX - 88 && clickX <= rightX - 16) {
                            onRemoveUser(u);
                        }
                    }
                }
            }
        });

        usersTable.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = usersTable.rowAtPoint(e.getPoint());
                int col = usersTable.columnAtPoint(e.getPoint());
                if (row >= 0 && col == 3 && row < usersList.size()) {
                    java.awt.Rectangle rect = usersTable.getCellRect(row, col, false);
                    int clickX = e.getX() - rect.x;
                    int rightX = rect.width;
                    User u = usersList.get(row);
                    boolean hasRole = controller.isSuperadmin() && !u.getId().equalsIgnoreCase("u1");
                    boolean hasRemove = (u.getRole() == UserRole.REGULAR);

                    boolean overRem = hasRemove && (clickX >= rightX - 88 && clickX <= rightX - 16);
                    boolean overRole = (hasRole && hasRemove && (clickX >= rightX - 194 && clickX <= rightX - 94))
                            || (hasRole && !hasRemove && (clickX >= rightX - 116 && clickX <= rightX - 16));

                    if (overRem || overRole) {
                        usersTable.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        return;
                    }
                }
                usersTable.setCursor(Cursor.getDefaultCursor());
            }
        });

        JScrollPane scroll = new JScrollPane(usersTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        card.add(scroll, BorderLayout.CENTER);

        panel.add(card);
        return panel;
    }

    public void refreshData() {
        roomsList.clear();
        roomsList.addAll(controller.getAllRoomsQuiet());
        if (roomsTableModel != null) {
            roomsTableModel.fireTableDataChanged();
        }

        usersList.clear();
        usersList.addAll(controller.getAllKnownUsers());
        if (usersTableModel != null) {
            usersTableModel.fireTableDataChanged();
        }
    }

    private void onDomainChanged() {
        refreshData();
    }

    private void onAddRoom() {
        MainFrame top = (MainFrame) SwingUtilities.getWindowAncestor(this);
        if (top != null) {
            top.openAddRoom();
        } else {
            JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
            RoomEditorDialog dialog = new RoomEditorDialog(parent, controller, null);
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                refreshData();
            }
        }
    }

    private void onEditRoom(Room room) {
        MainFrame top = (MainFrame) SwingUtilities.getWindowAncestor(this);
        if (top != null) {
            top.openEditRoom(room);
        } else {
            JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
            RoomEditorDialog dialog = new RoomEditorDialog(parent, controller, room);
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                refreshData();
            }
        }
    }

    private void onDeleteRoom(Room room) {
        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        boolean confirmed = ConfirmationDialog.confirm(
                parent,
                "Delete room?",
                room.getId() + " will be deleted. Its future bookings will also be canceled.",
                "Delete room"
        );

        if (confirmed) {
            try {
                controller.deleteRoom(room.getId());
                refreshData();
                MainFrame top = (MainFrame) SwingUtilities.getWindowAncestor(this);
                if (top != null) {
                    top.showToast("Change saved");
                }
            } catch (CrobsException e) {
                JOptionPane.showMessageDialog(parent, "Could not delete room: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onAddUser() {
        MainFrame top = (MainFrame) SwingUtilities.getWindowAncestor(this);
        if (top != null) {
            top.openAddUser();
        }
    }

    private void onChangeRole(User user) {
        if (!controller.isSuperadmin()) {
            JOptionPane.showMessageDialog(this, "Only Superadmin can change user roles.", "Access Denied", JOptionPane.WARNING_MESSAGE);
            return;
        }

        MainFrame top = (MainFrame) SwingUtilities.getWindowAncestor(this);
        if (top != null) {
            top.openChangeRole(user);
        }
    }

    private void onRemoveUser(User user) {
        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        boolean confirmed = ConfirmationDialog.confirm(
                parent,
                "Remove user?",
                user.getName() + " (" + user.getId() + ") and all their bookings will be removed.",
                "Remove user"
        );

        if (confirmed) {
            try {
                controller.removeUser(user.getId());
                refreshData();
                MainFrame top = (MainFrame) SwingUtilities.getWindowAncestor(this);
                if (top != null) {
                    top.showToast("Change saved");
                }
            } catch (CrobsException e) {
                JOptionPane.showMessageDialog(parent, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
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

    // --- Table Models & Renderers ---

    private class RoomsTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private final String[] cols = {"Room ID", "Room type", "Seats", "Equipment", ""};

        @Override
        public int getRowCount() {
            return roomsList.size();
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
            return roomsList.get(r);
        }
    }

    private class UsersTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private final String[] cols = {"User ID", "Name", "Role", ""};

        @Override
        public int getRowCount() {
            return usersList.size();
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
            return usersList.get(r);
        }
    }

    private static class BoldCellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int col) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            l.setFont(new Font("Segoe UI", Font.BOLD, 13));
            l.setForeground(GuiTheme.COLOR_TEXT);
            l.setBorder(new EmptyBorder(0, 22, 0, 10));
            if (value instanceof Room) {
                l.setText(((Room) value).getId());
            } else if (value instanceof User) {
                l.setText(((User) value).getId());
            }
            return l;
        }
    }

    private static class RoomTypeCellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int col) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            l.setFont(GuiTheme.FONT_BODY);
            l.setForeground(new Color(69, 80, 93));
            l.setBorder(new EmptyBorder(0, 10, 0, 10));
            if (value instanceof Room) {
                l.setText(formatRoomType(((Room) value).getType()));
            }
            return l;
        }
    }

    private static class PlainCellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int col) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            l.setFont(GuiTheme.FONT_BODY);
            l.setForeground(new Color(69, 80, 93));
            l.setBorder(new EmptyBorder(0, 10, 0, 10));
            if (value instanceof Room) {
                Room r = (Room) value;
                if (col == 2) {
                    l.setText(String.valueOf(r.getCapacity()));
                } else if (col == 3) {
                    l.setText(r.getFeatures().isEmpty() ? "—" : String.join(", ", r.getFeatures()));
                }
            } else if (value instanceof User) {
                User u = (User) value;
                if (col == 1) {
                    l.setText(u.getName().replace("_", " "));
                } else if (col == 2) {
                    l.setText(u.getRole().name());
                }
            }
            return l;
        }
    }

    private class RoomActionsCellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int col) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 13));
            p.setBackground(isSelected ? GuiTheme.COLOR_LIGHT : Color.WHITE);
            p.setBorder(new EmptyBorder(0, 0, 0, 16));

            JButton editBtn = GuiTheme.createTextButton("Edit");
            editBtn.setPreferredSize(new Dimension(54, 30));

            JButton delBtn = GuiTheme.createDangerTextButton("Delete");
            delBtn.setPreferredSize(new Dimension(68, 30));

            p.add(editBtn);
            p.add(delBtn);
            return p;
        }
    }

    private class RoomActionsCellEditor extends AbstractCellEditor implements TableCellEditor {
        private static final long serialVersionUID = 1L;
        private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 13));
        private final JButton editBtn = GuiTheme.createTextButton("Edit");
        private final JButton delBtn = GuiTheme.createDangerTextButton("Delete");
        private Room currentRoom;

        RoomActionsCellEditor() {
            editBtn.setPreferredSize(new Dimension(54, 30));
            delBtn.setPreferredSize(new Dimension(68, 30));

            editBtn.addActionListener(e -> {
                fireEditingStopped();
                if (currentRoom != null) {
                    onEditRoom(currentRoom);
                }
            });

            delBtn.addActionListener(e -> {
                fireEditingStopped();
                if (currentRoom != null) {
                    onDeleteRoom(currentRoom);
                }
            });

            panel.add(editBtn);
            panel.add(delBtn);
            panel.setBorder(new EmptyBorder(0, 0, 0, 16));
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int col) {
            panel.setBackground(GuiTheme.COLOR_LIGHT);
            if (value instanceof Room) {
                currentRoom = (Room) value;
            }
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return currentRoom;
        }
    }

    private class UserActionsCellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int col) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 13));
            p.setBackground(isSelected ? GuiTheme.COLOR_LIGHT : Color.WHITE);
            p.setBorder(new EmptyBorder(0, 0, 0, 16));

            if (value instanceof User) {
                User u = (User) value;
                if (controller.isSuperadmin() && !u.getId().equalsIgnoreCase("u1")) {
                    JButton roleBtn = GuiTheme.createTextButton("Change role");
                    roleBtn.setPreferredSize(new Dimension(100, 30));
                    p.add(roleBtn);
                }
                if (u.getRole() == UserRole.REGULAR) {
                    JButton remBtn = GuiTheme.createDangerTextButton("Remove");
                    remBtn.setPreferredSize(new Dimension(72, 30));
                    p.add(remBtn);
                }
            }
            return p;
        }
    }

    private class UserActionsCellEditor extends AbstractCellEditor implements TableCellEditor {
        private static final long serialVersionUID = 1L;
        private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 13));
        private final JButton roleBtn = GuiTheme.createTextButton("Change role");
        private final JButton remBtn = GuiTheme.createDangerTextButton("Remove");
        private User currentUser;

        UserActionsCellEditor() {
            roleBtn.setPreferredSize(new Dimension(100, 30));
            remBtn.setPreferredSize(new Dimension(72, 30));

            roleBtn.addActionListener(e -> {
                fireEditingStopped();
                if (currentUser != null) {
                    onChangeRole(currentUser);
                }
            });

            remBtn.addActionListener(e -> {
                fireEditingStopped();
                if (currentUser != null) {
                    onRemoveUser(currentUser);
                }
            });

            panel.add(roleBtn);
            panel.add(remBtn);
            panel.setBorder(new EmptyBorder(0, 0, 0, 16));
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int col) {
            panel.setBackground(GuiTheme.COLOR_LIGHT);
            panel.removeAll();
            if (value instanceof User) {
                currentUser = (User) value;
                if (controller.isSuperadmin() && !currentUser.getId().equalsIgnoreCase("u1")) {
                    panel.add(roleBtn);
                }
                if (currentUser.getRole() == UserRole.REGULAR) {
                    panel.add(remBtn);
                }
            }
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return currentUser;
        }
    }
}
