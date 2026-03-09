package com.remy.iso.room.interactions;

import com.remy.iso.room.RoomFurniture;

public class DefaultInteraction implements IFurnitureInteraction {
    public RoomFurniture parent;

    @Override
    public void onLoad(RoomFurniture furni) {
        this.parent = furni;
    }

    @Override
    public void onStateChange() {
        // do nothing for static items
    }

    @Override
    public void onDispose() {

    }
}
