package hk.edu.polyu.comp.comp2021.crobs.command;

import hk.edu.polyu.comp.comp2021.crobs.model.CROBS;
import hk.edu.polyu.comp.comp2021.crobs.model.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.model.User;

import java.util.Collections;

/**
 * Command representing an addUser operation (REQ2, BON2).
 */
public class AddUserCommand implements CrobsCommand {

    private final CROBS crobs;
    private final User user;

    /**
     * Constructs a command for an executed addUser operation.
     *
     * @param crobs the CROBS model instance.
     * @param user  the user that was created.
     */
    public AddUserCommand(CROBS crobs, User user) {
        this.crobs = crobs;
        this.user = user;
    }

    @Override
    public void execute() throws CrobsException {
        // Redo operation
        redo();
    }

    @Override
    public void undo() throws CrobsException {
        crobs.removeUserDirect(user.getId());
    }

    @Override
    public void redo() throws CrobsException {
        crobs.restoreUser(user, Collections.emptyList());
    }

    @Override
    public String getDescription() {
        return "Add user " + user.getName() + " (" + user.getId() + ")";
    }

    /**
     * Gets the user associated with this command.
     *
     * @return User instance.
     */
    public User getUser() {
        return user;
    }
}
