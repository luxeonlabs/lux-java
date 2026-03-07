package com.remy.iso.networking.incoming.room;

import com.remy.iso.networking.Packet;

public class RoomData extends Packet {

    public String floor;
    public String wall;
    public String[] model;

    public RoomData() {
    }

    public void parse() {
        this.floor = readString();
        this.wall = readString();

        this.model = readArray(String.class, () -> readString());
    }
}
