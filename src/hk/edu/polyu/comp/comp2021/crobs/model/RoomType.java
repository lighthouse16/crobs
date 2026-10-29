package hk.edu.polyu.comp.comp2021.crobs.model;

/**
 * Defines the supported room types in the CROBS system.
 * Required values: lectureTheatre, computerLab, seminarRoom, meetingRoom.
 */
public enum RoomType {

    /**
     * Large lecture theatre room type.
     */
    lectureTheatre,

    /**
     * Computer laboratory room type.
     */
    computerLab,

    /**
     * Seminar room type.
     */
    seminarRoom,

    /**
     * Meeting room type.
     */
    meetingRoom;

    /**
     * Parses a string into a RoomType matching the exact names required by spec.
     *
     * @param str input string.
     * @return corresponding RoomType or null if not found.
     */
    public static RoomType fromString(String str) {
        if (str == null) {
            return null;
        }
        for (RoomType type : values()) {
            if (type.name().equalsIgnoreCase(str)) {
                return type;
            }
        }
        return null;
    }
}
