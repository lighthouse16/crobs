package hk.edu.polyu.comp.comp2021.crobs.command;

import hk.edu.polyu.comp.comp2021.crobs.model.CROBS;
import hk.edu.polyu.comp.comp2021.crobs.model.CrobsException;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Command representing a modifyBooking operation (REQ11, BON2).
 */
public class ModifyBookingCommand implements CrobsCommand {

    private final CROBS.MutationAccess access;
    private final String bookingId;

    private final String oldRoomId;
    private final LocalDate oldDate;
    private final LocalTime oldStartTime;
    private final LocalTime oldEndTime;
    private final String oldPurpose;

    private final String newRoomId;
    private final LocalDate newDate;
    private final LocalTime newStartTime;
    private final LocalTime newEndTime;
    private final String newPurpose;

    /**
     * Constructs a command capturing state changes of a modified booking.
     *
     * @param access       internal model mutation access.
     * @param bookingId    booking ID.
     * @param oldRoomId    room ID prior to modification.
     * @param oldDate      date prior to modification.
     * @param oldStartTime start time prior to modification.
     * @param oldEndTime   end time prior to modification.
     * @param oldPurpose   purpose prior to modification.
     * @param newRoomId    room ID after modification.
     * @param newDate      date after modification.
     * @param newStartTime start time after modification.
     * @param newEndTime   end time after modification.
     * @param newPurpose   purpose after modification.
     */
    public ModifyBookingCommand(CROBS.MutationAccess access, String bookingId,
                                String oldRoomId, LocalDate oldDate, LocalTime oldStartTime, LocalTime oldEndTime, String oldPurpose,
                                String newRoomId, LocalDate newDate, LocalTime newStartTime, LocalTime newEndTime, String newPurpose) {
        this.access = access;
        this.bookingId = bookingId;
        this.oldRoomId = oldRoomId;
        this.oldDate = oldDate;
        this.oldStartTime = oldStartTime;
        this.oldEndTime = oldEndTime;
        this.oldPurpose = oldPurpose;
        this.newRoomId = newRoomId;
        this.newDate = newDate;
        this.newStartTime = newStartTime;
        this.newEndTime = newEndTime;
        this.newPurpose = newPurpose;
    }

    @Override
    public void execute() throws CrobsException {
        redo();
    }

    @Override
    public void undo() throws CrobsException {
        access.modifyBooking(bookingId, oldRoomId, oldDate, oldStartTime, oldEndTime, oldPurpose);
    }

    @Override
    public void redo() throws CrobsException {
        access.modifyBooking(bookingId, newRoomId, newDate, newStartTime, newEndTime, newPurpose);
    }

    @Override
    public String getDescription() {
        return "Modify booking " + bookingId + " to " + newRoomId + " on " + newDate;
    }

    /**
     * Gets the modified booking ID.
     *
     * @return booking ID.
     */
    public String getBookingId() {
        return bookingId;
    }
}
