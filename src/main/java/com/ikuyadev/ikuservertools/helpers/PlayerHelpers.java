package com.ikuyadev.ikuservertools.helpers;

import com.ikuyadev.ikuservertools.data.HomeData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class PlayerHelpers {
    public static final String BACK_TAG = "ikuservertools_back";

    public record BackLocation(
            double x,
            double y,
            double z,
            float yaw,
            float pitch,
            String dimension
    ) {}


    public static boolean didPlayerMove(ServerPlayer player, Vec3 pos) {
        double moved = player.position().distanceTo(pos);
        return moved > 0.15;
    }

    public static void teleportPlayer(
            ServerPlayer player,
            ServerLevel targetLevel,
            double x,
            double y,
            double z,
            float yaw,
            float pitch,
            boolean trackBack
    ) {
        if (trackBack) {
            saveBackLocation(player);
        }

        player.teleportTo(
                targetLevel,
                x, y, z,
                yaw, pitch
        );

        // Keep camera/head/body orientation in sync after server-side teleports.
        player.setYRot(yaw);
        player.setXRot(pitch);
        player.setYHeadRot(yaw);
        player.setYBodyRot(yaw);
    }

    public static void teleportPlayer(
            ServerPlayer player,
            ServerLevel targetLevel,
            double x,
            double y,
            double z,
            float yaw,
            float pitch
    ) {
        teleportPlayer(player, targetLevel, x, y, z, yaw, pitch, false);
    }

    public static void teleportPlayer(
           ServerPlayer player,
           ServerLevel targetLevel,
           HomeData.HomeLocation loc,
           boolean trackBack
    ) {
        teleportPlayer(
                player,
                targetLevel,
                loc.x(), loc.y(), loc.z(),
                loc.yaw(), loc.pitch(),
                trackBack
        );
    }

    public static void saveBackLocation(ServerPlayer player, BackLocation location) {
        CompoundTag backTag = new CompoundTag();
        backTag.putDouble("x", location.x());
        backTag.putDouble("y", location.y());
        backTag.putDouble("z", location.z());
        backTag.putFloat("yaw", location.yaw());
        backTag.putFloat("pitch", location.pitch());
        backTag.putString("dimension", location.dimension());
        player.getPersistentData().put(BACK_TAG, backTag);
    }

    public static void saveBackLocation(ServerPlayer player) {
        saveBackLocation(player, new BackLocation(
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYHeadRot(),
                player.getXRot(),
                player.serverLevel().dimension().location().toString()
        ));
    }

    public static Optional<BackLocation> getBackLocation(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(BACK_TAG, 10)) {
            return Optional.empty();
        }

        CompoundTag backTag = data.getCompound(BACK_TAG);
        if (!backTag.contains("dimension")) {
            return Optional.empty();
        }

        return Optional.of(new BackLocation(
                backTag.getDouble("x"),
                backTag.getDouble("y"),
                backTag.getDouble("z"),
                backTag.getFloat("yaw"),
                backTag.getFloat("pitch"),
                backTag.getString("dimension")
        ));
    }
}
