package com.ikuyadev.ikuservertools.events;

import com.ikuyadev.ikuservertools.managers.PermissionsManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;

public class PermissionEvents {
    @SubscribeEvent
    public void onPermissionGather(PermissionGatherEvent.Nodes event) {
        PermissionsManager.onPermissionGather(event);
    }
}
