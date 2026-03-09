package com.remy.iso;

import java.util.HashMap;
import java.util.Map;

import org.lwjgl.glfw.GLFW;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.remy.iso.networking.GameClient;
import com.remy.iso.networking.incoming.room.RoomData;
import com.remy.iso.room.RoomScreen;
import com.remy.iso.ui.AUI;
import com.remy.iso.ui.NavigatorUI;
import com.remy.iso.ui.RoomUI;
import com.remy.iso.utils.ConfigManager;

import net.mgsx.gltf.loaders.glb.GLBAssetLoader;
import net.mgsx.gltf.scene3d.scene.SceneAsset;

public class GameMain extends Game {
    private static GameMain instance;
    public String sso;
    public SpriteBatch batch;
    private GameClient client;
    private AssetManager assets;

    private RoomScreen room;

    private ConfigManager config;

    public static float DEMETALLIC = 0.4f;

    private Map<String, AUI> ui = new HashMap<>();

    public GameMain() {
    }

    public GameMain(String sso) {
        instance = this;
        this.sso = sso;
    }

    public static GameMain getInstance() {
        if (instance == null)
            instance = new GameMain();
        return instance;
    }

    @Override
    public void create() {
        GLFW.glfwSwapInterval(1);

        assets = new AssetManager();
        assets.setLoader(SceneAsset.class, ".glb", new GLBAssetLoader());
        loadAll(assets);
        assets.finishLoading();

        batch = new SpriteBatch();
        config = new ConfigManager();
        client = new GameClient();

        try {
            client.connect("localhost", 3000, this.sso);
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.ui.put("navigator", new NavigatorUI());
    }

    @Override
    public void render() {
        super.render();

        this.client.processCallbacks();

        float delta = Gdx.graphics.getDeltaTime();
        for (AUI ui : this.ui.values()) {
            ui.render(delta);
        }
    }

    public RoomScreen loadRoom(RoomData data) {
        room = new RoomScreen(data);
        setScreen(room);

        this.ui.put("room", new RoomUI());
        return room;
    }

    @Override
    public void dispose() {
        batch.dispose();
    }

    public GameClient client() {
        return this.client;
    }

    public AssetManager assets() {
        return this.assets;
    }

    public RoomScreen room() {
        return this.room;
    }

    public ConfigManager config() {
        return this.config;
    }

    public static void loadAll(AssetManager assets) {
        FileHandle root = Gdx.files.internal("assets");
        loadDir(assets, root, root.path());
    }

    private static void loadDir(AssetManager assets, FileHandle dir, String prefix) {
        for (FileHandle file : dir.list()) {
            if (file.isDirectory()) {
                loadDir(assets, file, prefix);
            } else {
                String path = file.path().substring(prefix.length() + 1);
                switch (file.extension()) {
                    case "png":
                    case "jpg":
                        assets.load(path, Texture.class);
                        break;
                    case "glb":
                        assets.load(path, SceneAsset.class);
                        break;
                }
            }
        }
    }

    public Map<String, AUI> getUI() {
        return this.ui;
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        for (AUI ui : this.ui.values()) {
            ui.resize(width, height);
        }
    }
}
