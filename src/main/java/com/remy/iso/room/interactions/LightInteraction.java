package com.remy.iso.room.interactions;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.remy.iso.room.RoomFurniture;

import net.mgsx.gltf.scene3d.lights.PointLightEx;

public class LightInteraction extends DefaultInteraction {
    private List<PointLightEx> lights = new ArrayList<>();

    @Override
    public void onLoad(RoomFurniture furni) {
        super.onLoad(furni);

        if (furni.item.state == 1)
            findLights();
    }

    @Override
    public void onStateChange() {
        boolean on = parent.item.state == 1;

        System.out.println("on state change " + on);

        if (on) {
            // Add lights if not already added
            if (lights.isEmpty())
                findLights();
            for (PointLightEx light : lights) {
                light.intensity = 2f;
            }
        } else {
            for (PointLightEx light : lights) {
                light.intensity = 0f;
            }
        }
    }

    @Override
    public void onDispose() {
        for (PointLightEx light : lights) {
            parent.sceneManager.environment.remove(light);
        }
        lights.clear();
    }

    private void findLights() {
        for (Node node : parent.scene.modelInstance.nodes) {
            findBulbNodes(node);
        }
    }

    private void findBulbNodes(Node node) {
        parent.scene.modelInstance.calculateTransforms();
        if (node.id.startsWith("bulb")) {
            Matrix4 worldMatrix = new Matrix4(parent.scene.modelInstance.transform).mul(node.globalTransform);
            Vector3 worldPos = worldMatrix.getTranslation(new Vector3());

            // Point light for illumination
            PointLightEx light = new PointLightEx();
            light.color.set(0.3f, 0.3f, 0.3f, 0.3f);
            light.position.set(worldPos);

            lights.add(light);
            parent.sceneManager.environment.add(light);
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
}
