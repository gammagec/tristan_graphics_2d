package com.pixel_pioneer.world;

import com.pixel_pioneer.util.PointI;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Tile {

    private boolean isBlocking;
    private final ImageAsset imageAsset;
    private final String name;
    private final int id;
    private int damage = 0;
    private static int nextId = 0;
    private boolean swim = false;
    private ImageAsset swimAsset;
    private final List<ImageAsset> variants = new ArrayList<>();
    private final List<GameObject> growObjects = new ArrayList<>();

    private static final Random RANDOM = new Random();

    Tile(String name, ImageAsset imageAsset, Map<GameObject, Integer> growObjectWeights) {
        this.isBlocking = false;
        this.imageAsset = imageAsset;
        this.name = name;
        this.id = nextId++;
        for (GameObject obj : growObjectWeights.keySet().toArray(new GameObject[0])) {
            int weight = growObjectWeights.get(obj);
            for (int i = 0; i < weight; i++) {
                growObjects.add(obj);
            }
        }
    }

    public void addVariant(ImageAsset variant) {
        this.variants.add(variant);
    }

    public void setBlocking(boolean blocking) {
        this.isBlocking = blocking;
    }

    public void setSwim(boolean swim) { this.swim = swim; }

    public boolean isSwim() {
        return this.swim;
    }

    public void setSwimAsset(ImageAsset swimAsset) {
        this.swimAsset = swimAsset;
    }

    public ImageAsset getSwimAsset() {
        return swimAsset;
    }

    public void setDamage(int damage) {
        this.damage = damage;
    }

    public int getDamage() {
        return damage;
    }

    public ImageAsset getImageAsset() {
        return imageAsset;
    }

    public ImageAsset getImageAssetWithVariants(PointI loc, World world) {
        int x = loc.getX();
        int y = loc.getY();
        if (variants.size() > 0) {
            double noise = world.getVariantAt(loc);
            if (noise > 0) {
                int variant = (int) Math.floor(noise * variants.size());
                return variants.get(variant);
            }
        }
        return imageAsset;
    }

    public GameObject getGrowObject() {
        if (growObjects.size() > 0) {
            return growObjects.get(RANDOM.nextInt(growObjects.size()));
        } else {
            return null;
        }
    }

    public String getName() {
        return this.name;
    }

    public int getId() {
        return id;
    }

    public boolean isBlocking() {
        return isBlocking;
    }
}
