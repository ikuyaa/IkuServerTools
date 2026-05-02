package com.ikuyadev.ikuservertools.commands;

import com.ikuyadev.ikuservertools.Config;
import com.ikuyadev.ikuservertools.data.TPAData;
import com.ikuyadev.ikuservertools.helpers.CommandHelpers;
import com.ikuyadev.ikuservertools.managers.PermissionsManager;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

public class TPDenyCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("tpdeny")
                        .requires(src -> src.isPlayer()
                                && PermissionsManager.canUseTpdenyCommand(src.getPlayer()))
                        .executes(ctx -> execute(ctx.getSource(), null))
                        .then(CommandHelpers.tpaArgument()
                                .executes(ctx -> {
                                    Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(ctx, "tp-requests");
                                    GameProfile requester = profiles.iterator().next();
                                    return execute(ctx.getSource(), requester.getName());
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

        CommandHelpers.success(source, () -> Component.literal("Denied teleport request from ")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(requester.getGameProfile().getName()).withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE))
                .append(Component.literal(".").withStyle(ChatFormatting.GREEN))
        );

        requester.sendSystemMessage(Component.literal("Your teleport request to ")
                .withStyle(ChatFormatting.RED)
                .append(Component.literal(target.getGameProfile().getName()).withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE))
                .append(Component.literal(" was denied.").withStyle(ChatFormatting.RED))
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
                CommandHelpers.failure(source, "You have multiple pending teleport requests. Use /tpdeny <player>.");
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
}
