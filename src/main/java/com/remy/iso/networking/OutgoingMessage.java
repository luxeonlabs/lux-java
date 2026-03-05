package com.remy.iso.networking;

public abstract class OutgoingMessage {
    public final String type;
    public final Object data;

    protected OutgoingMessage(String type, Object data) {
        this.type = type;
        this.data = data;
    }
}
