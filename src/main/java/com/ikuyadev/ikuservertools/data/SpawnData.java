package com.ikuyadev.ikuservertools.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

public class SpawnData extends SavedData {
    private static final String DATA_NAME = "ikuservertools_spawn";

    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private ResourceKey<Level> dimension;
    private boolean hasCustomSpawn = false;

    // Used when no save exists yet. it just pulls from world spawn
    public SpawnData(ServerLevel overworld) {
        this.dimension = Level.OVERWORLD;
        this.x = overworld.getSharedSpawnPos().getX() + 0.5;
        this.y = overworld.getSharedSpawnPos().getY();
        this.z = overworld.getSharedSpawnPos().getZ() + 0.5;
        this.yaw = overworld.getSharedSpawnAngle();
        this.pitch = 0.0f;
    }

    // Used when loading from NBT
    private SpawnData() {}

    // Note the extra HolderLookup.Provider parameter
    public static SpawnData load(CompoundTag tag, HolderLookup.Provider registries) {
        SpawnData data = new SpawnData();
        data.x = tag.getDouble("x");
        data.y = tag.getDouble("y");
        data.z = tag.getDouble("z");
        data.yaw = tag.getFloat("yaw");
        data.pitch = tag.getFloat("pitch");
        data.hasCustomSpawn = tag.getBoolean("hasCustomSpawn");
        data.dimension = ResourceKey.create(
                Registries.DIMENSION,
                ResourceLocation.parse(tag.getString("dimension"))
        );
        return data;
    }

    // Note the extra HolderLookup.Provider parameter
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putDouble("x", x);
        tag.putDouble("y", y);
        tag.putDouble("z", z);
        tag.putFloat("yaw", yaw);
        tag.putFloat("pitch", pitch);
        tag.putBoolean("hasCustomSpawn", hasCustomSpawn);
        tag.putString("dimension", dimension.location().toString());
        return tag;
    }

    public static SpawnData get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(
                        () -> new SpawnData(overworld),
                        SpawnData::load,
                        null // DataFixTypes — not needed
                ),
                DATA_NAME
        );
    }

    public void setSpawn(
            double x, double y, double z,
            float yaw, float pitch,
            ResourceKey<Level> dimension
    ) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.dimension = dimension;
        this.hasCustomSpawn = true;
        this.setDirty();
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public ResourceKey<Level> getDimension() { return dimension; }
    public boolean hasCustomSpawn() { return hasCustomSpawn; }
}
