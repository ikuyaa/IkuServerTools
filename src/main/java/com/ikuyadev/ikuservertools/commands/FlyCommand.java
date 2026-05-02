package com.ikuyadev.ikuservertools.commands;

import com.ikuyadev.ikuservertools.helpers.CommandHelpers;
import com.ikuyadev.ikuservertools.managers.PermissionsManager;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForgeMod;

import java.util.Collection;

public class FlyCommand {
    public static final String FLY_KEY = "fly";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("fly")
                        .requires(src -> src.isPlayer()
                                && PermissionsManager.canUseFlyCommand(src.getPlayer()))
                        .executes(ctx -> execute(ctx.getSource()))
                        .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                .requires(src -> src.isPlayer()
                                        && PermissionsManager.canUseFlyOthers(src.getPlayer()))
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
        if (player == null) {
            return 0;
        }

        toggleFly(source, player, player);
        return 1;
    }

    private static int execute(CommandSourceStack source, String playerName) {
        ServerPlayer sourcePlayer = CommandHelpers.requirePlayer(source);
        if (sourcePlayer == null) {
            return 0;
        }

        ServerPlayer target = source.getServer().getPlayerList().getPlayerByName(playerName);
        if (target == null) {
            CommandHelpers.failure(source, Component.literal("Player ")
                    .append(Component.literal(playerName))
                    .append(Component.literal(" not found."))
            );
            return 0;
        }

        toggleFly(source, sourcePlayer, target);
        return 1;
    }

    private static void toggleFly(CommandSourceStack source, ServerPlayer sourcePlayer, ServerPlayer target) {
        boolean isFlying = target.getPersistentData().getBoolean(FLY_KEY);
        boolean newState = !isFlying;

        var flight = target.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (flight != null) {
            flight.setBaseValue(newState ? 1.0 : 0.0);
        }
        target.onUpdateAbilities();

        // Save the fly state to player's persistent data
        target.getPersistentData().putBoolean(FLY_KEY, newState);

        String message = newState ? "Flying enabled!" : "Flying disabled.";
        ChatFormatting chatColor = newState ? ChatFormatting.GREEN : ChatFormatting.RED;

        if (target.getUUID().equals(sourcePlayer.getUUID())) {
            CommandHelpers.success(source, message, chatColor);
            return;
        }

        String targetName = target.getName().getString();
        CommandHelpers.success(source, (newState ? "Enabled flight for " : "Disabled flight for ") + targetName + ".", chatColor);
        CommandHelpers.success(target.createCommandSourceStack(), (newState ? "Your flight was enabled by " : "Your flight was disabled by ") + sourcePlayer.getName().getString() + ".", chatColor);
        return;
    }
}
