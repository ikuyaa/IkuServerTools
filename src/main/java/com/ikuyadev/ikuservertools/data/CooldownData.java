package com.ikuyadev.ikuservertools.data;

import com.ikuyadev.ikuservertools.enums.CooldownSource;
import com.ikuyadev.ikuservertools.helpers.CommandHelpers;
import com.ikuyadev.ikuservertools.managers.PermissionsManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class CooldownData extends SavedData {
    private static final String DATA_NAME = "ikuservertools_cooldowns";

    private static final String SOURCES_KEY = "sources";
    private static final String SOURCE_KEY = "source";
    private static final String PLAYER_COOLDOWNS_KEY = "command_cooldowns";
    private static final String UUID_KEY = "uuid";
    private static final String EXPIRY_KEY = "expiry";

    private static CooldownData instance;

    private static final Map<CooldownSource, Map<UUID, Date>> cooldownData = new ConcurrentHashMap<>();

    public CooldownData() {
        // Initialize empty maps for each cooldown source
        for (CooldownSource source : CooldownSource.values()) {
            cooldownData.put(source, new ConcurrentHashMap<>());
        }
    }

    public static CooldownData get() {
        if (instance == null) {
            throw new IllegalStateException(
                    "CooldownData has not been loaded yet! " +
                            "Is the server running?"
            );
        }
        return instance;
    }

    public static void setInstance(CooldownData _instance) {
        instance = _instance;
    }

    public static String getDataName() {
        return DATA_NAME;
    }

    //region -- Saving and Loading --
    @Override
    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        ListTag sourceList = new ListTag();

        cooldownData.forEach((source, playerCooldowns) -> {
            CompoundTag sourceTag = new CompoundTag();
            sourceTag.putString(SOURCE_KEY, source.name());

            ListTag cooldownList = new ListTag();
            playerCooldowns.forEach((playerId, expiry) -> {
                CompoundTag playerTag = new CompoundTag();
                playerTag.putUUID(UUID_KEY, playerId);
                playerTag.putLong(EXPIRY_KEY, expiry.getTime());
                cooldownList.add(playerTag);
            });

            sourceTag.put(PLAYER_COOLDOWNS_KEY, cooldownList);
            sourceList.add(sourceTag);
        });

        tag.put(SOURCES_KEY, sourceList);
        return tag;
    }

    public static CooldownData load(CompoundTag tag) {
        CooldownData cdData = new CooldownData();
        ListTag sourceList = tag.getList(SOURCES_KEY, Tag.TAG_COMPOUND);

        for(int i=0; i<sourceList.size(); i++) {
            CompoundTag sourceTag = sourceList.getCompound(i);
            CooldownSource source = CooldownSource.valueOf(sourceTag.getString("source"));
            Map<UUID, Date> playerCooldowns = CooldownData.cooldownData.get(source);

            ListTag cooldownList = sourceTag.getList(PLAYER_COOLDOWNS_KEY, Tag.TAG_COMPOUND);
            for(int j=0; j<cooldownList.size(); j++) {
                CompoundTag cooldownTag = cooldownList.getCompound(j);
                UUID uuid = cooldownTag.getUUID(UUID_KEY);
                long expiry = cooldownTag.getLong(EXPIRY_KEY);
                playerCooldowns.put(uuid, new Date(expiry));

            }
        }

        return cdData;
    }
    //endregion

    //region -- Helpers --
    public void setCooldown(UUID playerId, CooldownSource source, Date expiry) {
        cooldownData.get(source).put(playerId, expiry);
        setDirty();
    }

    public void clearCooldown(UUID playerId, CooldownSource source) {
        if(cooldownData.get(source).remove(playerId) != null) {
            setDirty();
        }
    }

    @SuppressWarnings("unused")
    public Date getCooldown(UUID playerId, CooldownSource source) {
        return cooldownData.get(source).get(playerId);
    }

    public long getCooldownRemaining(UUID playerId, CooldownSource source) {
        Date expiry = cooldownData.get(source).get(playerId);
        if (expiry == null) {
            return 0;
        }
        long remaining = expiry.getTime() - System.currentTimeMillis();
        return Math.max(remaining, 0);
    }

    public void clearExpiredCooldowns() {
        long now = System.currentTimeMillis();
        boolean changed = false;
        // Create a snapshot of sources to avoid concurrent modification issues
        for (CooldownSource source : CooldownSource.values()) {
            Map<UUID, Date> playerCooldowns = cooldownData.get(source);
            if (playerCooldowns != null && 
                playerCooldowns.entrySet().removeIf(entry -> entry.getValue().getTime() <= now)) {
                changed = true;
            }
        }
        if (changed) {
            setDirty();
        }
    }

    public static boolean isOnCooldown(ServerPlayer player, CooldownSource source) {
        if (PermissionsManager.forcesCooldowns(player)) {
            return get().getCooldownRemaining(player.getUUID(), source) > 0;
        }

        if (player.hasPermissions(2)) return false;
        return get().getCooldownRemaining(player.getUUID(), source) > 0;
    }

    public Component formatCooldownMessage(CooldownSource source, long cooldownRemainingMs) {
        String timeText = CommandHelpers.parseCooldown(cooldownRemainingMs);
        return Component.literal("You cannot use ")
                .withStyle(ChatFormatting.RED)
                .append(Component.literal(CommandHelpers.sourceToCommand(source)).withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE))
                .append(Component.literal(" for another ").withStyle(ChatFormatting.RED))
                .append(Component.literal(timeText).withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE)
                );
    }
    //endregion
}
