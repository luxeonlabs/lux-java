package com.remy.iso.room;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.remy.iso.GameMain;
import com.remy.iso.networking.incoming.room.RoomItems.RoomItem;

import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.mgsx.gltf.scene3d.scene.SceneManager;

public class RoomFurniture {
    public Scene scene;
    private SceneManager sceneManager;
    private SceneAsset asset;

    public RoomFurniture(RoomItem item, SceneManager sm) {
        asset = GameMain.getInstance().assets().get("furniture/" + item.model + ".glb", SceneAsset.class);
        this.sceneManager = sm;
        this.scene = new Scene(asset.scene);

        sceneManager.addScene(this.scene);

        float worldX = item.x * RoomLayout.TILE_SIZE + RoomLayout.TILE_SIZE / 2f;
        float worldY = item.z * RoomLayout.LEVEL_HEIGHT; // z is height in your RoomItem
        float worldZ = item.y * RoomLayout.TILE_SIZE + RoomLayout.TILE_SIZE / 2f;

        scene.modelInstance.transform
                .setToRotation(Vector3.Y, MathUtils.radDeg * rotationIndexToRadians(0))
                .trn(worldX, worldY, worldZ);

        String[] colours = item.colour.split(";");
        int i = 0;
        for (String colour : colours) {
            String hex = colour.startsWith("#") ? colour.substring(1) : colour;
            if (scene.modelInstance.materials.get(i) != null && !hex.toLowerCase().contains("ffff"))
                scene.modelInstance.materials.get(i).set(PBRColorAttribute.createBaseColorFactor(Color.valueOf(hex)));
            i++;
        }
    }

    private float rotationIndexToRadians(int index) {
        return MathUtils.degRad * (index * -45f);
    }
}
