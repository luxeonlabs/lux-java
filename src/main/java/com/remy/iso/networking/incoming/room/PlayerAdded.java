package com.remy.iso.networking.incoming.room;

import com.remy.iso.networking.Packet;
import com.remy.iso.networking.incoming.room.RoomPlayers.RoomPlayer;

public class PlayerAdded extends Packet {
    public RoomPlayer player;

    public PlayerAdded() {

    }

    @Override
    public void parse() {
        this.player = new RoomPlayer();
        player.id = readString();
        player.name = readString();
        player.x = readInt();
        player.y = readInt();
    }
}
