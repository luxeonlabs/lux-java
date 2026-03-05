package com.remy.iso.networking.handlers;

import com.remy.iso.GameMain;
import com.remy.iso.networking.GameClient;
import com.remy.iso.networking.Incoming;
import com.remy.iso.networking.incoming.PlayerMove;
import com.remy.iso.networking.incoming.PlayerState;
import com.remy.iso.networking.incoming.RoomData;
import com.remy.iso.networking.incoming.RoomPlayers;
import com.remy.iso.networking.incoming.RoomPlayers.RoomPlayer;

public class RoomHandler {
    public RoomHandler() {
        GameClient gc = GameClient.getInstance();

        gc.register(Incoming.ROOM_DATA, RoomData::new, d -> {
            GameMain.getInstance().loadRoom(d);
        });

        gc.register(Incoming.ROOM_PLAYERS, RoomPlayers::new, d -> {
            for (RoomPlayer player : d.players) {
                GameMain.getInstance().room().addPlayer(player);
            }
        });

        gc.register(Incoming.PLAYER_MOVEMENT, PlayerMove::new, d -> {
            GameMain.getInstance().room().movePlayer(d);
        });

        gc.register(Incoming.PLAYER_STATE, PlayerState::new, d -> {
            GameMain.getInstance().room().setPlayerState(d);
        });
    }
}
