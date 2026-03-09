package com.remy.iso.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class AUI {
    public final Stage stage;

    public boolean visible = false;

    public AUI() {
        ScreenViewport viewport = new ScreenViewport();
        viewport.setUnitsPerPixel(2f);

        stage = new Stage(viewport);
    }

    public void setInputProcessor() {
        Gdx.input.setInputProcessor(stage);
    }

    public void render(float delta) {
        stage.act();
        if (visible) {
            stage.draw();
        }
    }

    public void toggle() {
        this.visible = !this.visible;
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
