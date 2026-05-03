package com.ikuyadev.ikuservertools.commands;

import com.ikuyadev.ikuservertools.Config;
import com.ikuyadev.ikuservertools.data.WarmupData;
import com.ikuyadev.ikuservertools.data.WarpData;
import com.ikuyadev.ikuservertools.enums.CooldownSource;
import com.ikuyadev.ikuservertools.helpers.CommandHelpers;
import com.ikuyadev.ikuservertools.helpers.PlayerHelpers;
import com.ikuyadev.ikuservertools.helpers.WarpSuggestionHelper;
import com.ikuyadev.ikuservertools.managers.PermissionsManager;
import com.ikuyadev.ikuservertools.managers.WarmupManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
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

public class WarpCommand {
    private record WarmupPayload(
            String warpName,
            ServerLevel targetLevel,
            WarpData.WarpLocation location
    ) {}

    private static final WarmupManager<WarmupData<WarmupPayload>> WARMUP_MANAGER = new WarmupManager<>(
            WarmupData::endTimeMs,
            WarmupData::startPos,
            WarmupData::lastAnnouncedSecond,
            WarmupData::withLastAnnouncedSecond,
            (player, data) -> {
                WarmupPayload payload = data.payload();
                doTeleport(player, payload.targetLevel(), payload.location(), payload.warpName());
            },
            (player, data) -> {
                player.sendSystemMessage(teleportTargetMessage(data.payload().warpName(), data.lastAnnouncedSecond()));
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
                Commands.literal("warp")
                        .executes(ctx -> {
                            CommandHelpers.failure(ctx.getSource(), "Usage: /warp <name>");
                            return 0;
                        })
                        .then(WarpAllowCommand.build())
                        .then(WarpDenyCommand.build())
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(WarpSuggestionHelper.SUGGEST_ACCESSIBLE_WARPS)
                                .executes(ctx -> execute(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name")
                                ))
                        )
        );
    }

    private static int execute(CommandSourceStack source, String warpName) {
        ServerPlayer player = CommandHelpers.requirePlayer(source);
        if (player == null) return 0;

        if (!PermissionsManager.canUseWarpCommand(player)) {
            CommandHelpers.failure(source, "You do not have permission to use /warp.");
            return 0;
        }

        if (WARMUP_MANAGER.hasPending(player)) {
            CommandHelpers.failure(source, "You are already waiting to warp!");
            return 0;
        }

        if (CommandHelpers.isUserInCombat(player, "/warp")) return 0;
        if (CommandHelpers.isCommandOnCooldown(player, CooldownSource.WARP)) return 0;

        Optional<WarpData.WarpLocation> warpOpt = WarpData.get().getWarp(warpName);
        if (warpOpt.isEmpty()) {
            CommandHelpers.failure(source, Component.literal("Warp ")
                    .withStyle(ChatFormatting.RED)
                    .append(Component.literal(warpName)
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE))
                    .append(Component.literal(" not found.").withStyle(ChatFormatting.RED))
            );
            return 0;
        }

        WarpData.WarpLocation warp = warpOpt.get();
        if (!canPlayerAccess(player, warpName, warp)) {
            CommandHelpers.failure(source, "You do not have access to that warp.");
            return 0;
        }

        ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(warp.dimension()));
        ServerLevel targetLevel = source.getServer().getLevel(dimKey);
        if (targetLevel == null) {
            CommandHelpers.failure(source, Component.literal("The dimension for warp ")
                    .withStyle(ChatFormatting.RED)
                    .append(Component.literal(warp.dimension())
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE))
                    .append(Component.literal(" is not available on this server.")
                            .withStyle(ChatFormatting.RED))
            );
            return 0;
        }

        if (!Config.WARP_ALLOW_CROSS_DIMENSION.get() && !targetLevel.equals(player.serverLevel())) {
            CommandHelpers.failure(source, "Cross-dimension teleportation for warps are disabled on this server.");
            return 0;
        }

        int warmupSeconds = Config.WARP_WARMUP.get();
        if (warmupSeconds > 0 && !PermissionsManager.canBypassWarpWarmup(player)) {
            startWarmup(player, warmupSeconds, targetLevel, warp, warpName);
        } else {
            doTeleport(player, targetLevel, warp, warpName);
        }

        return 1;
    }

    private static void startWarmup(
            ServerPlayer player,
            int seconds,
            ServerLevel targetLevel,
            WarpData.WarpLocation location,
            String warpName
    ) {
        WarmupPayload payload = new WarmupPayload(warpName, targetLevel, location);
        WarmupData.createOptional(seconds, player.position(), true, "warp " + warpName, payload)
                .ifPresent(data -> WARMUP_MANAGER.start(player, data.withLastAnnouncedSecond(seconds)));

        player.sendSystemMessage(teleportTargetMessage(warpName, seconds));
        player.playNotifySound(
                SoundEvents.NOTE_BLOCK_PLING.value(),
                SoundSource.PLAYERS,
                0.7f,
                1.0f
        );
    }

    private static void doTeleport(
            ServerPlayer player,
            ServerLevel targetLevel,
            WarpData.WarpLocation location,
            String warpName
    ) {
        PlayerHelpers.teleportPlayer(
                player,
                targetLevel,
                location.x(),
                location.y(),
                location.z(),
                location.yaw(),
                location.pitch(),
                true
        );

        int cooldownSecs = Config.WARP_COOLDOWN.get();
        if (CommandHelpers.setCommandCooldown(player, CooldownSource.WARP, cooldownSecs) && player.getServer() != null) {
            player.getServer().overworld().getDataStorage().save();
        }
        player.sendSystemMessage(teleportTargetCompleteMessage(warpName));
    }

    private static Component teleportTargetMessage(String warpName, int seconds) {
        return CommandHelpers.teleportWarmupMessage(
                warpDestinationComponent(warpName),
                seconds
        );
    }

    private static Component teleportTargetCompleteMessage(String warpName) {
        return CommandHelpers.teleportCompleteMessage(
                warpDestinationComponent(warpName)
        );
    }

    private static Component warpDestinationComponent(String warpName) {
        return Component.literal("warp ")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(warpName)
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE));
    }

    private static boolean canPlayerAccess(ServerPlayer player, String warpName, WarpData.WarpLocation location) {
        return PermissionsManager.canAccessPrivateWarp(player, warpName, location);
    }

    public static void tickWarmups(MinecraftServer server) {
        WARMUP_MANAGER.tick(server);
    }
}
