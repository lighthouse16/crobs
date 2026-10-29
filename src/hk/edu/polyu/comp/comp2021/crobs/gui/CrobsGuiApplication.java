package hk.edu.polyu.comp.comp2021.crobs.gui;

import hk.edu.polyu.comp.comp2021.crobs.model.CROBS;
import hk.edu.polyu.comp.comp2021.crobs.model.RoomType;
import hk.edu.polyu.comp.comp2021.crobs.model.User;
import hk.edu.polyu.comp.comp2021.crobs.model.UserRole;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Main application entry point for the CROBS Java Swing GUI (BON1).
 * Supports optional CLI log arguments:
 *   -html <filepath> : Path to generate REQ1 HTML session log table.
 *   -txt  <filepath> : Path to generate REQ1 plain text command log.
 */
public class CrobsGuiApplication {

    public static void main(String[] args) {
        String htmlLog = null;
        String txtLog = null;

        for (int i = 0; i < args.length; i++) {
            if ("-html".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                htmlLog = args[++i];
            } else if ("-txt".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                txtLog = args[++i];
            }
        }

        final String finalHtmlLog = htmlLog;
        final String finalTxtLog = txtLog;

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }

            CROBS model = new CROBS();
            GuiSessionLogger logger = new GuiSessionLogger(finalHtmlLog, finalTxtLog);
            GuiController controller = new GuiController(model, logger);

            // Auto-login as SuperAdmin u1 and initialize prototype campus rooms if empty
            initDefaultState(controller);

            MainFrame frame = new MainFrame(controller);
            frame.setVisible(true);
        });
    }

    private static void initDefaultState(GuiController controller) {
        try {
            // Default active session: u1 (SuperAdmin)
            controller.loginQuiet("u1");

            // Seed initial campus rooms and bookings matching CROBS_GUI_V3.html
            if (controller.getAllRoomsQuiet().isEmpty()) {
                controller.addRoomQuiet(RoomType.lectureTheatre, 120, new HashSet<>(Arrays.asList("projector", "wifi", "audio")));
                controller.addRoomQuiet(RoomType.seminarRoom, 24, new HashSet<>(Arrays.asList("whiteboard", "wifi", "display")));
                controller.addRoomQuiet(RoomType.computerLab, 32, new HashSet<>(Arrays.asList("computers", "wifi", "projector")));
                controller.addRoomQuiet(RoomType.meetingRoom, 8, new HashSet<>(Arrays.asList("wifi", "display")));
                controller.addRoomQuiet(RoomType.seminarRoom, 40, new HashSet<>(Arrays.asList("whiteboard", "projector")));

                User u2 = controller.addUserQuiet("Alex_Chen");
                User u3 = controller.addUserQuiet("Jamie_Lee");
                controller.changeRoleQuiet(u3.getId(), UserRole.ADMIN);

                LocalDate dNov3 = LocalDate.of(2026, 11, 3);
                LocalDate dNov4 = LocalDate.of(2026, 11, 4);
                LocalDate dNov5 = LocalDate.of(2026, 11, 5);

                controller.loginQuiet(u2.getId());
                controller.bookQuiet("r1", dNov3, LocalTime.of(9, 0), LocalTime.of(10, 30), "Lecture_planning");
                controller.bookQuiet("r2", dNov3, LocalTime.of(10, 0), LocalTime.of(11, 30), "Seminar_preparation");
                controller.bookQuiet("r3", dNov3, LocalTime.of(12, 0), LocalTime.of(14, 0), "Programming_lab");
                controller.bookQuiet("r2", dNov4, LocalTime.of(9, 0), LocalTime.of(10, 0), "Revision");

                controller.loginQuiet(u3.getId());
                controller.bookQuiet("r1", dNov3, LocalTime.of(15, 0), LocalTime.of(17, 0), "Team_session");
                controller.bookQuiet("r2", dNov3, LocalTime.of(16, 0), LocalTime.of(17, 0), "Group_meeting");
                controller.bookQuiet("r4", dNov3, LocalTime.of(14, 0), LocalTime.of(16, 0), "Research_discussion");

                controller.loginQuiet("u1");
                controller.bookQuiet("r3", dNov5, LocalTime.of(11, 0), LocalTime.of(12, 30), "Demonstration");
            }
        } catch (Exception ignored) {
        } finally {
            // Guarantee unauthenticated initial state on startup so app begins on Login screen
            controller.logoutQuiet();
        }
    }
}
