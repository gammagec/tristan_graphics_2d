package com.pixel_pioneer.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import com.pixel_pioneer.Const;
import com.pixel_pioneer.actions.KeyboardHandler;
import com.pixel_pioneer.clock.Clock;
import com.pixel_pioneer.util.PointI;
import com.pixel_pioneer.world.entities.Mob;
import com.pixel_pioneer.world.entities.MobInstance;
import com.pixel_pioneer.world.entities.Player;
import com.pixel_pioneer.world.*;

import static com.pixel_pioneer.Const.MAX_TIME;
import static java.lang.System.*;

public class GameWindow extends JFrame implements WorldUpdateHandler {

    final private World world;
    final private Hud hud;
    final private Inventory inventory;

    private BufferedImage backBuffer = null;

    private BufferedImage nightOverlay = null;

    final private Player player;
    final private MiniMap miniMap;
    private final Clock clock;
    private final FadeCircle fadeCircle;

    final private CraftingMenu craftingMenu;

    public GameWindow(World world, Hud hud, Inventory inventory, KeyboardHandler keyboardHandler,
                      MiniMap miniMap, CraftingMenu craftingMenu, Clock clock, FadeCircle fadeCircle) {
        super("Pixel Pioneer");
        this.craftingMenu = craftingMenu;
        this.inventory = inventory;
        this.world = world;
        this.hud = hud;
        this.player = world.getPlayer();
        this.miniMap = miniMap;
        this.clock = clock;
        this.fadeCircle = fadeCircle;

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exit(0);
            }
        });
        setSize(1200, 1200);
        addKeyListener(keyboardHandler);
        world.setWorldUpdateHandler(this);

        BufferedImage cornersMask;
        File file = new File("images/all_corners.png");
        try {
            cornersMask = ImageIO.read(file);
        } catch (IOException e) {
            System.out.println("Error loading corners mask");
            throw new RuntimeException(e);
        }

        for(ImageAsset imageAsset : ImageAsset.ASSETS_BY_ID.values()) {
            if (imageAsset.getType() == AssetType.TILE_ASSET) {
                SpriteSheets.TILE_SPRITES.addImageAsset(imageAsset);
            } else {
                SpriteSheets.OBJ_SPRITES.addImageAsset(imageAsset);
            }
        }
        hud.update();
        inventory.update();
        miniMap.update();
        craftingMenu.update();
    }

    public void paint(Graphics g) {
        drawBoard(g);
    }

    @Override
    public void worldUpdated() {
        repaint();
    }

    @Override
    public void playerUpdated() {
        hud.update();
    }

    public void drawBoard(Graphics g) {
        int mapWidth = getWidth();
        int mapHeight = getHeight();
        if (backBuffer == null || backBuffer.getWidth() != mapWidth || backBuffer.getHeight() != mapHeight) {
            backBuffer = new BufferedImage(mapWidth, mapHeight, BufferedImage.TYPE_INT_ARGB);
        }
        Graphics2D g2d = backBuffer.createGraphics();

        int tileHeight = Const.TILE_RENDER_SIZE;
        int tileWidth = Const.TILE_RENDER_SIZE;
        int centerTileX = mapWidth / 2 - (tileWidth / 2);
        int centerTileY = mapHeight / 2 - (tileHeight / 2);
        int numX = Math.ceilDiv(mapWidth, tileWidth);
        int numY = Math.ceilDiv(mapHeight, tileHeight);
        PointI loc = player.getLocation();
        int leftIndex = loc.getX() - numX / 2;
        int topIndex = loc.getY() - numY / 2;
        int startX = centerTileX - (numX / 2 * tileWidth);
        int startY = centerTileY - (numY / 2 * tileHeight);

        List<Light> lights = new ArrayList<>();
        for (int y = 0; y <= numY; y++) {
            for (int x = 0; x <= numX; x++) {
                // Comment below is from the FIRST version!
                // Draw the tile (red or black square), color[0] is black, color[1] is red
                PointI tLoc = new PointI(leftIndex + x, topIndex + y);
                if (tLoc.inBounds(0, 0, Const.WORLD_SIZE, Const.WORLD_SIZE)) {
                    Tile tile = world.getTileAt(tLoc);
                    Tile north = world.getTileAt(tLoc.delta(0, -1));
                    Tile northEast = world.getTileAt(tLoc.delta(1, -1));
                    Tile east = world.getTileAt(tLoc.delta(1, 0));
                    Tile southEast = world.getTileAt(tLoc.delta(1, 1));
                    Tile south = world.getTileAt(tLoc.delta(0, 1));
                    Tile southWest = world.getTileAt(tLoc.delta(-1, 1));
                    Tile west = world.getTileAt(tLoc.delta(-1, 0));
                    Tile northWest = world.getTileAt(tLoc.delta(-1, -1));

                    ImageAsset topLeft = null;
                    ImageAsset topRight = null;
                    ImageAsset bottomLeft = null;
                    ImageAsset bottomRight = null;

                    if (north != null && north.getId() != tile.getId() && northEast != null && east != null && north.getId() == northEast.getId() && east.getId() == north.getId()) {
                        topRight = north.getImageAsset();
                    }
                    if (east != null && east.getId() != tile.getId() && southEast != null && south != null && east.getId() == southEast.getId() && south.getId() == east.getId()) {
                        bottomRight = east.getImageAsset();
                    }
                    if (south != null && south.getId() != tile.getId() && southWest != null && west != null && south.getId() == southWest.getId() && west.getId() == south.getId()) {
                        bottomLeft = south.getImageAsset();
                    }
                    if (west != null && west.getId() != tile.getId() && northWest != null && north != null && west.getId() == northWest.getId() && north.getId() == west.getId()) {
                        topLeft = west.getImageAsset();
                    }
                    SpriteSheets.TILE_SPRITES.drawTile(g2d, x * tileWidth + startX, y * tileHeight + startY,
                            tileWidth, tileHeight, tile.getImageAssetWithVariants(tLoc, world), topLeft, topRight, bottomLeft, bottomRight);

                    ObjectInstance obj = world.getObjectAt(tLoc);
                    if (obj != null) {
                        GameObject gObj = GameObject.OBJECTS_BY_ID.get(obj.getObjectId());
                        SpriteSheets.OBJ_SPRITES.drawTile(g2d, x * tileWidth + startX, y * tileHeight + startY,
                                tileWidth, tileHeight, gObj.getImageAsset(obj.getUsesLeft()));
                        int light = gObj.getLightRadius();
                        if (light > 0) {
                            lights.add(new Light(new PointI(x * tileWidth + startX + tileWidth / 2, y * tileHeight + startY + tileHeight / 2), light, gObj.getLightFlicker()));
                        }
                    }
                } else {
                    // Off the map
                    g2d.setColor(new Color(255, 255, 255));
                    g2d.fillRect(x * tileWidth + startX, y * tileHeight + startY, tileWidth, tileHeight);
                }
                MobInstance mobInst = world.getMobAt(tLoc);
                if (mobInst != null) {
                    Mob mob = Mob.MOBS_BY_ID.get(mobInst.getMobId());
                    SpriteSheets.OBJ_SPRITES.drawTile(g2d, x * tileWidth + startX, y * tileHeight + startY,
                            tileWidth, tileHeight, mob.getImageAsset());
                    int light = mob.getLight();
                    if (light > 0) {
                        lights.add(new Light(new PointI(x * tileWidth + startX + tileWidth / 2, y * tileHeight + startY + tileHeight / 2), light, 0));
                    }
                }
                if (tLoc.equals(loc)) {
                    if (player.getHealth() > 0) {
                        SpriteSheets.OBJ_SPRITES.drawTile(g2d, x * tileWidth + startX, y * tileHeight + startY,
                                tileWidth, tileHeight,
                                player.isFlying() ? ImageAssets.GUY_FLY : ImageAssets.GUY); // guy block
                    } else {
                        SpriteSheets.OBJ_SPRITES.drawTile(g2d, x * tileWidth + startX, y * tileHeight + startY,
                                tileWidth, tileHeight, ImageAssets.DEAD); // dead guy block
                    }
                    if (player.getBuildingIndex() > 0) {
                        Integer bObj = player.getBuildingObjectIndex();
                        if (bObj != null) {
                            GameObject gObj = GameObject.OBJECTS_BY_ID.get(bObj);
                            System.out.println("Drawing object " + gObj.getName());
                            SpriteSheets.OBJ_SPRITES.drawTile(g2d, x * tileWidth + startX, y * tileHeight + startY,
                                    tileWidth / 2, tileHeight / 2, gObj.getImageAsset(0));
                        }
                    }
                    Tile tileAt = world.getTileAt(tLoc);
                    if (tileAt.isSwim() && !player.isFlying()) {
                        SpriteSheets.OBJ_SPRITES.drawTile(g2d, x * tileWidth + startX, y * tileHeight + startY,
                                tileWidth, tileHeight, tileAt.getSwimAsset()); //swim cover
                    }
                }
            }
        }
        // 0 = day
        // MAX_TIME / 2 = night (120)
        // MAX_TIME = day
        int distFromNight = Math.abs(clock.getTime() - (MAX_TIME / 2));
        double darkness = 1 - ((distFromNight * 2.0) / MAX_TIME);
        // darkness = 1;
        int alpha = (int) Math.floor(darkness * 255.0); // zero is brightest

        Color nightColor = new Color(0, 0, 0, alpha);
        if (nightOverlay == null || nightOverlay.getWidth() != mapWidth || nightOverlay.getHeight() != mapHeight) {
            nightOverlay = new BufferedImage(mapWidth, mapHeight, BufferedImage.TYPE_INT_ARGB);
        }
        Graphics2D nightG2d = nightOverlay.createGraphics();
        nightG2d.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
        nightG2d.fillRect(0,0,mapWidth,mapHeight);
        nightG2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
        nightG2d.setColor(nightColor);
        nightG2d.fillRect(0, 0, mapWidth, mapHeight);
        fadeCircle.drawFade(nightG2d, mapWidth, mapHeight, mapWidth / 2, mapHeight / 2, 400);
        for (Light light : lights) {
            PointI lightLoc = light.getLocation();
            int diameter = light.getDiameter();
            if (clock.getTime() % 2 == 0) {
                diameter -= light.getFlicker();
            }
            fadeCircle.drawFade(nightG2d, mapWidth, mapHeight, lightLoc.getX(), lightLoc.getY(), diameter);
        }

        g2d.drawImage(nightOverlay, 0, 0, null);
        miniMap.draw(g2d, mapWidth, mapHeight);
        hud.draw(g2d, mapWidth, mapHeight);
        inventory.draw(g2d, mapWidth, mapHeight);
        craftingMenu.draw(g2d, mapWidth, mapHeight);

        Graphics2D g2d2 = (Graphics2D) g;
        g2d2.drawImage(backBuffer, 8, 32, null);
    }
}
