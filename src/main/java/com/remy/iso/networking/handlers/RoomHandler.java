package com.remy.iso.networking.handlers;

import com.remy.iso.GameMain;
import com.remy.iso.networking.GameClient;
import com.remy.iso.networking.Incoming;
import com.remy.iso.networking.incoming.room.PlayerAdded;
import com.remy.iso.networking.incoming.room.PlayerMove;
import com.remy.iso.networking.incoming.room.PlayerState;
import com.remy.iso.networking.incoming.room.RoomChat;
import com.remy.iso.networking.incoming.room.RoomData;
import com.remy.iso.networking.incoming.room.RoomItems;
import com.remy.iso.networking.incoming.room.RoomPlayers;
import com.remy.iso.networking.incoming.room.RoomPlayers.RoomPlayer;
import com.remy.iso.networking.incoming.room.UpdateItem;

public class RoomHandler {
    public RoomHandler() {
        GameClient gc = GameClient.getInstance();

        gc.register(Incoming.ROOM_DATA, RoomData::new, d -> {
            GameMain.getInstance().loadRoom(d);
        });

        gc.register(Incoming.PLAYER_ADDED, PlayerAdded::new, d -> {
            GameMain.getInstance().room().addPlayer(d.player);
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

        gc.register(Incoming.ROOM_ITEMS, RoomItems::new, d -> {
            GameMain.getInstance().room().setItems(d.items);
        });

        gc.register(Incoming.UPDATE_ITEM, UpdateItem::new, d -> {
            GameMain.getInstance().room().updateItem(d.item);
        });

        gc.register(Incoming.ROOM_CHAT, RoomChat::new, d -> {
            GameMain.getInstance().room().onChat(d);
        });
    }
}
