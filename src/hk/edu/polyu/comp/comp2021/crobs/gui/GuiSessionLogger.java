package hk.edu.polyu.comp.comp2021.crobs.gui;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * Independent session logger implementing REQ1 for the CROBS graphical user interface.
 * Outputs canonical CLI command strings to both an HTML table and plain text log file.
 */
public class GuiSessionLogger {

    private final String htmlLogPath;
    private final String txtLogPath;
    private PrintWriter htmlWriter;
    private PrintWriter txtWriter;
    private int commandIndex = 1;
    private boolean closed = false;

    /**
     * Constructs a new GuiSessionLogger with specified output paths.
     *
     * @param htmlLogPath file path for the HTML table log.
     * @param txtLogPath  file path for the TXT command log.
     */
    public GuiSessionLogger(String htmlLogPath, String txtLogPath) {
        this.htmlLogPath = (htmlLogPath != null) ? htmlLogPath.trim() : "";
        this.txtLogPath = (txtLogPath != null) ? txtLogPath.trim() : "";
        initLoggers();
    }

    private void initLoggers() {
        if (!txtLogPath.isEmpty()) {
            try {
                File txtFile = new File(txtLogPath);
                File parent = txtFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                txtWriter = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(new FileOutputStream(txtFile, false), StandardCharsets.UTF_8)));
            } catch (IOException e) {
                System.err.println("Warning: Could not open TXT log file: " + e.getMessage());
            }
        }

        if (!htmlLogPath.isEmpty()) {
            try {
                File htmlFile = new File(htmlLogPath);
                File parent = htmlFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                htmlWriter = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(new FileOutputStream(htmlFile, false), StandardCharsets.UTF_8)));
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

    /**
     * Logs an executed command in canonical CLI grammar.
     *
     * @param canonicalCommand canonical CLI command string.
     */
    public synchronized void logCommand(String canonicalCommand) {
        if (closed || canonicalCommand == null || canonicalCommand.trim().isEmpty()) {
            return;
        }
        String cmd = canonicalCommand.trim();

        if (txtWriter != null) {
            txtWriter.println(cmd);
            txtWriter.flush();
        }

        if (htmlWriter != null) {
            htmlWriter.printf("<tr><td>%d</td><td>%s</td></tr>%n", commandIndex, escapeHtml(cmd));
            htmlWriter.flush();
        }

        commandIndex++;
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    /**
     * Flushes and closes all log streams upon application exit (REQ18).
     */
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;

        if (htmlWriter != null) {
            htmlWriter.println("</table></body></html>");
            htmlWriter.flush();
            htmlWriter.close();
            htmlWriter = null;
        }

        if (txtWriter != null) {
            txtWriter.flush();
            txtWriter.close();
            txtWriter = null;
        }
    }
}
