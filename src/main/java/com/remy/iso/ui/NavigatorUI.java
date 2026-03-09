package com.remy.iso.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;

public class NavigatorUI extends AUI {
    public NavigatorUI() {
        super();
        XmlUI ui = new XmlUI(2f, "ui/left");
        ui.setActionContainer(this);
        Actor root = ui.parse("ui/navi.xml");
        stage.addActor(root);

        setInputProcessor();
    }
}
