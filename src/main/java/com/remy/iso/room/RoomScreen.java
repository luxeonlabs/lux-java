package com.remy.iso.room;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.remy.iso.GameMain;
import com.remy.iso.networking.GameClient;
import com.remy.iso.networking.incoming.room.PlayerMove;
import com.remy.iso.networking.incoming.room.PlayerState;
import com.remy.iso.networking.incoming.room.RoomData;
import com.remy.iso.networking.incoming.room.RoomItems.RoomItem;
import com.remy.iso.networking.incoming.room.RoomPlayers;
import com.remy.iso.networking.outgoing.RequestMove;
import com.remy.iso.room.ui.RoomUI;

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

    // Zoom limits
    private static final float MIN_ZOOM = 0.3f;
    private static final float MAX_ZOOM = 3.0f;

    private RoomUI hud;
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

        hud = new RoomUI();

        // Input multiplexer — UI first, then game input
        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(hud.getStage());
        multiplexer.addProcessor(new InputAdapter() {
            @Override
            public boolean scrolled(float amountX, float amountY) {
                camera.zoom = MathUtils.clamp(camera.zoom + amountY * 0.1f, MIN_ZOOM, MAX_ZOOM);
                return true;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (button == Input.Buttons.RIGHT) {
                    isPanning = true;
                    // Unproject click onto the floor plane (y=0)
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
                if (button == Input.Buttons.RIGHT) {
                    isPanning = false;
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                if (!isPanning)
                    return false;

                // Unproject current mouse onto the floor plane (y=0)
                com.badlogic.gdx.math.collision.Ray ray = camera.getPickRay(screenX, screenY);
                if (Math.abs(ray.direction.y) < 0.0001f)
                    return false;

                float t = -ray.origin.y / ray.direction.y;
                Vector3 panCurrent = new Vector3(
                        ray.origin.x + ray.direction.x * t,
                        0,
                        ray.origin.z + ray.direction.z * t);

                // Move pivot so the grabbed world point stays under the mouse
                pivot.x -= panCurrent.x - panStart.x;
                pivot.z -= panCurrent.z - panStart.z;
                updateCamera();
                return true;
            }
        });
        Gdx.input.setInputProcessor(multiplexer);

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
        shadow.intensity = 0.2f;
        sceneManager.environment.set(new PBRFloatAttribute(PBRFloatAttribute.ShadowBias, 1f / 2048f));
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

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.RED);
        for (RoomFurniture furniture : items.values()) {
            BoundingBox bounds = new BoundingBox();
            furniture.scene.modelInstance.calculateBoundingBox(bounds);
            bounds.mul(furniture.scene.modelInstance.transform);
            Vector3 min = bounds.min;
            Vector3 max = bounds.max;
            shapeRenderer.box(min.x, min.y, max.z, max.x - min.x, max.y - min.y, max.z - min.z);
        }
        shapeRenderer.end();

        hud.render(delta);
    }

    private void trackMouse() {
        com.badlogic.gdx.math.collision.Ray ray = camera.getPickRay(Gdx.input.getX(), Gdx.input.getY());

        float minT = Float.MAX_VALUE;
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

    @Override
    public void resize(int width, int height) {
        float w = Gdx.graphics.getBackBufferWidth();
        float h = Gdx.graphics.getBackBufferHeight();
        camera.viewportWidth = w / VIEWPORT_SCALE;
        camera.viewportHeight = h / VIEWPORT_SCALE;
        camera.update();
        hud.resize(width, height);
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