package com.remy.iso;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public class DesktopLauncher {
    public static void main(String[] args) {
        String ssoToken = null;
        for (String arg : args) {
            if (arg.startsWith("game://")) {
                ssoToken = arg.replace("game://sso=", "");
            }
        }
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Isometric MMO");
        config.setWindowedMode(1280, 720);
        config.setForegroundFPS(60);

        new Lwjgl3Application(new GameMain(ssoToken), config);
    }
}