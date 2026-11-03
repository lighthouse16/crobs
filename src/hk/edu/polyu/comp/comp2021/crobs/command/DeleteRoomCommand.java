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

    private final CROBS.MutationAccess access;
    private final Room removedRoom;
    private final List<Booking> cancelledBookings;

    /**
     * Constructs a command for a deleteRoom operation.
     *
     * @param access            internal model mutation access.
     * @param removedRoom       the room being deleted.
     * @param cancelledBookings future bookings of the room that were cancelled.
     */
    public DeleteRoomCommand(CROBS.MutationAccess access, Room removedRoom, List<Booking> cancelledBookings) {
        this.access = access;
        this.removedRoom = new Room(removedRoom.getId(), removedRoom.getType(),
                removedRoom.getCapacity(), removedRoom.getFeatures());
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
        access.restoreRoom(removedRoom, cancelledBookings);
    }

    @Override
    public void redo() throws CrobsException {
        access.removeRoom(removedRoom.getId());
        for (Booking b : cancelledBookings) {
            access.deleteBooking(b.getId());
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
        return new Room(removedRoom.getId(), removedRoom.getType(),
                removedRoom.getCapacity(), removedRoom.getFeatures());
    }

    /**
     * Gets the cancelled bookings list.
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
