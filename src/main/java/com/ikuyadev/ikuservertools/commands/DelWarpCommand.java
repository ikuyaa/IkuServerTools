package com.ikuyadev.ikuservertools.commands;

import com.ikuyadev.ikuservertools.data.WarpData;
import com.ikuyadev.ikuservertools.helpers.CommandHelpers;
import com.ikuyadev.ikuservertools.helpers.WarpSuggestionHelper;
import com.ikuyadev.ikuservertools.managers.PermissionsManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class DelWarpCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("delwarp")
                        .requires(src -> src.isPlayer() && PermissionsManager.canUseWarpCommand(src.getPlayer()))
                        .executes(ctx -> {
                            CommandHelpers.failure(ctx.getSource(), "Usage: /delwarp <name>");
                            return 0;
                        })
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(WarpSuggestionHelper.SUGGEST_ACCESSIBLE_WARPS)
                                .executes(ctx -> prompt(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name")
                                ))
                                .then(Commands.literal("--confirm")
                                        .executes(ctx -> execute(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "name")
                                        ))
                                )
                        )
        );
    }

    private static int prompt(CommandSourceStack source, String warpName) {
        ServerPlayer player = CommandHelpers.requirePlayer(source);
        if (player == null) return 0;

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
        if (!PermissionsManager.canAccessPrivateWarp(player, warpName, warp)) {
            CommandHelpers.failure(source, "You do not have access to that warp.");
            return 0;
        }

        player.sendSystemMessage(
                Component.literal("Are you sure you want to delete warp ")
                        .withStyle(ChatFormatting.RED)
                        .append(Component.literal(warpName)
                                .withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE))
                        .append(Component.literal("? This action cannot be undone. ")
                                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                        )
                        .append(Component.literal("[✔]")
                                .withStyle(style -> style
                                        .withColor(ChatFormatting.GREEN)
                                        .withBold(true)
                                        .withClickEvent(new ClickEvent(
                                                ClickEvent.Action.RUN_COMMAND,
                                                "/delwarp " + warpName + " --confirm"
                                        ))
                                        .withHoverEvent(new HoverEvent(
                                                HoverEvent.Action.SHOW_TEXT,
                                                Component.literal("Click to confirm deletion of warp " + warpName)
                                                        .withStyle(ChatFormatting.GREEN)
                                        ))
                                ))
        );

        return 1;
    }

    private static int execute(CommandSourceStack source, String warpName) {
        ServerPlayer player = CommandHelpers.requirePlayer(source);
        if (player == null) return 0;

        WarpData warpData = WarpData.get();
        Optional<WarpData.WarpLocation> warpOpt = warpData.getWarp(warpName);
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
        if (!PermissionsManager.canAccessPrivateWarp(player, warpName, warp)) {
            CommandHelpers.failure(source, "You do not have access to that warp.");
            return 0;
        }

        if (!warpData.deleteWarp(warpName)) {
            CommandHelpers.failure(source, "Failed to delete warp.");
            return 0;
        }

        if (player.getServer() != null) {
            player.getServer().overworld().getDataStorage().save();
        }

        CommandHelpers.success(source, () -> Component.literal("Deleted warp ")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(warpName)
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE))
                .append(Component.literal(".").withStyle(ChatFormatting.GREEN))
        );
        return 1;
    }
}
