package com.remy.iso.networking.incoming.room;

import com.remy.iso.networking.Packet;

public class PlayerState extends Packet {
    public String id;
    public int state;
    public int rotation;

    public PlayerState() {
    }

    public void parse() {
        this.id = readString();
        this.state = readInt();
        this.rotation = readInt();
    }
}
