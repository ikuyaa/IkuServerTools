package com.ikuyadev.ikuservertools.commands;

import com.ikuyadev.ikuservertools.helpers.CommandHelpers;
import com.ikuyadev.ikuservertools.managers.PermissionsManager;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

public class FeedCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("feed")
                        .requires(src -> src.isPlayer()
                                && PermissionsManager.canUseFeedCommand(src.getPlayer()))
                        .executes(ctx -> execute(ctx.getSource()))
                        .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                .requires(src -> src.isPlayer()
                                        && PermissionsManager.canUseFeedOthers(src.getPlayer()))
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                        ctx.getSource().getServer().getPlayerList().getPlayers().stream()
                                                .map(p -> p.getGameProfile().getName()),
                                        builder
                                ))
                                .executes(ctx -> {
                                    Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(ctx, "player");
                                    GameProfile target = profiles.iterator().next();
                                    return execute(ctx.getSource(), target.getName());
                                })
                        )
        );
    }

    private static int execute(CommandSourceStack source) {
        ServerPlayer player = CommandHelpers.requirePlayer(source);
        if (player == null) return 0;

        feed(player);
        CommandHelpers.success(source, "Hunger has been replenished!");

        return 1;
    }

    private static int execute(CommandSourceStack source, String playerName) {
        ServerPlayer sourcePlayer = CommandHelpers.requirePlayer(source);
        if (sourcePlayer == null) return 0;

        ServerPlayer target = source.getServer().getPlayerList().getPlayerByName(playerName);
        if (target == null) {
            CommandHelpers.failure(source, Component.literal("Player ")
                    .append(Component.literal(playerName))
                    .append(Component.literal(" not found."))
            );
            return 0;
        }

        feed(target);

        if (target.getUUID().equals(sourcePlayer.getUUID())) {
            CommandHelpers.success(source, "Hunger has been replenished!");
            return 1;
        }

        CommandHelpers.success(source, "Fed " + target.getName().getString() + ".");
        CommandHelpers.success(target.createCommandSourceStack(), "You were fed by " + sourcePlayer.getName().getString() + ".");
        return 1;
    }

    private static void feed(ServerPlayer player) {
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5.0f);
    }
}
