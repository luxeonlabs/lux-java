package com.remy.iso.networking.incoming;

import com.remy.iso.networking.Packet;

public class PlayerState extends Packet {
    public String id;
    public int state;

    public PlayerState() {
    }

    public void parse() {
        this.id = readString();
        this.state = readInt();
    }
}
