package com.remy.iso.networking.incoming;

import com.remy.iso.networking.Packet;

public class PlayerMove extends Packet {
    public String id;
    public int x;
    public int y;
    public int z;
    public int rotation;

    public PlayerMove() {

    }

    @Override
    public void parse() {
        this.id = readString();
        System.out.println("Read ID: " + this.id + ", offset now: " + this.readOffset);

        this.x = readInt();
        System.out.println("Read X: " + this.x + ", offset now: " + readOffset);

        this.y = readInt();
        System.out.println("Read Y: " + this.y + ", offset now: " + readOffset);

        this.z = readInt();
        this.rotation = readInt();
    }
}
