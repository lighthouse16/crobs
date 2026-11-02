package hk.edu.polyu.comp.comp2021.crobs.command;

import hk.edu.polyu.comp.comp2021.crobs.model.Booking;
import hk.edu.polyu.comp.comp2021.crobs.model.CROBS;
import hk.edu.polyu.comp.comp2021.crobs.model.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.model.Room;

import java.util.ArrayList;
import java.util.List;

/**
 * Command representing a deleteRoom operation with cascading future bookings cancellation (REQ9, BON2).
 */
public class DeleteRoomCommand implements CrobsCommand {

    private final CROBS crobs;
    private final Room removedRoom;
    private final List<Booking> cancelledBookings;

    /**
     * Constructs a command for a deleteRoom operation.
     *
     * @param crobs             the CROBS model instance.
     * @param removedRoom       the room being deleted.
     * @param cancelledBookings future bookings of the room that were cancelled.
     */
    public DeleteRoomCommand(CROBS crobs, Room removedRoom, List<Booking> cancelledBookings) {
        this.crobs = crobs;
        this.removedRoom = removedRoom;
        this.cancelledBookings = new ArrayList<>(cancelledBookings);
    }

    @Override
    public void execute() throws CrobsException {
        redo();
    }

    @Override
    public void undo() throws CrobsException {
        crobs.restoreRoom(removedRoom, cancelledBookings);
    }

    @Override
    public void redo() throws CrobsException {
        crobs.removeRoomDirect(removedRoom.getId());
        for (Booking b : cancelledBookings) {
            crobs.deleteBookingDirect(b.getId());
        }
    }

    @Override
    public String getDescription() {
        return "Delete room " + removedRoom.getId();
    }

    /**
     * Gets the removed room.
     *
     * @return Room instance.
     */
    public Room getRemovedRoom() {
        return removedRoom;
    }

    /**
     * Gets the cancelled bookings list.
     *
     * @return list of bookings.
     */
    public List<Booking> getCancelledBookings() {
        return new ArrayList<>(cancelledBookings);
    }
}
