package hk.edu.polyu.comp.comp2021.crobs.command;

import hk.edu.polyu.comp.comp2021.crobs.model.CROBS;
import hk.edu.polyu.comp.comp2021.crobs.model.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.model.Room;

import java.util.Collections;

/**
 * Command representing an addRoom operation (REQ7, BON2).
 */
public class AddRoomCommand implements CrobsCommand {

    private final CROBS crobs;
    private final Room room;

    /**
     * Constructs a command for an addRoom operation.
     *
     * @param crobs the CROBS model instance.
     * @param room  the room created.
     */
    public AddRoomCommand(CROBS crobs, Room room) {
        this.crobs = crobs;
        this.room = room;
    }

    @Override
    public void execute() throws CrobsException {
        redo();
    }

    @Override
    public void undo() throws CrobsException {
        crobs.removeRoomDirect(room.getId());
    }

    @Override
    public void redo() throws CrobsException {
        crobs.restoreRoom(room, Collections.emptyList());
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
        return room;
    }
}
