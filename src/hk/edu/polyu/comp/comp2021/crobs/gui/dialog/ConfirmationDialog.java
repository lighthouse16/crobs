package hk.edu.polyu.comp.comp2021.crobs.gui.dialog;

import hk.edu.polyu.comp.comp2021.crobs.gui.GuiTheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

/**
 * Clean confirmation modal dialog matching the CROBS V3 specification.
 */
public class ConfirmationDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private boolean confirmed = false;

    /**
     * Constructs a confirmation dialog.
     *
     * @param parent       parent JFrame.
     * @param title        dialog title.
     * @param message      explanatory confirmation message.
     * @param actionButton destructive action button text (e.g. "Delete room", "Cancel booking").
     */
    public ConfirmationDialog(JFrame parent, String title, String message, String actionButton) {
        super(parent, title, true);
        initComponents(title, message, actionButton);
    }

    private void initComponents(String title, String message, String actionButton) {
        setResizable(false);
        getContentPane().setBackground(GuiTheme.COLOR_PANEL);
        setLayout(new BorderLayout());

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(GuiTheme.COLOR_PANEL);
        card.setBorder(new EmptyBorder(28, 28, 20, 28));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(GuiTheme.FONT_H2);
        titleLabel.setForeground(GuiTheme.COLOR_TEXT);
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);

        JLabel msgLabel = new JLabel("<html><body style='width: 340px;'>" + message + "</body></html>");
        msgLabel.setFont(GuiTheme.FONT_BODY);
        msgLabel.setForeground(GuiTheme.COLOR_MUTED);
        msgLabel.setAlignmentX(LEFT_ALIGNMENT);
        msgLabel.setBorder(new EmptyBorder(12, 0, 24, 0));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.setAlignmentX(LEFT_ALIGNMENT);

        JButton keepBtn = GuiTheme.createSecondaryButton("Keep");
        keepBtn.addActionListener(e -> {
            confirmed = false;
            dispose();
        });

        JButton actionBtn = GuiTheme.createDangerButton(actionButton);
        actionBtn.addActionListener(e -> {
            confirmed = true;
            dispose();
        });

        actions.add(keepBtn);
        actions.add(actionBtn);

        card.add(titleLabel);
        card.add(msgLabel);
        card.add(actions);

        add(card, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getParent());
    }

    /**
     * Checks if the user confirmed the destructive action.
     *
     * @return true if confirmed.
     */
    public boolean isConfirmed() {
        return confirmed;
    }

    /**
     * Static helper to show confirmation and return result.
     *
     * @param parent       parent window.
     * @param title        modal title.
     * @param message      modal text.
     * @param actionButton action text.
     * @return true if confirmed.
     */
    public static boolean confirm(JFrame parent, String title, String message, String actionButton) {
        ConfirmationDialog dialog = new ConfirmationDialog(parent, title, message, actionButton);
        dialog.setVisible(true);
        return dialog.isConfirmed();
    }
}
