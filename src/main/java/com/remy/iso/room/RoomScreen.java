package com.remy.iso.room;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.remy.iso.GameMain;
import com.remy.iso.networking.GameClient;
import com.remy.iso.networking.incoming.room.PlayerMove;
import com.remy.iso.networking.incoming.room.PlayerState;
import com.remy.iso.networking.incoming.room.RoomChat;
import com.remy.iso.networking.incoming.room.RoomData;
import com.remy.iso.networking.incoming.room.RoomItems.RoomItem;
import com.remy.iso.networking.incoming.room.RoomPlayers;
import com.remy.iso.networking.outgoing.room.RequestInteract;
import com.remy.iso.networking.outgoing.room.RequestMove;
import com.remy.iso.ui.AUI;
import com.remy.iso.ui.RoomUI;
import com.remy.iso.utils.AssetLoader;

import net.mgsx.gltf.scene3d.attributes.PBRFloatAttribute;
import net.mgsx.gltf.scene3d.lights.DirectionalShadowLight;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.mgsx.gltf.scene3d.scene.SceneManager;

public class RoomScreen implements Screen {
    private SceneManager sceneManager;
    private SceneAsset playerAsset;
    private ModelBatch modelBatch;
    private Environment environment;
    private OrthographicCamera camera;

    private SceneAsset avatarAsset;

    private String[] model;
    private RoomLayout room;

    private Scene floorScene;
    private Scene stairScene;
    private Scene wallScene;

    private Vector3 pivot;
    private float yaw = 45f;
    private float pitch = 27f;
    private float distance = 15f;
    private static float VIEWPORT_SCALE = 0f;

    // Pan state
    private boolean isPanning = false;
    private final Vector3 panStart = new Vector3();
    private RoomData data;

    private Map<String, RoomAvatar> players = new HashMap<>();
    private Map<Integer, RoomFurniture> items = new HashMap<>();

    private int lastClickedItemId = -1;
    private long lastClickTime = 0;
    private static final long DOUBLE_CLICK_MS = 300;

    private ShapeRenderer shapeRenderer;
    private PickingRenderer picker;

    public RoomScreen(RoomData data) {
        this.model = data.model;
        this.data = data;
    }

    @Override
    public void show() {
        sceneManager = new SceneManager(70);

        shapeRenderer = new ShapeRenderer();

        VIEWPORT_SCALE = 50 * Gdx.graphics.getBackBufferScale();
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean scrolled(float amountX, float amountY) {
                VIEWPORT_SCALE = MathUtils.clamp(VIEWPORT_SCALE - amountY * 2f, 20f, 150f);
                float w = Gdx.graphics.getBackBufferWidth();
                float h = Gdx.graphics.getBackBufferHeight();
                camera.viewportWidth = w / VIEWPORT_SCALE;
                camera.viewportHeight = h / VIEWPORT_SCALE;
                camera.update();
                return true;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                // Forward to UI first
                for (AUI ui : GameMain.getInstance().getUI().values()) {
                    if (ui.visible && ui.stage.touchDown(screenX, screenY, pointer, button))
                        return true;
                }

                if (button == Input.Buttons.RIGHT) {
                    isPanning = true;
                    com.badlogic.gdx.math.collision.Ray ray = camera.getPickRay(screenX, screenY);
                    if (Math.abs(ray.direction.y) > 0.0001f) {
                        float t = -ray.origin.y / ray.direction.y;
                        panStart.set(
                                ray.origin.x + ray.direction.x * t,
                                0,
                                ray.origin.z + ray.direction.z * t);
                    }
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                for (AUI ui : GameMain.getInstance().getUI().values()) {
                    if (ui.visible)
                        ui.stage.touchUp(screenX, screenY, pointer, button);
                }
                if (button == Input.Buttons.RIGHT) {
                    isPanning = false;
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                for (AUI ui : GameMain.getInstance().getUI().values()) {
                    if (ui.visible)
                        ui.stage.touchDragged(screenX, screenY, pointer);
                }

                if (!isPanning)
                    return false;

                com.badlogic.gdx.math.collision.Ray ray = camera.getPickRay(screenX, screenY);
                if (Math.abs(ray.direction.y) < 0.0001f)
                    return false;

                float t = -ray.origin.y / ray.direction.y;
                Vector3 panCurrent = new Vector3(
                        ray.origin.x + ray.direction.x * t,
                        0,
                        ray.origin.z + ray.direction.z * t);

                pivot.x -= panCurrent.x - panStart.x;
                pivot.z -= panCurrent.z - panStart.z;
                updateCamera();
                return true;
            }

            @Override
            public boolean mouseMoved(int screenX, int screenY) {
                for (AUI ui : GameMain.getInstance().getUI().values()) {
                    if (ui.visible)
                        ui.stage.mouseMoved(screenX, screenY);
                }
                return false;
            }

            @Override
            public boolean keyDown(int keycode) {
                for (AUI ui : GameMain.getInstance().getUI().values()) {
                    if (ui.visible && ui.stage.keyDown(keycode))
                        return true;
                }
                return false;
            }

            @Override
            public boolean keyUp(int keycode) {
                for (AUI ui : GameMain.getInstance().getUI().values()) {
                    if (ui.visible && ui.stage.keyUp(keycode))
                        return true;
                }
                return false;
            }

            @Override
            public boolean keyTyped(char character) {
                for (AUI ui : GameMain.getInstance().getUI().values()) {
                    if (ui.visible && ui.stage.keyTyped(character))
                        return true;
                }
                return false;
            }

        });

        AssetLoader.getInstance().loadModel("avatar/character.glb", asset -> {
            avatarAsset = asset;
            System.out.println("ready");
        });

        float w = Gdx.graphics.getBackBufferWidth();
        float h = Gdx.graphics.getBackBufferHeight();
        camera = new OrthographicCamera(w / VIEWPORT_SCALE, h / VIEWPORT_SCALE);
        camera.near = -300f;
        camera.far = 300f;
        sceneManager.setCamera(camera);

        modelBatch = new ModelBatch();
        DirectionalShadowLight shadow = new DirectionalShadowLight(2048, 2048);
        shadow.set(Color.WHITE, -2.5f, -7f, -5f);
        shadow.intensity = 0.2f;
        sceneManager.environment.set(new PBRFloatAttribute(PBRFloatAttribute.ShadowBias, 1f / 512f));
        sceneManager.environment.add(shadow);
        sceneManager.environment.shadowMap = shadow;
        sceneManager.environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0f, 0f, 0f, 1f));

        // Generate room
        room = new RoomLayout();
        room.setFloor(data.floor);
        room.generateFromLayout(this.model);
        updateRoomScenes();
        room.createMarker();

        int rows = this.model.length;
        int cols = this.model[0].length();
        pivot = new Vector3(
                (cols * RoomLayout.TILE_SIZE) / 2f,
                0f,
                (rows * RoomLayout.TILE_SIZE) / 2f);

        Gdx.app.log("Memory", "Java heap: "
                + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024 + "MB");
        Gdx.app.log("Memory", "Textures: " + com.badlogic.gdx.graphics.Texture.getManagedStatus());

        picker = new PickingRenderer((int) w, (int) h);
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

        for (RoomAvatar avatar : this.players.values()) {
            avatar.update(delta);
        }

        sceneManager.update(delta);
        sceneManager.renderShadows();
        sceneManager.renderColors();

        modelBatch.begin(camera);
        if (room.markerInstance != null) {
            modelBatch.render(room.markerInstance, environment);
        }
        modelBatch.end();

        // shapeRenderer.setProjectionMatrix(camera.combined);
        // shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        // // shapeRenderer.setColor(Color.RED);
        // // for (RoomFurniture furniture : items.values()) {
        // // BoundingBox bounds = new BoundingBox();
        // // furniture.scene.modelInstance.calculateBoundingBox(bounds);
        // // bounds.mul(furniture.scene.modelInstance.transform);
        // // Vector3 min = bounds.min;
        // // Vector3 max = bounds.max;
        // // shapeRenderer.box(min.x, min.y, max.z, max.x - min.x, max.y - min.y, max.z
        // -
        // // min.z);
        // // }
        // shapeRenderer.end();
    }

    private boolean isHoveringUI() {
        for (AUI ui : GameMain.getInstance().getUI().values()) {
            if (!ui.visible)
                continue;
            Vector2 stageCoords = ui.stage.screenToStageCoordinates(
                    new Vector2(Gdx.input.getX(), Gdx.input.getY()));
            com.badlogic.gdx.scenes.scene2d.Actor hit = ui.stage.hit(stageCoords.x, stageCoords.y, true);
            if (hit == null)
                continue;
            if (hit.getTouchable() == com.badlogic.gdx.scenes.scene2d.Touchable.enabled)
                return true;
        }
        return false;
    }

    private void trackMouse() {
        com.badlogic.gdx.math.collision.Ray ray = camera.getPickRay(Gdx.input.getX(), Gdx.input.getY());

        float minT = Float.MAX_VALUE;
        int selectedGridX = -1;
        int selectedGridZ = -1;

        if (isHoveringUI()) {
            room.updateMarkerPosition(0, -9999f, 0);
            return;
        }

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
                        selectedGridX = x;
                        selectedGridZ = z;
                    }
                }
            }
        }

        // Update marker — hide if not on a tile
        if (minT < Float.MAX_VALUE) {
            float snappedX = (selectedGridX * RoomLayout.TILE_SIZE) + (RoomLayout.TILE_SIZE / 2f);
            float snappedZ = (selectedGridZ * RoomLayout.TILE_SIZE) + (RoomLayout.TILE_SIZE / 2f);
            float currentTileY = room.worldY(selectedGridX, selectedGridZ);
            room.updateMarkerPosition(snappedX, currentTileY + 0.01f, snappedZ);
            room.markerInstance.transform.getTranslation(new Vector3()); // visible
        } else {
            room.updateMarkerPosition(0, -9999f, 0); // hide below floor
        }

        // Handle click — furniture check happens regardless of tile
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && !isPanning) {
            RoomFurniture clicked = getFurnitureAt(Gdx.input.getX(), Gdx.input.getY());

            if (clicked != null) {
                long now = System.currentTimeMillis();
                if (clicked.id == lastClickedItemId && now - lastClickTime < DOUBLE_CLICK_MS) {
                    // Double click — interact
                    System.out.println("interact?" + lastClickedItemId);
                    GameClient.getInstance().send(new RequestInteract(lastClickedItemId));
                    lastClickedItemId = -1;
                } else {
                    // Single click on furniture — still move to that tile
                    if (minT < Float.MAX_VALUE) {
                        GameClient.getInstance().send(new RequestMove(selectedGridX, selectedGridZ));
                    }
                    lastClickedItemId = clicked.id;
                    lastClickTime = now;
                }
            } else if (minT < Float.MAX_VALUE) {
                // Clicked empty tile — move
                GameClient.getInstance().send(new RequestMove(selectedGridX, selectedGridZ));
                lastClickedItemId = -1;
            }
        }
    }

    private void handleInput() {
        boolean changed = false;

        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            yaw -= 2f;
            changed = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            yaw += 2f;
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
        if (floorScene != null)
            sceneManager.removeScene(floorScene);
        if (stairScene != null)
            sceneManager.removeScene(stairScene);
        if (wallScene != null)
            sceneManager.removeScene(wallScene);

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

    public void setItems(RoomItem[] items) {
        this.items.clear();
        for (RoomItem item : items) {
            this.items.put(item.id, new RoomFurniture(item, sceneManager));
        }
    }

    private RoomFurniture getFurnitureAt(int screenX, int screenY) {
        float scale = Gdx.graphics.getBackBufferScale();
        int id = picker.pick(
                (int) (screenX * scale),
                (int) (screenY * scale),
                camera, items);
        return id == -1 ? null : items.get(id);
    }

    public void updateItem(RoomItem item) {
        RoomFurniture furni = this.items.get(item.id);
        if (furni == null)
            return;
        furni.serverUpdate(item);
    }

    public void onChat(RoomChat chat) {
        RoomAvatar avatar = players.get(chat.userId);
        if (avatar == null)
            return;
        RoomUI ui = (RoomUI) GameMain.getInstance().getUI().get("room");

        Vector3 pos = avatar.pos();
        Vector2 screenPos = GameMain.getInstance().room().worldToScreen(pos);

        ui.chat.addMessage(avatar.player.name, chat.msg, screenPos.x);
    }

    public Vector2 worldToScreen(Vector3 worldPos) {
        Vector3 projected = camera.project(worldPos.cpy());
        return new Vector2(projected.x, Gdx.graphics.getHeight() - projected.y);
    }

    @Override
    public void resize(int width, int height) {
        float w = Gdx.graphics.getBackBufferWidth();
        float h = Gdx.graphics.getBackBufferHeight();
        camera.viewportWidth = w / VIEWPORT_SCALE;
        camera.viewportHeight = h / VIEWPORT_SCALE;
        camera.update();
        picker.resize((int) Gdx.graphics.getBackBufferWidth(), (int) Gdx.graphics.getBackBufferHeight());
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

        picker.dispose();
    }
}