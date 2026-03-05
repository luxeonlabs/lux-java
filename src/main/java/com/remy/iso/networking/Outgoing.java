package com.remy.iso.networking;

public enum Outgoing {
    AuthToken(1),
    REQUEST_MOVE(2);

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
