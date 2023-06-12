package com.graphics_2d.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

import static java.lang.System.exit;

import com.graphics_2d.Const;
import com.graphics_2d.actions.KeyboardHandler;
import com.graphics_2d.world.entities.Player;
import com.graphics_2d.world.*;

public class GameWindow extends JFrame implements WorldUpdateHandler {

    final private World world;
    final private SpriteSheet spriteSheet;
    final private Hud hud;
    final private Inventory inventory;

    private BufferedImage backBuffer = null;

    final private Player player;

    public GameWindow(World world, Hud hud, Inventory inventory, KeyboardHandler keyboardHandler, SpriteSheet spriteSheet) {
        super("Game Window");
        this.inventory = inventory;
        this.spriteSheet = spriteSheet;
        this.world = world;
        this.hud = hud;
        this.player = world.getPlayer();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exit(0);
            }
        });
        setSize(900, 900);
        addKeyListener(keyboardHandler);
        world.setWorldUpdateHandler(this);

        for(ImageAsset imageAsset : ImageAssets.ALL_ASSETS) {
            imageAsset.addToSpriteSheet(spriteSheet);
        }
        hud.update();
        inventory.update();
    }

    public void paint(Graphics g) {
        drawBoard(g);
    }

    @Override
    public void worldUpdated() {
        repaint();
    }

    public void drawMiniMap(Graphics2D g2d) {
        BufferedImage mm = new BufferedImage(Const.WORLD_SIZE, Const.WORLD_SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < Const.WORLD_SIZE; x++) {
            for (int y = 0; y < Const.WORLD_SIZE; y++) {
                Biome biome = world.getBiomeAt(x, y);
                Color color = biome.getMapColor();
                mm.setRGB(x, y, color.getRGB());
            }
        }
        g2d.drawImage(mm, 0, 0, null);
        g2d.setColor(Color.RED);
        g2d.drawRect(player.getX(), player.getY(), 2, 2);
    }

    public void drawBoard(Graphics g) {
        int mapWidth = getWidth();
        int mapHeight = getHeight();
        if (backBuffer == null || backBuffer.getWidth() != mapWidth || backBuffer.getHeight() != mapHeight) {
            backBuffer = new BufferedImage(mapWidth, mapHeight, BufferedImage.TYPE_INT_ARGB);
        }
        Graphics2D g2d = backBuffer.createGraphics();

        int tileHeight = 128;
        int tileWidth = 128;
        int centerTileX = mapWidth / 2 - (tileWidth / 2);
        int centerTileY = mapHeight / 2 - (tileHeight / 2);
        int numX = Math.ceilDiv(mapWidth, tileWidth);
        int numY = Math.ceilDiv(mapHeight, tileHeight);
        int leftIndex = player.getX() - numX / 2;
        int topIndex = player.getY() - numY / 2;
        int startX = centerTileX - (numX / 2 * tileWidth);
        int startY = centerTileY - (numY / 2 * tileHeight);

        for (int y = 0; y <= numY; y++) {
            for (int x = 0; x <= numX; x++) {
                // Draw the tile (red or black square), color[0] is black, color[1] is red
                int tx = leftIndex + x;
                int ty = topIndex + y;
                if (tx >= 0 && tx < Const.WORLD_SIZE && ty >= 0 && ty < Const.WORLD_SIZE) {
                    Tile tile = world.getTileAt(tx, ty);
                    spriteSheet.drawTile(g2d, x * tileWidth + startX, y * tileHeight + startY,
                            tileWidth, tileHeight, tile.getImageAsset().getIndex());
                    GameObject obj = world.getObjectAt(tx, ty);
                    if (obj != null) {
                        spriteSheet.drawTile(g2d, x * tileWidth + startX, y * tileHeight + startY,
                                tileWidth, tileHeight, obj.getImageAsset().getIndex());
                    }
                } else {
                    // Off the map
                    g2d.setColor(new Color(255, 255, 255));
                    g2d.fillRect(x * tileWidth + startX, y * tileHeight + startY, tileWidth, tileHeight);
                }
                if (tx == player.getX() && ty == player.getY()) {
                    if (player.getHealth() > 0) {
                        spriteSheet.drawTile(g2d, x * tileWidth + startX, y * tileHeight + startY,
                                tileWidth, tileHeight,
                                player.isFlying() ? ImageAssets.GUY_FLY.getIndex() : ImageAssets.GUY.getIndex()); // guy block
                    } else {
                        spriteSheet.drawTile(g2d, x * tileWidth + startX, y * tileHeight + startY,
                                tileWidth, tileHeight, ImageAssets.DEAD.getIndex()); // dead guy block
                    }
                    Tile tileAt = world.getTileAt(tx, ty);
                    System.out.println("tileAt " + tileAt.getName() + " " + tileAt.isSwim());
                    if (tileAt.isSwim()) {
                        spriteSheet.drawTile(g2d, x * tileWidth + startX, y * tileHeight + startY,
                                tileWidth, tileHeight, tileAt.getSwimAsset().getIndex()); //swim cover
                    }
                }
            }
        }
        drawMiniMap(g2d);
        hud.draw(g2d, mapWidth, mapHeight);
        inventory.draw(g2d, mapWidth, mapHeight);

        Graphics2D g2d2 = (Graphics2D) g;
        g2d2.drawImage(backBuffer, 0, 0, null);
    }
}