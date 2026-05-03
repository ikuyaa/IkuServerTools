package com.ikuyadev.ikuservertools.commands;

import com.ikuyadev.ikuservertools.data.WarpData;
import com.ikuyadev.ikuservertools.helpers.CommandHelpers;
import com.ikuyadev.ikuservertools.helpers.WarpSuggestionHelper;
import com.ikuyadev.ikuservertools.managers.PermissionsManager;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public class WarpAllowCommand {
    private static final String WARP_ARG = "warp";
    private static final String PLAYER_ARG = "player";

    private WarpAllowCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("allow")
                .then(Commands.argument(WARP_ARG, StringArgumentType.word())
                        .suggests(WarpSuggestionHelper.SUGGEST_MANAGEABLE_PRIVATE_WARPS)
                        .then(Commands.argument(PLAYER_ARG, GameProfileArgument.gameProfile())
                                .suggests((ctx, builder) -> {
                                    ServerPlayer self = ctx.getSource().getPlayer();
                                    return net.minecraft.commands.SharedSuggestionProvider.suggest(
                                            ctx.getSource().getServer().getPlayerList().getPlayers().stream()
                                                    .filter(p -> self == null || !p.getUUID().equals(self.getUUID()))
                                                    .map(p -> p.getGameProfile().getName()),
                                            builder
                                    );
                                })
                                .executes(ctx -> {
                                    Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(ctx, PLAYER_ARG);
                                    if (profiles.isEmpty()) {
                                        CommandHelpers.failure(ctx.getSource(), "Player not found.");
                                        return 0;
                                    }

                                    GameProfile target = profiles.iterator().next();
                                    return execute(
                                            ctx.getSource(),
                                            StringArgumentType.getString(ctx, WARP_ARG),
                                            target.getId(),
                                            target.getName()
                                    );
                                })
                        )
                );
    }

    private static int execute(CommandSourceStack source, String warpName, UUID targetUuid, String targetName) {
        ServerPlayer player = source.getPlayer();

        Optional<WarpData.WarpLocation> warpOpt = WarpData.get().getWarp(warpName);
        if (warpOpt.isEmpty()) {
            CommandHelpers.failure(source, Component.literal("Warp ")
                    .withStyle(ChatFormatting.RED)
                    .append(Component.literal(warpName).withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE))
                    .append(Component.literal(" not found.").withStyle(ChatFormatting.RED))
            );
            return 0;
        }

        WarpData.WarpLocation warp = warpOpt.get();
        if (warp.type() != WarpData.WarpType.PRIVATE) {
            CommandHelpers.failure(source, "Only private warps can have allowed players.");
            return 0;
        }

        boolean canManage = player != null
            ? PermissionsManager.canManagePrivateWarp(player, warp)
            : source.hasPermission(2);
        if (!canManage) {
            CommandHelpers.failure(source, "Only the warp owner or an operator can modify this warp.");
            return 0;
        }

        if (warp.creator().equals(targetUuid)) {
            CommandHelpers.failure(source, "The warp owner already has access.");
            return 0;
        }

        boolean added = WarpData.get().addAllowedPlayer(warpName, targetUuid);
        if (!added) {
            CommandHelpers.failure(source, "That player already has access, or the warp is invalid.");
            return 0;
        }

        if (source.getServer() != null) {
            source.getServer().overworld().getDataStorage().save();
        }

        CommandHelpers.success(source, () -> Component.literal("Granted access to ")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(targetName).withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE))
                .append(Component.literal(" for warp ").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(warpName).withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE))
                .append(Component.literal(".").withStyle(ChatFormatting.GREEN))
        );
        return 1;
    }
}
