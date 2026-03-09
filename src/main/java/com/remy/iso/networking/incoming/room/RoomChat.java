package com.remy.iso.networking.incoming.room;

import com.remy.iso.networking.Packet;

public class RoomChat extends Packet {
    public String userId;
    public String msg;

    public RoomChat() {
    }

    @Override
    public void parse() {
        this.userId = readString();
        this.msg = readString();
    }
}
