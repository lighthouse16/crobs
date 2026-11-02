package hk.edu.polyu.comp.comp2021.crobs.command;

import hk.edu.polyu.comp.comp2021.crobs.model.CrobsException;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Manages dual stacks for command history to support undo and redo (BON2).
 * Follows the Command Pattern with strict history clearing upon executing new mutating commands.
 */
public class UndoRedoManager {

    private final Deque<CrobsCommand> undoStack = new ArrayDeque<>();
    private final Deque<CrobsCommand> redoStack = new ArrayDeque<>();

    /**
     * Executes a new command, pushes it onto the undo stack, and clears the redo history.
     *
     * @param command the command to execute.
     * @throws CrobsException if command execution fails.
     */
    public void executeCommand(CrobsCommand command) throws CrobsException {
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }
        command.execute();
        undoStack.push(command);
        redoStack.clear();
    }

    /**
     * Directly records an already executed command into the undo history.
     * Used when the command was already executed by the model facade.
     *
     * @param command the executed command.
     */
    public void recordExecutedCommand(CrobsCommand command) {
        if (command != null) {
            undoStack.push(command);
            redoStack.clear();
        }
    }

    /**
     * Undoes the most recently executed command.
     *
     * @return true if an operation was successfully undone, false if undo stack was empty.
     * @throws CrobsException if undo operation fails.
     */
    public boolean undo() throws CrobsException {
        if (undoStack.isEmpty()) {
            return false;
        }
        CrobsCommand cmd = undoStack.pop();
        cmd.undo();
        redoStack.push(cmd);
        return true;
    }

    /**
     * Redoes the most recently undone command.
     *
     * @return true if an operation was successfully redone, false if redo stack was empty.
     * @throws CrobsException if redo operation fails.
     */
    public boolean redo() throws CrobsException {
        if (redoStack.isEmpty()) {
            return false;
        }
        CrobsCommand cmd = redoStack.pop();
        cmd.redo();
        undoStack.push(cmd);
        return true;
    }

    /**
     * Checks if there are operations available to undo.
     *
     * @return true if undo stack is not empty.
     */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    /**
     * Checks if there are operations available to redo.
     *
     * @return true if redo stack is not empty.
     */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /**
     * Inspects the command at the top of the undo stack without removing it.
     *
     * @return the top undo command or null if empty.
     */
    public CrobsCommand peekUndo() {
        return undoStack.peek();
    }

    /**
     * Inspects the command at the top of the redo stack without removing it.
     *
     * @return the top redo command or null if empty.
     */
    public CrobsCommand peekRedo() {
        return redoStack.peek();
    }

    /**
     * Clears all undo and redo history.
     */
    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }

    /**
     * Returns the number of commands currently stored in the undo stack.
     *
     * @return undo history size.
     */
    public int getUndoCount() {
        return undoStack.size();
    }

    /**
     * Returns the number of commands currently stored in the redo stack.
     *
     * @return redo history size.
     */
    public int getRedoCount() {
        return redoStack.size();
    }
}
