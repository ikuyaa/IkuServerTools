package com.ikuyadev.ikuservertools.data;

import com.ikuyadev.ikuservertools.IkuServerTools;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

public class HomeData extends SavedData {
    private static final String DATA_NAME = "ikuservertools_homes";

    private static HomeData instance;

    public record HomeLocation(
            double x,
            double y,
            double z,
            float yaw,
            float pitch,
            String dimension
    ) {}

    // UUID -> (home name -> HomeLocation)
    private final Map<UUID, Map<String, HomeLocation>> homes = new HashMap<>();

    public HomeData() {}

    public static HomeData get() {
        if (instance == null) {
            throw new IllegalStateException(
                    "HomeData has not been loaded yet! " +
                            "Is the server running?"
            );
        }
        return instance;
    }

    public static void setInstance(HomeData _instance) {
        instance = _instance;
    }

    public static String getDataName() {
        return DATA_NAME;
    }

    //region -- Save / Load --
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag playerList = new ListTag();

        homes.forEach((uuid, homeMap) -> {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID("uuid", uuid);

            ListTag homeList = new ListTag();
            homeMap.forEach((homeName, location) -> {
                CompoundTag homeTag = new CompoundTag();
                homeTag.putString("name", homeName);
                homeTag.putDouble("x", location.x);
                homeTag.putDouble("y", location.y);
                homeTag.putDouble("z", location.z);
                homeTag.putFloat("yaw", location.yaw);
                homeTag.putFloat("pitch", location.pitch);
                homeTag.putString("dimension", location.dimension);

                homeList.add(homeTag);
            });

            playerTag.put("homes", homeList);
            playerList.add(playerTag);
        });

        tag.put("players", playerList);
        return tag;
    }

    public static HomeData load(CompoundTag tag) {
        HomeData manager = new HomeData();
        ListTag playerList = tag.getList("players", Tag.TAG_COMPOUND);

        for (int i = 0; i < playerList.size(); i++) {
            CompoundTag playerTag = playerList.getCompound(i);
            UUID uuid = playerTag.getUUID("uuid");
            Map<String, HomeLocation> homeMap = new HashMap<>();

            ListTag homeList = playerTag.getList("homes", Tag.TAG_COMPOUND);
            for (int j = 0; j < homeList.size(); j++) {
                CompoundTag homeTag = homeList.getCompound(j);
                homeMap.put(
                        homeTag.getString("name"),
                        new HomeLocation(
                                homeTag.getDouble("x"),
                                homeTag.getDouble("y"),
                                homeTag.getDouble("z"),
                                homeTag.getFloat("yaw"),
                                homeTag.getFloat("pitch"),
                                homeTag.getString("dimension")
                        )
                );
            }

            manager.homes.put(uuid, homeMap);
        }

        return manager;
    }
    //endregion

    //region -- Helpers --
    public Map<String, HomeLocation> getHomes(UUID playerId) {
        return Collections.unmodifiableMap(
                homes.getOrDefault(playerId, Collections.emptyMap())
        );
    }

    public Optional<HomeLocation> getHome(UUID playerId, String homeName) {
        return Optional.ofNullable(
                homes.getOrDefault(playerId, Collections.emptyMap()).get(homeName.toLowerCase())
        );
    }

    public boolean setHome(UUID playerId, String homeName, HomeLocation location) {
        homes.computeIfAbsent(playerId, k -> new HashMap<>())
                .put(homeName.toLowerCase(), location);
        setDirty();
        IkuServerTools.LOGGER.info(
                "IkuServerTools: Set home '{}' for {}. Dirty flag set.",
                homeName, playerId
        );

        return true;
    }

    public boolean deleteHome(UUID playerId, String homeName) {
        Map<String, HomeLocation> playerHomes = homes.get(playerId);
        if(playerHomes == null) return false;
        boolean removed = playerHomes.remove(homeName.toLowerCase()) != null;
        if(removed) setDirty();
        return removed;
    }

    public int getHomeCount(UUID playerUuid) {
        return homes.getOrDefault(playerUuid, Collections.emptyMap()).size();
    }

    public Map<UUID, Map<String, HomeLocation>> getHomesMap() {
        return homes;
    }

    public boolean homeExists(UUID playerUuid, String name) {
        return homes.getOrDefault(playerUuid, Collections.emptyMap())
                .containsKey(name.toLowerCase());
    }

    //endregion
}
