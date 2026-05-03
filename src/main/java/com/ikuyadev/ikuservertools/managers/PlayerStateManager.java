package com.ikuyadev.ikuservertools.managers;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.ikuyadev.ikuservertools.commands.FlyCommand;
import com.ikuyadev.ikuservertools.commands.GodCommand;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForgeMod;

public class PlayerStateManager {
    private static final Set<UUID> restored = ConcurrentHashMap.newKeySet();

    public static void ensureRestored(ServerPlayer player) {
        if (player == null) return;
        UUID id = player.getUUID();
        if (restored.contains(id)) return;

        // Restore fly
        boolean fly = player.getPersistentData().getBoolean(FlyCommand.FLY_KEY);
        if (fly) {
            var flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
            if (flight != null) flight.setBaseValue(1.0);
            player.onUpdateAbilities();
        }

        // Restore godmode
        boolean god = player.getPersistentData().getBoolean(GodCommand.GOD_KEY);
        if (god) {
            player.getAbilities().invulnerable = true;
            player.setInvulnerable(true);
            player.onUpdateAbilities();
        }

        restored.add(id);
    }
}
