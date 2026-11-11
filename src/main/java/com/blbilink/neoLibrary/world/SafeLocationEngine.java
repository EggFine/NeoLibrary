package com.blbilink.neoLibrary.world;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * 套件共享三维安全落点推演引擎：
 * 识别虚空、岩浆、火焰、岩浆块、营火、细雪、仙人掌、甜浆果丛、凋零玫瑰、传送门方块、窒息固体、水下溺水以及下界基岩顶层（{@code Y >= 127}）。
 */
public final class SafeLocationEngine {

    private static final Set<Material> HAZARDOUS_FLOORS = EnumSet.of(
            Material.LAVA,
            Material.FIRE,
            Material.SOUL_FIRE,
            Material.MAGMA_BLOCK,
            Material.CAMPFIRE,
            Material.SOUL_CAMPFIRE,
            Material.CACTUS,
            Material.SWEET_BERRY_BUSH,
            Material.WITHER_ROSE,
            Material.POWDER_SNOW,
            Material.POINTED_DRIPSTONE,
            Material.END_PORTAL,
            Material.NETHER_PORTAL,
            Material.END_GATEWAY
    );

    private static final Set<Material> HAZARDOUS_BODY_BLOCKS = EnumSet.of(
            Material.LAVA,
            Material.FIRE,
            Material.SOUL_FIRE,
            Material.CAMPFIRE,
            Material.SOUL_CAMPFIRE,
            Material.CACTUS,
            Material.SWEET_BERRY_BUSH,
            Material.WITHER_ROSE,
            Material.POWDER_SNOW,
            Material.COBWEB,
            Material.END_PORTAL,
            Material.NETHER_PORTAL,
            Material.END_GATEWAY
    );

    private static final Set<Material> OFFLINE_PASSABLE_BLOCKS = EnumSet.of(
            Material.WATER,
            Material.SHORT_GRASS,
            Material.TALL_GRASS,
            Material.FERN,
            Material.LARGE_FERN,
            Material.DEAD_BUSH,
            Material.TORCH,
            Material.WALL_TORCH,
            Material.SOUL_TORCH,
            Material.SNOW,
            Material.VINE,
            Material.GLOW_LICHEN
    );

    private SafeLocationEngine() {
    }

    public static boolean isAirMaterial(Material mat) {
        return mat == Material.AIR || mat == Material.CAVE_AIR || mat == Material.VOID_AIR;
    }

    private static boolean isSolidMaterial(Material mat) {
        if (mat == null || isAirMaterial(mat)) {
            return false;
        }
        if (Bukkit.getServer() != null) {
            return mat.isSolid();
        }
        return !OFFLINE_PASSABLE_BLOCKS.contains(mat) && !HAZARDOUS_BODY_BLOCKS.contains(mat);
    }

    public static boolean isSafeStandMaterials(Material floor, Material feet, Material head, boolean allowWaterFloor) {
        if (floor == null || feet == null || head == null) {
            return false;
        }
        if (isAirMaterial(floor) || HAZARDOUS_FLOORS.contains(floor)) {
            return false;
        }
        if (floor == Material.WATER) {
            if (!allowWaterFloor) {
                return false;
            }
        } else if (!isSolidMaterial(floor)) {
            return false;
        }

        if (!isBreathablePassable(feet, allowWaterFloor)) {
            return false;
        }
        return isBreathablePassable(head, false);
    }

    private static boolean isBreathablePassable(Material mat, boolean allowWater) {
        if (isAirMaterial(mat)) {
            return true;
        }
        if (HAZARDOUS_BODY_BLOCKS.contains(mat)) {
            return false;
        }
        if (mat == Material.WATER) {
            return allowWater;
        }
        return !isSolidMaterial(mat);
    }

    public static boolean isWaterBiome(Biome biome) {
        if (biome == null) {
            return false;
        }
        String key = biome.getKey().getKey().toLowerCase(Locale.ROOT);
        return key.contains("ocean") || key.contains("river");
    }

    public static Optional<Location> findSafeLanding(Location origin, int horizontalRadius, int verticalRadius) {
        if (origin == null || origin.getWorld() == null) {
            return Optional.empty();
        }
        World world = origin.getWorld();
        int minY = world.getMinHeight() + 1;
        int maxY = world.getEnvironment() == World.Environment.NETHER
                ? Math.min(122, world.getMaxHeight() - 3)
                : world.getMaxHeight() - 2;

        int baseX = origin.getBlockX();
        int baseY = Math.max(minY, Math.min(maxY, origin.getBlockY()));
        int baseZ = origin.getBlockZ();

        if (isPositionSafeInLoadedWorld(world, baseX, baseY, baseZ, minY, maxY)) {
            return Optional.of(centeredLocation(world, baseX, baseY, baseZ, origin.getYaw(), origin.getPitch()));
        }
        if (baseY - 1 >= minY && isPositionSafeInLoadedWorld(world, baseX, baseY - 1, baseZ, minY, maxY)) {
            return Optional.of(centeredLocation(world, baseX, baseY - 1, baseZ, origin.getYaw(), origin.getPitch()));
        }

        for (int r = 0; r <= horizontalRadius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (r > 0 && Math.abs(dx) != r && Math.abs(dz) != r) {
                        continue;
                    }
                    int x = baseX + dx;
                    int z = baseZ + dz;
                    if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                        continue;
                    }
                    for (int dy = 0; dy <= verticalRadius; dy++) {
                        int upY = baseY + dy;
                        if (upY <= maxY && isPositionSafeInLoadedWorld(world, x, upY, z, minY, maxY)) {
                            return Optional.of(centeredLocation(world, x, upY, z, origin.getYaw(), origin.getPitch()));
                        }
                        if (dy > 0) {
                            int downY = baseY - dy;
                            if (downY >= minY && isPositionSafeInLoadedWorld(world, x, downY, z, minY, maxY)) {
                                return Optional.of(centeredLocation(world, x, downY, z, origin.getYaw(), origin.getPitch()));
                            }
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    public static Optional<Location> findColumnSafeSpot(World world, int x, int z, boolean avoidWaterBiomes) {
        if (world == null || !world.isChunkLoaded(x >> 4, z >> 4)) {
            return Optional.empty();
        }
        World.Environment env = world.getEnvironment();
        int minY = world.getMinHeight() + 1;

        if (env == World.Environment.NETHER) {
            for (int y = Math.min(120, world.getMaxHeight() - 4); y >= Math.max(32, minY); y--) {
                if (isPositionSafeInLoadedWorld(world, x, y, z, minY, 122)) {
                    return Optional.of(centeredLocation(world, x, y, z, 0.0f, 0.0f));
                }
            }
            return Optional.empty();
        }

        int highestY = world.getHighestBlockYAt(x, z);
        if (highestY < minY || highestY >= world.getMaxHeight() - 2) {
            return Optional.empty();
        }
        if (avoidWaterBiomes) {
            Biome surfaceBiome = world.getBiome(x, highestY, z);
            if (isWaterBiome(surfaceBiome)) {
                return Optional.empty();
            }
        }
        int standY = highestY + 1;
        if (isPositionSafeInLoadedWorld(world, x, standY, z, minY, world.getMaxHeight() - 2)) {
            return Optional.of(centeredLocation(world, x, standY, z, 0.0f, 0.0f));
        }
        return Optional.empty();
    }

    private static boolean isPositionSafeInLoadedWorld(World world, int x, int standY, int z, int minY, int maxY) {
        if (standY < minY || standY > maxY || !world.isChunkLoaded(x >> 4, z >> 4)) {
            return false;
        }
        Block floor = world.getBlockAt(x, standY - 1, z);
        Block feet = world.getBlockAt(x, standY, z);
        Block head = world.getBlockAt(x, standY + 1, z);
        return isSafeStandMaterials(floor.getType(), feet.getType(), head.getType(), false);
    }

    private static Location centeredLocation(World world, int x, int standY, int z, float yaw, float pitch) {
        return new Location(world, x + 0.5d, standY, z + 0.5d, yaw, pitch);
    }
}
