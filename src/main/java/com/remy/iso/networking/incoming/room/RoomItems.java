package com.remy.iso.networking.incoming.room;

import com.remy.iso.networking.Packet;

public class RoomItems extends Packet {

    public RoomItem[] items;

    public RoomItems() {
    }

    public void parse() {
        this.items = readArray(RoomItem.class, () -> {
            RoomItem item = new RoomItem();

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

            return item;
        });
    }

    public class RoomItem {
        public int id;
        public int x;
        public int y;
        public int z;
        public int rot;
        public int state;
        public String data;

        public String name;
        public String desc;
        public String colour;
        public String model;
    }
}
