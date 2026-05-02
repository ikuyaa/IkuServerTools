package com.ikuyadev.ikuservertools.managers;

import com.ikuyadev.ikuservertools.helpers.PlayerHelpers;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

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
        for(Map.Entry<UUID, T> entry : pending.entrySet()) {
            UUID playerId = entry.getKey();
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            T data = entry.getValue();

            // Player offline
            if (player == null || player.isRemoved()) {
                pending.remove(playerId, data);
                continue;
            }

            // Movement check
            if(PlayerHelpers.didPlayerMove(player, getStartPos.apply(data))) {
                cancel(player, "You moved!");
                continue;
            }

            long msLeft = getEndTime.apply(data) - now;
            int secsLeft = (int) Math.ceil(msLeft / 1000.0);

            // Announce each second.
            if(secsLeft != getLastAnnounced.apply(data) && secsLeft > 0) {
                T updated = withLastAnnounced.apply(data, secsLeft);
                pending.put(playerId, updated);
                onCountdown.accept(player, updated);
            }

            // Warmup complete
            if(msLeft <= 0) {
                pending.remove(playerId, data);
                onComplete.accept(player, data);
            }
        }
    }
}
