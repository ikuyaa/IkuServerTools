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
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

public class ListHomesCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("listhomes")
                        .requires(src -> src.isPlayer() && PermissionsManager.canUseListHomesCommand(src.getPlayer()))
                        .executes(ctx -> execute(ctx.getSource()))
        );
    }

    private static int execute(CommandSourceStack source) {
        ServerPlayer player = CommandHelpers.requirePlayer(source);
        if (player == null) return 0;

        Map<String, HomeData.HomeLocation> homes = HomeData.get().getHomes(player.getUUID());

        if(homes.isEmpty()) {
            CommandHelpers.failure(source, Component.literal("You have no homes set. Use ")
                    .withStyle(ChatFormatting.RED)
                    .append(Component.literal("/sethome <name>").withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE))
                    .append(" to set one.").withStyle(ChatFormatting.RED)
            );
            return 1;
        }

        MutableComponent homeListMessage = Component.literal("Your homes: ").withStyle(ChatFormatting.GREEN);
        boolean first = true;
        for (String homeName : homes.keySet()) {
            if (!first) {
                homeListMessage.append(Component.literal(" | ").withStyle(ChatFormatting.GREEN));
            }
            homeListMessage.append(clickableHomeNameComponent(homeName));
            first = false;
        }
        CommandHelpers.success(source, () -> homeListMessage);

        return 1;
    }

    private static Component clickableHomeNameComponent(String homeName) {
        return Component.literal(homeName)
                .withStyle(style -> style
                        .withColor(ChatFormatting.GOLD)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/home " + homeName))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.literal("Click to teleport home").withStyle(ChatFormatting.GREEN))))
                .append(Component.literal(" [✖]")
                        .withStyle(style -> style
                                .withColor(ChatFormatting.RED)
                                .withUnderlined(false)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/delhome " + homeName))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        Component.literal("Click to delete home").withStyle(ChatFormatting.RED)))));
    }

}
