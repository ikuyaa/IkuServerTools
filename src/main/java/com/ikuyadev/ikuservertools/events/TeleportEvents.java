package com.ikuyadev.ikuservertools.events;

import com.ikuyadev.ikuservertools.helpers.PlayerHelpers;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;

public class TeleportEvents {

    @SubscribeEvent
    public void onTeleportCommand(EntityTeleportEvent.TeleportCommand event) {
        // This event is fired for both /tp and /teleport commands.
        // We can use this to capture the player's location before they are teleported for /back.
        if(event.getEntity() instanceof ServerPlayer player) {
            PlayerHelpers.saveBackLocation(player);
        }
    }
}
