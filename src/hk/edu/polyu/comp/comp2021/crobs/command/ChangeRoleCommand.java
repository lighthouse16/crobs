package hk.edu.polyu.comp.comp2021.crobs.command;

import hk.edu.polyu.comp.comp2021.crobs.model.CROBS;
import hk.edu.polyu.comp.comp2021.crobs.model.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.model.UserRole;

/**
 * Command representing a changeRole operation (REQ4, BON2).
 */
public class ChangeRoleCommand implements CrobsCommand {

    private final CROBS.MutationAccess access;
    private final String targetUserId;
    private final UserRole oldRole;
    private final UserRole newRole;

    /**
     * Constructs a command for a changeRole operation.
     *
     * @param access       internal model mutation access.
     * @param targetUserId user ID whose role is being changed.
     * @param oldRole      the role before the change.
     * @param newRole      the role after the change.
     */
    public ChangeRoleCommand(CROBS.MutationAccess access, String targetUserId, UserRole oldRole, UserRole newRole) {
        this.access = access;
        this.targetUserId = targetUserId;
        this.oldRole = oldRole;
        this.newRole = newRole;
    }

    @Override
    public void execute() throws CrobsException {
        redo();
    }

    @Override
    public void undo() throws CrobsException {
        access.changeRole(targetUserId, oldRole);
    }

    @Override
    public void redo() throws CrobsException {
        access.changeRole(targetUserId, newRole);
    }

    @Override
    public String getDescription() {
        return "Change role for " + targetUserId + " to " + newRole;
    }

    /**
     * Gets the target user ID.
     *
     * @return target user ID.
     */
    public String getTargetUserId() {
        return targetUserId;
    }

    /**
     * Gets the old role.
     *
     * @return previous role.
     */
    public UserRole getOldRole() {
        return oldRole;
    }

    /**
     * Gets the new role.
     *
     * @return new role.
     */
    public UserRole getNewRole() {
        return newRole;
    }
}
