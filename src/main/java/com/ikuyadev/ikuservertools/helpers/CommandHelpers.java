package com.ikuyadev.ikuservertools.helpers;

import com.ikuyadev.ikuservertools.data.CooldownData;
import com.ikuyadev.ikuservertools.data.PlayerCombatData;
import com.ikuyadev.ikuservertools.data.TPAData;
import com.ikuyadev.ikuservertools.enums.CooldownSource;
import com.ikuyadev.ikuservertools.managers.PermissionsManager;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public class CommandHelpers {
    private static final String PLAYER_ONLY_MESSAGE = "This command can only be used by a player.";

    public static String parseCooldown(long cooldownRemainingMs) {
        if (cooldownRemainingMs <= 0) {
            return "0s";
        }

        long totalSeconds = (long) Math.ceil(cooldownRemainingMs / 1000.0);
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return minutes > 0 ? String.format("%dh %dm", hours, minutes) : String.format("%dh", hours);
        }

        if (minutes > 0) {
            return String.format("%dm", minutes);
        }

        return String.format("%ds", seconds);
    }

    public static String sourceToCommand(CooldownSource source) {
        return switch(source) {
            case SPAWN -> "/spawn";
            case HOME -> "/home";
            case TPA -> "/tpa";
            case WARP -> "/warp";
            case BACK -> "/back";
            default -> "Unknown";
        };
    }

    public static ServerPlayer requirePlayer(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal(PLAYER_ONLY_MESSAGE));
        }
        return player;
    }

    public static void success(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message).withStyle(ChatFormatting.GREEN), false);
    }

    public static void success(CommandSourceStack source, String message, boolean allowLogging) {
        source.sendSuccess(() -> Component.literal(message).withStyle(ChatFormatting.GREEN), allowLogging);
    }

    public static void success(CommandSourceStack source, String message, ChatFormatting color) {
        source.sendSuccess(() -> Component.literal(message).withStyle(color), false);
    }

    public static void success(CommandSourceStack source, Supplier<Component> component) {
        source.sendSuccess(component, false);
    }

    public static void failure(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal(message).withStyle(ChatFormatting.RED));
    }

    public static boolean isUserInCombat(ServerPlayer player, String commandName) {
        boolean inCombat = PlayerCombatData.shouldCombatBlock(player);
        if(inCombat) {
            player.sendSystemMessage(PlayerCombatData.formatCombatMessage(commandName, PlayerCombatData.getMsRemaining(player)));
            return true;
        }

        return false;
    }

    public static boolean isCommandOnCooldown(ServerPlayer player, CooldownSource source) {
        boolean cooldown = CooldownData.isOnCooldown(player, source);
        if (cooldown) {
            long remaining = CooldownData.get().getCooldownRemaining(player.getUUID(), source);
            player.sendSystemMessage(CooldownData.get().formatCooldownMessage(source, remaining));
            return true;
        }

        return false;
    }

    public static boolean setCommandCooldown(ServerPlayer player, CooldownSource source, int cooldownSecs) {
        if (cooldownSecs <= 0) {
            return false;
        }

        if (PermissionsManager.forcesCooldowns(player)) {
            CooldownData.get().setCooldown(player.getUUID(), source, secondsToExpiry(cooldownSecs));
            return true;
        }

        if (player.hasPermissions(2)) {
            return false;
        }

        CooldownData.get().setCooldown(player.getUUID(), source, secondsToExpiry(cooldownSecs));
        return true;
    }

    public static void failure(CommandSourceStack source, Component component) {
        source.sendFailure(component);
    }

    public static Component teleportCompleteMessage(Component destination) {
        return Component.literal("Teleported to ")
                .withStyle(ChatFormatting.GREEN)
                .append(destination)
                .append(Component.literal("!").withStyle(ChatFormatting.GREEN));
    }

    public static Component teleportWarmupMessage(Component destination, int seconds) {
        return Component.literal("Teleporting to ")
                .withStyle(ChatFormatting.GREEN)
                .append(destination)
                .append(Component.literal(" in " + seconds + " second(s). ")
                        .withStyle(ChatFormatting.GREEN))
                .append(Component.literal("Don't move!!!")
                        .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
    }

    public static RequiredArgumentBuilder<CommandSourceStack, GameProfileArgument.Result> playerArgument() {
        return Commands.argument("player", GameProfileArgument.gameProfile())
                .suggests((ctx, builder) -> {
                    ServerPlayer self = ctx.getSource().getPlayer();
                    return SharedSuggestionProvider.suggest(
                            ctx.getSource().getServer().getPlayerList().getPlayers().stream()
                                    .filter(p -> self == null || !p.getUUID().equals(self.getUUID()))
                                    .map(p -> p.getGameProfile().getName()),
                            builder
                    );
                });
    }

    public static RequiredArgumentBuilder<CommandSourceStack, GameProfileArgument.Result> tpaArgument() {
        return Commands.argument("tp-requests", GameProfileArgument.gameProfile())
                .suggests((ctx, builder) -> {
                    ServerPlayer self = ctx.getSource().getPlayer();
                    if (self == null) return builder.buildFuture();

                    TPAData.clearExpiredTPARequests();

                    Map<UUID, Date> requests = TPAData.getPendingRequestsForTarget(self.getUUID());
                    return SharedSuggestionProvider.suggest(
                            requests.keySet().stream()
                                    .map(uuid -> ctx.getSource().getServer().getPlayerList().getPlayer(uuid))
                                    .filter(Objects::nonNull)
                                    .map(p -> p.getGameProfile().getName()),
                            builder
                    );
                });
    }




    /**
     * Validates that coordinates are within safe Minecraft bounds.
     * Y must be between -64 and 320 (Minecraft world height).
     * X and Z can technically be anything, but extreme values might cause issues.
     * @param y the Y coordinate to validate
     * @return true if coordinates are valid
     */
    public static boolean isValidTeleportCoordinate(double y) {
        // Y must be within Minecraft world bounds (-64 to 320)
        return y >= -64 && y <= 320;
    }

    /**
     * Converts milliseconds to a Date object representing when the cooldown will expire.
     * @param cooldownMs the cooldown duration in milliseconds
     * @return a Date object set to the expiry time
     */
    public static Date millisToExpiry(long cooldownMs) {
        return new Date(System.currentTimeMillis() + cooldownMs);
    }

    /**
     * Converts seconds to a Date object representing when the cooldown will expire.
     * @param cooldownSeconds the cooldown duration in seconds
     * @return a Date object set to the expiry time
     */
    public static Date secondsToExpiry(long cooldownSeconds) {
        return millisToExpiry(cooldownSeconds * 1000L);
    }

    /**
     * Safely parses a dimension ID string with error handling.
     * @param dimensionId the dimension identifier string (e.g., "minecraft:overworld")
     * @return ResourceLocation if valid, null if parse fails
     */
    public static net.minecraft.resources.ResourceLocation parseDimensionString(String dimensionId) {
        if (dimensionId == null || dimensionId.isBlank()) {
            return null;
        }
        try {
            return net.minecraft.resources.ResourceLocation.parse(dimensionId);
        } catch (Exception e) {
            return null;
        }
    }
}
