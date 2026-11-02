package hk.edu.polyu.comp.comp2021.crobs.command;

import hk.edu.polyu.comp.comp2021.crobs.model.Booking;
import hk.edu.polyu.comp.comp2021.crobs.model.CROBS;
import hk.edu.polyu.comp.comp2021.crobs.model.CrobsException;

/**
 * Command representing a book operation (REQ10, BON2).
 */
public class BookCommand implements CrobsCommand {

    private final CROBS crobs;
    private final Booking booking;

    /**
     * Constructs a command for a book operation.
     *
     * @param crobs   the CROBS model instance.
     * @param booking the booking created.
     */
    public BookCommand(CROBS crobs, Booking booking) {
        this.crobs = crobs;
        this.booking = booking;
    }

    @Override
    public void execute() throws CrobsException {
        redo();
    }

    @Override
    public void undo() throws CrobsException {
        crobs.deleteBookingDirect(booking.getId());
    }

    @Override
    public void redo() throws CrobsException {
        crobs.restoreBooking(booking);
    }

    @Override
    public String getDescription() {
        return "Book " + booking.getRoomId() + " (" + booking.getId() + ") on "
                + booking.getDate() + " " + booking.getStartTime() + "–" + booking.getEndTime();
    }

    /**
     * Gets the booking instance.
     *
     * @return Booking instance.
     */
    public Booking getBooking() {
        return booking;
    }
}
