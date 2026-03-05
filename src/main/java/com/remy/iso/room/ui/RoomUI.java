package com.remy.iso.room.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.remy.iso.GameMain;

public class RoomUI {
    private final Stage stage;

    public RoomUI() {
        stage = new Stage(new ScreenViewport());

        Image avatar = new Image(GameMain.getInstance().assets().get("ui/left/base.png", Texture.class));
        avatar.setSize(avatar.getWidth(), avatar.getHeight());
        avatar.setPosition(0, 0);

        Image leftbtn = new Image(GameMain.getInstance().assets().get("ui/left/left.png", Texture.class));
        leftbtn.setPosition(30, 30);

        leftbtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                System.out.println("test");
            }
        });
        stage.addActor(avatar);
        stage.addActor(leftbtn);
    }

    public void setInputProcessor() {
        Gdx.input.setInputProcessor(stage);
    }

    public void render(float delta) {
        stage.act(delta);
        stage.draw();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    public void dispose() {
        stage.dispose();
    }
}