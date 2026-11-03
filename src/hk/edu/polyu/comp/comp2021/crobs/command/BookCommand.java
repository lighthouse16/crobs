package hk.edu.polyu.comp.comp2021.crobs.command;

import hk.edu.polyu.comp.comp2021.crobs.model.Booking;
import hk.edu.polyu.comp.comp2021.crobs.model.CROBS;
import hk.edu.polyu.comp.comp2021.crobs.model.CrobsException;

/**
 * Command representing a book operation (REQ10, BON2).
 */
public class BookCommand implements CrobsCommand {

    private final CROBS.MutationAccess access;
    private final Booking booking;

    /**
     * Constructs a command for a book operation.
     *
     * @param access  internal model mutation access.
     * @param booking the booking created.
     */
    public BookCommand(CROBS.MutationAccess access, Booking booking) {
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
        access.deleteBooking(booking.getId());
    }

    @Override
    public void redo() throws CrobsException {
        access.restoreBooking(booking);
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
        return new Booking(booking.getId(), booking.getRoomId(), booking.getUserId(),
                booking.getDate(), booking.getStartTime(), booking.getEndTime(), booking.getPurpose());
    }
}
