package com.remy.iso.networking.outgoing;

import com.remy.iso.networking.Outgoing;
import com.remy.iso.networking.Packet;

public class RequestMove extends Packet {

    record Data(int x, int y) {
    }

    public RequestMove(int x, int y) {
        super(Outgoing.REQUEST_MOVE);
        writeInt(x);
        writeInt(y);
    }
}
