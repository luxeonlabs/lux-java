package com.remy.iso.networking.outgoing.room;

import com.remy.iso.networking.Outgoing;
import com.remy.iso.networking.Packet;

public class RequestInteract extends Packet {

    public RequestInteract(int id) {
        super(Outgoing.ITEM_INTERACT);
        writeInt(id);
    }
}
