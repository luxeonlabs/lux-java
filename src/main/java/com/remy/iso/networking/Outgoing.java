package com.remy.iso.networking;

public enum Outgoing {
    AuthToken(1),
    CLIENT_READY(2),
    REQUEST_MOVE(3),
    ITEM_INTERACT(4),
    SEND_CHAT(5);

    private final int id;

    Outgoing(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    /** Optional: get enum from id */
    public static Outgoing fromId(int id) {
        for (Outgoing e : values()) {
            if (e.id == id)
                return e;
        }
        return null; // or throw exception
    }
}
