package hk.edu.polyu.comp.comp2021.crobs.command;

import hk.edu.polyu.comp.comp2021.crobs.model.Booking;
import hk.edu.polyu.comp.comp2021.crobs.model.CROBS;
import hk.edu.polyu.comp.comp2021.crobs.model.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Command representing a removeUser operation with cascading bookings cancellation (REQ3, BON2).
 */
public class RemoveUserCommand implements CrobsCommand {

    private final CROBS crobs;
    private final User removedUser;
    private final List<Booking> cancelledBookings;

    /**
     * Constructs a command for a removeUser operation.
     *
     * @param crobs             the CROBS model instance.
     * @param removedUser       the user being removed.
     * @param cancelledBookings list of bookings that were cancelled due to removing this user.
     */
    public RemoveUserCommand(CROBS crobs, User removedUser, List<Booking> cancelledBookings) {
        this.crobs = crobs;
        this.removedUser = removedUser;
        this.cancelledBookings = new ArrayList<>(cancelledBookings);
    }

    @Override
    public void execute() throws CrobsException {
        redo();
    }

    @Override
    public void undo() throws CrobsException {
        crobs.restoreUser(removedUser, cancelledBookings);
    }

    @Override
    public void redo() throws CrobsException {
        crobs.removeUserDirect(removedUser.getId());
        for (Booking b : cancelledBookings) {
            crobs.deleteBookingDirect(b.getId());
        }
    }

    @Override
    public String getDescription() {
        return "Remove user " + removedUser.getName() + " (" + removedUser.getId() + ")";
    }

    /**
     * Gets the removed user.
     *
     * @return User instance.
     */
    public User getRemovedUser() {
        return removedUser;
    }

    /**
     * Gets the list of cancelled bookings associated with this user.
     *
     * @return list of bookings.
     */
    public List<Booking> getCancelledBookings() {
        return new ArrayList<>(cancelledBookings);
    }
}
