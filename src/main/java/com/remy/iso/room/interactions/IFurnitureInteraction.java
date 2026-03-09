package com.remy.iso.room.interactions;

import com.remy.iso.room.RoomFurniture;

public interface IFurnitureInteraction {
    void onLoad(RoomFurniture furniture);

    void onStateChange();

    void onDispose();
}