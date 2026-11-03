package hk.edu.polyu.comp.comp2021.crobs.command;

import hk.edu.polyu.comp.comp2021.crobs.model.CROBS;
import hk.edu.polyu.comp.comp2021.crobs.model.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.model.Room;

import java.util.Collections;

/**
 * Command representing an addRoom operation (REQ7, BON2).
 */
public class AddRoomCommand implements CrobsCommand {

    private final CROBS.MutationAccess access;
    private final Room room;

    /**
     * Constructs a command for an addRoom operation.
     *
     * @param access internal model mutation access.
     * @param room   the room created.
     */
    public AddRoomCommand(CROBS.MutationAccess access, Room room) {
        this.access = access;
        this.room = new Room(room.getId(), room.getType(), room.getCapacity(), room.getFeatures());
    }

    @Override
    public void execute() throws CrobsException {
        redo();
    }

    @Override
    public void undo() throws CrobsException {
        access.removeRoom(room.getId());
    }

    @Override
    public void redo() throws CrobsException {
        access.restoreRoom(room, Collections.emptyList());
    }

    @Override
    public String getDescription() {
        return "Add room " + room.getId() + " (" + room.getType() + ", " + room.getCapacity() + " seats)";
    }

    /**
     * Gets the room instance.
     *
     * @return Room instance.
     */
    public Room getRoom() {
        return new Room(room.getId(), room.getType(), room.getCapacity(), room.getFeatures());
    }
}
