package com.remy.iso.networking.handlers;

import com.remy.iso.GameMain;
import com.remy.iso.networking.GameClient;
import com.remy.iso.networking.Incoming;
import com.remy.iso.networking.incoming.generic.ClientConfig;
import com.remy.iso.networking.outgoing.auth.ClientReady;
import com.remy.iso.utils.AssetLoader;

public class AuthHandler {
    public AuthHandler() {
        GameClient gc = GameClient.getInstance();

        gc.register(Incoming.CLIENT_CONFIG, ClientConfig::new, d -> {
            GameMain.getInstance().config().setConfigs(d.configs);

            AssetLoader.getInstance().setBaseUrl(
                    GameMain.getInstance().config().getString("assets_base", ""));

            AssetLoader.getInstance().preload(GameMain.getInstance().config().getString("assets_preload", ""), () -> {
                System.out.println("complete or wtv");
                GameClient.getInstance().send(new ClientReady());
            });
        });
    }
}
