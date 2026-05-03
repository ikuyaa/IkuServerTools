package com.ikuyadev.ikuservertools.commands;

import com.ikuyadev.ikuservertools.Config;
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
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class BackCommand {
    private record WarmupPayload(PlayerHelpers.BackLocation location) {}

    private static final WarmupManager<WarmupData<WarmupPayload>> WARMUP_MANAGER = new WarmupManager<>(
            WarmupData::endTimeMs,
            WarmupData::startPos,
            WarmupData::lastAnnouncedSecond,
            WarmupData::withLastAnnouncedSecond,
            (player, data) -> doBackTeleport(player, data.payload().location()),
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
                Commands.literal("back")
                        .requires(src -> src.isPlayer()
                                && PermissionsManager.canUseBackCommand(src.getPlayer()))
                        .executes(ctx -> execute(ctx.getSource()))
        );
    }

    private static int execute(CommandSourceStack source) {
        ServerPlayer player = CommandHelpers.requirePlayer(source);
        if (player == null) return 0;

        // Check combat
        if(CommandHelpers.isUserInCombat(player, "/back")) return 0;

        // Check cooldown
        if(CommandHelpers.isCommandOnCooldown(player, CooldownSource.BACK)) return 0;

        if (WARMUP_MANAGER.hasPending(player)) {
            CommandHelpers.failure(source, "You are already waiting to teleport back!");
            return 0;
        }

        Optional<PlayerHelpers.BackLocation> optBack = PlayerHelpers.getBackLocation(player);
        if (optBack.isEmpty()) {
            CommandHelpers.failure(source, "No previous teleport location was found.");
            return 0;
        }

        PlayerHelpers.BackLocation location = optBack.get();
        ServerLevel targetLevel = resolveLevel(player.getServer(), location.dimension());
        if (targetLevel == null) {
            CommandHelpers.failure(
                    source,
                    Component.literal("Your last location is in a dimension that is not currently available.")
                            .withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        int warmup = Config.BACK_WARMUP.get();
        if (warmup > 0 && !PermissionsManager.canBypassBackWarmup(player)) {
            startWarmup(player, warmup, location);
        } else {
            doBackTeleport(player, location);
        }

        return 1;
    }

    private static void startWarmup(ServerPlayer player, int seconds, PlayerHelpers.BackLocation location) {
        WarmupData.createOptional(
                seconds,
                player.position(),
                true,
                "back",
                new WarmupPayload(location)
        ).ifPresent(data -> WARMUP_MANAGER.start(player, data.withLastAnnouncedSecond(seconds)));

        player.sendSystemMessage(teleportTargetMessage(seconds));
        player.playNotifySound(
                SoundEvents.NOTE_BLOCK_PLING.value(),
                SoundSource.PLAYERS,
                0.7f,
                1.0f
        );
    }

    private static void doBackTeleport(ServerPlayer player, PlayerHelpers.BackLocation location) {
        ServerLevel targetLevel = resolveLevel(player.getServer(), location.dimension());
        if (targetLevel == null) {
            player.sendSystemMessage(
                    Component.literal("Your last location is in a dimension that is not currently available.")
                            .withStyle(ChatFormatting.RED)
            );
            return;
        }

        // Set cooldown
        int cooldownSecs = Config.BACK_COOLDOWN.get();
        CommandHelpers.setCommandCooldown(player, CooldownSource.BACK, cooldownSecs);

        // /back should swap locations so players can go forward/backward between two points.
        PlayerHelpers.saveBackLocation(player);
        PlayerHelpers.teleportPlayer(
                player,
                targetLevel,
                location.x(),
                location.y(),
                location.z(),
                location.yaw(),
                location.pitch(),
                false
        );
        player.sendSystemMessage(teleportTargetCompleteMessage());
    }

    private static ServerLevel resolveLevel(MinecraftServer server, String dimensionId) {
        if (server == null) return null;

        ResourceLocation loc = CommandHelpers.parseDimensionString(dimensionId);
        if (loc == null) return null;

        ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, loc);
        return server.getLevel(dimKey);
    }

    private static Component teleportTargetMessage(int seconds) {
        return CommandHelpers.teleportWarmupMessage(
                backDestinationComponent(),
                seconds
        );
    }

    private static Component teleportTargetCompleteMessage() {
        return CommandHelpers.teleportCompleteMessage(
                backDestinationComponent()
        );
    }

    private static Component backDestinationComponent() {
        return Component.literal("your last position").withStyle(ChatFormatting.GREEN);
    }

    public static boolean hasWarmupPending(ServerPlayer player) {
        return WARMUP_MANAGER.hasPending(player);
    }

    public static void tickWarmups(MinecraftServer server) {
        WARMUP_MANAGER.tick(server);
    }

}
