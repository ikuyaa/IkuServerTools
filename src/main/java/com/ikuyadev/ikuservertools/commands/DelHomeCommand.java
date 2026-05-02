package com.ikuyadev.ikuservertools.commands;

import com.ikuyadev.ikuservertools.data.HomeData;
import com.ikuyadev.ikuservertools.helpers.CommandHelpers;
import com.ikuyadev.ikuservertools.managers.PermissionsManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;

public class DelHomeCommand {

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_HOMES = (ctx, builder) -> {
        ServerPlayer player = ctx.getSource().getPlayer();
        if(player == null) return builder.buildFuture();

        return SharedSuggestionProvider.suggest(
                HomeData.get().getHomes(player.getUUID()).keySet(),
                builder
        );
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("delhome")
                        .requires(src -> src.isPlayer()
                                && PermissionsManager.canUseDelHomeCommand(src.getPlayer()))
                        .executes(ctx -> {
                            ctx.getSource().sendFailure(Component.literal("Usage: /delhome <name>"));
                            return 0;
                        })
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(SUGGEST_HOMES)
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

    private static int prompt(CommandSourceStack source, String homeName) {
        ServerPlayer player = CommandHelpers.requirePlayer(source);
        if (player == null) return 0;

        // Check if the home exists before prompting
        if (!HomeData.get().homeExists(player.getUUID(), homeName)) {
            CommandHelpers.failure(source, Component.literal("No home named ")
                    .withStyle(ChatFormatting.RED)
                    .append(Component.literal(homeName)
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE))
                    .append(Component.literal(" found.")
                            .withStyle(ChatFormatting.RED))
            );
            return 0;

        }

        player.sendSystemMessage(
                Component.literal("Are you sure you want to delete home ")
                        .withStyle(ChatFormatting.RED)
                        .append(Component.literal(homeName)
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
                                                "/delhome " + homeName + " --confirm"
                                        ))
                                        .withHoverEvent(new HoverEvent(
                                                HoverEvent.Action.SHOW_TEXT,
                                                Component.literal("Click to confirm deletion of home " + homeName)
                                                        .withStyle(ChatFormatting.GREEN)
                                        ))
                                ))
        );

        return 1;
    }

    private static int execute(CommandSourceStack source, String homeName) {
        ServerPlayer player = CommandHelpers.requirePlayer(source);
        if (player == null) return 0;

        boolean deleted = HomeData.get().deleteHome(player.getUUID(), homeName);
        if (deleted) {
            CommandHelpers.success(source, () -> Component.literal("Home ")
                    .withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(homeName)
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE))
                    .append(Component.literal(" has been deleted.")
                            .withStyle(ChatFormatting.GREEN))
            );
        } else {
            CommandHelpers.failure(source, Component.literal("No home named ")
                    .withStyle(ChatFormatting.RED)
                    .append(Component.literal(homeName)
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE))
                    .append(Component.literal(" found.")
                            .withStyle(ChatFormatting.RED))
            );
        }

        return deleted ? 1 : 0;
    }
}
