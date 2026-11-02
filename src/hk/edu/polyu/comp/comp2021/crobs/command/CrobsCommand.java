package hk.edu.polyu.comp.comp2021.crobs.command;

import hk.edu.polyu.comp.comp2021.crobs.model.CrobsException;

/**
 * Interface representing an undoable and redoable domain command in CROBS (BON2).
 * Follows the standard GoF Command pattern with Memento state snapshotting.
 */
public interface CrobsCommand {

    /**
     * Executes the domain operation for the first time.
     * Captures pre-execution or post-execution memento state required for reversal.
     *
     * @throws CrobsException if operation fails domain validation or constraints.
     */
    void execute() throws CrobsException;

    /**
     * Reverses the effect of this command, restoring system state exactly as it was before.
     *
     * @throws CrobsException on reversal failure.
     */
    void undo() throws CrobsException;

    /**
     * Re-applies the effect of this command after an undo operation.
     * Guarantees identical entity IDs and state as the original execution.
     *
     * @throws CrobsException on re-application failure.
     */
    void redo() throws CrobsException;

    /**
     * Returns a human-readable summary of this operation for CLI feedback, tooltips, and toasts.
     *
     * @return operation description string.
     */
    String getDescription();
}
