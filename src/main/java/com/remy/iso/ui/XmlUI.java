package com.remy.iso.ui;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;

public class XmlUI {

    private Skin skin;
    private Object actionContainer;
    private float scale = 1f;
    private String prefix = "";
    private final Map<String, Drawable> drawableCache = new HashMap<>();

    private float s(float value) {
        return value * scale;
    }

    // Resolve path with optional prefix
    private String path(String src) {
        if (prefix.isEmpty() || src.contains("/"))
            return src;
        return prefix + "/" + src;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    // ── Constructors ──────────────────────────────────────────────────────────

    public XmlUI() {
        buildDefaultSkin();
    }

    public XmlUI(float scale) {
        this();
        this.scale = scale;
    }

    public XmlUI(float scale, String prefix) {
        this();
        this.scale = scale;
        this.prefix = prefix;
    }

    public XmlUI(Skin skin) {
        this.skin = skin;
    }

    public XmlUI(Skin skin, float scale) {
        this.skin = skin;
        this.scale = scale;
    }

    public XmlUI(Skin skin, float scale, String prefix) {
        this.skin = skin;
        this.scale = scale;
        this.prefix = prefix;
    }

    private void buildDefaultSkin() {
        skin = new Skin();

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("ui/noto.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter params = new FreeTypeFontGenerator.FreeTypeFontParameter();
        params.size = (int) (14 * Gdx.graphics.getBackBufferScale());
        BitmapFont font = generator.generateFont(params);
        generator.dispose();
        skin.add("default", font);

        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = font;
        labelStyle.fontColor = com.badlogic.gdx.graphics.Color.WHITE;
        skin.add("default", labelStyle);

        TextButton.TextButtonStyle btnStyle = new TextButton.TextButtonStyle();
        btnStyle.font = font;
        skin.add("default", btnStyle);

        Window.WindowStyle windowStyle = new Window.WindowStyle();
        windowStyle.titleFont = font;
        windowStyle.titleFontColor = com.badlogic.gdx.graphics.Color.WHITE;
        skin.add("default", windowStyle);
    }

    public void setActionContainer(Object container) {
        this.actionContainer = container;
    }

    // ── Parse ─────────────────────────────────────────────────────────────────

    public Actor parse(FileHandle file) {
        try {
            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(file.read());
            doc.getDocumentElement().normalize();
            return buildActor(doc.getDocumentElement());
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse UI file: " + file.name(), e);
        }
    }

    public Actor parse(String internalPath) {
        return parse(Gdx.files.internal(internalPath));
    }

    // ── Build ─────────────────────────────────────────────────────────────────

    private Actor buildActor(Element el) {
        Actor actor = createElement(el);
        applyAttributes(actor, el);

        NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (!(child instanceof Element))
                continue;

            Actor childActor = buildActor((Element) child);

            if (actor instanceof Table) {
                Table table = (Table) actor;
                Cell<?> cell = table.add(childActor);
                applyCell(cell, (Element) child);
                if (((Element) child).hasAttribute("row")) {
                    table.row();
                }
            } else if (actor instanceof Group) {
                ((Group) actor).addActor(childActor);
            }
        }

        return actor;
    }

    private Actor createElement(Element el) {
        switch (el.getTagName().toLowerCase()) {

            case "table": {
                Table table = new Table();
                String bg = el.getAttribute("bg");
                if (!bg.isEmpty()) {
                    if (el.hasAttribute("left")) {
                        int left = Integer.parseInt(el.getAttribute("left"));
                        int right = Integer.parseInt(el.getAttribute("right"));
                        int top = Integer.parseInt(el.getAttribute("top"));
                        int bottom = Integer.parseInt(el.getAttribute("bottom"));
                        NinePatch patch = new NinePatch(
                                new Texture(Gdx.files.internal(path(bg))),
                                left, right, top, bottom);
                        table.setBackground(new NinePatchDrawable(patch));
                    } else {
                        table.setBackground(getDrawable(path(bg)));
                    }
                }
                return table;
            }

            case "label": {
                String text = el.getAttribute("text");
                return new Label(text, skin);
            }

            case "button":
            case "textbutton": {
                String text = el.getAttribute("text");
                return new TextButton(text, skin);
            }

            case "image": {
                String src = el.getAttribute("src");
                if (!src.isEmpty()) {
                    return new Image(getDrawable(path(src)));
                }
                return new Image();
            }

            case "inputfield": {
                String hint = el.getAttribute("hint");
                TextField.TextFieldStyle style = new TextField.TextFieldStyle();
                style.font = skin.getFont("default");
                style.fontColor = com.badlogic.gdx.graphics.Color.WHITE;
                style.messageFontColor = new com.badlogic.gdx.graphics.Color(1, 1, 1, 0.5f);
                style.messageFont = skin.getFont("default");
                String bg = el.getAttribute("bg");
                if (!bg.isEmpty() && el.hasAttribute("left")) {
                    int left = Integer.parseInt(el.getAttribute("left"));
                    int right = Integer.parseInt(el.getAttribute("right"));
                    int top = Integer.parseInt(el.getAttribute("top"));
                    int bottom = Integer.parseInt(el.getAttribute("bottom"));
                    NinePatch patch = new NinePatch(
                            new Texture(Gdx.files.internal(path(bg))),
                            left, right, top, bottom);
                    style.background = new NinePatchDrawable(patch);
                }
                Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
                pixmap.setColor(Color.WHITE);
                pixmap.fill();
                skin.add("white", new Texture(pixmap));
                pixmap.dispose();
                style.cursor = skin.newDrawable("white", com.badlogic.gdx.graphics.Color.WHITE);
                style.selection = skin.newDrawable("white", new com.badlogic.gdx.graphics.Color(0.5f, 0.5f, 1f, 0.5f));

                String onSubmit = el.getAttribute("onSubmit");
                if (!onSubmit.isEmpty()) {
                    TextField textField = new TextField("", style);
                    textField.setMessageText(hint);
                    textField.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
                        @Override
                        public boolean keyDown(com.badlogic.gdx.scenes.scene2d.InputEvent event, int keycode) {
                            if (keycode == com.badlogic.gdx.Input.Keys.ENTER) {
                                if (actionContainer == null)
                                    return false;
                                try {
                                    try {
                                        Method m = actionContainer.getClass().getMethod(onSubmit, String.class,
                                                TextField.class);
                                        m.invoke(actionContainer, textField.getText(), textField);
                                    } catch (NoSuchMethodException e) {
                                        Method m = actionContainer.getClass().getMethod(onSubmit);
                                        m.invoke(actionContainer);
                                    }
                                } catch (Exception e) {
                                    Gdx.app.error("XmlUI", "Failed to invoke onSubmit: " + onSubmit, e);
                                }
                                return true;
                            }
                            return false;
                        }
                    });
                    return textField;
                }
                return new TextField(hint, style);
            }

            case "imagebutton": {
                String up = el.getAttribute("up");
                String over = el.getAttribute("over");
                String down = el.getAttribute("down");

                ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
                if (!up.isEmpty())
                    style.up = getDrawable(path(up));
                if (!over.isEmpty())
                    style.over = getDrawable(path(over));
                if (!down.isEmpty())
                    style.down = getDrawable(path(down));

                return new ImageButton(style);
            }

            case "window": {
                String title = el.getAttribute("title");
                Window.WindowStyle style = new Window.WindowStyle();
                style.titleFont = skin.getFont("default");
                style.titleFontColor = com.badlogic.gdx.graphics.Color.WHITE;
                String bg = el.getAttribute("bg");
                if (!bg.isEmpty()) {
                    if (el.hasAttribute("left")) {
                        int left = Integer.parseInt(el.getAttribute("left"));
                        int right = Integer.parseInt(el.getAttribute("right"));
                        int top = Integer.parseInt(el.getAttribute("top"));
                        int bottom = Integer.parseInt(el.getAttribute("bottom"));
                        float bgScale = el.hasAttribute("bgScale") ? Float.parseFloat(el.getAttribute("bgScale")) : 1f;
                        NinePatch patch = new NinePatch(
                                new Texture(Gdx.files.internal(path(bg))),
                                left, right, top, bottom);
                        patch.scale(bgScale, bgScale);
                        style.background = new NinePatchDrawable(patch);
                    } else {
                        style.background = getDrawable(path(bg));
                    }
                }
                Window window = new Window(title, style);
                window.setMovable(false);
                window.setResizable(false);
                window.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
                    float startX, startY;

                    @Override
                    public boolean touchDown(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y,
                            int pointer, int button) {
                        startX = x;
                        startY = y;
                        return true;
                    }

                    @Override
                    public void touchDragged(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y,
                            int pointer) {
                        window.moveBy(x - startX, y - startY);
                    }
                });
                return window;
            }

            case "ninepatch": {
                String src = el.getAttribute("src");
                int left = Integer.parseInt(el.getAttribute("left"));
                int right = Integer.parseInt(el.getAttribute("right"));
                int top = Integer.parseInt(el.getAttribute("top"));
                int bottom = Integer.parseInt(el.getAttribute("bottom"));
                float npScale = el.hasAttribute("scale") ? Float.parseFloat(el.getAttribute("scale")) : 1f;

                String cacheKey = src + ":" + left + ":" + right + ":" + top + ":" + bottom + ":" + npScale;
                Drawable drawable = drawableCache.computeIfAbsent(cacheKey, k -> {
                    NinePatch patch = new NinePatch(
                            new Texture(Gdx.files.internal(path(src))),
                            left, right, top, bottom);
                    patch.scale(npScale, npScale);
                    return new NinePatchDrawable(patch);
                });
                return new Image(drawable);
            }

            case "scrollpane":
                return new Table();

            case "group":
                return new Group();

            default:
                throw new RuntimeException("Unknown UI tag: <" + el.getTagName() + ">");
        }
    }

    // ── Attributes ────────────────────────────────────────────────────────────

    private void applyAttributes(Actor actor, Element el) {
        NamedNodeMap attrs = el.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Node attr = attrs.item(i);
            String name = attr.getNodeName();
            String value = attr.getNodeValue();

            if (name.equals("row") || name.equals("src") || name.equals("up") ||
                    name.equals("over") || name.equals("down") || name.equals("text") ||
                    name.equals("title") || name.equals("bg") || name.equals("left") ||
                    name.equals("right") || name.equals("top") || name.equals("bottom") ||
                    name.equals("scale") || name.equals("bgScale"))
                continue;

            applyAttribute(actor, name, value);
        }
    }

    private void applyAttribute(Actor actor, String name, String value) {
        switch (name) {
            case "id":
                actor.setName(value);
                break;
            case "x":
                actor.setX(s(Float.parseFloat(value)));
                break;
            case "y":
                actor.setY(s(Float.parseFloat(value)));
                break;
            case "width":
                actor.setWidth(s(Float.parseFloat(value)));
                break;
            case "height":
                actor.setHeight(s(Float.parseFloat(value)));
                break;
            case "visible":
                actor.setVisible(Boolean.parseBoolean(value));
                break;
            case "fillParent":
                if (actor instanceof Table)
                    ((Table) actor).setFillParent(Boolean.parseBoolean(value));
                break;
            case "align":
                if (actor instanceof Table)
                    ((Table) actor).align(parseAlign(value));
                break;
            case "pad":
                if (actor instanceof Table)
                    ((Table) actor).pad(s(Float.parseFloat(value)));
                break;
            case "padTop":
                if (actor instanceof Table)
                    ((Table) actor).padTop(s(Float.parseFloat(value)));
                break;
            case "padBottom":
                if (actor instanceof Table)
                    ((Table) actor).padBottom(s(Float.parseFloat(value)));
                break;
            case "padLeft":
                if (actor instanceof Table)
                    ((Table) actor).padLeft(s(Float.parseFloat(value)));
                break;
            case "padRight":
                if (actor instanceof Table)
                    ((Table) actor).padRight(s(Float.parseFloat(value)));
                break;
            case "padX":
                if (actor instanceof Table) {
                    float v = s(Float.parseFloat(value));
                    ((Table) actor).padLeft(v).padRight(v);
                }
                break;
            case "padY":
                if (actor instanceof Table) {
                    float v = s(Float.parseFloat(value));
                    ((Table) actor).padTop(v).padBottom(v);
                }
                break;

            case "touchable":
                switch (value) {
                    case "disabled":
                        actor.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
                        break;
                    case "childrenOnly":
                        actor.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.childrenOnly);
                        break;
                    default:
                        actor.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
                        break;
                }
                break;
            case "spacing":
                if (actor instanceof Table)
                    ((Table) actor).defaults().space(s(Float.parseFloat(value)));
                break;
            case "spacingX":
                if (actor instanceof Table) {
                    float v = s(Float.parseFloat(value));
                    ((Table) actor).defaults().spaceLeft(v).spaceRight(v);
                }
                break;
            case "spacingY":
                if (actor instanceof Table) {
                    float v = s(Float.parseFloat(value));
                    ((Table) actor).defaults().spaceTop(v).spaceBottom(v);
                }
                break;
            case "onClick":
                attachClickListener(actor, value);
                break;
            case "debug":
                if (Boolean.parseBoolean(value) && actor instanceof Group) {
                    ((Group) actor).setDebug(true, true);
                }
                break;
            case "scaleX":
                actor.setScaleX(Float.parseFloat(value));
                break;
            case "scaleY":
                actor.setScaleY(Float.parseFloat(value));
                break;
            case "actorScale":
                actor.setScale(Float.parseFloat(value));
                break;
        }
    }

    // Cell-level attributes
    private void applyCell(Cell<?> cell, Element el) {
        if (el.hasAttribute("cellWidth"))
            cell.width(s(Float.parseFloat(el.getAttribute("cellWidth"))));
        if (el.hasAttribute("cellHeight"))
            cell.height(s(Float.parseFloat(el.getAttribute("cellHeight"))));
        if (el.hasAttribute("expand"))
            cell.expand();
        if (el.hasAttribute("expandX"))
            cell.expandX();
        if (el.hasAttribute("expandY"))
            cell.expandY();
        if (el.hasAttribute("fill"))
            cell.fill();
        if (el.hasAttribute("fillX"))
            cell.fillX();
        if (el.hasAttribute("fillY"))
            cell.fillY();
        if (el.hasAttribute("cellPad"))
            cell.pad(s(Float.parseFloat(el.getAttribute("cellPad"))));
        if (el.hasAttribute("cellPadLeft"))
            cell.padLeft(s(Float.parseFloat(el.getAttribute("cellPadLeft"))));
        if (el.hasAttribute("cellPadRight"))
            cell.padRight(s(Float.parseFloat(el.getAttribute("cellPadRight"))));
        if (el.hasAttribute("cellPadTop"))
            cell.padTop(s(Float.parseFloat(el.getAttribute("cellPadTop"))));
        if (el.hasAttribute("cellPadBottom"))
            cell.padBottom(s(Float.parseFloat(el.getAttribute("cellPadBottom"))));
        if (el.hasAttribute("space"))
            cell.space(s(Float.parseFloat(el.getAttribute("space"))));
        if (el.hasAttribute("cellAlign"))
            cell.align(parseAlign(el.getAttribute("cellAlign")));
    }

    // ── Click Handlers ────────────────────────────────────────────────────────

    private void attachClickListener(Actor actor, String methodName) {
        actor.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (actionContainer == null)
                    return;
                try {
                    try {
                        Method m = actionContainer.getClass().getMethod(methodName, Actor.class);
                        m.invoke(actionContainer, actor);
                    } catch (NoSuchMethodException e) {
                        Method m = actionContainer.getClass().getMethod(methodName);
                        m.invoke(actionContainer);
                    }
                } catch (Exception e) {
                    Gdx.app.error("XmlUI", "Failed to invoke onClick handler: " + methodName, e);
                }
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Drawable getDrawable(String path) {
        if (drawableCache.containsKey(path))
            return drawableCache.get(path);
        Drawable d = new TextureRegionDrawable(new Texture(Gdx.files.internal(path)));
        drawableCache.put(path, d);
        return d;
    }

    private int parseAlign(String value) {
        switch (value) {
            case "bottomLeft":
                return Align.bottomLeft;
            case "bottomRight":
                return Align.bottomRight;
            case "topLeft":
                return Align.topLeft;
            case "topRight":
                return Align.topRight;
            case "top":
                return Align.top;
            case "bottom":
                return Align.bottom;
            case "left":
                return Align.left;
            case "right":
                return Align.right;
            default:
                return Align.center;
        }
    }

    public Skin getSkin() {
        return skin;
    }

    public void dispose() {
        for (Drawable d : drawableCache.values()) {
            if (d instanceof TextureRegionDrawable) {
                ((TextureRegionDrawable) d).getRegion().getTexture().dispose();
            }
        }
        drawableCache.clear();
    }
}