package com.ikuyadev.ikuservertools.commands;

import com.ikuyadev.ikuservertools.helpers.CommandHelpers;
import com.ikuyadev.ikuservertools.helpers.AuditLogger;
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

import java.util.Collection;

public class GodCommand {
    public static final String GOD_KEY = "godmode";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("god")
                        .requires(src -> src.isPlayer()
                                && PermissionsManager.canUseGodCommand(src.getPlayer()))
                        .executes(ctx -> execute(ctx.getSource()))
                        .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                .requires(src -> src.isPlayer()
                                        && PermissionsManager.canUseGodOthers(src.getPlayer()))
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

        toggleGodmode(source, player, player);
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

        toggleGodmode(source, sourcePlayer, target);

        return 1;
    }

    private static void toggleGodmode(CommandSourceStack source, ServerPlayer sourcePlayer, ServerPlayer target) {
        boolean isGod = target.getPersistentData().getBoolean(GOD_KEY);
        boolean newState = !isGod;

        target.getAbilities().invulnerable = newState;
        target.setInvulnerable(newState);
        target.onUpdateAbilities();

        // Save to player's persistent data
        target.getPersistentData().putBoolean(GOD_KEY, newState);

        // Audit log
        AuditLogger.logStateChange(target, "godmode", newState);

        String message = newState ? "Godmode enabled." : "Godmode disabled.";
        ChatFormatting chatColor = newState ? ChatFormatting.GREEN : ChatFormatting.RED;

        if (target.getUUID().equals(sourcePlayer.getUUID())) {
            CommandHelpers.success(source, message, chatColor);
            return;
        }

        String targetName = target.getName().getString();
        CommandHelpers.success(source, (newState ? "Enabled godmode for " : "Disabled godmode for ") + targetName + ".", chatColor);
        CommandHelpers.success(target.createCommandSourceStack(), (newState ? "Your godmode was enabled by " : "Your godmode was disabled by ") + sourcePlayer.getName().getString() + ".", chatColor);
    }
}
