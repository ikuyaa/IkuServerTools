package com.ikuyadev.ikuservertools.events;

import com.ikuyadev.ikuservertools.commands.BackCommand;
import com.ikuyadev.ikuservertools.commands.HomeCommand;
import com.ikuyadev.ikuservertools.commands.SpawnCommand;
import com.ikuyadev.ikuservertools.commands.TPAcceptCommand;
import com.ikuyadev.ikuservertools.commands.WarpCommand;
import com.ikuyadev.ikuservertools.data.PlayerCombatData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public class ServerTickEvents {

    private static int cleanupCounter = 0;
    private static int tpaCleanupCounter = 0;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // set all command tick warmups
        HomeCommand.tickWarmups(event.getServer());
        BackCommand.tickWarmups(event.getServer());
        SpawnCommand.tickWarmups(event.getServer());
        WarpCommand.tickWarmups(event.getServer());
        TPAcceptCommand.tickWarmups(event.getServer());

        // Clean up expired /tpa requests once per second.
        if (++tpaCleanupCounter >= 20) {
            tpaCleanupCounter = 0;
            int expirationSeconds = 10; // Config.TPA_REQUEST_EXPIRATION.get();
            if (expirationSeconds > 0) {
                // TPAData.clearExpiredRequests(expirationSeconds * 1000L);
            }
        }

        // Prune state combat entries every 10 seconds to keep the map bounded.
        if (++cleanupCounter >= 200) {
            cleanupCounter = 0;
            PlayerCombatData.cleanupExpiredEntries();
        }
    }
}
