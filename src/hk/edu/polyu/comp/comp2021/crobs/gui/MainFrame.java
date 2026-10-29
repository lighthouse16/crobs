package hk.edu.polyu.comp.comp2021.crobs.gui;

import hk.edu.polyu.comp.comp2021.crobs.gui.dialog.LoginDialog;
import hk.edu.polyu.comp.comp2021.crobs.gui.view.FindRoomsPanel;
import hk.edu.polyu.comp.comp2021.crobs.gui.view.ManagePanel;
import hk.edu.polyu.comp.comp2021.crobs.gui.view.MyBookingsPanel;
import hk.edu.polyu.comp.comp2021.crobs.gui.view.RoomSchedulePanel;
import hk.edu.polyu.comp.comp2021.crobs.model.Booking;
import hk.edu.polyu.comp.comp2021.crobs.model.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.model.Room;
import hk.edu.polyu.comp.comp2021.crobs.model.User;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Main application window for CROBS desktop workspace (BON1).
 * 100% faithful to the official CROBS V3 specification, featuring full-width edge-aligned
 * layout, unified slide-out drawer on the right, instant user-switcher preview menu,
 * and bottom toast notifications.
 */
public class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    public static final String VIEW_FIND = "VIEW_FIND";
    public static final String VIEW_MY_BOOKINGS = "VIEW_MY_BOOKINGS";
    public static final String VIEW_SCHEDULE = "VIEW_SCHEDULE";
    public static final String VIEW_MANAGE = "VIEW_MANAGE";

    private final GuiController controller;

    private CardLayout cardLayout;
    private JPanel cardsPanel;
    private DrawerPanel drawerPanel;

    private JButton navFindBtn;
    private JButton navBookingsBtn;
    private JButton navManageBtn;

    private JLabel avatarCircle;
    private JLabel accountNameLabel;
    private JPanel accountBtn;

    private FindRoomsPanel findRoomsPanel;
    private MyBookingsPanel myBookingsPanel;
    private RoomSchedulePanel roomSchedulePanel;
    private ManagePanel managePanel;

    private String currentViewKey = VIEW_FIND;

    // Toast Notification Layer
    private JPanel toastPanel;
    private JLabel toastLabel;
    private Timer toastTimer;

    public MainFrame(GuiController controller) {
        super("CROBS — Campus Room Booking System");
        this.controller = controller;

        initComponents();
        controller.addListener(this::onSessionChanged);
        updateNavigationState();
    }

    private void initComponents() {
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                controller.quit();
            }
        });

        setSize(GuiTheme.WINDOW_WIDTH, GuiTheme.WINDOW_HEIGHT);
        setMinimumSize(new Dimension(GuiTheme.MIN_WINDOW_WIDTH, GuiTheme.MIN_WINDOW_HEIGHT));
        setLocationRelativeTo(null);
        getContentPane().setBackground(GuiTheme.COLOR_BG);
        setLayout(new BorderLayout());

        // Top Navigation Bar (Height: 68px)
        JPanel topBar = createTopNavBar();
        add(topBar, BorderLayout.NORTH);

        // Center Wrapper: Views in CENTER, Slide-out Drawer in EAST
        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setOpaque(false);

        // Views Container (CardLayout)
        cardLayout = new CardLayout();
        cardsPanel = new JPanel(cardLayout);
        cardsPanel.setBackground(GuiTheme.COLOR_BG);

        findRoomsPanel = new FindRoomsPanel(
                controller,
                this::openScheduleView,
                () -> navigateTo(VIEW_MY_BOOKINGS),
                () -> navigateTo(VIEW_MANAGE)
        );

        myBookingsPanel = new MyBookingsPanel(
                controller,
                () -> navigateTo(VIEW_FIND)
        );

        roomSchedulePanel = new RoomSchedulePanel(
                controller,
                () -> navigateTo(VIEW_FIND)
        );

        managePanel = new ManagePanel(controller);

        cardsPanel.add(findRoomsPanel, VIEW_FIND);
        cardsPanel.add(myBookingsPanel, VIEW_MY_BOOKINGS);
        cardsPanel.add(roomSchedulePanel, VIEW_SCHEDULE);
        cardsPanel.add(managePanel, VIEW_MANAGE);

        centerWrapper.add(cardsPanel, BorderLayout.CENTER);

        // Unified Slide-out Drawer (Screen B and all action panels)
        drawerPanel = new DrawerPanel(controller, this::showToast);
        centerWrapper.add(drawerPanel, BorderLayout.EAST);

        // Setup Floating Toast Notification Layer at bottom
        JPanel contentWithToast = new JPanel(new BorderLayout());
        contentWithToast.setOpaque(false);
        contentWithToast.add(centerWrapper, BorderLayout.CENTER);

        toastPanel = createToastPanel();
        contentWithToast.add(toastPanel, BorderLayout.SOUTH);

        add(contentWithToast, BorderLayout.CENTER);
    }

    private JPanel createTopNavBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(GuiTheme.COLOR_PANEL);
        bar.setPreferredSize(new Dimension(0, 68));
        Border bottomBorder = BorderFactory.createMatteBorder(0, 0, 1, 0, GuiTheme.COLOR_BORDER);
        Border padBorder = new EmptyBorder(0, 30, 0, 30);
        bar.setBorder(new CompoundBorder(bottomBorder, padBorder));

        // Left Section: Brand Mark + Navigation Tabs (Edge-aligned at 30px)
        JPanel leftGroup = new JPanel();
        leftGroup.setLayout(new BoxLayout(leftGroup, BoxLayout.X_AXIS));
        leftGroup.setOpaque(false);

        // Brand Icon (25x25 rounded box with 4 4x4 squares) + Name
        JPanel brandBox = new JPanel();
        brandBox.setLayout(new BoxLayout(brandBox, BoxLayout.X_AXIS));
        brandBox.setOpaque(false);

        JPanel brandIcon = new JPanel() {
            private static final long serialVersionUID = 1L;
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(GuiTheme.COLOR_BRAND);
                // Outer 25x25 rect, border width 2, radius 6
                g2.setStroke(new java.awt.BasicStroke(2f));
                g2.drawRoundRect(1, 1, 23, 23, 6, 6);
                // 4 squares (4x4) with 3px gap, 1px radius
                g2.fillRoundRect(6, 6, 4, 4, 1, 1);
                g2.fillRoundRect(13, 6, 4, 4, 1, 1);
                g2.fillRoundRect(6, 13, 4, 4, 1, 1);
                g2.fillRoundRect(13, 13, 4, 4, 1, 1);
                g2.dispose();
            }
        };
        brandIcon.setPreferredSize(new Dimension(25, 25));
        brandIcon.setMaximumSize(new Dimension(25, 25));
        brandIcon.setMinimumSize(new Dimension(25, 25));
        brandIcon.setOpaque(false);

        JLabel brandText = new JLabel("CROBS");
        brandText.setFont(new Font("Segoe UI", Font.BOLD, 17));
        brandText.setForeground(GuiTheme.COLOR_BRAND);

        brandBox.add(brandIcon);
        brandBox.add(Box.createHorizontalStrut(10));
        brandBox.add(brandText);
        leftGroup.add(brandBox);
        leftGroup.add(Box.createHorizontalStrut(38));

        // Navigation Tabs (67px height, 28px gap)
        JPanel tabsGroup = new JPanel();
        tabsGroup.setLayout(new BoxLayout(tabsGroup, BoxLayout.X_AXIS));
        tabsGroup.setOpaque(false);

        navFindBtn = createNavTab("Find rooms", () -> navigateTo(VIEW_FIND));
        navBookingsBtn = createNavTab("My bookings", () -> navigateTo(VIEW_MY_BOOKINGS));
        navManageBtn = createNavTab("Manage", () -> navigateTo(VIEW_MANAGE));

        tabsGroup.add(navFindBtn);
        tabsGroup.add(Box.createHorizontalStrut(28));
        tabsGroup.add(navBookingsBtn);
        tabsGroup.add(Box.createHorizontalStrut(28));
        tabsGroup.add(navManageBtn);
        leftGroup.add(tabsGroup);

        bar.add(leftGroup, BorderLayout.WEST);

        // Right Section: Account Toggle button (Edge-aligned at 30px)
        JPanel rightGroup = new JPanel();
        rightGroup.setLayout(new BoxLayout(rightGroup, BoxLayout.X_AXIS));
        rightGroup.setOpaque(false);

        avatarCircle = new JLabel("–", JLabel.CENTER) {
            private static final long serialVersionUID = 1L;
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(238, 240, 244));
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        avatarCircle.setPreferredSize(new Dimension(30, 30));
        avatarCircle.setMaximumSize(new Dimension(30, 30));
        avatarCircle.setMinimumSize(new Dimension(30, 30));
        avatarCircle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        avatarCircle.setForeground(new Color(73, 81, 90));

        accountNameLabel = new JLabel("Sign in");
        accountNameLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        accountNameLabel.setForeground(GuiTheme.COLOR_TEXT);

        // Vector-drawn crisp down chevron (eliminates Windows tofu box glyph)
        JComponent downArrow = new JComponent() {
            private static final long serialVersionUID = 1L;
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(118, 128, 140));
                g2.setStroke(new java.awt.BasicStroke(1.6f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                g2.drawLine(cx - 4, cy - 2, cx, cy + 2);
                g2.drawLine(cx, cy + 2, cx + 4, cy - 2);
                g2.dispose();
            }
        };
        downArrow.setPreferredSize(new Dimension(10, 10));
        downArrow.setMaximumSize(new Dimension(10, 10));
        downArrow.setMinimumSize(new Dimension(10, 10));

        accountBtn = new JPanel();
        accountBtn.setLayout(new BoxLayout(accountBtn, BoxLayout.X_AXIS));
        accountBtn.setOpaque(false);
        accountBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        accountBtn.add(avatarCircle);
        accountBtn.add(Box.createHorizontalStrut(10));
        accountBtn.add(accountNameLabel);
        accountBtn.add(Box.createHorizontalStrut(6));
        accountBtn.add(downArrow);

        MouseAdapter accountAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                showAccountPopup();
            }
        };
        accountBtn.addMouseListener(accountAdapter);
        avatarCircle.addMouseListener(accountAdapter);
        accountNameLabel.addMouseListener(accountAdapter);
        downArrow.addMouseListener(accountAdapter);

        rightGroup.add(accountBtn);
        bar.add(rightGroup, BorderLayout.EAST);

        return bar;
    }

    private JButton createNavTab(String text, Runnable action) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(new Color(103, 113, 126));
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width + 12, 67));
        btn.setMaximumSize(new Dimension(btn.getPreferredSize().width + 12, 67));
        btn.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        btn.addActionListener(e -> action.run());
        return btn;
    }

    private JPanel createToastPanel() {
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 12));
        wrapper.setOpaque(false);
        wrapper.setVisible(false);

        JPanel pill = new JPanel() {
            private static final long serialVersionUID = 1L;
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(40, 49, 59));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pill.setOpaque(false);
        pill.setBorder(new EmptyBorder(10, 18, 10, 18));

        toastLabel = new JLabel("");
        toastLabel.setFont(GuiTheme.FONT_BODY);
        toastLabel.setForeground(Color.WHITE);
        pill.add(toastLabel);

        wrapper.add(pill);
        return wrapper;
    }

    public void showToast(String message) {
        if (toastTimer != null && toastTimer.isRunning()) {
            toastTimer.stop();
        }
        toastLabel.setText(message);
        toastPanel.setVisible(true);
        revalidate();
        repaint();

        toastTimer = new Timer(3100, e -> {
            toastPanel.setVisible(false);
            revalidate();
            repaint();
        });
        toastTimer.setRepeats(false);
        toastTimer.start();
    }

    private void showAccountPopup() {
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(Color.WHITE);
        menu.setBorder(BorderFactory.createLineBorder(GuiTheme.COLOR_BORDER, 1));

        // Header label: Switch user · preview
        JLabel headerLabel = new JLabel("Switch user · preview");
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        headerLabel.setForeground(new Color(123, 131, 140));
        headerLabel.setBorder(new EmptyBorder(8, 12, 6, 12));
        menu.add(headerLabel);

        // User list
        List<User> users = controller.getAllKnownUsers();
        for (User u : users) {
            String html = "<html><table width='190' style='border:0; margin:0; padding:0;'>"
                    + "<tr>"
                    + "<td align='left' style='font-family:\"Segoe UI\"; font-size:12px; font-weight:bold; color:#20252c;'>"
                    + u.getName().replace("_", " ") + "</td>"
                    + "<td align='right' style='font-family:Consolas; font-size:11px; color:#9aa1aa;'>"
                    + u.getId() + "</td>"
                    + "</tr></table></html>";
            JMenuItem userItem = new JMenuItem(html);
            userItem.setBackground(Color.WHITE);
            userItem.setPreferredSize(new Dimension(236, 34));
            userItem.setBorder(new EmptyBorder(4, 10, 4, 10));
            userItem.setCursor(new Cursor(Cursor.HAND_CURSOR));

            userItem.addActionListener(e -> {
                try {
                    controller.login(u.getId());
                    navigateTo(VIEW_FIND);
                    showToast("Switched to " + u.getName().replace("_", " ") + " (" + u.getId() + ")");
                } catch (Exception ignored) {
                }
            });
            menu.add(userItem);
        }

        menu.addSeparator();

        JMenuItem signoutItem = new JMenuItem(controller.isLoggedIn() ? "Sign out" : "Sign in");
        signoutItem.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        signoutItem.setForeground(controller.isLoggedIn() ? GuiTheme.COLOR_DANGER : GuiTheme.COLOR_BLUE);
        signoutItem.setBackground(Color.WHITE);
        signoutItem.setPreferredSize(new Dimension(236, 34));
        signoutItem.setBorder(new EmptyBorder(4, 10, 4, 10));
        signoutItem.setCursor(new Cursor(Cursor.HAND_CURSOR));
        signoutItem.addActionListener(e -> {
            if (controller.isLoggedIn()) {
                try {
                    controller.logout();
                    navigateTo(VIEW_FIND);
                    showToast("Signed out");
                } catch (CrobsException ignored) {
                }
            } else {
                LoginDialog.showLogin(this, controller);
                navigateTo(VIEW_FIND);
            }
        });
        menu.add(signoutItem);

        int xOffset = accountBtn.getWidth() - 236;
        menu.show(accountBtn, xOffset, accountBtn.getHeight() + 8);
    }

    public void navigateTo(String viewKey) {
        closeDrawer();
        this.currentViewKey = viewKey;
        cardLayout.show(cardsPanel, viewKey);
        updateNavigationState();

        if (VIEW_FIND.equals(viewKey)) {
            findRoomsPanel.executeSearch(false);
        } else if (VIEW_MY_BOOKINGS.equals(viewKey)) {
            myBookingsPanel.refreshBookings(false);
        } else if (VIEW_MANAGE.equals(viewKey)) {
            managePanel.refreshData();
        }
    }

    public void openScheduleView(String roomId, LocalDate date) {
        closeDrawer();
        this.currentViewKey = VIEW_SCHEDULE;
        roomSchedulePanel.loadScheduleForRoom(roomId, date);
        cardLayout.show(cardsPanel, VIEW_SCHEDULE);
        updateNavigationState();
    }

    public void updateNavigationState() {
        boolean isFind = VIEW_FIND.equals(currentViewKey) || VIEW_SCHEDULE.equals(currentViewKey);
        boolean isBookings = VIEW_MY_BOOKINGS.equals(currentViewKey);
        boolean isManage = VIEW_MANAGE.equals(currentViewKey);

        styleTab(navFindBtn, isFind);
        styleTab(navBookingsBtn, isBookings);
        styleTab(navManageBtn, isManage);

        // Hide Manage tab if not admin
        navManageBtn.setVisible(controller.isAdmin());

        User current = controller.getCurrentUser();
        if (current != null) {
            String initial = current.getId().toUpperCase();
            avatarCircle.setText(initial.length() > 2 ? initial.substring(0, 2) : initial);
            accountNameLabel.setText(current.getName().replace("_", " "));
        } else {
            avatarCircle.setText("–");
            accountNameLabel.setText("Sign in");
        }
    }

    private void styleTab(JButton tab, boolean active) {
        tab.setForeground(active ? GuiTheme.COLOR_TEXT : new Color(103, 113, 126));
        Border bottomLine = BorderFactory.createMatteBorder(0, 0, 2, 0,
                active ? GuiTheme.COLOR_ACTIVE_TAB : new Color(0, 0, 0, 0));
        Border padding = BorderFactory.createEmptyBorder(0, 2, 0, 2);
        tab.setBorder(new CompoundBorder(bottomLine, padding));
    }

    private void onSessionChanged() {
        updateNavigationState();
    }

    // --- Public Drawer Openers ---

    public void openBookFromSearch(Room room, LocalDate date, LocalTime start, LocalTime end) {
        if (!controller.isLoggedIn()) {
            LoginDialog.showLogin(this, controller);
            if (!controller.isLoggedIn()) return;
        }
        drawerPanel.openBookFromSearch(room, date, start, end);
    }

    public void openBookFromWeek(Room room, LocalDate date, LocalTime start, LocalTime end) {
        if (!controller.isLoggedIn()) {
            LoginDialog.showLogin(this, controller);
            if (!controller.isLoggedIn()) return;
        }
        drawerPanel.openBookFromWeek(room, date, start, end);
    }

    public void openEditBooking(Booking booking) {
        drawerPanel.openEditBooking(booking);
    }

    public void openAddRoom() {
        drawerPanel.openAddRoom();
    }

    public void openEditRoom(Room room) {
        drawerPanel.openEditRoom(room);
    }

    public void openAddUser() {
        drawerPanel.openAddUser();
    }

    public void openChangeRole(User user) {
        drawerPanel.openChangeRole(user);
    }

    public void closeDrawer() {
        if (drawerPanel != null) {
            drawerPanel.closeDrawer();
        }
    }

    public DrawerPanel getDrawerPanel() {
        return drawerPanel;
    }
}
