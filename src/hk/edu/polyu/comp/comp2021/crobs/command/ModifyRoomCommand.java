package hk.edu.polyu.comp.comp2021.crobs.command;

import hk.edu.polyu.comp.comp2021.crobs.model.CROBS;
import hk.edu.polyu.comp.comp2021.crobs.model.CrobsException;

/**
 * Command representing a modifyRoom operation (REQ8, BON2).
 */
public class ModifyRoomCommand implements CrobsCommand {

    private final CROBS.MutationAccess access;
    private final String roomId;
    private final String field;
    private final String oldValue;
    private final String newValue;

    /**
     * Constructs a command for a modifyRoom operation.
     *
     * @param access   internal model mutation access.
     * @param roomId   room ID being modified.
     * @param field    the field name ("type", "capacity", or "features").
     * @param oldValue the field value prior to modification.
     * @param newValue the field value after modification.
     */
    public ModifyRoomCommand(CROBS.MutationAccess access, String roomId, String field, String oldValue, String newValue) {
        this.access = access;
        this.roomId = roomId;
        this.field = field;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    @Override
    public void execute() throws CrobsException {
        redo();
    }

    @Override
    public void undo() throws CrobsException {
        access.modifyRoom(roomId, field, oldValue);
    }

    @Override
    public void redo() throws CrobsException {
        access.modifyRoom(roomId, field, newValue);
    }

    @Override
    public String getDescription() {
        return "Modify room " + roomId + " " + field + " to " + newValue;
    }

    /**
     * Gets the modified room ID.
     *
     * @return room ID.
     */
    public String getRoomId() {
        return roomId;
    }

    /**
     * Gets the field that was modified.
     *
     * @return field name.
     */
    public String getField() {
        return field;
    }

    /**
     * Gets the old field value.
     *
     * @return old value.
     */
    public String getOldValue() {
        return oldValue;
    }

    /**
     * Gets the new field value.
     *
     * @return new value.
     */
    public String getNewValue() {
        return newValue;
    }
}
