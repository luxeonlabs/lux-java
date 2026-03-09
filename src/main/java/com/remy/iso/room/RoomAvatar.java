package com.remy.iso.room;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.remy.iso.networking.incoming.room.RoomPlayers.RoomPlayer;

import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.mgsx.gltf.scene3d.scene.SceneManager;

public class RoomAvatar {

    public enum State {
        IDLE, WALKING, SITTING
    }

    private static final float TILE_MOVE_DURATION = 0.5f;
    private static final float ROTATION_SPEED = 7f;

    public Scene scene;
    private SceneManager sceneManager;

    public RoomPlayer player;

    // Movement
    private Vector3 moveStart = new Vector3();
    private Vector3 moveTarget = new Vector3();
    private float moveTimer = 0f;
    private boolean moving = false;
    private boolean pendingIdle = false;

    // Rotation
    private float currentRotationY = 0f;
    private float targetRotationY = 0f;

    // State / animation
    private State state = State.IDLE;
    private String currentAnim = "";

    // Clothing
    private final Map<String, Scene> clothingLayers = new HashMap<>();

    public RoomAvatar(RoomPlayer player, SceneAsset asset, SceneManager sm) {
        this.sceneManager = sm;
        this.scene = new Scene(asset.scene);
        sceneManager.addScene(this.scene);

        this.player = player;

        setPosition(player.x, player.y, player.z);
        playAnim("idle");
    }

    // ── Update (call from RoomScreen.render) ──────────────────────────────────
    private Vector3 currentPosition = new Vector3();

    public void update(float delta) {
        // Smooth rotation
        currentRotationY = lerpAngle(currentRotationY, targetRotationY, ROTATION_SPEED * delta);

        // Smooth movement
        if (moving) {
            moveTimer += delta;
            float t = Math.min(moveTimer / TILE_MOVE_DURATION, 1f);
            currentPosition.set(moveStart).lerp(moveTarget, t);

            if (t >= 1f) {
                moving = false;
                currentPosition.set(moveTarget);
                if (pendingIdle) {
                    pendingIdle = false;
                    playAnim("idle");
                }
            }
        }

        // Apply position + rotation together
        scene.modelInstance.transform
                .setToRotation(Vector3.Y, MathUtils.radDeg * currentRotationY)
                .setTranslation(currentPosition);

        syncClothing();
    }

    // ── Movement ──────────────────────────────────────────────────────────────
    public void serverMoveTo(int tileX, int tileZ, float height, int rotationIndex) {
        targetRotationY = rotationIndexToRadians(rotationIndex);

        moveStart.set(currentPosition);
        moveTarget.set(tileToWorld(tileX, tileZ, height));
        moveTimer = 0f;
        moving = true;

        setState(State.WALKING);
    }

    public void setPosition(float x, float y, float z) {
        currentPosition.set(tileToWorld((int) x, (int) y, (int) z));
    }

    private void applyPosition(Vector3 pos) {
        currentPosition.set(pos);
    }

    private Vector3 tileToWorld(int tileX, int tileZ, float height) {
        return new Vector3(
                tileX * RoomLayout.TILE_SIZE + RoomLayout.TILE_SIZE / 2f,
                height * RoomLayout.LEVEL_HEIGHT,
                tileZ * RoomLayout.TILE_SIZE + RoomLayout.TILE_SIZE / 2f);
    }

    // ── State ─────────────────────────────────────────────────────────────────
    public void setState(State newState) {
        if (state == newState)
            return;
        state = newState;
        switch (state) {
            case IDLE:
                pendingIdle = true;
                break;
            case WALKING:
                pendingIdle = false;
                playAnim("walk");
                break;
            case SITTING:
                playAnim("sitting");
                break;
        }
    }

    public void setRotation(int rotationIndex) {
        targetRotationY = rotationIndexToRadians(rotationIndex);
    }

    // ── Animation ─────────────────────────────────────────────────────────────
    public void playAnim(String animName) {
        if (animName.equals(currentAnim))
            return;
        if (scene.animationController == null)
            return;
        currentAnim = animName;
        scene.animationController.animate(animName, -1, 1f, null, 0.2f);

        for (Scene clothing : clothingLayers.values()) {
            if (clothing.animationController != null)
                clothing.animationController.animate(animName, -1, 1f, null, 0.2f);
        }
    }

    // ── Clothing ──────────────────────────────────────────────────────────────
    public void setClothing(String id, SceneAsset asset) {
        removeClothing(id);
        Scene clothingScene = new Scene(asset.scene);
        sceneManager.addScene(clothingScene);
        clothingLayers.put(id, clothingScene);

        // Sync current animation and transform immediately
        if (clothingScene.animationController != null && !currentAnim.isEmpty())
            clothingScene.animationController.animate(currentAnim, -1, 1f, null, 0f);
        syncClothing();
    }

    public void removeClothing(String id) {
        Scene existing = clothingLayers.remove(id);
        if (existing != null)
            sceneManager.removeScene(existing);
    }

    private void syncClothing() {
        for (Scene clothing : clothingLayers.values()) {
            clothing.modelInstance.transform.set(scene.modelInstance.transform);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private float rotationIndexToRadians(int index) {
        return MathUtils.degRad * (index * 45f);
    }

    private float lerpAngle(float from, float to, float t) {
        float diff = ((to - from + MathUtils.PI) % MathUtils.PI2) - MathUtils.PI;
        return from + diff * Math.min(t, 1f);
    }

    public void say(String message) {
        // chatMessage = message;
        // chatTimer = 5f; // show for 5 seconds
    }

    public Vector3 pos() {
        return scene.modelInstance.transform.getTranslation(new Vector3());
    }

    public void dispose() {
        sceneManager.removeScene(this.scene);
        for (Scene s : clothingLayers.values())
            sceneManager.removeScene(s);
        clothingLayers.clear();
    }

    public static State stateFromInt(int i) {
        switch (i) {
            case 1:
                return State.WALKING;
            case 2:
                return State.SITTING;
            default:
                return State.IDLE;
        }
    }
}