package hk.edu.polyu.comp.comp2021.crobs.command;

import hk.edu.polyu.comp.comp2021.crobs.model.Booking;
import hk.edu.polyu.comp.comp2021.crobs.model.CROBS;
import hk.edu.polyu.comp.comp2021.crobs.model.CrobsException;

/**
 * Command representing a deleteBooking operation (REQ12, BON2).
 */
public class DeleteBookingCommand implements CrobsCommand {

    private final CROBS crobs;
    private final Booking booking;

    /**
     * Constructs a command for a deleteBooking operation.
     *
     * @param crobs   CROBS model.
     * @param booking the booking being deleted.
     */
    public DeleteBookingCommand(CROBS crobs, Booking booking) {
        this.crobs = crobs;
        this.booking = booking;
    }

    @Override
    public void execute() throws CrobsException {
        redo();
    }

    @Override
    public void undo() throws CrobsException {
        crobs.restoreBooking(booking);
    }

    @Override
    public void redo() throws CrobsException {
        crobs.deleteBookingDirect(booking.getId());
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
        return booking;
    }
}
