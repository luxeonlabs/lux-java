package com.remy.iso.room;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.remy.iso.GameMain;

import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRFloatAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;

public class RoomLayout implements Disposable {

    public static final float TILE_SIZE = 1.5f;
    public static final float LEVEL_HEIGHT = 1.0f;
    public static final int STAIR_STEPS = 4;
    public static final float FLOOR_DEPTH = LEVEL_HEIGHT / STAIR_STEPS;
    public static final float MARKER_SIZE = TILE_SIZE * 0.5f;
    public static final float MARKER_OFFSET = 0.01f;

    public ModelInstance floorInstance;
    public ModelInstance stairInstance;
    public ModelInstance wallInstance;
    public ModelInstance markerInstance;

    private Model floorModel;
    private Model stairModel;
    private Model wallModel;
    private Model markerModel;

    private Material floorMat;

    public int[][] hMap;
    public int rows, cols;

    private static final float DEFAULT_TEX_SCALE = 1.0f;
    private final java.util.Map<String, Float> texScales = new java.util.HashMap<>();

    // Initialize scales in your constructor or a setup method
    {
        texScales.put("wood", 3.0f); // Wood texture is large/zoomed in
        texScales.put("tile", 0.5f); // Tiles are small/repeated many times
        texScales.put("carpet", 1.2f);
    }

    private float currentTexScale = DEFAULT_TEX_SCALE;

    private static class Geo {
        final List<Float> verts = new ArrayList<>();
        final List<Short> indices = new ArrayList<>();
        short next = 0;

        // Add u and v to the parameters
        void quad(float x0, float y0, float z0, float x1, float y1, float z1,
                float x2, float y2, float z2, float x3, float y3, float z3,
                float nx, float ny, float nz, float u, float v, float u2, float v2) {

            // Passing specific UVs for each corner of the quad
            short a = p(x0, y0, z0, nx, ny, nz, u, v);
            short b = p(x1, y1, z1, nx, ny, nz, u, v2);
            short c = p(x2, y2, z2, nx, ny, nz, u2, v2);
            short d = p(x3, y3, z3, nx, ny, nz, u2, v);

            indices.add(a);
            indices.add(b);
            indices.add(c);
            indices.add(a);
            indices.add(c);
            indices.add(d);
        }

        short p(float x, float y, float z, float nx, float ny, float nz, float u, float v) {
            verts.add(x);
            verts.add(y);
            verts.add(z); // Position
            verts.add(nx);
            verts.add(ny);
            verts.add(nz); // Normal
            verts.add(u);
            verts.add(v); // UV Coordinates
            return next++;
        }

        Model build(Material mat) {
            float[] va = new float[verts.size()];
            for (int i = 0; i < va.length; i++)
                va[i] = verts.get(i);
            short[] ia = new short[indices.size()];
            for (int i = 0; i < ia.length; i++)
                ia[i] = indices.get(i);

            // ADDED TextureCoordinates attribute here
            Mesh m = new Mesh(true, next, ia.length,
                    new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
                    new VertexAttribute(VertexAttributes.Usage.Normal, 3, "a_normal"),
                    new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord0"));

            m.setVertices(va);
            m.setIndices(ia);
            ModelBuilder mb = new ModelBuilder();
            mb.begin();
            mb.part("p", m, GL20.GL_TRIANGLES, mat);
            return mb.end();
        }
    }

    void faceUp(Geo g, float x0, float x1, float z0, float z1, float y) {
        g.quad(x0, y, z0, x0, y, z1, x1, y, z1, x1, y, z0, 0, 1, 0, x0 / currentTexScale, z0 / currentTexScale,
                x1 / currentTexScale, z1 / currentTexScale);
    }

    void faceNorth(Geo g, float x0, float x1, float yB, float yT, float wz) {
        g.quad(x1, yB, wz, x0, yB, wz, x0, yT, wz, x1, yT, wz, 0, 0, -1, x1 / currentTexScale, yB / currentTexScale,
                x0 / currentTexScale, yT / currentTexScale);
    }

    void faceSouth(Geo g, float x0, float x1, float yB, float yT, float wz) {
        g.quad(x0, yB, wz, x1, yB, wz, x1, yT, wz, x0, yT, wz, 0, 0, 1, x0 / currentTexScale, yB / currentTexScale,
                x1 / currentTexScale, yT / currentTexScale);
    }

    void faceWest(Geo g, float z0, float z1, float yB, float yT, float wx) {
        g.quad(wx, yB, z0, wx, yB, z1, wx, yT, z1, wx, yT, z0, -1, 0, 0, z0 / currentTexScale, yB / currentTexScale,
                z1 / currentTexScale, yT / currentTexScale);
    }

    void faceEast(Geo g, float z0, float z1, float yB, float yT, float wx) {
        g.quad(wx, yB, z1, wx, yB, z0, wx, yT, z0, wx, yT, z1, 1, 0, 0, z1 / currentTexScale, yB / currentTexScale,
                z0 / currentTexScale, yT / currentTexScale);
    }

    public void generateFromLayout(String[] layout) {
        disposeModels();

        rows = layout.length;
        cols = 0;
        for (String r : layout)
            cols = Math.max(cols, r.length());
        hMap = new int[rows][cols];
        for (int z = 0; z < rows; z++) {
            String row = layout[z];
            for (int x = 0; x < cols; x++) {
                char ch = x < row.length() ? row.charAt(x) : 'x';
                hMap[z][x] = Character.isDigit(ch) ? (ch - '0') : -1;
            }
        }
        Geo floor = new Geo(), stair = new Geo(), wall = new Geo();
        buildFloor(floor, stair);
        buildOuterWalls(wall);

        Material wallMat = new Material(PBRColorAttribute.createBaseColorFactor(Color.WHITE));
        wallMat.set(PBRFloatAttribute.createMetallic(GameMain.DEMETALLIC));
        floorModel = floor.build(floorMat);
        stairModel = stair.build(floorMat);
        wallModel = wall.build(wallMat);
        floorInstance = new ModelInstance(floorModel);
        stairInstance = new ModelInstance(stairModel);
        wallInstance = new ModelInstance(wallModel);
    }

    private void disposeModels() {
        if (floorModel != null) {
            floorModel.dispose();
            floorModel = null;
        }
        if (stairModel != null) {
            stairModel.dispose();
            stairModel = null;
        }
        if (wallModel != null) {
            wallModel.dispose();
            wallModel = null;
        }
        floorInstance = stairInstance = wallInstance = null;
    }

    public void setFloor(String data) {
        String[] parts = data.split(";");
        String texturePath = parts.length > 0 ? parts[0].trim() : null;
        String colourHex = parts.length > 1 ? parts[1].trim() : null;

        // Initialize with a default PBR Color (BaseColor)
        floorMat = new Material(PBRColorAttribute.createBaseColorFactor(Color.WHITE));

        if (colourHex != null && !colourHex.isEmpty()) {
            String hex = colourHex.startsWith("#") ? colourHex.substring(1) : colourHex;
            floorMat.set(PBRColorAttribute.createBaseColorFactor(Color.valueOf(hex)));
            floorMat.remove(ColorAttribute.Specular);
        }

        if (texturePath != null && !texturePath.isEmpty()) {
            currentTexScale = texScales.getOrDefault(texturePath, DEFAULT_TEX_SCALE);
            Texture tex = GameMain.getInstance().assets().get("floor/" + texturePath + ".png", Texture.class);
            tex.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);

            floorMat.set(PBRTextureAttribute.createBaseColorTexture(tex));
        }

        floorMat.set(PBRFloatAttribute.createMetallic(GameMain.DEMETALLIC));
    }

    private void buildFloor(Geo floor, Geo stair) {
        boolean[][] merged = new boolean[rows][cols];

        // PASS 1 — greedy merged top faces
        for (int z = 0; z < rows; z++) {
            for (int x = 0; x < cols; x++) {
                if (merged[z][x])
                    continue;
                int h = hMap[z][x];
                if (h < 0)
                    continue;
                if (getStairType(z, x) != null)
                    continue;

                int width = 1;
                while (x + width < cols && !merged[z][x + width] &&
                        hMap[z][x + width] == h && getStairType(z, x + width) == null)
                    width++;

                int height = 1;
                outer: while (z + height < rows) {
                    for (int k = 0; k < width; k++) {
                        if (merged[z + height][x + k] || hMap[z + height][x + k] != h ||
                                getStairType(z + height, x + k) != null)
                            break outer;
                    }
                    height++;
                }

                faceUp(floor, x * TILE_SIZE, (x + width) * TILE_SIZE,
                        z * TILE_SIZE, (z + height) * TILE_SIZE,
                        h * LEVEL_HEIGHT);

                for (int dz = 0; dz < height; dz++)
                    for (int dx = 0; dx < width; dx++)
                        merged[z + dz][x + dx] = true;
            }
        }

        // PASS 2 — side faces and stairs
        for (int z = 0; z < rows; z++) {
            for (int x = 0; x < cols; x++) {
                int h = hMap[z][x];
                if (h < 0)
                    continue;

                float x0 = x * TILE_SIZE, x1 = x0 + TILE_SIZE;
                float z0 = z * TILE_SIZE, z1 = z0 + TILE_SIZE;
                float y = h * LEVEL_HEIGHT;
                int hn = n(z - 1, x), hs = n(z + 1, x), hw = n(z, x - 1), he = n(z, x + 1);

                String stairType = getStairType(z, x);

                if (stairType == null) {
                    if (hn < h)
                        faceNorth(floor, x0, x1, y - FLOOR_DEPTH, y, z0);
                    if (hs < h)
                        faceSouth(floor, x0, x1, y - FLOOR_DEPTH, y, z1);
                    if (hw < h)
                        faceWest(floor, z0, z1, y - FLOOR_DEPTH, y, x0);
                    if (he < h)
                        faceEast(floor, z0, z1, y - FLOOR_DEPTH, y, x1);
                } else {
                    if (stairType.startsWith("inner"))
                        buildInnerCornerStairs(stair, x0, x1, z0, z1, y, stairType);
                    else if (stairType.startsWith("outer"))
                        buildOuterCornerStairs(stair, x0, x1, z0, z1, y, stairType);
                    else
                        buildStairs(stair, x0, x1, z0, z1, y, stairType);
                }
            }
        }
    }

    public String getStairType(int z, int x) {
        int h = n(z, x);
        if (h < 0)
            return null;
        int north = n(z - 1, x), south = n(z + 1, x), west = n(z, x - 1), east = n(z, x + 1);
        int nw = n(z - 1, x - 1), ne = n(z - 1, x + 1), sw = n(z + 1, x - 1), se = n(z + 1, x + 1);

        if (north == h + 1 && west == h + 1)
            return "inner_nw";
        if (north == h + 1 && east == h + 1)
            return "inner_ne";
        if (south == h + 1 && west == h + 1)
            return "inner_sw";
        if (south == h + 1 && east == h + 1)
            return "inner_se";
        if (nw == h + 1 && north == h && west == h)
            return "outer_nw";
        if (ne == h + 1 && north == h && east == h)
            return "outer_ne";
        if (sw == h + 1 && south == h && west == h)
            return "outer_sw";
        if (se == h + 1 && south == h && east == h)
            return "outer_se";
        if (north == h + 1)
            return "north";
        if (south == h + 1)
            return "south";
        if (west == h + 1)
            return "west";
        if (east == h + 1)
            return "east";
        return null;
    }

    private void buildStairs(Geo g, float wx0, float wx1, float wz0, float wz1, float yBase, String dir) {
        float sh = LEVEL_HEIGHT / STAIR_STEPS;
        float sd = TILE_SIZE / STAIR_STEPS;
        for (int i = 0; i < STAIR_STEPS; i++) {
            float yB = yBase + i * sh, yT = yBase + (i + 1) * sh;
            float x0, x1, z0, z1;
            switch (dir) {
                case "north": {
                    x0 = wx0;
                    x1 = wx1;
                    z1 = wz1 - i * sd;
                    z0 = wz1 - (i + 1) * sd;
                    faceUp(g, x0, x1, z0, z1, yT);
                    faceSouth(g, x0, x1, yB, yT, z1);
                    faceWest(g, z0, z1, yB, yT, x0);
                    faceEast(g, z0, z1, yB, yT, x1);
                    break;
                }
                case "south": {
                    x0 = wx0;
                    x1 = wx1;
                    z0 = wz0 + i * sd;
                    z1 = wz0 + (i + 1) * sd;
                    faceUp(g, x0, x1, z0, z1, yT);
                    faceNorth(g, x0, x1, yB, yT, z1);
                    faceWest(g, z0, z1, yB, yT, x0);
                    faceEast(g, z0, z1, yB, yT, x1);
                    break;
                }
                case "west": {
                    z0 = wz0;
                    z1 = wz1;
                    x1 = wx1 - i * sd;
                    x0 = wx1 - (i + 1) * sd;
                    faceUp(g, x0, x1, z0, z1, yT);
                    faceEast(g, z0, z1, yB, yT, x1);
                    faceNorth(g, x0, x1, yB, yT, z0);
                    faceSouth(g, x0, x1, yB, yT, z1);
                    break;
                }
                case "east": {
                    z0 = wz0;
                    z1 = wz1;
                    x0 = wx0 + i * sd;
                    x1 = wx0 + (i + 1) * sd;
                    faceUp(g, x0, x1, z0, z1, yT);
                    faceWest(g, z0, z1, yB, yT, x0);
                    faceNorth(g, x0, x1, yB, yT, z0);
                    faceSouth(g, x0, x1, yB, yT, z1);
                    break;
                }
            }
        }
    }

    private void buildOuterCornerStairs(Geo g, float wx0, float wx1, float wz0, float wz1, float yBase, String type) {
        float sh = LEVEL_HEIGHT / STAIR_STEPS;
        float sd = TILE_SIZE / STAIR_STEPS;
        for (int i = 0; i < STAIR_STEPS; i++) {
            float yB = yBase + i * sh, yT = yBase + (i + 1) * sh;
            float x0 = wx0, x1 = wx1, z0 = wz0, z1 = wz1;
            switch (type) {
                case "outer_nw":
                    x1 = wx1 - i * sd;
                    z1 = wz1 - i * sd;
                    break;
                case "outer_ne":
                    x0 = wx0 + i * sd;
                    z1 = wz1 - i * sd;
                    break;
                case "outer_sw":
                    x1 = wx1 - i * sd;
                    z0 = wz0 + i * sd;
                    break;
                case "outer_se":
                    x0 = wx0 + i * sd;
                    z0 = wz0 + i * sd;
                    break;
            }
            faceUp(g, x0, x1, z0, z1, yT);
            faceNorth(g, x0, x1, yB, yT, z0);
            faceSouth(g, x0, x1, yB, yT, z1);
            faceWest(g, z0, z1, yB, yT, x0);
            faceEast(g, z0, z1, yB, yT, x1);
        }
    }

    private void buildInnerCornerStairs(Geo g, float wx0, float wx1, float wz0, float wz1, float yBase, String type) {
        float sh = LEVEL_HEIGHT / STAIR_STEPS;
        float sd = TILE_SIZE / STAIR_STEPS;
        for (int i = 0; i < STAIR_STEPS; i++) {
            float yB = yBase + i * sh, yT = yBase + (i + 1) * sh;
            float inset = (STAIR_STEPS - i) * sd;
            switch (type) {
                case "inner_nw":
                    faceUp(g, wx0, wx1, wz1 - inset, wz1, yT);
                    faceUp(g, wx0, wx0 + inset, wz0, wz1, yT);
                    faceNorth(g, wx0, wx1, yB, yT, wz1 - inset);
                    faceSouth(g, wx0, wx1, yB, yT, wz1);
                    faceWest(g, wz1 - inset, wz1, yB, yT, wx0);
                    faceEast(g, wz1 - inset, wz1, yB, yT, wx1);
                    faceWest(g, wz0, wz1, yB, yT, wx0);
                    faceEast(g, wz0, wz1, yB, yT, wx0 + inset);
                    faceNorth(g, wx0, wx0 + inset, yB, yT, wz0);
                    faceSouth(g, wx0, wx0 + inset, yB, yT, wz1);
                    break;
                case "inner_se":
                    faceUp(g, wx0, wx1, wz1 - inset, wz1, yT);
                    faceUp(g, wx1 - inset, wx1, wz0, wz1, yT);
                    faceNorth(g, wx0, wx1, yB, yT, wz1 - inset);
                    faceSouth(g, wx0, wx1, yB, yT, wz1);
                    faceWest(g, wz1 - inset, wz1, yB, yT, wx0);
                    faceEast(g, wz1 - inset, wz1, yB, yT, wx1);
                    faceWest(g, wz0, wz1, yB, yT, wx1 - inset);
                    faceEast(g, wz0, wz1, yB, yT, wx1);
                    faceNorth(g, wx1 - inset, wx1, yB, yT, wz0);
                    faceSouth(g, wx1 - inset, wx1, yB, yT, wz1);
                    break;
                case "inner_sw":
                    faceUp(g, wx0, wx1, wz0, wz0 + inset, yT);
                    faceUp(g, wx0, wx0 + inset, wz0, wz1, yT);
                    faceNorth(g, wx0, wx1, yB, yT, wz0);
                    faceSouth(g, wx0, wx1, yB, yT, wz0 + inset);
                    faceWest(g, wz0, wz0 + inset, yB, yT, wx0);
                    faceEast(g, wz0, wz0 + inset, yB, yT, wx1);
                    faceWest(g, wz0, wz1, yB, yT, wx0);
                    faceEast(g, wz0, wz1, yB, yT, wx0 + inset);
                    faceNorth(g, wx0, wx0 + inset, yB, yT, wz0);
                    faceSouth(g, wx0, wx0 + inset, yB, yT, wz1);
                    break;
                case "inner_ne":
                    faceUp(g, wx0, wx1, wz0, wz0 + inset, yT);
                    faceUp(g, wx1 - inset, wx1, wz0, wz1, yT);
                    faceNorth(g, wx0, wx1, yB, yT, wz0);
                    faceSouth(g, wx0, wx1, yB, yT, wz0 + inset);
                    faceWest(g, wz0, wz0 + inset, yB, yT, wx0);
                    faceEast(g, wz0, wz0 + inset, yB, yT, wx1);
                    faceWest(g, wz0, wz1, yB, yT, wx1 - inset);
                    faceEast(g, wz0, wz1, yB, yT, wx1);
                    faceNorth(g, wx1 - inset, wx1, yB, yT, wz0);
                    faceSouth(g, wx1 - inset, wx1, yB, yT, wz1);
                    break;
            }
        }
    }

    private void buildOuterWalls(Geo wallGeo) {
        float WORLD_TOP_Y = 3.5f;
        float SHORT_TOP_Y = 1.2f;
        float THK = 0.2f;
        float yB = -FLOOR_DEPTH;

        float northT = shortNorth ? SHORT_TOP_Y : WORLD_TOP_Y;
        float southT = shortSouth ? SHORT_TOP_Y : WORLD_TOP_Y;
        float westT = shortWest ? SHORT_TOP_Y : WORLD_TOP_Y;
        float eastT = shortEast ? SHORT_TOP_Y : WORLD_TOP_Y;

        boolean[][] dN = new boolean[rows][cols];
        boolean[][] dS = new boolean[rows][cols];
        boolean[][] dW = new boolean[rows][cols];
        boolean[][] dE = new boolean[rows][cols];

        for (int z = 0; z < rows; z++) {
            for (int x = 0; x < cols; x++) {
                if (hMap[z][x] < 0)
                    continue;

                float x0 = x * TILE_SIZE, x1 = x0 + TILE_SIZE;
                float z0 = z * TILE_SIZE, z1 = z0 + TILE_SIZE;

                // North run
                if (n(z - 1, x) == -1 && !dN[z][x]) {
                    int run = 1;
                    while (x + run < cols && hMap[z][x + run] >= 0 && n(z - 1, x + run) == -1 && !dN[z][x + run])
                        run++;
                    for (int k = 0; k < run; k++)
                        dN[z][x + k] = true;
                    float rx1 = (x + run) * TILE_SIZE;
                    // only extend into corner if there's a west/east wall meeting here
                    float ex0 = n(z, x - 1) == -1 ? x0 - THK : x0;
                    float ex1 = n(z, x + run) == -1 ? rx1 + THK : rx1;
                    faceNorth(wallGeo, ex0, ex1, yB, northT, z0 - THK);
                    faceSouth(wallGeo, ex0, ex1, yB, northT, z0);
                    faceUp(wallGeo, ex0, ex1, z0 - THK, z0, northT);
                    faceWest(wallGeo, z0 - THK, z0, yB, northT, ex0);
                    faceEast(wallGeo, z0 - THK, z0, yB, northT, ex1);
                }

                // South run
                if (n(z + 1, x) == -1 && !dS[z][x]) {
                    int run = 1;
                    while (x + run < cols && hMap[z][x + run] >= 0 && n(z + 1, x + run) == -1 && !dS[z][x + run])
                        run++;
                    for (int k = 0; k < run; k++)
                        dS[z][x + k] = true;
                    float rx1 = (x + run) * TILE_SIZE;
                    float ex0 = n(z, x - 1) == -1 ? x0 - THK : x0;
                    float ex1 = n(z, x + run) == -1 ? rx1 + THK : rx1;
                    faceSouth(wallGeo, ex0, ex1, yB, southT, z1 + THK);
                    faceNorth(wallGeo, ex0, ex1, yB, southT, z1);
                    faceUp(wallGeo, ex0, ex1, z1, z1 + THK, southT);
                    faceWest(wallGeo, z1, z1 + THK, yB, southT, ex0);
                    faceEast(wallGeo, z1, z1 + THK, yB, southT, ex1);
                }

                // West run
                if (n(z, x - 1) == -1 && !dW[z][x]) {
                    int run = 1;
                    while (z + run < rows && hMap[z + run][x] >= 0 && n(z + run, x - 1) == -1 && !dW[z + run][x])
                        run++;
                    for (int k = 0; k < run; k++)
                        dW[z + k][x] = true;
                    float rz1 = (z + run) * TILE_SIZE;
                    faceWest(wallGeo, z0, rz1, yB, westT, x0 - THK);
                    faceEast(wallGeo, z0, rz1, yB, westT, x0);
                    faceUp(wallGeo, x0 - THK, x0, z0, rz1, westT);
                    faceNorth(wallGeo, x0 - THK, x0, yB, westT, z0);
                    faceSouth(wallGeo, x0 - THK, x0, yB, westT, rz1);
                }

                // East run
                if (n(z, x + 1) == -1 && !dE[z][x]) {
                    int run = 1;
                    while (z + run < rows && hMap[z + run][x] >= 0 && n(z + run, x + 1) == -1 && !dE[z + run][x])
                        run++;
                    for (int k = 0; k < run; k++)
                        dE[z + k][x] = true;
                    float rz1 = (z + run) * TILE_SIZE;
                    faceEast(wallGeo, z0, rz1, yB, eastT, x1 + THK);
                    faceWest(wallGeo, z0, rz1, yB, eastT, x1);
                    faceUp(wallGeo, x1, x1 + THK, z0, rz1, eastT);
                    faceNorth(wallGeo, x1, x1 + THK, yB, eastT, z0);
                    faceSouth(wallGeo, x1, x1 + THK, yB, eastT, rz1);
                }
            }
        }
    }

    // Which walls face the camera — call this from RoomScreen when yaw changes,
    // then call generateFromLayout() again to rebuild geometry.
    // At yaw=45: camera looks from NW → south and east walls are back walls (tall)
    // north and west walls face camera (short)
    public boolean shortNorth = false;
    public boolean shortWest = false;
    public boolean shortSouth = true;
    public boolean shortEast = true;

    public void setCameraYaw(float yaw) {
        yaw = ((yaw % 360) + 360) % 360;
        if (yaw < 90) {
            shortNorth = false;
            shortWest = false;
            shortSouth = true;
            shortEast = true;
        } else if (yaw < 180) {
            shortNorth = true;
            shortWest = false;
            shortSouth = false;
            shortEast = true;
        } else if (yaw < 270) {
            shortNorth = true;
            shortWest = true;
            shortSouth = false;
            shortEast = false;
        } else {
            shortNorth = false;
            shortWest = true;
            shortSouth = true;
            shortEast = false;
        }
    }

    private int n(int z, int x) {
        if (z < 0 || z >= rows || x < 0 || x >= cols)
            return -1;
        return hMap[z][x];
    }

    public boolean isValid(int x, int z) {
        if (x < 0 || x >= cols || z < 0 || z >= rows)
            return false;
        return hMap[z][x] >= 0;
    }

    public float centerX() {
        return (cols * TILE_SIZE) / 2f;
    }

    public float centerZ() {
        return (rows * TILE_SIZE) / 2f;
    }

    public float worldY(int x, int z) {
        return isValid(x, z) ? hMap[z][x] * LEVEL_HEIGHT : 0f;
    }

    public int[] worldToGrid(float worldX, float worldZ) {
        // Floor ensures 0.0 to 1.499 is Tile 0 when TILE_SIZE is 1.5
        int x = (int) Math.floor(worldX / TILE_SIZE);
        int z = (int) Math.floor(worldZ / TILE_SIZE);
        return new int[] { x, z };
    }

    public float[] gridToWorldCenter(int gridX, int gridZ) {
        return new float[] {
                (gridX * TILE_SIZE) + (TILE_SIZE / 2f),
                (gridZ * TILE_SIZE) + (TILE_SIZE / 2f)
        };
    }

    public void createMarker() {
        if (markerModel != null)
            return;

        // Make the marker a bit smaller than the tile so it has a "border"
        float half = (TILE_SIZE * 0.9f) / 2f;

        Geo g = new Geo();
        // Centered at 0,0 locally so we can translate the instance easily
        // Args: x0,y0,z0, x1,y1,z1, x2,y2,z2, x3,y3,z3, nx,ny,nz, u,v, u2,v2
        g.quad(-half, 0, -half,
                -half, 0, half,
                half, 0, half,
                half, 0, -half,
                0, 1, 0, 0, 0, 1, 1);

        // Use a bright, semi-transparent color
        Material markerMat = new Material(
                ColorAttribute.createDiffuse(new Color(0f, 1f, 0f, 0.1f)),
                // This helps it show up better on dark floors
                ColorAttribute.createSpecular(Color.WHITE));

        markerModel = g.build(markerMat);
        markerInstance = new ModelInstance(markerModel);
    }

    public void updateMarkerPosition(float worldX, float worldY, float worldZ) {
        if (markerInstance == null)
            return;

        // We no longer calculate grid/worldY here because RoomScreen already did it!
        // Just set the position directly to the instance.
        markerInstance.transform.setTranslation(worldX, worldY, worldZ);
    }

    public int[] getMarkerGridPosition() {
        if (markerInstance == null)
            return null;
        Vector3 pos = new Vector3();
        markerInstance.transform.getTranslation(pos);
        return worldToGrid(pos.x, pos.z);
    }

    @Override
    public void dispose() {
        if (floorModel != null) {
            floorModel.dispose();
            floorModel = null;
        }
        if (stairModel != null) {
            stairModel.dispose();
            stairModel = null;
        }
        if (wallModel != null) {
            wallModel.dispose();
            wallModel = null;
        }
        if (markerModel != null) {
            markerModel.dispose();
            markerModel = null;
        }
        floorInstance = stairInstance = wallInstance = markerInstance = null;
    }
}