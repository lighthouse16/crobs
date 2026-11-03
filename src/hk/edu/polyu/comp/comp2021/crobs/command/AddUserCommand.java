package hk.edu.polyu.comp.comp2021.crobs.command;

import hk.edu.polyu.comp.comp2021.crobs.model.CROBS;
import hk.edu.polyu.comp.comp2021.crobs.model.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.model.User;

import java.util.Collections;

/**
 * Command representing an addUser operation (REQ2, BON2).
 */
public class AddUserCommand implements CrobsCommand {

    private final CROBS.MutationAccess access;
    private final User user;

    /**
     * Constructs a command for an executed addUser operation.
     *
     * @param access internal model mutation access.
     * @param user   the user that was created.
     */
    public AddUserCommand(CROBS.MutationAccess access, User user) {
        this.access = access;
        this.user = new User(user.getId(), user.getName(), user.getRole());
    }

    @Override
    public void execute() throws CrobsException {
        // Redo operation
        redo();
    }

    @Override
    public void undo() throws CrobsException {
        access.removeUser(user.getId());
    }

    @Override
    public void redo() throws CrobsException {
        access.restoreUser(user, Collections.emptyList());
    }

    @Override
    public String getDescription() {
        return "Add user " + user.getName() + " (" + user.getId() + ")";
    }

    /**
     * Gets a snapshot of the user associated with this command.
     *
     * @return User instance.
     */
    public User getUser() {
        return new User(user.getId(), user.getName(), user.getRole());
    }
}
