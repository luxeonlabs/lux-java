package com.remy.iso.room;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.remy.iso.networking.incoming.room.RoomItems.RoomItem;
import com.remy.iso.room.interactions.DefaultInteraction;
import com.remy.iso.room.interactions.IFurnitureInteraction;
import com.remy.iso.room.interactions.LightInteraction;
import com.remy.iso.utils.AssetLoader;

import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.mgsx.gltf.scene3d.scene.SceneManager;

public class RoomFurniture {
    public int id;
    public RoomItem item;
    public Scene scene;
    public SceneManager sceneManager;
    private SceneAsset asset;
    private IFurnitureInteraction interaction;

    public RoomFurniture(RoomItem item, SceneManager sm) {
        this.sceneManager = sm;
        this.id = item.id;
        this.item = item; // store item for later

        AssetLoader.getInstance().loadModel("generic/placeholder-furni.glb", asset -> {
            loadScene(asset);
        });
        AssetLoader.getInstance().loadModel("furniture/" + item.model + ".glb", asset -> {
            swapScene(asset);
        });
    }

    private void loadScene(SceneAsset asset) {
        if (this.scene != null) {
            sceneManager.removeScene(this.scene);
        }
        this.asset = asset;
        this.scene = new Scene(asset.scene);
        sceneManager.addScene(this.scene);

        applyTransform();
        applyColours();
    }

    private void swapScene(SceneAsset newAsset) {
        loadScene(newAsset);

        this.interaction = createInteraction(item.interaction);
        this.interaction.onLoad(this);
    }

    private void applyTransform() {
        float worldX = item.x * RoomLayout.TILE_SIZE + RoomLayout.TILE_SIZE / 2f;
        float worldY = item.z * RoomLayout.LEVEL_HEIGHT;
        float worldZ = item.y * RoomLayout.TILE_SIZE + RoomLayout.TILE_SIZE / 2f;

        scene.modelInstance.transform
                .setToRotation(Vector3.Y, MathUtils.radDeg * rotationIndexToRadians(0))
                .trn(worldX, worldY, worldZ);

        scene.modelInstance.calculateTransforms();
    }

    private void applyColours() {
        String[] colours = item.colour.split(";");
        for (String colourEntry : colours) {
            String[] parts = colourEntry.split("-");
            if (parts.length != 2)
                continue;

            String materialName = parts[0].trim();
            String hex = parts[1].trim();
            hex = hex.startsWith("#") ? hex.substring(1) : hex;

            for (Material material : scene.modelInstance.materials) {
                if (material.id.equalsIgnoreCase(materialName)) {
                    material.set(PBRColorAttribute.createBaseColorFactor(Color.valueOf(hex)));
                    break;
                }
            }
        }
    }

    private float rotationIndexToRadians(int index) {
        return MathUtils.degRad * (index * -45f);
    }

    public void serverUpdate(RoomItem item) {
        this.item = item;
        interaction.onStateChange();
    }

    private IFurnitureInteraction createInteraction(String type) {
        if (type == null)
            return new DefaultInteraction();
        switch (type) {
            case "light":
                return new LightInteraction();
            default:
                return new DefaultInteraction();
        }
    }

    public void dispose() {
        this.interaction.onDispose();
        sceneManager.removeScene(scene);
    }
}