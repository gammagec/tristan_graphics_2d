package com.graphics_2d;

import com.graphics_2d.actions.Actions;
import com.graphics_2d.actions.KeyboardHandler;
import com.graphics_2d.ai.AiEngine;
import com.graphics_2d.sound.SoundEngine;
import com.graphics_2d.ui.*;
import com.graphics_2d.world.World;

import java.util.Timer;
import java.util.TimerTask;

public class Main {

    // Pixel Pioneer: Guy's Odyssey
    // - By Tristan Gammage
    // - And Christopher Gammage
    // --- Started May 29, 2023

    public static void main(String[] args) {
        SoundEngine soundEngine = new SoundEngine();
        World world = new World();
        AiEngine aiEngine = new AiEngine(world);
        SpriteSheet spriteSheet = new SpriteSheet();
        Hud hud = new Hud(world.getPlayer(), spriteSheet);
        Inventory inventory = new Inventory(world.getPlayer(), spriteSheet);
        MiniMap miniMap = new MiniMap(world);
        CraftingMenu craftingMenu = new CraftingMenu(world, spriteSheet);
        Actions actions = new Actions(world, hud, inventory, soundEngine, aiEngine, miniMap, craftingMenu);
        KeyboardHandler keyboardHandler = new KeyboardHandler(actions);
        actions.setKeyboardHandler(keyboardHandler);
        soundEngine.playBackgroundMusic();
        GameWindow gameWindow = new GameWindow(
                world, hud, inventory, keyboardHandler, spriteSheet, miniMap, craftingMenu);
        aiEngine.populateMobs();
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                aiEngine.updateMobs();
            }
        }, 500, 500);
        gameWindow.setVisible(true);
    }
}