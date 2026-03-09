package com.remy.iso.networking;

public enum Incoming {
    ROOM_DATA(1),
    PLAYER_ADDED(2),
    ROOM_PLAYERS(3),
    PLAYER_MOVEMENT(4),
    PLAYER_STATE(5),
    ROOM_ITEMS(6),
    CLIENT_CONFIG(7),
    UPDATE_ITEM(8),
    ROOM_CHAT(9);

    private final int id;

    Incoming(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    /** Optional: get enum from id */
    public static Incoming fromId(int id) {
        for (Incoming e : values()) {
            if (e.id == id)
                return e;
        }
        return null; // or throw exception
    }
}
