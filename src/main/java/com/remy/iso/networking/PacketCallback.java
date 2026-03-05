package com.remy.iso.networking;

@FunctionalInterface
public interface PacketCallback<T extends Packet> {
    void handle(GameClient client, T packet);
}