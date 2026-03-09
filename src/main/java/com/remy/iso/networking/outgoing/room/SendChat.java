package com.remy.iso.networking.outgoing.room;

import com.remy.iso.networking.Outgoing;
import com.remy.iso.networking.Packet;

public class SendChat extends Packet {
    public SendChat(String msg) {
        super(Outgoing.SEND_CHAT);
        this.writeString(msg);
    }
}
