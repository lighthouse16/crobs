package hk.edu.polyu.comp.comp2021.crobs.gui.dialog;

import hk.edu.polyu.comp.comp2021.crobs.gui.GuiController;
import hk.edu.polyu.comp.comp2021.crobs.gui.GuiTheme;
import hk.edu.polyu.comp.comp2021.crobs.model.Booking;
import hk.edu.polyu.comp.comp2021.crobs.model.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.model.Room;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Modal dialog for modifying an existing booking (REQ11).
 */
public class ModifyBookingDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final GuiController controller;
    private final Booking booking;
    private boolean saved = false;

    private JComboBox<String> roomCombo;
    private JTextField dateField;
    private JTextField startField;
    private JTextField endField;
    private JTextArea purposeArea;
    private JLabel errorLabel;

    /**
     * Constructs a ModifyBookingDialog.
     *
     * @param parent     parent JFrame.
     * @param controller GUI controller facade.
     * @param booking    the Booking to modify.
     */
    public ModifyBookingDialog(JFrame parent, GuiController controller, Booking booking) {
        super(parent, "Edit Booking · " + booking.getId(), true);
        this.controller = controller;
        this.booking = booking;

        initComponents();
    }

    private void initComponents() {
        setResizable(false);
        getContentPane().setBackground(GuiTheme.COLOR_PANEL);
        setLayout(new BorderLayout());

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(GuiTheme.COLOR_PANEL);
        card.setBorder(new EmptyBorder(28, 28, 20, 28));

        JLabel titleLabel = new JLabel("Edit booking · " + booking.getId());
        titleLabel.setFont(GuiTheme.FONT_H2);
        titleLabel.setForeground(GuiTheme.COLOR_TEXT);
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Update the reservation details below.");
        subtitleLabel.setFont(GuiTheme.FONT_BODY);
        subtitleLabel.setForeground(GuiTheme.COLOR_MUTED);
        subtitleLabel.setAlignmentX(LEFT_ALIGNMENT);
        subtitleLabel.setBorder(new EmptyBorder(4, 0, 18, 0));

        // Room selection
        JLabel roomLabel = new JLabel("Room");
        roomLabel.setFont(GuiTheme.FONT_SMALL_BOLD);
        roomLabel.setForeground(GuiTheme.COLOR_MUTED);
        roomLabel.setAlignmentX(LEFT_ALIGNMENT);

        roomCombo = new JComboBox<>();
        List<Room> allRooms = controller.getAllRoomsQuiet();
        for (Room r : allRooms) {
            roomCombo.addItem(r.getId() + " (" + r.getType() + ", " + r.getCapacity() + " seats)");
            if (r.getId().equalsIgnoreCase(booking.getRoomId())) {
                roomCombo.setSelectedIndex(roomCombo.getItemCount() - 1);
            }
        }
        roomCombo.setMaximumSize(new Dimension(380, 38));
        roomCombo.setAlignmentX(LEFT_ALIGNMENT);

        // Date field
        JLabel dateLabel = new JLabel("Date (YYYY-MM-DD)");
        dateLabel.setFont(GuiTheme.FONT_SMALL_BOLD);
        dateLabel.setForeground(GuiTheme.COLOR_MUTED);
        dateLabel.setAlignmentX(LEFT_ALIGNMENT);

        dateField = GuiTheme.createTextField(14);
        dateField.setText(booking.getDate().toString());
        dateField.setMaximumSize(new Dimension(380, 38));
        dateField.setAlignmentX(LEFT_ALIGNMENT);

        // Timeslot
        JPanel timeGrid = new JPanel(new GridLayout(1, 2, 12, 0));
        timeGrid.setOpaque(false);
        timeGrid.setMaximumSize(new Dimension(380, 60));
        timeGrid.setAlignmentX(LEFT_ALIGNMENT);

        JPanel startPanel = new JPanel();
        startPanel.setLayout(new BoxLayout(startPanel, BoxLayout.Y_AXIS));
        startPanel.setOpaque(false);
        JLabel startLabel = new JLabel("Start (HH:MM)");
        startLabel.setFont(GuiTheme.FONT_SMALL_BOLD);
        startLabel.setForeground(GuiTheme.COLOR_MUTED);
        startField = GuiTheme.createTextField(6);
        startField.setText(booking.getStartTime().toString());
        startPanel.add(startLabel);
        startPanel.add(Box.createVerticalStrut(4));
        startPanel.add(startField);

        JPanel endPanel = new JPanel();
        endPanel.setLayout(new BoxLayout(endPanel, BoxLayout.Y_AXIS));
        endPanel.setOpaque(false);
        JLabel endLabel = new JLabel("End (HH:MM)");
        endLabel.setFont(GuiTheme.FONT_SMALL_BOLD);
        endLabel.setForeground(GuiTheme.COLOR_MUTED);
        endField = GuiTheme.createTextField(6);
        endField.setText(booking.getEndTime().toString());
        endPanel.add(endLabel);
        endPanel.add(Box.createVerticalStrut(4));
        endPanel.add(endField);

        timeGrid.add(startPanel);
        timeGrid.add(endPanel);

        // Purpose
        JLabel purposeLabel = new JLabel("Purpose · optional");
        purposeLabel.setFont(GuiTheme.FONT_SMALL_BOLD);
        purposeLabel.setForeground(GuiTheme.COLOR_MUTED);
        purposeLabel.setAlignmentX(LEFT_ALIGNMENT);

        purposeArea = new JTextArea(3, 20);
        purposeArea.setFont(GuiTheme.FONT_BODY);
        purposeArea.setText(booking.getPurpose());
        purposeArea.setLineWrap(true);
        purposeArea.setWrapStyleWord(true);
        JScrollPane purposeScroll = new JScrollPane(purposeArea);
        purposeScroll.setBorder(BorderFactory.createLineBorder(GuiTheme.COLOR_BORDER, 1));
        purposeScroll.setMaximumSize(new Dimension(380, 80));
        purposeScroll.setAlignmentX(LEFT_ALIGNMENT);

        errorLabel = new JLabel(" ");
        errorLabel.setFont(GuiTheme.FONT_SMALL);
        errorLabel.setForeground(GuiTheme.COLOR_DANGER);
        errorLabel.setAlignmentX(LEFT_ALIGNMENT);
        errorLabel.setBorder(new EmptyBorder(6, 0, 12, 0));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.setMaximumSize(new Dimension(380, 42));
        actions.setAlignmentX(LEFT_ALIGNMENT);

        JButton cancelBtn = GuiTheme.createSecondaryButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());

        JButton saveBtn = GuiTheme.createPrimaryButton("Save changes");
        saveBtn.addActionListener(e -> attemptSave());

        actions.add(cancelBtn);
        actions.add(saveBtn);

        card.add(titleLabel);
        card.add(subtitleLabel);
        card.add(roomLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(roomCombo);
        card.add(Box.createVerticalStrut(10));
        card.add(dateLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(dateField);
        card.add(Box.createVerticalStrut(10));
        card.add(timeGrid);
        card.add(Box.createVerticalStrut(10));
        card.add(purposeLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(purposeScroll);
        card.add(errorLabel);
        card.add(actions);

        add(card, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getParent());
    }

    private void attemptSave() {
        String selectedRoomStr = (String) roomCombo.getSelectedItem();
        if (selectedRoomStr == null) {
            errorLabel.setText("Please select a room.");
            return;
        }
        String targetRoomId = selectedRoomStr.split(" ")[0].trim();

        LocalDate date;
        try {
            date = LocalDate.parse(dateField.getText().trim());
        } catch (DateTimeParseException e) {
            errorLabel.setText("Invalid date format (YYYY-MM-DD).");
            return;
        }

        LocalTime start;
        LocalTime end;
        try {
            start = LocalTime.parse(startField.getText().trim());
            end = LocalTime.parse(endField.getText().trim());
        } catch (DateTimeParseException e) {
            errorLabel.setText("Invalid time format (HH:MM).");
            return;
        }

        if (!start.isBefore(end)) {
            errorLabel.setText("Start time must be strictly before end time.");
            return;
        }

        try {
            controller.modifyBooking(booking.getId(), targetRoomId, date, start, end, purposeArea.getText());
            saved = true;
            dispose();
        } catch (CrobsException e) {
            errorLabel.setText(e.getMessage());
        } catch (Exception e) {
            errorLabel.setText("Error: " + e.getMessage());
        }
    }

    /**
     * Returns true if booking was successfully modified.
     *
     * @return true if saved.
     */
    public boolean isSaved() {
        return saved;
    }
}
