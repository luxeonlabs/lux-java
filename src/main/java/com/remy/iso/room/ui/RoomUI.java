package com.remy.iso.room.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.remy.iso.GameMain;

public class RoomUI {
    private final Stage stage;

    public RoomUI() {
        stage = new Stage(new ScreenViewport());

        Group avatarContainer = new Group();

        Image avatar = new Image(GameMain.getInstance().assets().get("ui/left/base.png", Texture.class));
        avatarContainer.addActor(avatar);
        avatarContainer.setSize(avatar.getWidth(), avatar.getHeight());
        avatarContainer.setPosition(10, 10);

        Skin skin = new Skin();
        skin.add("button", new TextureRegion(new Texture("ui/left/button.png")));
        skin.add("button_hover", new TextureRegion(new Texture("ui/left/hoverbutton.png")));
        skin.add("world_btn", new TextureRegion(new Texture("ui/left/worldbtn.png")));
        skin.add("world_btn_hover", new TextureRegion(new Texture("ui/left/worldbtnhover.png")));
        skin.load(Gdx.files.internal("ui/left/skin.json"));

        ImageButton btn = new ImageButton(skin);
        btn.setPosition(16, 26);

        ImageButton world = new ImageButton(skin, "world");
        world.setPosition(90, 11);

        ImageButton worldd = new ImageButton(skin, "world");
        worldd.setPosition(90 + 66, 32);

        avatarContainer.addActor(btn);
        avatarContainer.addActor(world);
        avatarContainer.addActor(worldd);

        stage.addActor(avatarContainer);
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