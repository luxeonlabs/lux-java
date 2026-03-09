package com.remy.iso.ui.room;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.remy.iso.ui.RoomUI;

public class RoomChatUI {

    private final Stage stage;
    private final Skin skin;
    private final RoomUI roomUI;

    public NinePatchDrawable bubbleBg;

    private final List<Table> messages = new ArrayList<>();
    private float removeTimer = 0f;
    private static final float REMOVE_INTERVAL = 2f;

    public RoomChatUI(RoomUI roomUI) {
        this.roomUI = roomUI;
        this.stage = roomUI.stage;
        this.skin = roomUI.getSkin();

        NinePatch patch = new NinePatch(
                new Texture(Gdx.files.internal("ui/room/chat.png")),
                15, 15, 15, 15);
        bubbleBg = new NinePatchDrawable(patch);

        // chatLog = new Table();
        // chatLog.bottom().left();
        // stage.addActor(chatLog);
    }

    public void addMessage(String username, String message, float x) {
        Table row = new Table();
        row.setBackground(bubbleBg);
        row.pad(4f, 8f, 4f, 8f);

        Label label = new Label(username + ": " + message, skin);
        label.setWrap(false); // no wrap, let it measure natural width

        float maxWidth = stage.getWidth() * 0.25f;
        float naturalWidth = label.getPrefWidth();
        float labelWidth = Math.min(naturalWidth, maxWidth);

        if (naturalWidth > maxWidth) {
            label.setWrap(true); // only wrap if too long
        }

        row.add(label).width(labelWidth);
        row.pack();

        // start at bottom
        float baseY = stage.getHeight() * 0.25f;
        row.setPosition((x * 2) - row.getWidth() / 2, stage.getHeight() - baseY);

        stage.addActor(row);
        for (int i = 0; i < messages.size(); i++) {
            Table existing = messages.get(i);
            existing.setY(existing.getY() + row.getHeight());
        }
        this.removeTimer = 0;
        messages.add(row);
    }

    public void update(float delta) {
        removeTimer += delta;
        if (removeTimer <= REMOVE_INTERVAL)
            return;

        float baseY = (stage.getHeight() * Gdx.graphics.getBackBufferScale()) / 2;

        for (int i = 0; i < messages.size(); i++) {
            Table row = messages.get(i);
            row.setY(row.getY() + 30);

            if (row.getY() > baseY) {
                messages.remove(row);
                row.remove();
                i--;
            }
        }

        if (removeTimer >= REMOVE_INTERVAL) {
            removeTimer = 0f;
        }
    }
}
