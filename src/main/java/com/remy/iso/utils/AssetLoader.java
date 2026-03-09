package com.remy.iso.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net.HttpResponse;
import com.badlogic.gdx.Net.HttpResponseListener;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.net.HttpRequestBuilder;

import net.mgsx.gltf.loaders.glb.GLBLoader;
import net.mgsx.gltf.scene3d.scene.SceneAsset;

/**
 * Downloads assets from a remote URL, caches them locally, and loads them.
 * Supports: GLB models, PNG/JPG textures, JSON files.
 *
 * Usage:
 * AssetLoader.getInstance().loadModel("furniture/lamp.glb", asset -> { ... });
 * AssetLoader.getInstance().loadTexture("ui/icon.png", texture -> { ... });
 * AssetLoader.getInstance().loadJson("data/items.json", json -> { ... });
 */
public class AssetLoader {

    private static AssetLoader instance;

    public static AssetLoader getInstance() {
        if (instance == null)
            instance = new AssetLoader();
        return instance;
    }

    private String baseUrl = "";
    private final Map<String, Object> cache = new HashMap<>();

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void preload(String assetList, Runnable onComplete) {
        String[] paths = assetList.split(",");

        if (paths.length == 0 || assetList.isEmpty()) {
            onComplete.run();
            return;
        }

        // Track how many are left
        int[] remaining = { paths.length };

        for (String raw : paths) {
            String path = raw.trim();
            if (path.isEmpty()) {
                remaining[0]--;
                continue;
            }

            Runnable onAssetLoaded = () -> {
                remaining[0]--;
                Gdx.app.log("AssetLoader", "Preloaded: " + path + " (" + remaining[0] + " remaining)");
                if (remaining[0] == 0) {
                    Gdx.app.postRunnable(onComplete);
                }
            };

            if (path.endsWith(".glb")) {
                loadModel(path, asset -> onAssetLoaded.run());
            } else if (path.endsWith(".png") || path.endsWith(".jpg")) {
                loadTexture(path, texture -> onAssetLoaded.run());
            } else if (path.endsWith(".json")) {
                loadJson(path, json -> onAssetLoaded.run());
            } else {
                // Unknown type — skip but count down
                remaining[0]--;
            }
        }
    }

    // ── Models ────────────────────────────────────────────────────────────────

    public void loadModel(String path, Consumer<SceneAsset> onLoaded) {
        if (cache.containsKey(path)) {
            onLoaded.accept((SceneAsset) cache.get(path));
            return;
        }

        fetchBytes(path, bytes -> {
            // bytes received on background thread — post GL work to GL thread
            Gdx.app.postRunnable(() -> {
                FileHandle tmp = writeTempFile(path, bytes);
                SceneAsset asset = new GLBLoader().load(tmp, true);
                cache.put(path, asset);
                onLoaded.accept(asset);
            });
        });
    }

    // ── Textures ──────────────────────────────────────────────────────────────

    public void loadTexture(String path, Consumer<Texture> onLoaded) {
        if (cache.containsKey(path)) {
            onLoaded.accept((Texture) cache.get(path));
            return;
        }

        fetchBytes(path, bytes -> {
            Gdx.app.postRunnable(() -> {
                FileHandle tmp = writeTempFile(path, bytes);
                Texture texture = new Texture(tmp);
                cache.put(path, texture);
                onLoaded.accept(texture);
            });
        });
    }

    // ── JSON ──────────────────────────────────────────────────────────────────

    public void loadJson(String path, Consumer<String> onLoaded) {
        if (cache.containsKey(path)) {
            onLoaded.accept((String) cache.get(path));
            return;
        }

        fetchBytes(path, bytes -> {
            String json = new String(bytes);
            cache.put(path, json);
            Gdx.app.postRunnable(() -> onLoaded.accept(json));
        });
    }

    // ── Cache control ─────────────────────────────────────────────────────────

    public boolean isCached(String path) {
        return localFile(path).exists();
    }

    public void clearMemoryCache() {
        cache.clear();
    }

    public void clearDiskCache(String path) {
        localFile(path).delete();
        cache.remove(path);
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private void fetchBytes(String path, Consumer<byte[]> onComplete) {
        // Check disk cache first
        FileHandle local = localFile(path);
        if (local.exists()) {
            onComplete.accept(local.readBytes());
            return;
        }

        // Download from remote
        String url = baseUrl + path;
        Gdx.app.log("AssetLoader", "Downloading: " + url);

        Gdx.net.sendHttpRequest(new HttpRequestBuilder().newRequest()
                .method("GET")
                .url(url)
                .build(),
                new HttpResponseListener() {
                    @Override
                    public void handleHttpResponse(HttpResponse response) {
                        int status = response.getStatus().getStatusCode();
                        if (status != 200) {
                            Gdx.app.error("AssetLoader", "HTTP " + status + " for " + url);
                            return;
                        }
                        byte[] bytes = response.getResult();
                        local.writeBytes(bytes, false); // save to disk cache
                        onComplete.accept(bytes);
                    }

                    @Override
                    public void failed(Throwable t) {
                        Gdx.app.error("AssetLoader", "Failed to download: " + url, t);
                    }

                    @Override
                    public void cancelled() {
                    }
                });
    }

    private FileHandle writeTempFile(String path, byte[] bytes) {
        FileHandle f = localFile(path);
        f.writeBytes(bytes, false);
        return f;
    }

    private FileHandle localFile(String path) {
        return Gdx.files.local("cache/" + path);
    }
}
