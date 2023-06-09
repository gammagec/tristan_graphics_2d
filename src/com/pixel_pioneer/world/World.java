package com.pixel_pioneer.world;

import com.pixel_pioneer.Const;
import com.pixel_pioneer.clock.Clock;
import com.pixel_pioneer.sound.SoundEngine;
import com.pixel_pioneer.util.PointI;
import com.pixel_pioneer.world.biomes.*;
import com.pixel_pioneer.world.entities.Mob;
import com.pixel_pioneer.world.entities.MobInstance;
import com.pixel_pioneer.world.entities.Player;

import java.util.*;
import java.util.stream.Collectors;

public class World {
    private final Player player;
    private WorldUpdateHandler worldUpdateHandler = null;

    private final LocationInfo[][] locations = new LocationInfo[Const.WORLD_SIZE][Const.WORLD_SIZE];

    private final List<MobInstance> mobs = new ArrayList<>();
    private final Random random = new Random();

    private final BiomeGenerator biomeGenerator = new PerlinBiomeGenerator();
    private final BiomeGrower biomeGrower = new DefaultBiomeGrower();

    private final double[][] variantMap = new double[Const.WORLD_SIZE][Const.WORLD_SIZE];

    private final ObjectGrower objectGrower = new DefaultObjectGrower();

    private boolean dirty = true;
    private PointI lastPlayerLoc = new PointI(0, 0);

    public World(Clock clock, SoundEngine soundEngine) {
        player = new Player(clock, soundEngine, this);
        ImageAssets.initialize();

        for (int y = 0; y < Const.WORLD_SIZE; y++) {
            for (int x = 0; x < Const.WORLD_SIZE; x++) {
                locations[y][x] = new LocationInfo();
                //variantMap[y][x] = perlinNoiseGenerator.noise(x, y, 32);
                variantMap[y][x] = random.nextDouble(2) - 1;
            }
        }
        generateBiomes();
        generateMap();
        growInitialObjects();

        player.reset(this);
    }

    public void setDirty() {
        this.dirty = true;
    }

    public void growInitialObjects(){
        for (int i = 0; i < Const.STARTING_GROWTH_CYCLES; i++) {
            objectGrower.growObjects(this);
        }
        dirty = true;
    }

    public void addMob(MobInstance mob) {
        this.mobs.add(mob);
        dirty = true;
    }

    public MobInstance getMobAt(PointI loc) {
        for (MobInstance mob : mobs) {
            PointI p = mob.getLocation();
            if (Objects.equals(p.getX(), loc.getX()) && Objects.equals(p.getY(), loc.getY())) {
                return mob;
            }
        }
        return null;
    }

    public void killMob(MobInstance mob) {
        mobs.remove(mob);
        PointI loc = mob.getLocation();
        ObjectInstance drop = Mob.MOBS_BY_ID.get(mob.getMobId()).getDrop();
        if (drop != null) {
            putObject(loc, drop);
        }
        dirty = true;
    }

    public Player getPlayer() {
        return player;
    }

    public void generateBiomes() {
        biomeGenerator.generateBiomes(locations, this);

        for(int i = 0; i < Const.DEFAULT_GROW_BIOMES; i++) {
            growBiomes();
        }
        dirty = true;
    }

    public PointI randomSpawnPoint() {
        List<PointI> valid = getValidSpawnPoints(
                Arrays.stream(Biomes.ALL_BIOMES).map(Biome::getBiomeId).collect(Collectors.toSet()), false);
        return valid.get(random.nextInt(valid.size()));
    }

    public List<PointI> getValidSpawnPoints(Set<Integer> biomes, boolean includeSwim) {
        System.out.println("Finding a random spawn point");
        List<PointI> validSpawnPoints = new ArrayList<>();
        for (int y = 0; y < Const.WORLD_SIZE; y++) {
            for (int x = 0; x < Const.WORLD_SIZE; x++) {
                PointI loc = new PointI(x, y);
                Tile tile = getTileAt(loc);
                ObjectInstance obj = getObjectAt(loc);
                boolean objBlocking = false;
                if (obj != null) {
                    GameObject gObj = GameObject.OBJECTS_BY_ID.get(obj.getObjectId());
                    objBlocking = gObj.isBlocking();
                }
                Integer biome = getBiomeAt(loc).getBiomeId();
                if ((includeSwim || !tile.isSwim()) &&
                        !tile.isBlocking() &&
                        tile.getDamage() == 0 &&
                        !objBlocking &&
                        biomes.contains(biome)) {
                    validSpawnPoints.add(loc);
                }
            }
        }
        return validSpawnPoints;
    }

    public List<MobInstance> getMobs() {
        return mobs;
    }

    public void growBiomes() {
        biomeGrower.growBiomes(locations, new Integer[] {});
        dirty = true;
    }

    public void removeObject(int x, int y) {
        locations[y][x].setObjectInstance(null);
        dirty = true;
    }

    public void pickupObject(int x, int y) {
        ObjectInstance obj = locations[y][x].getObjectInstance();
        if (obj != null) {
            player.giveObject(obj);
            locations[y][x].setObjectInstance(null);
        }
        dirty = true;
    }

    public Tile getTileAt(PointI loc) {
        if (inBounds(loc)) {
            return Tiles.TILES_BY_ID.get(locations[loc.getY()][loc.getX()].getTileId());
        } else {
            return null;
        }
    }

    public Biome getBiomeAt(PointI loc) {
        if (inBounds(loc)) {
            int biomeId = locations[loc.getY()][loc.getX()].getBiomeId();
            return Biomes.BIOMES_BY_ID.get(biomeId);
        }
        return null;
    }

    public ObjectInstance getObjectAt(PointI loc) {
        if (inBounds(loc)) {
            return locations[loc.getY()][loc.getX()].getObjectInstance();
        } else {
            return null;
        }
    }

    public void generateMap() {
        // This is the map generation!
        for(int i = 0; i < Const.WORLD_SIZE; i++) {
            for (int j = 0; j < Const.WORLD_SIZE; j++) {
                int biomeId = locations[i][j].getBiomeId();
                Biome b = Biomes.BIOMES_BY_ID.get(biomeId);
                int tileId = b.getRandomTileIndex();
                locations[i][j].setTileId(tileId);
            }
        }
        for (int i = 0; i < Const.WORLD_SIZE; i++) {
            for (int j = 0; j < Const.WORLD_SIZE; j++) {
                int biomeId = locations[i][j].getBiomeId();
                Biome b = Biomes.BIOMES_BY_ID.get(biomeId);
                GameObject obj = b.getRandomObject();
                if (obj != GameObjects.NO_OBJECT && obj != null) {
                    locations[i][j].setObjectInstance(new ObjectInstance(obj.getId(), obj.getUses()));
                } else {
                    locations[i][j].setObjectInstance(null);
                }
            }
        }
        dirty = true;
    }

    public boolean inBounds(PointI loc) {
        return loc.inBounds(0, 0, Const.WORLD_SIZE, Const.WORLD_SIZE);
    }

    public boolean isNonBlocking(int x, int y) {
        ObjectInstance obj = locations[y][x].getObjectInstance();
        if (obj != null && GameObject.OBJECTS_BY_ID.get(obj.getObjectId()).isBlocking()) {
            return false;
        }
        return !Tiles.TILES_BY_ID.get(locations[y][x].getTileId()).isBlocking();
    }

    public void setWorldUpdateHandler(WorldUpdateHandler worldUpdateHandler) {
        this.worldUpdateHandler = worldUpdateHandler;
    }

    public void playerUpdated() {
        if (this.worldUpdateHandler != null) {
            // if player dirty
            this.worldUpdateHandler.playerUpdated();
        }
    }

    public void worldUpdated() {
        if (!lastPlayerLoc.equals(player.getLocation())) {
            dirty = true;
        }
        if(this.worldUpdateHandler != null && dirty) {
            this.worldUpdateHandler.worldUpdated();
            dirty = false;
            lastPlayerLoc = player.getLocation();
        }
    }

    public void putObject(PointI loc, ObjectInstance obj) {
        ObjectInstance existingObj = locations[loc.getY()][loc.getX()].getObjectInstance();
        if (existingObj == null) {
            locations[loc.getY()][loc.getX()].setObjectInstance(obj);
        }
        dirty = true;
    }

    public double getVariantAt(PointI loc) {
        return variantMap[loc.getY()][loc.getX()];
    }

    public void removeAllMobs() {
        this.mobs.clear();
        dirty = true;
    }
}
