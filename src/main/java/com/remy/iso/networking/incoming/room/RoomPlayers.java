package com.remy.iso.networking.incoming.room;

import com.remy.iso.networking.Packet;

public class RoomPlayers extends Packet {
    public RoomPlayer[] players;

    public RoomPlayers() {
    }

    public class RoomPlayer {
        public String id;
        public int x;
        public int y;
        public int z;
    }

    public void parse() {
        this.players = readArray(RoomPlayer.class, () -> {
            RoomPlayer player = new RoomPlayer();
            player.id = readString();
            player.x = readInt();
            player.y = readInt();
            player.z = readInt();
            return player;
        });
    }
}
