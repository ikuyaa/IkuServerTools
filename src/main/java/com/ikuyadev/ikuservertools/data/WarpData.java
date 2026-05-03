package com.ikuyadev.ikuservertools.data;

import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class WarpData extends SavedData {
    private static final String DATA_NAME = "ikuservertools_warps";

    private static WarpData instance;

    public enum WarpType {
        PUBLIC,
        PRIVATE
    }

    public record WarpLocation(
            double x,
            double y,
            double z,
            float yaw,
            float pitch,
            String dimension,
            WarpType type,
            Set<UUID> allowedPlayers, // Only for PRIVATE warps
            UUID creator
    ) {
        public WarpLocation {
            allowedPlayers = Collections.unmodifiableSet(new HashSet<>(allowedPlayers == null ? Set.of() : allowedPlayers));
        }
    }

    // Warp name -> WarpLocation
    private final Map<String, WarpLocation> warps = new HashMap<>();
    public WarpData() {}

    public static WarpData get() {
        if (instance == null) {
            throw new IllegalStateException(
                    "WarpData has not been loaded yet! " +
                            "Is the server running?"
            );
        }
        return instance;
    }

    public static void setInstance(WarpData _instance) {
        instance = _instance;
    }

    public static String getDataName() {
        return DATA_NAME;
    }

    //region -- Save / Load --
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag warpList = new ListTag();

        warps.forEach((warpName, location) -> {
            CompoundTag warpTag = new CompoundTag();
            warpTag.putString("name", warpName);

            warpTag.putDouble("x", location.x());
            warpTag.putDouble("y", location.y());
            warpTag.putDouble("z", location.z());
            warpTag.putFloat("yaw", location.yaw());
            warpTag.putFloat("pitch", location.pitch());
            warpTag.putString("dimension", location.dimension());
            warpTag.putString("type", location.type().name());
            warpTag.putUUID("creator", location.creator());

            ListTag allowedPlayerList = new ListTag();
            for (UUID playerUuid : location.allowedPlayers()) {
                CompoundTag allowedPlayerTag = new CompoundTag();
                allowedPlayerTag.putUUID("uuid", playerUuid);
                allowedPlayerList.add(allowedPlayerTag);
            }
            warpTag.put("allowedPlayers", allowedPlayerList);

            warpList.add(warpTag);
        });

        tag.put("warps", warpList);
        return tag;
    }

    public static WarpData load(CompoundTag tag) {
        WarpData manager = new WarpData();
        ListTag warpList = tag.getList("warps", Tag.TAG_COMPOUND);

        for (int i = 0; i < warpList.size(); i++) {
            CompoundTag warpTag = warpList.getCompound(i);

            Set<UUID> allowedPlayers = new HashSet<>();
            ListTag allowedPlayerList = warpTag.getList("allowedPlayers", Tag.TAG_COMPOUND);
            for (int j = 0; j < allowedPlayerList.size(); j++) {
                CompoundTag allowedPlayerTag = allowedPlayerList.getCompound(j);
                if (allowedPlayerTag.hasUUID("uuid")) {
                    allowedPlayers.add(allowedPlayerTag.getUUID("uuid"));
                }
            }

            UUID creator = warpTag.hasUUID("creator")
                    ? warpTag.getUUID("creator")
                    : Util.NIL_UUID;

            manager.warps.put(
                    normalizeWarpName(warpTag.getString("name")),
                    new WarpLocation(
                            warpTag.getDouble("x"),
                            warpTag.getDouble("y"),
                            warpTag.getDouble("z"),
                            warpTag.getFloat("yaw"),
                            warpTag.getFloat("pitch"),
                            warpTag.getString("dimension"),
                            parseWarpType(warpTag.getString("type")),
                            allowedPlayers,
                            creator
                    )
            );
        }

        return manager;
    }
    //endregion

    //region -- Helpers --
    public Map<String, WarpLocation> getWarps() {
        return Collections.unmodifiableMap(warps);
    }

    public Optional<WarpLocation> getWarp(String name) {
        return Optional.ofNullable(warps.get(normalizeWarpName(name)));
    }

    public boolean setWarp(String name, WarpLocation location) {
        warps.put(normalizeWarpName(name), location);
        setDirty();
        return true;
    }

    public boolean deleteWarp(String name) {
        boolean removed = warps.remove(normalizeWarpName(name)) != null;
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public boolean warpExists(String name) {
        return warps.containsKey(normalizeWarpName(name));
    }

    public int getWarpCountByCreator(UUID creatorUuid) {
        if (creatorUuid == null) {
            return 0;
        }

        return (int) warps.values().stream()
                .filter(location -> creatorUuid.equals(location.creator()))
                .count();
    }

    public boolean canAccessWarp(String name, UUID playerUuid, boolean hasPrivatePermission) {
        Optional<WarpLocation> warpOpt = getWarp(name);
        if (warpOpt.isEmpty()) {
            return false;
        }

        WarpLocation warp = warpOpt.get();
        if (warp.type() == WarpType.PUBLIC) {
            return true;
        }

        return warp.creator().equals(playerUuid)
                || warp.allowedPlayers().contains(playerUuid)
                || hasPrivatePermission;
    }

    public boolean addAllowedPlayer(String name, UUID playerUuid) {
        synchronized (warps) {
            WarpLocation warp = warps.get(normalizeWarpName(name));
            if (warp == null || warp.type() != WarpType.PRIVATE || warp.allowedPlayers().contains(playerUuid)) {
                return false;
            }

            Set<UUID> updatedAllowed = new HashSet<>(warp.allowedPlayers());
            updatedAllowed.add(playerUuid);
            warps.put(normalizeWarpName(name), copyWithAllowedPlayers(warp, updatedAllowed));
            setDirty();
            return true;
        }
    }

    public boolean removeAllowedPlayer(String name, UUID playerUuid) {
        synchronized (warps) {
            WarpLocation warp = warps.get(normalizeWarpName(name));
            if (warp == null || !warp.allowedPlayers().contains(playerUuid)) {
                return false;
            }

            Set<UUID> updatedAllowed = new HashSet<>(warp.allowedPlayers());
            updatedAllowed.remove(playerUuid);
            warps.put(normalizeWarpName(name), copyWithAllowedPlayers(warp, updatedAllowed));
            setDirty();
            return true;
        }
    }

    private static WarpType parseWarpType(String value) {
        try {
            return WarpType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return WarpType.PUBLIC;
        }
    }

    private static WarpLocation copyWithAllowedPlayers(WarpLocation source, Set<UUID> allowedPlayers) {
        return new WarpLocation(
                source.x(),
                source.y(),
                source.z(),
                source.yaw(),
                source.pitch(),
                source.dimension(),
                source.type(),
                allowedPlayers,
                source.creator()
        );
    }

    private static String normalizeWarpName(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
    //endregion
}
