package hk.edu.polyu.comp.comp2021.crobs.gui.view;

import hk.edu.polyu.comp.comp2021.crobs.gui.GuiController;
import hk.edu.polyu.comp.comp2021.crobs.gui.GuiTheme;
import hk.edu.polyu.comp.comp2021.crobs.gui.MainFrame;
import hk.edu.polyu.comp.comp2021.crobs.gui.dialog.LoginDialog;
import hk.edu.polyu.comp.comp2021.crobs.model.Booking;
import hk.edu.polyu.comp.comp2021.crobs.model.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.model.Room;
import hk.edu.polyu.comp.comp2021.crobs.model.RoomType;
import hk.edu.polyu.comp.comp2021.crobs.model.User;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Screen D: Weekly Room Schedule displaying 7 consecutive days starting from Monday of that week.
 * Enforces strict user privacy rules per specification (REQ14) and triggers the V3 slide-out drawer.
 */
public class RoomSchedulePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter DOW_FMT = DateTimeFormatter.ofPattern("EEE");
    private static final DateTimeFormatter DATE_SHORT_FMT = DateTimeFormatter.ofPattern("d MMM");
    private static final DateTimeFormatter FULL_DATE_FMT = DateTimeFormatter.ofPattern("d MMM yyyy");

    private final GuiController controller;
    private final Runnable backNavCallback;

    private String currentRoomId = "r1";
    private LocalDate currentWeekStart = LocalDate.now().with(DayOfWeek.MONDAY);

    private JLabel headingRoomLabel;
    private JLabel headingMetaLabel;
    private JLabel weekDateRangeLabel;
    private JPanel daysGridPanel;

    public RoomSchedulePanel(GuiController controller, Runnable backNavCallback) {
        this.controller = controller;
        this.backNavCallback = backNavCallback;

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

        // Back button: .back { border:0; color:#596678; font-size:13px; margin:-14px 0 17px; }
        JButton backBtn = new JButton("← Back to results");
        backBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        backBtn.setForeground(new Color(89, 102, 120));
        backBtn.setContentAreaFilled(false);
        backBtn.setBorderPainted(false);
        backBtn.setFocusPainted(false);
        backBtn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        backBtn.setBorder(new EmptyBorder(0, 0, 14, 0));
        backBtn.setAlignmentX(LEFT_ALIGNMENT);
        backBtn.addActionListener(e -> {
            if (backNavCallback != null) {
                backNavCallback.run();
            }
        });
        mainContent.add(backBtn);

        // Heading Area
        headingRoomLabel = new JLabel("Room Schedule");
        headingRoomLabel.setFont(GuiTheme.FONT_H1);
        headingRoomLabel.setForeground(GuiTheme.COLOR_TEXT);
        headingRoomLabel.setAlignmentX(LEFT_ALIGNMENT);

        headingMetaLabel = new JLabel("");
        headingMetaLabel.setFont(GuiTheme.FONT_BODY);
        headingMetaLabel.setForeground(GuiTheme.COLOR_MUTED);
        headingMetaLabel.setAlignmentX(LEFT_ALIGNMENT);
        headingMetaLabel.setBorder(new EmptyBorder(4, 0, 19, 0));

        mainContent.add(headingRoomLabel);
        mainContent.add(headingMetaLabel);

        // Week Navigation Header Card
        JPanel weekNavCard = createWeekNavCard();
        weekNavCard.setAlignmentX(LEFT_ALIGNMENT);
        mainContent.add(weekNavCard);
        mainContent.add(Box.createVerticalStrut(15));

        // 7-day grid container
        daysGridPanel = new JPanel(new GridLayout(1, 7, 8, 0));
        daysGridPanel.setOpaque(false);
        daysGridPanel.setAlignmentX(LEFT_ALIGNMENT);

        JScrollPane scroll = new JScrollPane(daysGridPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setAlignmentX(LEFT_ALIGNMENT);

        mainContent.add(scroll);
        mainContent.add(Box.createVerticalStrut(16));

        // Bottom Note
        JLabel noteLabel = new JLabel("Shows reservations for seven days. Booking times are checked again when you confirm.");
        noteLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        noteLabel.setForeground(new Color(135, 145, 157));
        noteLabel.setAlignmentX(LEFT_ALIGNMENT);
        mainContent.add(noteLabel);

        add(mainContent, BorderLayout.CENTER);
    }

    private JPanel createWeekNavCard() {
        JPanel card = GuiTheme.createCardPanel();
        card.setLayout(new BorderLayout());
        card.setBorder(new CompoundBorder(
                new LineBorder(GuiTheme.COLOR_BORDER, 1),
                new EmptyBorder(16, 22, 16, 22)
        ));
        card.setMaximumSize(new Dimension(Short.MAX_VALUE, 65));

        JPanel textGroup = new JPanel();
        textGroup.setLayout(new BoxLayout(textGroup, BoxLayout.Y_AXIS));
        textGroup.setOpaque(false);

        JLabel title = new JLabel("Weekly schedule");
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(GuiTheme.COLOR_TEXT);

        weekDateRangeLabel = new JLabel("");
        weekDateRangeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        weekDateRangeLabel.setForeground(new Color(136, 145, 158));

        textGroup.add(title);
        textGroup.add(Box.createVerticalStrut(3));
        textGroup.add(weekDateRangeLabel);
        card.add(textGroup, BorderLayout.WEST);

        // Prev / Next Week Buttons
        JPanel navButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        navButtons.setOpaque(false);

        JButton prevBtn = GuiTheme.createSmallSecondaryButton("Previous");
        prevBtn.addActionListener(e -> {
            currentWeekStart = currentWeekStart.minusDays(7);
            reloadSchedule(true);
        });

        JButton nextBtn = GuiTheme.createSmallSecondaryButton("Next");
        nextBtn.addActionListener(e -> {
            currentWeekStart = currentWeekStart.plusDays(7);
            reloadSchedule(true);
        });

        navButtons.add(prevBtn);
        navButtons.add(nextBtn);
        card.add(navButtons, BorderLayout.EAST);

        return card;
    }

    public void loadScheduleForRoom(String roomId, LocalDate date) {
        this.currentRoomId = roomId;
        // Align to Monday
        this.currentWeekStart = date.with(DayOfWeek.MONDAY);
        reloadSchedule(false);
    }

    private void reloadSchedule(boolean logCommand) {
        Room room = controller.getRoomQuiet(currentRoomId);
        if (room == null) {
            headingRoomLabel.setText("Room not found");
            headingMetaLabel.setText("");
            daysGridPanel.removeAll();
            daysGridPanel.revalidate();
            daysGridPanel.repaint();
            return;
        }

        String typeName = formatRoomType(room.getType());
        headingRoomLabel.setText("<html>" + typeName + " <span style='font-weight: normal; color: #576370;'>" + room.getId() + "</span></html>");

        String featStr = room.getFeatures().isEmpty() ? "No features" : String.join(" · ", room.getFeatures());
        headingMetaLabel.setText(room.getCapacity() + " seats · " + featStr);

        LocalDate weekEnd = currentWeekStart.plusDays(6);
        weekDateRangeLabel.setText(currentWeekStart.format(FULL_DATE_FMT) + " – " + weekEnd.format(FULL_DATE_FMT));

        // Fetch bookings for the week
        List<Booking> allBookings;
        if (logCommand) {
            try {
                allBookings = controller.schedule(currentRoomId, currentWeekStart);
            } catch (CrobsException ex) {
                allBookings = java.util.Collections.emptyList();
            }
        } else {
            allBookings = controller.scheduleQuiet(currentRoomId, currentWeekStart);
        }

        daysGridPanel.removeAll();

        User currentUser = controller.getCurrentUser();
        boolean isAdmin = controller.isAdmin();

        for (int i = 0; i < 7; i++) {
            LocalDate d = currentWeekStart.plusDays(i);
            daysGridPanel.add(createDayColumnCard(d, allBookings, currentUser, isAdmin));
        }

        daysGridPanel.revalidate();
        daysGridPanel.repaint();
    }

    private JPanel createDayColumnCard(LocalDate date, List<Booking> allBookings, User currentUser, boolean isAdmin) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new LineBorder(GuiTheme.COLOR_BORDER, 1),
                new EmptyBorder(15, 12, 15, 12)
        ));
        card.setPreferredSize(new Dimension(145, 225));
        card.setMinimumSize(new Dimension(130, 225));

        // Day Head: Short day name + Big Date
        JPanel head = new JPanel();
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.setOpaque(false);

        JLabel dowLabel = new JLabel(date.format(DOW_FMT));
        dowLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        dowLabel.setForeground(new Color(139, 147, 158));

        JLabel dateLabel = new JLabel(date.format(DATE_SHORT_FMT));
        dateLabel.setFont(new Font("Segoe UI", Font.BOLD, 19));
        dateLabel.setForeground(new Color(41, 52, 62));

        head.add(dowLabel);
        head.add(Box.createVerticalStrut(2));
        head.add(dateLabel);
        card.add(head, BorderLayout.NORTH);

        // Events List Center
        JPanel eventsPanel = new JPanel();
        eventsPanel.setLayout(new BoxLayout(eventsPanel, BoxLayout.Y_AXIS));
        eventsPanel.setOpaque(false);
        eventsPanel.setBorder(new EmptyBorder(14, 0, 14, 0));

        List<Booking> dayBookings = new ArrayList<>();
        for (Booking b : allBookings) {
            if (b.getDate().equals(date)) {
                dayBookings.add(b);
            }
        }

        if (dayBookings.isEmpty()) {
            JLabel freeLabel = new JLabel("No bookings");
            freeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            freeLabel.setForeground(new Color(173, 181, 190));
            eventsPanel.add(freeLabel);
        } else {
            for (Booking b : dayBookings) {
                JPanel eventP = new JPanel();
                eventP.setLayout(new BoxLayout(eventP, BoxLayout.Y_AXIS));
                eventP.setBackground(new Color(242, 244, 247));
                Border leftBar = BorderFactory.createMatteBorder(0, 2, 0, 0, new Color(187, 196, 207));
                Border pad = new EmptyBorder(8, 8, 8, 8);
                eventP.setBorder(new CompoundBorder(leftBar, pad));
                eventP.setMaximumSize(new Dimension(Short.MAX_VALUE, 58));

                JLabel timeLabel = new JLabel(b.getStartTime() + "–" + b.getEndTime());
                timeLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
                timeLabel.setForeground(new Color(77, 89, 103));

                // REQ14 Privacy check
                boolean isOwner = (currentUser != null && b.getUserId().equalsIgnoreCase(currentUser.getId()));
                String displayUser = (isAdmin || isOwner) ? b.getUserId() : "(hidden)";
                String descText = (isAdmin || isOwner)
                        ? (b.getPurpose().isEmpty() ? displayUser : (b.getPurpose().replace("_", " ") + " · " + displayUser))
                        : "Reserved";

                JLabel descLabel = new JLabel(descText);
                descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                descLabel.setForeground(new Color(137, 146, 156));

                eventP.add(timeLabel);
                eventP.add(Box.createVerticalStrut(3));
                eventP.add(descLabel);

                eventsPanel.add(eventP);
                eventsPanel.add(Box.createVerticalStrut(6));
            }
        }

        card.add(eventsPanel, BorderLayout.CENTER);

        // Book button at bottom of each day: 100% width, height 30px
        JButton bookBtn = GuiTheme.createSmallSecondaryButton("Book");
        bookBtn.setPreferredSize(new Dimension(0, 30));
        bookBtn.addActionListener(e -> onDayBookClicked(date));
        card.add(bookBtn, BorderLayout.SOUTH);

        return card;
    }

    private void onDayBookClicked(LocalDate date) {
        Room r = controller.getRoomQuiet(currentRoomId);
        if (r == null) return;

        MainFrame top = (MainFrame) SwingUtilities.getWindowAncestor(this);
        if (top != null) {
            top.openBookFromWeek(r, date, LocalTime.of(14, 0), LocalTime.of(15, 0));
        }
    }

    private void onDomainChanged() {
        reloadSchedule(false);
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
