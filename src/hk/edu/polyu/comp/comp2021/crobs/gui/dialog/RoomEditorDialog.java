package hk.edu.polyu.comp.comp2021.crobs.gui.dialog;

import hk.edu.polyu.comp.comp2021.crobs.gui.GuiController;
import hk.edu.polyu.comp.comp2021.crobs.gui.GuiTheme;
import hk.edu.polyu.comp.comp2021.crobs.model.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.model.Room;
import hk.edu.polyu.comp.comp2021.crobs.model.RoomType;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Modal dialog for creating (REQ7) or modifying (REQ8) a room.
 */
public class RoomEditorDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final GuiController controller;
    private final String roomId; // null if adding new room
    private boolean saved = false;

    private JComboBox<RoomType> typeCombo;
    private JTextField capacityField;
    private JTextField featuresField;
    private JLabel errorLabel;

    /**
     * Constructs the room editor dialog.
     *
     * @param parent     parent JFrame.
     * @param controller GUI controller facade.
     * @param room       existing Room to edit, or null to create new room.
     */
    public RoomEditorDialog(JFrame parent, GuiController controller, Room room) {
        super(parent, (room == null) ? "Add Room" : "Edit Room · " + room.getId(), true);
        this.controller = controller;
        this.roomId = (room != null) ? room.getId() : null;

        initComponents();
        if (roomId != null) {
            loadExistingRoomData();
        }
    }

    private void initComponents() {
        setResizable(false);
        getContentPane().setBackground(GuiTheme.COLOR_PANEL);
        setLayout(new BorderLayout());

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(GuiTheme.COLOR_PANEL);
        card.setBorder(new EmptyBorder(28, 28, 20, 28));

        JLabel titleLabel = new JLabel((roomId == null) ? "Add a new room" : "Edit room · " + roomId);
        titleLabel.setFont(GuiTheme.FONT_H2);
        titleLabel.setForeground(GuiTheme.COLOR_TEXT);
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);

        JLabel subtitleLabel = new JLabel((roomId == null)
                ? "Configure room type, seating capacity, and equipment."
                : "Modify room attributes. Note: Spec REQ8 applies one field per save.");
        subtitleLabel.setFont(GuiTheme.FONT_BODY);
        subtitleLabel.setForeground(GuiTheme.COLOR_MUTED);
        subtitleLabel.setAlignmentX(LEFT_ALIGNMENT);
        subtitleLabel.setBorder(new EmptyBorder(4, 0, 18, 0));

        // Type
        JLabel typeLabel = new JLabel("Room type");
        typeLabel.setFont(GuiTheme.FONT_SMALL_BOLD);
        typeLabel.setForeground(GuiTheme.COLOR_MUTED);
        typeLabel.setAlignmentX(LEFT_ALIGNMENT);

        typeCombo = new JComboBox<>(RoomType.values());
        typeCombo.setMaximumSize(new Dimension(360, 38));
        typeCombo.setAlignmentX(LEFT_ALIGNMENT);

        // Capacity
        JLabel capLabel = new JLabel("Capacity (seats)");
        capLabel.setFont(GuiTheme.FONT_SMALL_BOLD);
        capLabel.setForeground(GuiTheme.COLOR_MUTED);
        capLabel.setAlignmentX(LEFT_ALIGNMENT);

        capacityField = GuiTheme.createTextField(10);
        capacityField.setText("30");
        capacityField.setMaximumSize(new Dimension(360, 38));
        capacityField.setAlignmentX(LEFT_ALIGNMENT);

        // Features
        JLabel featLabel = new JLabel("Features · comma-separated");
        featLabel.setFont(GuiTheme.FONT_SMALL_BOLD);
        featLabel.setForeground(GuiTheme.COLOR_MUTED);
        featLabel.setAlignmentX(LEFT_ALIGNMENT);

        featuresField = GuiTheme.createTextField(20);
        featuresField.setText("wifi,projector");
        featuresField.setMaximumSize(new Dimension(360, 38));
        featuresField.setAlignmentX(LEFT_ALIGNMENT);

        errorLabel = new JLabel(" ");
        errorLabel.setFont(GuiTheme.FONT_SMALL);
        errorLabel.setForeground(GuiTheme.COLOR_DANGER);
        errorLabel.setAlignmentX(LEFT_ALIGNMENT);
        errorLabel.setBorder(new EmptyBorder(6, 0, 12, 0));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.setMaximumSize(new Dimension(360, 42));
        actions.setAlignmentX(LEFT_ALIGNMENT);

        JButton cancelBtn = GuiTheme.createSecondaryButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());

        JButton saveBtn = GuiTheme.createPrimaryButton((roomId == null) ? "Add room" : "Save changes");
        saveBtn.addActionListener(e -> attemptSave());

        actions.add(cancelBtn);
        actions.add(saveBtn);

        card.add(titleLabel);
        card.add(subtitleLabel);
        card.add(typeLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(typeCombo);
        card.add(Box.createVerticalStrut(10));
        card.add(capLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(capacityField);
        card.add(Box.createVerticalStrut(10));
        card.add(featLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(featuresField);
        card.add(errorLabel);
        card.add(actions);

        add(card, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getParent());
    }

    private void loadExistingRoomData() {
        Room r = controller.getRoomQuiet(roomId);
        if (r != null) {
            typeCombo.setSelectedItem(r.getType());
            capacityField.setText(String.valueOf(r.getCapacity()));
            featuresField.setText(String.join(",", r.getFeatures()));
        }
    }

    private void attemptSave() {
        RoomType type = (RoomType) typeCombo.getSelectedItem();
        int cap;
        try {
            cap = Integer.parseInt(capacityField.getText().trim());
            if (cap <= 0) {
                errorLabel.setText("Capacity must be greater than 0.");
                return;
            }
        } catch (NumberFormatException e) {
            errorLabel.setText("Capacity must be a positive integer.");
            return;
        }

        String featText = featuresField.getText().trim();
        Set<String> feats = new HashSet<>();
        if (!featText.isEmpty()) {
            for (String f : featText.split(",")) {
                if (!f.trim().isEmpty()) {
                    feats.add(f.trim().replace(' ', '_'));
                }
            }
        }

        try {
            if (roomId == null) {
                // Add room REQ7
                controller.addRoom(type, cap, feats);
            } else {
                // Modify room REQ8 - modify fields sequentially
                Room current = controller.getRoomQuiet(roomId);
                if (current != null) {
                    if (current.getType() != type) {
                        controller.modifyRoom(roomId, "type", type.name());
                    }
                    if (current.getCapacity() != cap) {
                        controller.modifyRoom(roomId, "capacity", String.valueOf(cap));
                    }
                    String newFeatStr = String.join(",", feats);
                    String oldFeatStr = String.join(",", current.getFeatures());
                    if (!newFeatStr.equalsIgnoreCase(oldFeatStr)) {
                        controller.modifyRoom(roomId, "features", newFeatStr);
                    }
                }
            }
            saved = true;
            dispose();
        } catch (CrobsException e) {
            errorLabel.setText(e.getMessage());
        } catch (Exception e) {
            errorLabel.setText("Error: " + e.getMessage());
        }
    }

    /**
     * Checks if changes were saved.
     *
     * @return true if room was added or modified.
     */
    public boolean isSaved() {
        return saved;
    }
}
