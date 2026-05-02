package com.ikuyadev.ikuservertools.commands;

import com.ikuyadev.ikuservertools.Config;
import com.ikuyadev.ikuservertools.data.SpawnData;
import com.ikuyadev.ikuservertools.data.WarmupData;
import com.ikuyadev.ikuservertools.enums.CooldownSource;
import com.ikuyadev.ikuservertools.helpers.CommandHelpers;
import com.ikuyadev.ikuservertools.helpers.PlayerHelpers;
import com.ikuyadev.ikuservertools.managers.PermissionsManager;
import com.ikuyadev.ikuservertools.managers.WarmupManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;

public class SpawnCommand {

    private record WarmupPayload() {}

    private static final WarmupManager<WarmupData<WarmupPayload>> WARMUP_MANAGER = new WarmupManager<>(
            WarmupData::endTimeMs,
            WarmupData::startPos,
            WarmupData::lastAnnouncedSecond,
            WarmupData::withLastAnnouncedSecond,
            (player, data) -> doSpawnTeleport(player),
            (player, data) -> {
                player.sendSystemMessage(teleportTargetMessage(data.lastAnnouncedSecond()));
                player.playNotifySound(
                        SoundEvents.NOTE_BLOCK_PLING.value(),
                        SoundSource.PLAYERS,
                        0.7f,
                        1.0f
                );
            }
    );

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("spawn")
                        .requires(src -> src.isPlayer()
                                && PermissionsManager.canUseSpawnCommand(src.getPlayer()))
                        .executes(ctx -> execute(ctx.getSource()))
        );
    }

    private static int execute(CommandSourceStack source) {
        ServerPlayer player = CommandHelpers.requirePlayer(source);
        if (player == null) return 0;

        // Check combat
        if(CommandHelpers.isUserInCombat(player, "/spawn")) return 0;

        // Check cooldown
        if(CommandHelpers.isCommandOnCooldown(player, CooldownSource.SPAWN)) return 0;

        if(WARMUP_MANAGER.hasPending(player)) {
            CommandHelpers.failure(source, "You are already waiting to teleport to spawn!");
            return 0;
        }

        int warmup = Config.SPAWN_WARMUP_TIME.get();
        if(warmup > 0 && !PermissionsManager.canBypassSpawnWarmup(player)) {
            startWarmup(player, warmup);
        } else {
            doSpawnTeleport(player);
        }
        return 1;
    }

    private static void startWarmup(ServerPlayer player, int seconds) {
        WarmupData.createOptional(
                seconds,
                player.position(),
                true,
                "spawn",
                new WarmupPayload()
        ).ifPresent(data -> WARMUP_MANAGER.start(player, data));

        player.sendSystemMessage(teleportTargetMessage(seconds));
        player.playNotifySound(
                SoundEvents.NOTE_BLOCK_PLING.value(),
                SoundSource.PLAYERS,
                0.7f,
                1.0f
        );
    }

    private static void doSpawnTeleport(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if(server == null) return;

        // Reread the latest spawn at completion time incase it was changed while the player was waiting
        SpawnData spawnData = SpawnData.get(server);
        ServerLevel targetLevel;
        double targetX;
        double targetY;
        double targetZ;
        float targetYaw;
        float targetPitch;

        if (!spawnData.hasCustomSpawn()) {
            // No custom spawn set yet; always use the current live world spawn.
            targetLevel = server.getLevel(Level.OVERWORLD);
            if (targetLevel == null) {
                player.sendSystemMessage(
                        Component.literal("The spawn dimension could not be found.")
                                .withStyle(ChatFormatting.RED)
                );
                return;
            }

            BlockPos spawnPos = targetLevel.getSharedSpawnPos();
            targetX = spawnPos.getX() + 0.5;
            // Spawn Y is block-based; +1 places player on top of the spawn block instead of inside it.
            targetY = spawnPos.getY() + 1.0;
            targetZ = spawnPos.getZ() + 0.5;
            targetYaw = targetLevel.getSharedSpawnAngle();
            targetPitch = 0.0f;
        } else {
            targetLevel = server.getLevel(spawnData.getDimension());
            targetX = spawnData.getX();
            targetY = spawnData.getY();
            targetZ = spawnData.getZ();
            targetYaw = spawnData.getYaw();
            targetPitch = spawnData.getPitch();
        }

        if(targetLevel == null) {
            player.sendSystemMessage(
                    Component.literal("The spawn dimension could not be found.")
                            .withStyle(ChatFormatting.RED)
            );
            return;
        }

        PlayerHelpers.teleportPlayer(
                player,
                targetLevel,
                targetX,
                targetY,
                targetZ,
                targetYaw,
                targetPitch,
                true
        );

        int cooldownSecs = Config.SPAWN_COOLDOWN.get();
        CommandHelpers.setCommandCooldown(player, CooldownSource.SPAWN, cooldownSecs);
        player.sendSystemMessage(teleportTargetCompleteMessage());
    }

    public static void cancelWarmup(ServerPlayer player, String reason) {
        WARMUP_MANAGER.cancel(player, reason);
    }

    public static boolean hasWarmupPending(ServerPlayer player) {
        return WARMUP_MANAGER.hasPending(player);
    }

    public static void tickWarmups(MinecraftServer server) {
        WARMUP_MANAGER.tick(server);
    }

    private static Component teleportTargetMessage(int seconds) {
        return CommandHelpers.teleportWarmupMessage(
                spawnDestinationComponent(),
                seconds
        );
    }

    private static Component teleportTargetCompleteMessage() {
        return CommandHelpers.teleportCompleteMessage(
                spawnDestinationComponent()
        );
    }

    private static Component spawnDestinationComponent() {
        return Component.literal("spawn").withStyle(ChatFormatting.GREEN);
    }

}
