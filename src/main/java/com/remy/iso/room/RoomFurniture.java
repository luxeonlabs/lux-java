package com.remy.iso.room;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.remy.iso.GameMain;
import com.remy.iso.networking.incoming.room.RoomItems.RoomItem;

import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute;
import net.mgsx.gltf.scene3d.lights.PointLightEx;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.mgsx.gltf.scene3d.scene.SceneManager;

public class RoomFurniture {
    public int id;
    public Scene scene;
    private SceneManager sceneManager;
    private SceneAsset asset;
    private List<PointLightEx> lights = new ArrayList<>();

    public RoomFurniture(RoomItem item, SceneManager sm) {
        asset = GameMain.getInstance().assets().get("furniture/" + item.model + ".glb", SceneAsset.class);
        this.sceneManager = sm;
        this.scene = new Scene(asset.scene);
        this.id = item.id;

        System.out.println(item.id);

        sceneManager.addScene(this.scene);

        float worldX = item.x * RoomLayout.TILE_SIZE + RoomLayout.TILE_SIZE / 2f;
        float worldY = item.z * RoomLayout.LEVEL_HEIGHT;
        float worldZ = item.y * RoomLayout.TILE_SIZE + RoomLayout.TILE_SIZE / 2f;

        scene.modelInstance.transform
                .setToRotation(Vector3.Y, MathUtils.radDeg * rotationIndexToRadians(0))
                .trn(worldX, worldY, worldZ);

        scene.modelInstance.calculateTransforms();

        String[] colours = item.colour.split(";");
        for (String colourEntry : colours) {
            String[] parts = colourEntry.split("-");
            if (parts.length != 2)
                continue;

            String materialName = parts[0].trim();
            String hex = parts[1].trim();
            hex = hex.startsWith("#") ? hex.substring(1) : hex;

            // Find material by name
            for (Material material : scene.modelInstance.materials) {
                if (material.id.equalsIgnoreCase(materialName)) {
                    material.set(PBRColorAttribute.createBaseColorFactor(Color.valueOf(hex)));
                    break;
                }
            }
        }

        for (Node node : scene.modelInstance.nodes) {
            findBulbNodes(node);
        }
    }

    private void findBulbNodes(Node node) {
        scene.modelInstance.calculateTransforms();
        if (node.id.startsWith("bulb")) {
            Matrix4 worldMatrix = new Matrix4(scene.modelInstance.transform).mul(node.globalTransform);
            Vector3 worldPos = worldMatrix.getTranslation(new Vector3());

            // Point light for illumination
            PointLightEx light = new PointLightEx();
            light.color.set(1f, 1f, 1f, 1f); // warm white
            light.intensity = 2f;
            light.position.set(worldPos);
            light.range = 0f;

            lights.add(light);
            sceneManager.environment.add(light);
        }

        if (node.hasChildren()) {
            for (Node child : node.getChildren()) {
                findBulbNodes(child);
            }
        }
    }

    public List<Vector3> getLightPositions() {
        List<Vector3> positions = new ArrayList<>();
        for (PointLightEx light : lights) {
            positions.add(new Vector3(light.position));
        }
        return positions;
    }

    private float rotationIndexToRadians(int index) {
        return MathUtils.degRad * (index * -45f);
    }

    public void dispose() {
        for (PointLightEx light : lights) {
            sceneManager.environment.remove(light);
        }
        lights.clear();
        sceneManager.removeScene(scene);
    }
}