package com.ikuyadev.ikuservertools.data;

import com.ikuyadev.ikuservertools.Config;
import com.ikuyadev.ikuservertools.enums.CooldownSource;
import com.ikuyadev.ikuservertools.helpers.CommandHelpers;
import com.ikuyadev.ikuservertools.managers.PermissionsManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class PlayerCombatData {
    public static void markPlayerInCombat(ServerPlayer player) {
        if (PermissionsManager.canBypassCombatBlock(player)) {
            return;
        }

        CooldownData cooldownData = CooldownData.get();

        int combatCooldownSeconds = Config.COMBAT_COOLDOWN.get();
        if(combatCooldownSeconds > 0) {
            cooldownData.setCooldown(
                    player.getUUID(),
                    CooldownSource.COMBAT,
                    CommandHelpers.secondsToExpiry(combatCooldownSeconds)
            );
        } else {
            cooldownData.clearCooldown(player.getUUID(), CooldownSource.COMBAT);
        }
    }


    @SuppressWarnings("unused")
    public static long getSecondsRemaining(ServerPlayer player) {
        long remainingMs = CooldownData.get().getCooldownRemaining(player.getUUID(), CooldownSource.COMBAT);

        return Math.max(0, (long) Math.ceil(remainingMs / 1000.0));
    }

    public static long getMsRemaining(ServerPlayer player) {
        return CooldownData.get().getCooldownRemaining(
                player.getUUID(),
                CooldownSource.COMBAT
        );
    }

    public static void cleanupExpiredEntries() {
        CooldownData.get().clearExpiredCooldowns();
    }

    public static Component formatCombatMessage(String commandName, long timeRemainingMS) {

        return Component.literal("You cannot use ")
                .withStyle(style -> style.withColor(ChatFormatting.RED).withUnderlined(false))
                .append(Component.literal(commandName)
                        .withStyle(style -> style.withColor(ChatFormatting.GOLD).withUnderlined(true)))
                .append(Component.literal(" for another ")
                        .withStyle(style -> style.withColor(ChatFormatting.RED).withUnderlined(false)))
                .append(Component.literal(CommandHelpers.parseCooldown(timeRemainingMS))
                        .withStyle(style -> style.withColor(ChatFormatting.GOLD).withUnderlined(true)))
                .append(Component.literal(" due to combat.")
                        .withStyle(style -> style.withColor(ChatFormatting.RED).withUnderlined(false)));
    }

    public static boolean isPlayerInCombat(ServerPlayer player) {
        if (PermissionsManager.shouldForceCombatCooldowns(player)) {
            return getMsRemaining(player) > 0;
        }

        if(player.hasPermissions(2)) return false; // Bypass combat for ops
        return getMsRemaining(player) > 0;
    }

    public static boolean shouldCombatBlock(ServerPlayer player) {
        return isPlayerInCombat(player) && !PermissionsManager.canBypassCombatBlock(player);
    }
}
