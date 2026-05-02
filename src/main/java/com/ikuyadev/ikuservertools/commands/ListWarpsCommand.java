package com.ikuyadev.ikuservertools.commands;

import com.ikuyadev.ikuservertools.data.WarpData;
import com.ikuyadev.ikuservertools.helpers.CommandHelpers;
import com.ikuyadev.ikuservertools.managers.PermissionsManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.stream.Collectors;

public class ListWarpsCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("listwarps")
                        .requires(src -> src.isPlayer() && PermissionsManager.canUseListWarpsCommand(src.getPlayer()))
                        .executes(ctx -> execute(ctx.getSource()))
        );
    }

    private static int execute(CommandSourceStack source) {
        ServerPlayer player = CommandHelpers.requirePlayer(source);
        if (player == null) return 0;

        Map<String, WarpData.WarpLocation> accessibleWarps = WarpData.get().getWarps().entrySet().stream()
                .filter(entry -> PermissionsManager.canAccessPrivateWarp(player, entry.getKey(), entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (accessibleWarps.isEmpty()) {
            CommandHelpers.failure(source, Component.literal("No accessible warps found. Use ")
                    .withStyle(ChatFormatting.RED)
                    .append(Component.literal("/setwarp <name>").withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE))
                    .append(" to create one.").withStyle(ChatFormatting.RED)
            );
            return 1;
        }

        MutableComponent warpListMessage = Component.literal("Accessible warps: ").withStyle(ChatFormatting.GREEN);
        boolean first = true;
        for (String warpName : accessibleWarps.keySet()) {
            if (!first) {
                warpListMessage.append(Component.literal(" | ").withStyle(ChatFormatting.GREEN));
            }
            warpListMessage.append(clickableWarpNameComponent(warpName));
            first = false;
        }

        CommandHelpers.success(source, () -> warpListMessage);
        return 1;
    }

    private static Component clickableWarpNameComponent(String warpName) {
        return Component.literal(warpName)
                .withStyle(style -> style
                        .withColor(ChatFormatting.GOLD)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/warp " + warpName))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.literal("Click to teleport to warp").withStyle(ChatFormatting.GREEN))));
    }
}
