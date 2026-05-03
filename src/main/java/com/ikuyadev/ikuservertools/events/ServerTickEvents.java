package com.ikuyadev.ikuservertools.events;

import com.ikuyadev.ikuservertools.commands.BackCommand;
import com.ikuyadev.ikuservertools.commands.HomeCommand;
import com.ikuyadev.ikuservertools.commands.SpawnCommand;
import com.ikuyadev.ikuservertools.commands.TPAcceptCommand;
import com.ikuyadev.ikuservertools.commands.WarpCommand;
import com.ikuyadev.ikuservertools.data.PlayerCombatData;
import com.ikuyadev.ikuservertools.data.TPAData;
import com.ikuyadev.ikuservertools.managers.PlayerStateManager;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public class ServerTickEvents {

    private static int cleanupCounter = 0;
    private static int tpaCleanupCounter = 0;
    private static int playerStateCounter = 0;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // Restore player persistent states (fly/god) once per second instead of every tick
        if (++playerStateCounter >= 20) {
            playerStateCounter = 0;
            event.getServer().getPlayerList().getPlayers().forEach(PlayerStateManager::ensureRestored);
        }

        // Set all command tick warmups
        HomeCommand.tickWarmups(event.getServer());
        BackCommand.tickWarmups(event.getServer());
        SpawnCommand.tickWarmups(event.getServer());
        WarpCommand.tickWarmups(event.getServer());
        TPAcceptCommand.tickWarmups(event.getServer());

        // Clean up expired /tpa requests once per second.
        if (++tpaCleanupCounter >= 20) {
            tpaCleanupCounter = 0;
            TPAData.clearExpiredTPARequests();
        }

        // Prune state combat entries every 10 seconds to keep the map bounded.
        if (++cleanupCounter >= 200) {
            cleanupCounter = 0;
            PlayerCombatData.cleanupExpiredEntries();
        }
    }
}
