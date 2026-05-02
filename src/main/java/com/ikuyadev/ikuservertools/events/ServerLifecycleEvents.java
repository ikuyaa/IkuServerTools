package com.ikuyadev.ikuservertools.events;

import com.ikuyadev.ikuservertools.IkuServerTools;
import com.ikuyadev.ikuservertools.data.CooldownData;
import com.ikuyadev.ikuservertools.data.HomeData;
import com.ikuyadev.ikuservertools.data.WarpData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

public class ServerLifecycleEvents {

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        ServerLevel overworld = event.getServer().overworld();

        // Load homes
        HomeData loadedHomes = overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(HomeData::new,
                        (tag, registries) -> HomeData.load(tag)),
                HomeData.getDataName()
        );
        HomeData.setInstance(loadedHomes);
        IkuServerTools.LOGGER.info(
                "IkuServerTools: Loaded home data. Homes tracked for {} player(s).",
                loadedHomes.getHomesMap().size()
        );

        // Load warps
        WarpData loadedWarps = overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(WarpData::new,
                        (tag, registries) -> WarpData.load(tag)),
                WarpData.getDataName()
        );
        WarpData.setInstance(loadedWarps);
        IkuServerTools.LOGGER.info(
                "IkuServerTools: Loaded warp data. Warps tracked: {}.",
                loadedWarps.getWarps().size()
        );

        // Load cooldowns
        CooldownData loadedCooldowns = overworld.getDataStorage().computeIfAbsent(
          new SavedData.Factory<>(CooldownData::new,
                  (tag, registries) -> CooldownData.load(tag)),
                CooldownData.getDataName()
        );
        CooldownData.setInstance(loadedCooldowns);
        IkuServerTools.LOGGER.info("IkuServerTools: Loaded cooldown data.");
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        IkuServerTools.LOGGER.info("IkuServerTools: Server stopping. Forcing data save...");
        event.getServer().overworld().getDataStorage().save();
        IkuServerTools.LOGGER.info("IkuServerTools: Data save complete.");
    }
}
