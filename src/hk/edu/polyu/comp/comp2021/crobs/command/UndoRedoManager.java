package hk.edu.polyu.comp.comp2021.crobs.command;

import hk.edu.polyu.comp.comp2021.crobs.model.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.model.UserRole;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Manages dual stacks for command history to support undo and redo (BON2).
 * Each history entry belongs to the user who executed it and records the
 * privilege required to reverse or re-apply it.
 */
public class UndoRedoManager {

    /** Required privilege for a recorded command. */
    public enum Permission {
        LOGGED_IN,
        ADMIN,
        SUPERADMIN
    }

    private static final class HistoryEntry {
        private final CrobsCommand command;
        private final String actorId;
        private final Permission permission;

        private HistoryEntry(CrobsCommand command, String actorId, Permission permission) {
            this.command = command;
            this.actorId = actorId;
            this.permission = permission;
        }
    }

    private final Deque<HistoryEntry> undoStack = new ArrayDeque<>();
    private final Deque<HistoryEntry> redoStack = new ArrayDeque<>();

    /**
     * Records an already executed command and clears the redo branch.
     *
     * @param command    executed command.
     * @param actorId    ID of the user who executed it.
     * @param permission privilege required for undo and redo.
     */
    public void recordExecutedCommand(CrobsCommand command, String actorId, Permission permission) {
        if (command == null || actorId == null || actorId.trim().isEmpty() || permission == null) {
            throw new IllegalArgumentException("Command history metadata cannot be null or empty");
        }
        undoStack.push(new HistoryEntry(command, normalize(actorId), permission));
        redoStack.clear();
    }

    /**
     * Undoes the latest command when the current user owns it and remains authorized.
     *
     * @param actorId current user ID.
     * @param role    current user role.
     * @return true if a command was undone; false if history is empty.
     * @throws CrobsException if another user owns the latest command or privilege is insufficient.
     */
    public boolean undo(String actorId, UserRole role) throws CrobsException {
        if (undoStack.isEmpty()) {
            return false;
        }
        HistoryEntry entry = undoStack.peek();
        authorize(entry, actorId, role);
        entry.command.undo();
        undoStack.pop();
        redoStack.push(entry);
        return true;
    }

    /**
     * Redoes the latest undone command when the current user owns it and remains authorized.
     *
     * @param actorId current user ID.
     * @param role    current user role.
     * @return true if a command was redone; false if history is empty.
     * @throws CrobsException if another user owns the command or privilege is insufficient.
     */
    public boolean redo(String actorId, UserRole role) throws CrobsException {
        if (redoStack.isEmpty()) {
            return false;
        }
        HistoryEntry entry = redoStack.peek();
        authorize(entry, actorId, role);
        entry.command.redo();
        redoStack.pop();
        undoStack.push(entry);
        return true;
    }

    /** Returns whether the current user may undo the latest command. */
    public boolean canUndo(String actorId, UserRole role) {
        return !undoStack.isEmpty() && isAuthorized(undoStack.peek(), actorId, role);
    }

    /** Returns whether the current user may redo the latest command. */
    public boolean canRedo(String actorId, UserRole role) {
        return !redoStack.isEmpty() && isAuthorized(redoStack.peek(), actorId, role);
    }

    /** Returns the latest undo command, or null when empty. */
    public CrobsCommand peekUndo() {
        HistoryEntry entry = undoStack.peek();
        return entry == null ? null : entry.command;
    }

    /** Returns the latest redo command, or null when empty. */
    public CrobsCommand peekRedo() {
        HistoryEntry entry = redoStack.peek();
        return entry == null ? null : entry.command;
    }

    /** Clears all history. */
    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }

    /** Returns undo history size. */
    public int getUndoCount() {
        return undoStack.size();
    }

    /** Returns redo history size. */
    public int getRedoCount() {
        return redoStack.size();
    }

    private static void authorize(HistoryEntry entry, String actorId, UserRole role) throws CrobsException {
        if (!isAuthorized(entry, actorId, role)) {
            if (actorId == null || !entry.actorId.equals(normalize(actorId))) {
                throw new CrobsException("Permission denied: Only the user who executed the command can undo or redo it.");
            }
            throw new CrobsException("Permission denied: Current role cannot undo or redo this command.");
        }
    }

    private static boolean isAuthorized(HistoryEntry entry, String actorId, UserRole role) {
        if (entry == null || actorId == null || role == null
                || !entry.actorId.equals(normalize(actorId))) {
            return false;
        }
        switch (entry.permission) {
            case SUPERADMIN:
                return role.canChangeRoles();
            case ADMIN:
                return role.canManageRooms() || role.canManageUsers();
            case LOGGED_IN:
                return true;
            default:
                return false;
        }
    }

    private static String normalize(String id) {
        return id.trim().toLowerCase();
    }
}
