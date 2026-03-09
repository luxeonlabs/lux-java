package com.remy.iso.networking.outgoing.auth;

import com.remy.iso.networking.Outgoing;
import com.remy.iso.networking.Packet;

public class ClientReady extends Packet {
    public ClientReady() {
        super(Outgoing.CLIENT_READY);

    }
}
