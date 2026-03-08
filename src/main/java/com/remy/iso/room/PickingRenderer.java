package com.remy.iso.room;

import java.nio.ByteBuffer;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.g3d.utils.ShaderProvider;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public class PickingRenderer {

    private static final String VERT = "attribute vec4 a_position;\n" +
            "uniform mat4 u_projTrans;\n" +
            "uniform mat4 u_worldTrans;\n" +
            "void main() {\n" +
            "    gl_Position = u_projTrans * u_worldTrans * a_position;\n" +
            "}\n";

    private static final String FRAG = "#ifdef GL_ES\n" +
            "precision highp float;\n" +
            "#endif\n" +
            "uniform vec3 u_pickColor;\n" +
            "void main() {\n" +
            "    gl_FragColor = vec4(u_pickColor, 1.0);\n" +
            "}\n";

    private final ShaderProgram shader;
    private FrameBuffer fbo;
    private final ModelBatch pickBatch;
    private int width, height;

    // Set before each item render, read inside FlatShader.render()
    private float pickR, pickG, pickB;

    public PickingRenderer(int width, int height) {
        this.width = width;
        this.height = height;

        ShaderProgram.pedantic = false;
        shader = new ShaderProgram(VERT, FRAG);
        if (!shader.isCompiled()) {
            Gdx.app.error("PickingRenderer", shader.getLog());
        }

        fbo = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, true);

        pickBatch = new ModelBatch(new ShaderProvider() {
            private final FlatShader flatShader = new FlatShader();

            @Override
            public Shader getShader(Renderable renderable) {
                return flatShader;
            }

            @Override
            public void dispose() {
                flatShader.dispose();
            }
        });
    }

    /**
     * Renders all furniture with unique ID-encoded colors and reads the pixel
     * at the given screen coordinates. Returns the item id or -1 if nothing hit.
     */
    public int pick(int screenX, int screenY, OrthographicCamera camera, Map<Integer, RoomFurniture> items) {
        fbo.begin();
        Gdx.gl.glViewport(0, 0, width, height);
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);

        pickBatch.begin(camera);
        for (Map.Entry<Integer, RoomFurniture> entry : items.entrySet()) {
            int id = entry.getKey();
            int encoded = id;
            pickR = (encoded & 0xFF) / 255f;
            pickG = ((encoded >> 8) & 0xFF) / 255f;
            pickB = ((encoded >> 16) & 0xFF) / 255f;
            pickBatch.render(entry.getValue().scene.modelInstance);
            pickBatch.flush();
        }
        pickBatch.end();

        // Read pixel — FBO is bottom-up so flip Y
        ByteBuffer pixels = ByteBuffer.allocateDirect(4);
        Gdx.gl.glReadPixels(screenX, height - screenY, 1, 1,
                GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, pixels);

        fbo.end();

        int r = pixels.get(0) & 0xFF;
        int g = pixels.get(1) & 0xFF;
        int b = pixels.get(2) & 0xFF;
        Gdx.app.log("Picking", "read back r=" + r + " g=" + g + " b=" + b);

        int pickedId = (r | (g << 8) | (b << 16));
        return pickedId == 0 ? -1 : pickedId;
    }

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        fbo.dispose();
        fbo = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, true);
    }

    public void dispose() {
        fbo.dispose();
        shader.dispose();
        pickBatch.dispose();
    }

    // ── Flat color shader ─────────────────────────────────────────────────────

    private class FlatShader implements Shader {
        @Override
        public void init() {
        }

        @Override
        public int compareTo(Shader other) {
            return 0;
        }

        @Override
        public boolean canRender(Renderable instance) {
            return true;
        }

        @Override
        public void begin(Camera camera, RenderContext context) {
            if (Gdx.gl30 != null)
                Gdx.gl30.glBindVertexArray(0);
            Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
            Gdx.gl.glDepthFunc(GL20.GL_LESS);
            Gdx.gl.glDepthMask(true);
            shader.bind();
            shader.setUniformMatrix("u_projTrans", camera.combined);
        }

        @Override
        public void render(Renderable renderable) {
            shader.setUniformMatrix("u_worldTrans", renderable.worldTransform);
            shader.setUniformf("u_pickColor", pickR, pickG, pickB);
            renderable.meshPart.render(shader, true);
        }

        @Override
        public void end() {
        }

        @Override
        public void dispose() {
        }
    }
}