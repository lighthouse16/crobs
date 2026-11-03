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

    private final CROBS.MutationAccess access;
    private final User removedUser;
    private final List<Booking> cancelledBookings;

    /**
     * Constructs a command for a removeUser operation.
     *
     * @param access            internal model mutation access.
     * @param removedUser       the user being removed.
     * @param cancelledBookings list of bookings that were cancelled due to removing this user.
     */
    public RemoveUserCommand(CROBS.MutationAccess access, User removedUser, List<Booking> cancelledBookings) {
        this.access = access;
        this.removedUser = new User(removedUser.getId(), removedUser.getName(), removedUser.getRole());
        this.cancelledBookings = new ArrayList<>();
        if (cancelledBookings != null) {
            for (Booking b : cancelledBookings) {
                this.cancelledBookings.add(new Booking(b.getId(), b.getRoomId(), b.getUserId(),
                        b.getDate(), b.getStartTime(), b.getEndTime(), b.getPurpose()));
            }
        }
    }

    @Override
    public void execute() throws CrobsException {
        redo();
    }

    @Override
    public void undo() throws CrobsException {
        access.restoreUser(removedUser, cancelledBookings);
    }

    @Override
    public void redo() throws CrobsException {
        access.removeUser(removedUser.getId());
        for (Booking b : cancelledBookings) {
            access.deleteBooking(b.getId());
        }
    }

    @Override
    public String getDescription() {
        return "Remove user " + removedUser.getName() + " (" + removedUser.getId() + ")";
    }

    /**
     * Gets a snapshot of the removed user.
     *
     * @return User instance.
     */
    public User getRemovedUser() {
        return new User(removedUser.getId(), removedUser.getName(), removedUser.getRole());
    }

    /**
     * Gets a copy of the cancelled bookings associated with this user.
     *
     * @return list of bookings.
     */
    public List<Booking> getCancelledBookings() {
        List<Booking> copy = new ArrayList<>();
        for (Booking b : cancelledBookings) {
            copy.add(new Booking(b.getId(), b.getRoomId(), b.getUserId(),
                    b.getDate(), b.getStartTime(), b.getEndTime(), b.getPurpose()));
        }
        return copy;
    }
}
