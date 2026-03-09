package com.remy.iso.networking.incoming.room;

import com.remy.iso.networking.Packet;
import com.remy.iso.networking.incoming.room.RoomItems.RoomItem;

public class UpdateItem extends Packet {
    public RoomItem item;

    public UpdateItem() {

    }

    @Override
    public void parse() {
        this.item = new RoomItem();
        item.id = readInt();
        item.x = readInt();
        item.y = readInt();
        item.z = readInt();
        item.rot = readInt();
        item.state = readInt();
        item.data = readString();

        item.name = readString();
        item.desc = readString();
        item.colour = readString();
        item.model = readString();
        item.interaction = readString();
    }
}
