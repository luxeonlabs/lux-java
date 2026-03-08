package com.remy.iso.room.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.remy.iso.ui.XmlUI;

public class RoomUI {
    private final Stage stage;

    public RoomUI() {
        ScreenViewport viewport = new ScreenViewport();
        viewport.setUnitsPerPixel(2f);

        stage = new Stage(viewport);

        XmlUI ui = new XmlUI(2f, "ui/left");
        ui.setActionContainer(this);
        Actor root = ui.parse("ui/main.xml");
        stage.addActor(root);
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

    public void onButtonClick() {
        System.out.println("clicked??");
    }

    public void dispose() {
        stage.dispose();
    }

    public Stage getStage() {
        return stage;
    }
}