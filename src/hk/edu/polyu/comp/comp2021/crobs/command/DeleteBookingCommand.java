package hk.edu.polyu.comp.comp2021.crobs.command;

import hk.edu.polyu.comp.comp2021.crobs.model.Booking;
import hk.edu.polyu.comp.comp2021.crobs.model.CROBS;
import hk.edu.polyu.comp.comp2021.crobs.model.CrobsException;

/**
 * Command representing a deleteBooking operation (REQ12, BON2).
 */
public class DeleteBookingCommand implements CrobsCommand {

    private final CROBS.MutationAccess access;
    private final Booking booking;

    /**
     * Constructs a command for a deleteBooking operation.
     *
     * @param access  internal model mutation access.
     * @param booking the booking being deleted.
     */
    public DeleteBookingCommand(CROBS.MutationAccess access, Booking booking) {
        this.access = access;
        this.booking = new Booking(booking.getId(), booking.getRoomId(), booking.getUserId(),
                booking.getDate(), booking.getStartTime(), booking.getEndTime(), booking.getPurpose());
    }

    @Override
    public void execute() throws CrobsException {
        redo();
    }

    @Override
    public void undo() throws CrobsException {
        access.restoreBooking(booking);
    }

    @Override
    public void redo() throws CrobsException {
        access.deleteBooking(booking.getId());
    }

    @Override
    public String getDescription() {
        return "Delete booking " + booking.getId() + " (" + booking.getRoomId() + ")";
    }

    /**
     * Gets the deleted booking instance.
     *
     * @return Booking instance.
     */
    public Booking getBooking() {
        return new Booking(booking.getId(), booking.getRoomId(), booking.getUserId(),
                booking.getDate(), booking.getStartTime(), booking.getEndTime(), booking.getPurpose());
    }
}
