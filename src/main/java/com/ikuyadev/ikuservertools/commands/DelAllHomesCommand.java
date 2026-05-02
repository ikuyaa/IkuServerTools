package com.ikuyadev.ikuservertools.commands;

import com.ikuyadev.ikuservertools.data.HomeData;
import com.ikuyadev.ikuservertools.helpers.CommandHelpers;
import com.ikuyadev.ikuservertools.managers.PermissionsManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Map;

public class DelAllHomesCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("delallhomes")
                        .requires(src -> src.isPlayer()
                                && PermissionsManager.canUseDelAllHomesCommand(src.getPlayer()))
                        .executes(ctx -> prompt(ctx.getSource()))
                        .then(Commands.literal("--confirm")
                                .executes(ctx -> execute(ctx.getSource()))
                        )
        );
    }

    private static int prompt(CommandSourceStack source) {
        ServerPlayer player = CommandHelpers.requirePlayer(source);
        if (player == null) return 0;

        Map<String, HomeData.HomeLocation> homes =
                HomeData.get().getHomes(player.getUUID());

        if (homes.isEmpty()) {
            CommandHelpers.failure(source, "You have no homes to delete.");
            return 0;
        }

        player.sendSystemMessage(
                Component.literal("Are you sure you want to delete all homes? ")
                        .withStyle(ChatFormatting.RED)
                        .append(Component.literal("This action cannot be undone. ")
                                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                        .append(Component.literal("[✔]")
                                .withStyle(style -> style
                                        .withColor(ChatFormatting.GREEN)
                                        .withBold(true)
                                        .withClickEvent(new ClickEvent(
                                                ClickEvent.Action.RUN_COMMAND,
                                                "/delallhomes --confirm"
                                        ))
                                        .withHoverEvent(new HoverEvent(
                                                HoverEvent.Action.SHOW_TEXT,
                                                Component.literal("Click to confirm deletion of all homes")
                                                        .withStyle(ChatFormatting.GREEN)
                                        ))
                                ))
        );

        return 1;
    }

    private static int execute(CommandSourceStack source) {
        ServerPlayer player = CommandHelpers.requirePlayer(source);
        if (player == null) return 0;

        Map<String, HomeData.HomeLocation> homes =
                HomeData.get().getHomes(player.getUUID());

        if(homes.isEmpty()) {
            CommandHelpers.failure(source, "You have no homes to delete.");
            return 0;
        }

        int delCount = 0;
        for(String home : new ArrayList<>(homes.keySet())) {
            HomeData.get().deleteHome(player.getUUID(), home);
            delCount++;
        }

        int finalDelCount = delCount;
        CommandHelpers.success(source,() -> Component.literal(("Deleted "))
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(String.valueOf(finalDelCount)).withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE))
                .append(Component.literal(" homes.").withStyle(ChatFormatting.GREEN))
        );
        return 1;
    }
}
