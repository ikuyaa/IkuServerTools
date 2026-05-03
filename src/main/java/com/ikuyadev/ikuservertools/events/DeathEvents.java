package com.ikuyadev.ikuservertools.events;

import com.ikuyadev.ikuservertools.data.PlayerCombatData;
import com.ikuyadev.ikuservertools.helpers.PlayerHelpers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class DeathEvents {

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerCombatData.clearPlayerCombat(player);
            // Keep /back aligned with the latest significant player event.
            PlayerHelpers.saveBackLocation(player);
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }

        CompoundTag originalData = event.getOriginal().getPersistentData();
        if (!originalData.contains(PlayerHelpers.BACK_TAG, 10)) {
            return;
        }

        event.getEntity().getPersistentData().put(
                PlayerHelpers.BACK_TAG,
                originalData.getCompound(PlayerHelpers.BACK_TAG).copy()
        );
    }
}
