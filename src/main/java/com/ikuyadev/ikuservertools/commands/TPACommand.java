package com.ikuyadev.ikuservertools.commands;

import com.ikuyadev.ikuservertools.Config;
import com.ikuyadev.ikuservertools.data.CooldownData;
import com.ikuyadev.ikuservertools.data.PlayerCombatData;
import com.ikuyadev.ikuservertools.data.TPAData;
import com.ikuyadev.ikuservertools.enums.CooldownSource;
import com.ikuyadev.ikuservertools.helpers.CommandHelpers;
import com.ikuyadev.ikuservertools.managers.PermissionsManager;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.Collection;

public class TPACommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("tpa")
                        .requires(src -> src.isPlayer()
                                && PermissionsManager.canUseTpaCommand(src.getPlayer()))
                        .executes(ctx -> {
                            ctx.getSource().sendFailure(Component.literal("Usage: /tpa <player>"));
                            return 0;
                        })
                        .then(CommandHelpers.playerArgument()
                                .executes(ctx -> {
                                    Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(ctx, "player");
                                    GameProfile target = profiles.iterator().next();
                                    ServerPlayer self = ctx.getSource().getPlayer();
                                    if (self != null && target.getId().equals(self.getUUID())) {
                                        CommandHelpers.failure(ctx.getSource(), "You cannot teleport to yourself.");
                                        return 0;
                                    }
                                    return execute(ctx.getSource(), target.getName());
                                })
                        )
        );
    }

    private static int execute(CommandSourceStack source, String playerName) {
        ServerPlayer player = CommandHelpers.requirePlayer(source);
        if (player == null) return 0;

        ServerPlayer target = source.getServer().getPlayerList().getPlayerByName(playerName);

        if (target == null) {
            CommandHelpers.failure(source, Component.literal("Player ")
                    .withStyle(ChatFormatting.RED)
                    .append(Component.literal(playerName).withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE))
                    .append(Component.literal(" not found.").withStyle(ChatFormatting.RED))
            );
            return 0;
        }

        // Check combat cooldown
        if (CommandHelpers.isUserInCombat(player, "/tpa")) return 0;

        // Cooldown check
        if(CommandHelpers.isCommandOnCooldown(player, CooldownSource.TPA)) return 0;

        // Check if there's already a pending request
        if (TPAData.hasPendingRequest(player.getUUID(), target.getUUID())) {
            CommandHelpers.failure(source, Component.literal("You already have a pending teleport request to ")
                    .withStyle(ChatFormatting.RED)
                    .append(Component.literal(target.getName().getString()).withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE))
            );
            return 0;
        }

        // Add the TPA request to the data manager
        TPAData.addTPARequest(player.getUUID(), target.getUUID());

        // Checking if we should apply a cooldown to the requester
        int cooldownSecs = Config.TPA_TELEPORT_COOLDOWN.get();
        CommandHelpers.setCommandCooldown(player, CooldownSource.TPA, cooldownSecs);

        // Send the message and sound alert to the target player
        target.sendSystemMessage(Component.literal("Player ")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(player.getName().getString()).withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE))
                .append(Component.literal(" is requesting to teleport to you. ")
                        .withStyle(ChatFormatting.GREEN)
                )
                .append(Component.literal(("[Accept] "))
                        .withStyle(style ->
                                style
                                        .withColor(ChatFormatting.GREEN)
                                        .withUnderlined(true)
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaccept " + player.getGameProfile().getName()))
                        )

                )
                .append(Component.literal(("[Deny]"))
                        .withStyle(style ->
                                style
                                        .withColor(ChatFormatting.RED)
                                        .withUnderlined(true)
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpdeny " + player.getGameProfile().getName()))
                        )
                ));
        target.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 0.7f, 4.0f);

        return 1;
    }
}
