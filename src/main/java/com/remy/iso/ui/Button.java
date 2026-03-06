package com.remy.iso.ui;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.remy.iso.GameMain;

public class Button extends ImageButton {
    public Button(String normal, String hover) {
        Texture normalButton = GameMain.getInstance().assets().get(normal, Texture.class);
        Texture hoverButton = GameMain.getInstance().assets().get(hover, Texture.class);

        super(new TextureRegionDrawable(normalButton), new TextureRegionDrawable(hoverButton));
    }
}
