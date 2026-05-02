package com.ikuyadev.ikuservertools.managers;

import com.ikuyadev.ikuservertools.IkuServerTools;
import com.ikuyadev.ikuservertools.events.*;
import net.neoforged.neoforge.common.NeoForge;

public class EventManager {

    public static void registerEvents() {

        NeoForge.EVENT_BUS.register(new CommandEvents());
        NeoForge.EVENT_BUS.register(new ServerLifecycleEvents());
        NeoForge.EVENT_BUS.register(new PermissionEvents());
        NeoForge.EVENT_BUS.register(new CombatEvents());
        NeoForge.EVENT_BUS.register(new TeleportEvents());
        NeoForge.EVENT_BUS.register(ServerTickEvents.class);

        IkuServerTools.LOGGER.info("IkuServerTools: Registered events successfully!");
    }
}
