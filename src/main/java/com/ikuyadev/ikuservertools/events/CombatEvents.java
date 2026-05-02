package com.ikuyadev.ikuservertools.events;

import com.ikuyadev.ikuservertools.data.PlayerCombatData;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public class CombatEvents {

    // Player takes damage (fires after damage is applied)
    @SubscribeEvent
    public void onLivingDamagePost(LivingDamageEvent.Post event) {
        if(event.getEntity() instanceof ServerPlayer player) {
            PlayerCombatData.markPlayerInCombat(player);
        }
    }

    // Player deals damage to something
    @SubscribeEvent
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            PlayerCombatData.markPlayerInCombat(player);
        }
    }
}
