package com.remy.iso.networking.outgoing;

import com.remy.iso.networking.Outgoing;
import com.remy.iso.networking.Packet;

public class AuthToken extends Packet {
    public AuthToken(String sso) {
        super(Outgoing.AuthToken);

        writeString(sso);
    }
}
