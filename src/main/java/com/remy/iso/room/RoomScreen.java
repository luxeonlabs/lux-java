package com.remy.iso.room;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.math.Vector3;
import com.remy.iso.GameMain;
import com.remy.iso.networking.GameClient;
import com.remy.iso.networking.incoming.PlayerMove;
import com.remy.iso.networking.incoming.PlayerState;
import com.remy.iso.networking.incoming.RoomData;
import com.remy.iso.networking.incoming.RoomPlayers;
import com.remy.iso.networking.outgoing.RequestMove;
import com.remy.iso.room.ui.RoomUI;

import net.mgsx.gltf.scene3d.lights.DirectionalShadowLight;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.mgsx.gltf.scene3d.scene.SceneManager;

public class RoomScreen implements Screen {
    private SceneManager sceneManager;
    private SceneAsset playerAsset;
    private Scene playerScene;
    private ModelBatch modelBatch;
    private Environment environment;
    private OrthographicCamera camera;

    private SceneAsset avatarAsset;

    private String[] model;
    private RoomLayout room;

    private Scene floorScene;
    private Scene stairScene;
    private Scene wallScene;

    // Pivot / orbit camera
    private Vector3 pivot;
    private float yaw = 45f;
    private float pitch = 30f;
    private float distance = 15f;
    private static final float VIEWPORT_SCALE = 100f;

    private RoomUI hud;

    private RoomData data;

    private Map<String, RoomAvatar> players = new HashMap<>();

    public RoomScreen(RoomData data) {
        this.model = data.model;
        this.data = data;
    }

    @Override
    public void show() {
        sceneManager = new SceneManager(70);

        hud = new RoomUI();
        hud.setInputProcessor();

        avatarAsset = GameMain.getInstance().assets().get("avatar/character.glb", SceneAsset.class);

        float w = Gdx.graphics.getBackBufferWidth();
        float h = Gdx.graphics.getBackBufferHeight();
        camera = new OrthographicCamera(w / VIEWPORT_SCALE, h / VIEWPORT_SCALE);
        camera.near = 0.1f;
        camera.far = 300f;
        sceneManager.setCamera(camera);

        modelBatch = new ModelBatch();
        DirectionalShadowLight shadow = new DirectionalShadowLight(2048, 2048);
        shadow.set(Color.WHITE, -2.5f, -7f, -5f);
        shadow.intensity = 1.5f;

        sceneManager.environment.add(shadow);

        sceneManager.environment.shadowMap = shadow;

        // Generate room
        room = new RoomLayout();
        room.setFloor(data.floor);
        room.generateFromLayout(this.model);
        updateRoomScenes();
        room.createMarker();

        // Centre pivot on room
        int rows = this.model.length;
        int cols = this.model[0].length();
        pivot = new Vector3(
                (cols * RoomLayout.TILE_SIZE) / 2f,
                0f,
                (rows * RoomLayout.TILE_SIZE) / 2f);

        updateCamera();
    }

    private void updateCamera() {
        float radYaw = (float) Math.toRadians(yaw);
        float radPitch = (float) Math.toRadians(pitch);

        camera.position.set(
                pivot.x + distance * (float) (Math.cos(radPitch) * Math.sin(radYaw)),
                pivot.y + distance * (float) (Math.sin(radPitch)),
                pivot.z + distance * (float) (Math.cos(radPitch) * Math.cos(radYaw)));

        camera.lookAt(pivot);
        camera.up.set(Vector3.Y);
        camera.update();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_BLEND);

        handleInput();
        trackMouse();

        sceneManager.update(delta);
        sceneManager.renderShadows();
        sceneManager.renderColors();

        for (RoomAvatar avatar : this.players.values()) {
            avatar.update(delta);
        }

        // ONLY render things NOT in the SceneManager here (like the UI-style marker)
        modelBatch.begin(camera);
        if (room.markerInstance != null) {
            modelBatch.render(room.markerInstance, environment);
        }
        modelBatch.end();

        hud.render(delta);
    }

    private void trackMouse() {
        com.badlogic.gdx.math.collision.Ray ray = camera.getPickRay(Gdx.input.getX(), Gdx.input.getY());

        float minT = Float.MAX_VALUE;
        float bestX = 0, bestZ = 0;
        int selectedGridX = -1;
        int selectedGridZ = -1;

        for (int z = 0; z < room.rows; z++) {
            for (int x = 0; x < room.cols; x++) {
                if (!room.isValid(x, z))
                    continue;

                float floorY = room.worldY(x, z);
                float tileX = x * RoomLayout.TILE_SIZE;
                float tileZ = z * RoomLayout.TILE_SIZE;
                float tileSize = RoomLayout.TILE_SIZE;

                if (Math.abs(ray.direction.y) < 0.0001f)
                    continue;
                float t = (floorY - ray.origin.y) / ray.direction.y;
                if (t < 0)
                    continue;

                float hitX = ray.origin.x + ray.direction.x * t;
                float hitZ = ray.origin.z + ray.direction.z * t;

                if (hitX >= tileX && hitX < tileX + tileSize &&
                        hitZ >= tileZ && hitZ < tileZ + tileSize) {
                    if (t < minT) {
                        minT = t;
                        bestX = hitX;
                        bestZ = hitZ;
                        // Store these for the click logic
                        selectedGridX = x;
                        selectedGridZ = z;
                    }
                }
            }
        }

        if (minT < Float.MAX_VALUE) {
            // --- HOVER / SNAP LOGIC ---
            float snappedX = (selectedGridX * RoomLayout.TILE_SIZE) + (RoomLayout.TILE_SIZE / 2f);
            float snappedZ = (selectedGridZ * RoomLayout.TILE_SIZE) + (RoomLayout.TILE_SIZE / 2f);
            float currentTileY = room.worldY(selectedGridX, selectedGridZ);

            room.updateMarkerPosition(snappedX, currentTileY + 0.01f, snappedZ);

            // --- CLICK LOGIC ---
            if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                System.out.println("Tile Clicked: [" + selectedGridX + ", " + selectedGridZ + "]");
                System.out.println("World Center: (" + snappedX + ", " + currentTileY + ", " + snappedZ + ")");

                // Example: Do something with the tile
                handleTileClick(selectedGridX, selectedGridZ, snappedX, snappedZ);
            }
        }
    }

    private void handleTileClick(int x, int z, float centerX, float centerZ) {
        GameClient.getInstance().send(new RequestMove(x, z));
    }

    private void handleInput() {
        boolean changed = false;

        if (Gdx.input.isKeyPressed(Input.Keys.EQUALS)) {
            camera.zoom -= 0.01f;
            changed = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.MINUS)) {
            camera.zoom += 0.01f;
            changed = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            yaw -= 1f;
            changed = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            yaw += 1f;
            changed = true;
        }

        if (changed) {
            updateCamera();
            room.setCameraYaw(yaw);
            room.generateFromLayout(this.model);
            updateRoomScenes();
        }
    }

    private void updateRoomScenes() {
        // 1. Remove old scenes from the SceneManager if they exist
        if (floorScene != null)
            sceneManager.removeScene(floorScene);
        if (stairScene != null)
            sceneManager.removeScene(stairScene);
        if (wallScene != null)
            sceneManager.removeScene(wallScene);

        // 2. Wrap the RoomLayout ModelInstances into GLTF Scenes
        // This allows them to use the PBR shader and interact with ShadowLight
        if (room.floorInstance != null) {
            floorScene = new Scene(room.floorInstance);
            sceneManager.addScene(floorScene);
        }

        if (room.stairInstance != null) {
            stairScene = new Scene(room.stairInstance);
            sceneManager.addScene(stairScene);
        }

        if (room.wallInstance != null) {
            wallScene = new Scene(room.wallInstance);
            sceneManager.addScene(wallScene);
        }
    }

    public void addPlayer(RoomPlayers.RoomPlayer player) {
        this.players.put(player.id, new RoomAvatar(player, avatarAsset, sceneManager));
    }

    public void movePlayer(PlayerMove data) {
        RoomAvatar avatar = this.players.get(data.id);

        if (avatar == null)
            return;

        avatar.serverMoveTo(data.x, data.y, data.z, data.rotation);
    }

    public void setPlayerState(PlayerState data) {
        RoomAvatar avatar = this.players.get(data.id);

        if (avatar == null)
            return;

        avatar.setState(RoomAvatar.stateFromInt(data.state));
    }

    @Override
    public void resize(int width, int height) {
        float w = Gdx.graphics.getBackBufferWidth();
        float h = Gdx.graphics.getBackBufferHeight();
        camera.viewportWidth = w / VIEWPORT_SCALE;
        camera.viewportHeight = h / VIEWPORT_SCALE;
        camera.update();

        hud.resize(width, height);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        sceneManager.dispose();
        if (room != null)
            room.dispose();
        if (playerAsset != null)
            playerAsset.dispose();
    }
}