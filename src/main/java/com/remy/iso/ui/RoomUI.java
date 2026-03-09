package com.remy.iso.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.remy.iso.GameMain;
import com.remy.iso.networking.GameClient;
import com.remy.iso.networking.outgoing.room.SendChat;
import com.remy.iso.ui.room.RoomChatUI;

public class RoomUI extends AUI {
    private XmlUI xmlUI;
    public NinePatchDrawable bubbleBg;

    public RoomChatUI chat;

    public RoomUI() {
        super();
        xmlUI = new XmlUI(2f, "ui/left");
        xmlUI.setActionContainer(this);
        Actor root = xmlUI.parse("ui/main.xml");
        stage.addActor(root);

        Actor chat = xmlUI.parse("ui/chatbar.xml");
        stage.addActor(chat);

        this.chat = new RoomChatUI(this);
        this.visible = true;
    }

    @Override
    public void render(float delta) {
        super.render(delta);
        if (chat != null)
            chat.update(delta);
    }

    public void onNavigator() {
        GameMain.getInstance().getUI()
                .get("navigator").toggle();
    }

    public void onChat(String msg, TextField field) {
        GameClient.getInstance().send(new SendChat(msg));
    }

    public Skin getSkin() {
        return xmlUI.getSkin();
    }
}