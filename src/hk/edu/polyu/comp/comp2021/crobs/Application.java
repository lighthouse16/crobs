package hk.edu.polyu.comp.comp2021.crobs;

import hk.edu.polyu.comp.comp2021.crobs.model.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Main entry point and CLI Controller for the CROBS system.
 * Handles CLI loop, argument parsing, logging (REQ1), and graceful error handling.
 */
public class Application {

    private final CROBS crobs;
    private final String htmlLogPath;
    private final String txtLogPath;
    private int commandIndex = 1;
    private PrintWriter htmlWriter;
    private PrintWriter txtWriter;

    /**
     * Constructs a new Application instance.
     *
     * @param htmlLogPath path to the HTML log file.
     * @param txtLogPath  path to the TXT log file.
     */
    public Application(String htmlLogPath, String txtLogPath) {
        this.crobs = new CROBS();
        this.htmlLogPath = htmlLogPath;
        this.txtLogPath = txtLogPath;
        initLoggers();
    }

    private void initLoggers() {
        if (txtLogPath != null) {
            try {
                txtWriter = new PrintWriter(new BufferedWriter(new FileWriter(txtLogPath, false)));
            } catch (IOException e) {
                System.err.println("Warning: Could not open TXT log file: " + e.getMessage());
            }
        }
        if (htmlLogPath != null) {
            try {
                htmlWriter = new PrintWriter(new BufferedWriter(new FileWriter(htmlLogPath, false)));
                htmlWriter.println("<!DOCTYPE html>");
                htmlWriter.println("<html><head><meta charset=\"UTF-8\"><title>CROBS Session Log</title></head><body>");
                htmlWriter.println("<table border=\"1\">");
                htmlWriter.println("<tr><th>Index</th><th>Command</th></tr>");
                htmlWriter.flush();
            } catch (IOException e) {
                System.err.println("Warning: Could not open HTML log file: " + e.getMessage());
            }
        }
    }

    private void logCommand(String rawCommand) {
        if (txtWriter != null) {
            txtWriter.println(rawCommand);
            txtWriter.flush();
        }
        if (htmlWriter != null) {
            htmlWriter.printf("<tr><td>%d</td><td>%s</td></tr>%n", commandIndex, escapeHtml(rawCommand));
            htmlWriter.flush();
        }
        commandIndex++;
    }

    private void closeLoggers() {
        if (htmlWriter != null) {
            htmlWriter.println("</table></body></html>");
            htmlWriter.close();
        }
        if (txtWriter != null) {
            txtWriter.close();
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    /**
     * Starts the interactive CLI command loop.
     */
    public void run() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to Campus Room Booking System (CROBS)!");
        System.out.println("Type commands or 'quit' to exit.");

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }

            // [REQ1] Log every executed operation
            logCommand(line);

            String[] tokens = line.split("\\s+");
            String command = tokens[0];

            if ("quit".equalsIgnoreCase(command)) { // [REQ18]
                System.out.println("Exiting CROBS. Goodbye!");
                break;
            }

            try {
                processCommand(command, tokens);
            } catch (CrobsException e) {
                System.out.println("Error: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid Input: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("Unexpected Error: " + e.getMessage());
            }
        }

        closeLoggers();
    }

    private void processCommand(String command, String[] tokens) throws CrobsException {
        switch (command) {
            case "login": // [REQ5]
                if (tokens.length < 2) throw new IllegalArgumentException("Usage: login <userID>");
                crobs.login(tokens[1]);
                System.out.println("Logged in successfully as " + crobs.getCurrentUser().getName() + " (" + crobs.getCurrentUser().getRole() + ")");
                break;

            case "logout": // [REQ6]
                crobs.logout();
                System.out.println("Logged out successfully.");
                break;

            case "addUser": // [REQ2]
                if (tokens.length < 2) throw new IllegalArgumentException("Usage: addUser <name>");
                User newUser = crobs.addUser(tokens[1]);
                System.out.println("User created: " + newUser.getId() + " (" + newUser.getName() + ")");
                break;

            case "removeUser": // [REQ3]
                if (tokens.length < 2) throw new IllegalArgumentException("Usage: removeUser <targetUserID>");
                crobs.removeUser(tokens[1]);
                System.out.println("User removed: " + tokens[1]);
                break;

            case "changeRole": // [REQ4]
                if (tokens.length < 3) throw new IllegalArgumentException("Usage: changeRole <targetUserId> <newRole>");
                UserRole role = UserRole.fromString(tokens[2]);
                if (role == null) throw new IllegalArgumentException("Invalid role. Must be SUPERADMIN, ADMIN, or REGULAR.");
                crobs.changeRole(tokens[1], role);
                System.out.println("Role updated for user " + tokens[1] + " to " + role);
                break;

            case "addRoom": // [REQ7]
                if (tokens.length < 4) throw new IllegalArgumentException("Usage: addRoom <roomType> <capacity> <features>");
                RoomType rType = RoomType.fromString(tokens[1]);
                if (rType == null) throw new IllegalArgumentException("Invalid room type: " + tokens[1]);
                int capacity = Integer.parseInt(tokens[2]);
                Set<String> features = parseFeatures(tokens[3]);
                Room newRoom = crobs.addRoom(rType, capacity, features);
                System.out.println("Room created: " + newRoom.getId());
                break;

            case "modifyRoom": // [REQ8]
                if (tokens.length < 4) throw new IllegalArgumentException("Usage: modifyRoom <roomID> <field> <newValue>");
                crobs.modifyRoom(tokens[1], tokens[2], tokens[3]);
                System.out.println("Room " + tokens[1] + " modified.");
                break;

            case "deleteRoom": // [REQ9]
                if (tokens.length < 2) throw new IllegalArgumentException("Usage: deleteRoom <roomID>");
                crobs.deleteRoom(tokens[1]);
                System.out.println("Room deleted: " + tokens[1]);
                break;

            case "book": // [REQ10]
                if (tokens.length < 6) throw new IllegalArgumentException("Usage: book <roomId> <date> <startTime> <endTime> <purpose>");
                LocalDate bDate = parseDate(tokens[2]);
                LocalTime bStart = parseTime(tokens[3]);
                LocalTime bEnd = parseTime(tokens[4]);
                Booking b = crobs.book(tokens[1], bDate, bStart, bEnd, tokens[5]);
                System.out.println("Booking created: " + b.getId());
                break;

            case "modifyBooking": // [REQ11]
                if (tokens.length < 7) throw new IllegalArgumentException("Usage: modifyBooking <bookingID> <roomId> <date> <startTime> <endTime> <purpose>");
                LocalDate mDate = parseDate(tokens[3]);
                LocalTime mStart = parseTime(tokens[4]);
                LocalTime mEnd = parseTime(tokens[5]);
                crobs.modifyBooking(tokens[1], tokens[2], mDate, mStart, mEnd, tokens[6]);
                System.out.println("Booking modified: " + tokens[1]);
                break;

            case "deleteBooking": // [REQ12]
                if (tokens.length < 2) throw new IllegalArgumentException("Usage: deleteBooking <bookingID>");
                crobs.deleteBooking(tokens[1]);
                System.out.println("Booking deleted: " + tokens[1]);
                break;

            case "search": // [REQ13]
                if (tokens.length < 6) throw new IllegalArgumentException("Usage: search <date> <startTime> <endTime> <minCapacity> <features>");
                LocalDate sDate = parseDate(tokens[1]);
                LocalTime sStart = parseTime(tokens[2]);
                LocalTime sEnd = parseTime(tokens[3]);
                int minCap = Integer.parseInt(tokens[4]);
                Set<String> reqFeatures = parseFeatures(tokens[5]);
                List<Room> foundRooms = crobs.search(sDate, sStart, sEnd, minCap, reqFeatures);
                System.out.println("Available Rooms (" + foundRooms.size() + "):");
                for (Room r : foundRooms) {
                    System.out.println("  " + r);
                }
                break;

            case "schedule": // [REQ14]
                if (tokens.length < 3) throw new IllegalArgumentException("Usage: schedule <roomId> <weekStartDate>");
                LocalDate wStart = parseDate(tokens[2]);
                List<Booking> sched = crobs.schedule(tokens[1], wStart);
                System.out.println("Weekly Schedule for Room " + tokens[1] + ":");
                boolean isAdmin = crobs.isLoggedIn() && (crobs.getCurrentUser().getRole() != UserRole.REGULAR);
                for (Booking item : sched) {
                    String userDisplay = (isAdmin || (crobs.isLoggedIn() && item.getUserId().equalsIgnoreCase(crobs.getCurrentUser().getId())))
                            ? item.getUserId() : "(hidden)";
                    System.out.printf("  Date: %s, Time: %s-%s, User: %s, Purpose: %s%n",
                            item.getDate(), item.getStartTime(), item.getEndTime(), userDisplay, item.getPurpose());
                }
                break;

            case "listRoom": // [REQ15]
                if (tokens.length < 2) throw new IllegalArgumentException("Usage: listRoom <roomID>");
                Room r = crobs.listRoom(tokens[1]);
                System.out.println(r);
                break;

            case "listAllRooms": // [REQ16]
                List<Room> allRooms = crobs.listAllRooms();
                System.out.println("All Rooms (" + allRooms.size() + "):");
                for (Room rm : allRooms) {
                    System.out.println("  " + rm);
                }
                break;

            case "listBookings": // [REQ17]
                List<Booking> myBookings = crobs.listBookings();
                System.out.println("My Bookings (" + myBookings.size() + "):");
                for (Booking bk : myBookings) {
                    System.out.println("  " + bk);
                }
                break;

            default:
                System.out.println("Unknown command: " + command);
        }
    }

    private Set<String> parseFeatures(String str) {
        Set<String> set = new HashSet<>();
        if (str != null && !str.trim().isEmpty()) {
            for (String f : str.split(",")) {
                if (!f.trim().isEmpty()) {
                    set.add(f.trim());
                }
            }
        }
        return set;
    }

    private LocalDate parseDate(String str) {
        try {
            return LocalDate.parse(str);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format (expected YYYY-MM-DD): " + str);
        }
    }

    private LocalTime parseTime(String str) {
        try {
            return LocalTime.parse(str);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid time format (expected HH:MM): " + str);
        }
    }

    /**
     * Main CLI launcher.
     * Parses -html and -txt parameters.
     *
     * @param args command line arguments.
     */
    public static void main(String[] args) {
        String htmlPath = null;
        String txtPath = null;

        for (int i = 0; i < args.length; i++) {
            if ("-html".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                htmlPath = args[++i];
            } else if ("-txt".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                txtPath = args[++i];
            }
        }

        Application app = new Application(htmlPath, txtPath);
        app.run();
    }
}
