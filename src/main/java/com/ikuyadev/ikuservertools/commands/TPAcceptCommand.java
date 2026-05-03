package com.ikuyadev.ikuservertools.commands;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import com.ikuyadev.ikuservertools.Config;
import com.ikuyadev.ikuservertools.data.TPAData;
import com.ikuyadev.ikuservertools.data.WarmupData;
import com.ikuyadev.ikuservertools.helpers.CommandHelpers;
import com.ikuyadev.ikuservertools.helpers.PlayerHelpers;
import com.ikuyadev.ikuservertools.managers.PermissionsManager;
import com.ikuyadev.ikuservertools.managers.WarmupManager;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public class TPAcceptCommand {

    private record WarmupPayload(
            String targetPlayerName,
            ServerLevel targetLevel,
            Vec3 targetPosition,
            float targetYaw,
            float targetPitch
    ) {}

    private static final WarmupManager<WarmupData<WarmupPayload>> WARMUP_MANAGER =
            new WarmupManager<>(
                    WarmupData::endTimeMs,
                    WarmupData::startPos,
                    WarmupData::lastAnnouncedSecond,
                    WarmupData::withLastAnnouncedSecond,
                    (player, data) -> {
                        // Save player's current position before teleporting
                        PlayerHelpers.saveBackLocation(player);
                        teleportToTarget(player, data.payload());
                    },
                    (player, data) -> {
                        player.sendSystemMessage(warmupMessage(data.payload().targetPlayerName(), data.lastAnnouncedSecond()));
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
                Commands.literal("tpaccept")
                        .requires(src -> src.isPlayer()
                                && PermissionsManager.canUseTpacceptCommand(src.getPlayer()))
                        .executes(ctx -> execute(ctx.getSource(), null))
                        .then(CommandHelpers.tpaArgument()
                                .executes(ctx -> {
                                    Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(ctx, "tp-requests");
                                    GameProfile target = profiles.iterator().next();
                                    return execute(ctx.getSource(), target.getName());
                                })
                        )
        );
    }

    private static int execute(CommandSourceStack source, String playerName) {
        ServerPlayer target = CommandHelpers.requirePlayer(source);
        if (target == null) return 0;

        TPAData.clearExpiredTPARequests();

        ServerPlayer requester = resolveRequester(source, target, playerName);
        if (requester == null) return 0;

        TPAData.removeTPARequest(requester.getUUID(), target.getUUID());

        if (WARMUP_MANAGER.hasPending(requester)) {
            CommandHelpers.failure(source, Component.literal(requester.getGameProfile().getName())
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE)
                    .append(Component.literal(" is already waiting for a teleport.").withStyle(ChatFormatting.RED))
            );
            return 0;
        }

        int warmupSeconds = PermissionsManager.canBypassTpaWarmup(requester) ? 0 : Config.TPA_WARMUP.get();
        if (warmupSeconds > 0) {
            startWarmup(requester, target, warmupSeconds);
        } else {
            teleportToTarget(requester, buildPayload(requester, target));
        }

        CommandHelpers.success(source, () -> Component.literal("Accepted teleport request from ")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(requester.getGameProfile().getName()).withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE))
                .append(Component.literal(".").withStyle(ChatFormatting.GREEN))
        );

        return 1;
    }

    private static ServerPlayer resolveRequester(CommandSourceStack source, ServerPlayer target, String playerName) {
        Map<UUID, Date> pendingRequests = TPAData.getPendingRequestsForTarget(target.getUUID());
        if (pendingRequests.isEmpty()) {
            CommandHelpers.failure(source, "You have no pending teleport requests.");
            return null;
        }

        MinecraftServer server = source.getServer();
        if (playerName == null || playerName.isBlank()) {
            if (pendingRequests.size() > 1) {
                CommandHelpers.failure(source, "You have multiple pending teleport requests. Use /tpaccept <player>.");
                return null;
            }

            UUID requesterUuid = pendingRequests.keySet().iterator().next();
            ServerPlayer requester = server.getPlayerList().getPlayer(requesterUuid);
            if (requester == null) {
                TPAData.removeTPARequest(requesterUuid, target.getUUID());
                CommandHelpers.failure(source, "Your pending teleport request is no longer valid.");
                return null;
            }
            return requester;
        }

        ServerPlayer requester = server.getPlayerList().getPlayerByName(playerName);
        if (requester == null || !pendingRequests.containsKey(requester.getUUID())) {
            CommandHelpers.failure(source, Component.literal("You do not have a pending teleport request from ")
                    .withStyle(ChatFormatting.RED)
                    .append(Component.literal(playerName).withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE))
                    .append(Component.literal(".").withStyle(ChatFormatting.RED))
            );
            return null;
        }

        return requester;
    }


    private static void startWarmup(ServerPlayer requester, ServerPlayer target, int seconds) {
        WarmupPayload payload = buildPayload(requester, target);
        WarmupData.createOptional(
                seconds,
                requester.position(),
                true,
                "tpa",
                payload
        ).ifPresent(data -> WARMUP_MANAGER.start(requester, data));

        requester.sendSystemMessage(warmupMessage(payload.targetPlayerName(), seconds));
        requester.playNotifySound(
                SoundEvents.NOTE_BLOCK_PLING.value(),
                SoundSource.PLAYERS,
                0.7f,
                1.0f
        );
    }

    private static WarmupPayload buildPayload(ServerPlayer requester, ServerPlayer target) {
        return new WarmupPayload(
                target.getGameProfile().getName(),
                target.serverLevel(),
                target.position(),
                target.getYRot(),
                target.getXRot()
        );
    }

    private static void teleportToTarget(ServerPlayer requester, WarmupPayload payload) {
        PlayerHelpers.teleportPlayer(
                requester,
                payload.targetLevel(),
                payload.targetPosition().x,
                payload.targetPosition().y,
                payload.targetPosition().z,
                payload.targetYaw(),
                payload.targetPitch(),
                false
        );

        requester.sendSystemMessage(CommandHelpers.teleportCompleteMessage(
                Component.literal(payload.targetPlayerName()).withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE)
        ));
    }

    private static Component warmupMessage(String targetPlayerName, int seconds) {
        return CommandHelpers.teleportWarmupMessage(
                Component.literal(targetPlayerName).withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE),
                seconds
        );
    }

    public static void tickWarmups(MinecraftServer server) {
        WARMUP_MANAGER.tick(server);
    }

}
