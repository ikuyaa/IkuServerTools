package com.ikuyadev.ikuservertools.commands;

import com.ikuyadev.ikuservertools.helpers.CommandHelpers;
import com.ikuyadev.ikuservertools.managers.PermissionsManager;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameModeArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.Collection;

public class GMCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("gm")
                        .requires(src -> src.isPlayer()
                                && PermissionsManager.canUseGmCommand(src.getPlayer()))
                        .then(Commands.argument("mode", GameModeArgument.gameMode())
                                .executes(ctx -> execute(
                                        ctx.getSource(),
                                        GameModeArgument.getGameMode(ctx, "mode")
                                ))
                                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                        .requires(src -> src.isPlayer()
                                                && PermissionsManager.canUseGmOthers(src.getPlayer()))
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                ctx.getSource().getServer().getPlayerList().getPlayers().stream()
                                                        .map(p -> p.getGameProfile().getName()),
                                                builder
                                        ))
                                        .executes(ctx -> {
                                            Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(ctx, "player");
                                            GameProfile target = profiles.iterator().next();
                                            return execute(
                                                    ctx.getSource(),
                                                    GameModeArgument.getGameMode(ctx, "mode"),
                                                    target.getName()
                                            );
                                        })
                                )
                        )
        );
    }

    public static int execute(CommandSourceStack source, GameType gameMode) {
        return execute(source, gameMode, null);
    }

    public static int execute(CommandSourceStack source, GameType gameMode, String playerName) {
        if(gameMode == null) {
            CommandHelpers.failure(source, "Invalid game mode specified.");
            return 0;
        }
        ServerPlayer sourcePlayer = CommandHelpers.requirePlayer(source);
        if (sourcePlayer == null) return 0;
        ServerPlayer targetPlayer = playerName != null ? source.getServer().getPlayerList().getPlayerByName(playerName) : CommandHelpers.requirePlayer(source);
        if(targetPlayer == null) {
            CommandHelpers.failure(source, Component.literal("Player ").withStyle(ChatFormatting.RED)
                    .append(Component.literal(playerName).withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE))
                    .append(Component.literal(" not found.").withStyle(ChatFormatting.RED))
            );
            return 0;
        }

        targetPlayer.setGameMode(gameMode);

        if(targetPlayer != sourcePlayer) {
            CommandHelpers.success(source, () -> Component.literal("Changed ").withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(targetPlayer.getName().getString() + "'s").withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE))
                    .append(Component.literal(" game mode to ").withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(gameMode.getName()).withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE))
                    .append(Component.literal(".").withStyle(ChatFormatting.GREEN))
            );

            targetPlayer.sendSystemMessage(Component.literal("Your game mode has been set to ").withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(gameMode.getName()).withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE))
                    .append(Component.literal(" by ").withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(source.getPlayer().getName().getString()).withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE))
                    .append(Component.literal(".").withStyle(ChatFormatting.GREEN))
            );
        } else {
            CommandHelpers.success(source, () -> Component.literal("Your game mode has been set to ").withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(gameMode.getName()).withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE))
                    .append(Component.literal(".").withStyle(ChatFormatting.GREEN))
            );
        }

        return 1;

    }
}
