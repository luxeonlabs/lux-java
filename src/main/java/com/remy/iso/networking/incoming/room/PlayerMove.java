package com.remy.iso.networking.incoming.room;

import com.remy.iso.networking.Packet;

public class PlayerMove extends Packet {
    public String id;
    public int x;
    public int y;
    public float z;
    public int rotation;

    public PlayerMove() {

    }

    @Override
    public void parse() {
        this.id = readString();
        this.x = readInt();
        this.y = readInt();
        this.z = readFloat();
        this.rotation = readInt();
    }
}
