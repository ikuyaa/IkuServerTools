package com.ikuyadev.ikuservertools.managers;

import com.ikuyadev.ikuservertools.helpers.PlayerHelpers;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;


public class WarmupManager<T> {
    private final Map<UUID, T> pending = new ConcurrentHashMap<>();

    private final Function<T, Long> getEndTime;
    private final Function<T, Vec3> getStartPos;
    private final Function<T, Integer> getLastAnnounced;
    private final BiFunction<T, Integer, T> withLastAnnounced;
    private final BiConsumer<ServerPlayer, T> onComplete;
    private final BiConsumer<ServerPlayer, T> onCountdown;

    public WarmupManager(
            Function<T, Long> getEndTime,
            Function<T, Vec3> getStartPos,
            Function<T, Integer> getLastAnnounced,
            BiFunction<T, Integer, T> withLastAnnounced,
            BiConsumer<ServerPlayer, T> onComplete,
            BiConsumer<ServerPlayer, T> onCountdown
    ) {
        this.getEndTime = getEndTime;
        this.getStartPos = getStartPos;
        this.getLastAnnounced = getLastAnnounced;
        this.withLastAnnounced = withLastAnnounced;
        this.onComplete = onComplete;
        this.onCountdown = onCountdown;
    }

    public void start(ServerPlayer player, T data) {
        pending.put(player.getUUID(), data);
    }

    public void cancel(ServerPlayer player, String reason) {
        if (pending.remove(player.getUUID()) != null) {
            player.sendSystemMessage(
                    Component.literal("Teleport cancelled! " + reason)
                            .withStyle(ChatFormatting.RED));
        }
    }

    public boolean hasPending(ServerPlayer player) {
        return pending.containsKey(player.getUUID());
    }

    public void tick(MinecraftServer server) {
        if(pending.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        List<UUID> toRemove = new ArrayList<>();

        for(Map.Entry<UUID, T> entry : pending.entrySet()) {
            UUID playerId = entry.getKey();
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null || player.isRemoved()) {
                toRemove.add(playerId);
                continue;
            }

            T data = entry.getValue();
            long msLeft = getEndTime.apply(data) - now;

            // Warmup complete
            if(msLeft <= 0) {
                toRemove.add(playerId);
                onComplete.accept(player, data);
                continue;
            }

            // Movement check
            if(PlayerHelpers.didPlayerMove(player, getStartPos.apply(data))) {
                cancel(player, "You moved!");
                toRemove.add(playerId);
                continue;
            }

            // Announce each second (only if secsLeft changed)
            int secsLeft = (int) Math.ceil(msLeft / 1000.0);
            int lastAnnounced = getLastAnnounced.apply(data);
            if(secsLeft != lastAnnounced && secsLeft > 0) {
                T updated = withLastAnnounced.apply(data, secsLeft);
                pending.put(playerId, updated);
                onCountdown.accept(player, updated);
            }
        }

        // Remove all completed/cancelled warmups
        toRemove.forEach(pending::remove);
    }
}
