package com.ikuyadev.ikuservertools.managers;

import com.ikuyadev.ikuservertools.Config;
import com.ikuyadev.ikuservertools.IkuServerTools;
import com.ikuyadev.ikuservertools.data.WarpData;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.exceptions.UnregisteredPermissionException;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

import java.util.UUID;

import static com.ikuyadev.ikuservertools.IkuServerTools.MODID;

public class PermissionsManager {
    private static final int MAX_HOME_COUNT_NODES = 100;
    private static final int MAX_WARP_COUNT_NODES = 100;

    //region -- Permission Nodes --

    //region -- General --
    public static final PermissionNode<Boolean> PERM_BYPASS_COMBAT_BLOCK = new PermissionNode<>(
            MODID, "bypass.combatblock",
            PermissionTypes.BOOLEAN,
            (player, uuid, ctx) -> player!=null && playerIsOp(player)
    );

    //region -- Home --
    public static final PermissionNode<Boolean> PERM_HOME =
            new PermissionNode<>(MODID, "home",
                    PermissionTypes.BOOLEAN,
                    (player, uuid, ctx) -> player != null
            );
    public static final PermissionNode<Boolean> PERM_UNLIMITED_HOMES =
            new PermissionNode<>(MODID, "home.unlimited",
                    PermissionTypes.BOOLEAN,
                    (player, uuid, ctx) -> player != null && playerIsOp(player)
            );

    public static final PermissionNode<Boolean> PERM_HOME_BYPASS_WARMUP =
            new PermissionNode<>(MODID, "home.bypasswarmup",
                    PermissionTypes.BOOLEAN,
                    (player, uuid, ctx) -> player != null && playerIsOp(player)
            );

    // -- OP --
    public static final PermissionNode<Boolean> PERM_GOD =
            new PermissionNode<>(MODID, "god",
                    PermissionTypes.BOOLEAN,
                    (player, uuid, ctx) -> player != null && playerIsOp(player)
            );

    public static final PermissionNode<Boolean> PERM_GOD_OTHERS =
            new PermissionNode<>(MODID, "god.others",
                    PermissionTypes.BOOLEAN,
                    (player, uuid, ctx) -> player!=null && playerIsOp(player)
            );

    public static final PermissionNode<Boolean> PERM_HEAL =
            new PermissionNode<>(MODID, "heal",
                    PermissionTypes.BOOLEAN,
                    (player, uuid, ctx) -> player != null && playerIsOp(player)
            );

    public static final PermissionNode<Boolean> PERM_HEAL_OTHERS =
            new PermissionNode<>(MODID, "heal.others",
                    PermissionTypes.BOOLEAN,
                    (player, uuid, ctx) -> player!=null && playerIsOp(player)
            );

    public static final PermissionNode<Boolean> PERM_FEED =
            new PermissionNode<>(MODID, "feed",
                    PermissionTypes.BOOLEAN,
                    (player, uuid, ctx) -> player != null && playerIsOp(player)
            );

    public static final PermissionNode<Boolean> PERM_FEED_OTHERS =
            new PermissionNode<>(MODID, "feed.others",
                    PermissionTypes.BOOLEAN,
                    (player, uuid, ctx) -> player!=null && playerIsOp(player)
            );

    public static final PermissionNode<Boolean> PERM_FLY =
            new PermissionNode<>(MODID, "fly",
                    PermissionTypes.BOOLEAN,
                    (player, uuid, ctx) -> player != null && playerIsOp(player)
            );

    public static final PermissionNode<Boolean> PERM_FLY_OTHERS =
            new PermissionNode<>(MODID, "fly.others",
                    PermissionTypes.BOOLEAN,
                    (player, uuid, ctx) -> player!=null && playerIsOp(player)
            );

    public static final PermissionNode<Boolean> PERM_GM =
            new PermissionNode<>(MODID, "gm",
                    PermissionTypes.BOOLEAN,
                    (player, uuid, ctx) -> player != null && playerIsOp(player)
            );

    public static final PermissionNode<Boolean> PERM_GM_OTHERS =
            new PermissionNode<>(MODID, "gm.others",
                    PermissionTypes.BOOLEAN,
                    (player, uuid, ctx) -> player!=null && playerIsOp(player)
            );

    // Spawn
    public static final PermissionNode<Boolean> PERM_SPAWN =
            new PermissionNode<>(MODID, "spawn",
                    PermissionTypes.BOOLEAN,
                    (player, uuid, ctx) -> player != null
            );

    public static final PermissionNode<Boolean> PERM_SPAWN_BYPASS_WARMUP =
            new PermissionNode<>(MODID, "spawn.bypasswarmup",
                    PermissionTypes.BOOLEAN,
                    (player, uuid, ctx) -> player != null && playerIsOp(player)
            );

    public static final PermissionNode<Boolean> PERM_SPAWN_SET =
            new PermissionNode<>(MODID, "spawn.set",
                    PermissionTypes.BOOLEAN,
                    (player, uuid, ctx) -> player != null && playerIsOp(player)
            );

    // TP
    public static final PermissionNode<Boolean> PERM_BACK =
            new PermissionNode<>(MODID, "back",
                    PermissionTypes.BOOLEAN,
                    (player, uuid, ctx) -> player != null && playerIsOp(player)
            );

    public static final PermissionNode<Boolean> PERM_BACK_BYPASS_WARMUP =
            new PermissionNode<>(MODID, "back.bypasswarmup",
                    PermissionTypes.BOOLEAN,
                    (player, uuid, ctx) -> player != null && playerIsOp(player)
            );

    public static final PermissionNode<Boolean> PERM_TPA =
            new PermissionNode<>(MODID, "tpa",
                    PermissionTypes.BOOLEAN,
                    (player, uuid, ctx) -> player != null
            );
    public static final PermissionNode<Boolean> PERM_TPA_BYPASS_WARMUP =
            new PermissionNode<>(MODID, "tpa.bypasswarmup",
                    PermissionTypes.BOOLEAN,
                    (player, uuid, ctx) -> player != null && playerIsOp(player)
            );

    // Warp
    public static final PermissionNode<Boolean> PERM_WARP =
            new PermissionNode<>(MODID, "warp",
                    PermissionTypes.BOOLEAN,
                    (player, uuid, ctx) -> player != null
            );

    public static final PermissionNode<Boolean> PERM_WARP_CREATE =
            new PermissionNode<>(MODID, "warp.create",
                    PermissionTypes.BOOLEAN,
                    (player, uuid, ctx) -> player != null && playerIsOp(player)
            );

    public static final PermissionNode<Boolean> PERM_WARP_UNLIMITED =
            new PermissionNode<>(MODID, "warp.unlimited",
                    PermissionTypes.BOOLEAN,
                    (player, uuid, ctx) -> player != null && playerIsOp(player)
            );

    public static final PermissionNode<Boolean> PERM_WARP_BYPASS_WARMUP =
            new PermissionNode<>(MODID, "warp.bypasswarmup",
                    PermissionTypes.BOOLEAN,
                    (player, uuid, ctx) -> player != null && playerIsOp(player)
            );

    // Dev
    public static final PermissionNode<Boolean> PERM_FORCE_WARMUPS =
            new PermissionNode<>(MODID, "forcewarmups",
                    PermissionTypes.BOOLEAN,
                    (player, uuid, ctx) -> false
            );

    public static final PermissionNode<Boolean> PERM_FORCE_COMBATCD =
            new PermissionNode<>(MODID, "forcecombatcd",
                    PermissionTypes.BOOLEAN,
                    (player, uuid, ctx) -> false
            );

    public static final PermissionNode<Boolean> PERM_FORCE_COOLDOWNS =
            new PermissionNode<>(MODID, "forcecooldowns",
                    PermissionTypes.BOOLEAN,
                    (player, uuid, ctx) -> false
            );


    //endregion

    //endregion

    private static final PermissionNode<Boolean>[] HOME_COUNT_NODES = buildHomeCountNodes();
    private static final PermissionNode<Boolean>[] WARP_COUNT_NODES = buildWarpCountNodes();

    // Making the count nodes so we can assign ikuservertools.homes.count.{number} permissions without needing dynamic node registration support from the permission system.
    @SuppressWarnings("unchecked")
    private static PermissionNode<Boolean>[] buildHomeCountNodes() {
        // Index 0 unused; valid indices are 1 - MAX_HOME_COUNT_NODES
        PermissionNode<Boolean>[] nodes = new PermissionNode[MAX_HOME_COUNT_NODES + 1];
        for (int i = 1; i <= MAX_HOME_COUNT_NODES; i++) {
            nodes[i] = new PermissionNode<>(
                    MODID, "home.count." + i,
                    PermissionTypes.BOOLEAN,
                    // No player should have this by default; explicit grant only
                    (player, uuid, ctx) -> false
            );
        }
        return nodes;
    }

    @SuppressWarnings("unchecked")
    private static PermissionNode<Boolean>[] buildWarpCountNodes() {
        // Index 0 unused; valid indices are 1 - MAX_WARP_COUNT_NODES
        PermissionNode<Boolean>[] nodes = new PermissionNode[MAX_WARP_COUNT_NODES + 1];
        for (int i = 1; i <= MAX_WARP_COUNT_NODES; i++) {
            nodes[i] = new PermissionNode<>(
                    MODID, "warp.count." + i,
                    PermissionTypes.BOOLEAN,
                    // No player should have this by default; explicit grant only
                    (player, uuid, ctx) -> false
            );
        }
        return nodes;
    }

    // Register all permissions here
    public static void onPermissionGather(PermissionGatherEvent.Nodes event) {
        event.addNodes(
                // General
                PERM_BYPASS_COMBAT_BLOCK,

                // Home
                PERM_HOME,
                PERM_UNLIMITED_HOMES,
                PERM_HOME_BYPASS_WARMUP,

                // OP
                PERM_GOD,
                PERM_GOD_OTHERS,
                PERM_HEAL,
                PERM_HEAL_OTHERS,
                PERM_FEED,
                PERM_FEED_OTHERS,
                PERM_FLY,
                PERM_FLY_OTHERS,
                PERM_GM,
                PERM_GM_OTHERS,

                // Spawn
                PERM_SPAWN,
                PERM_SPAWN_BYPASS_WARMUP,
                PERM_SPAWN_SET,

                // TP
                PERM_BACK,
                PERM_BACK_BYPASS_WARMUP,
                PERM_TPA,
                PERM_TPA_BYPASS_WARMUP,

                // Warp
                PERM_WARP,
                PERM_WARP_CREATE,
                PERM_WARP_UNLIMITED,
                PERM_WARP_BYPASS_WARMUP,

                // Dev
                PERM_FORCE_WARMUPS,
                PERM_FORCE_COMBATCD,
                PERM_FORCE_COOLDOWNS
        );

        // Register all the home count nodes
        for(int i = 1; i <= MAX_HOME_COUNT_NODES; i++) {
            event.addNodes(HOME_COUNT_NODES[i]);
        }

        // Register all the warp count nodes
        for(int i = 1; i <= MAX_WARP_COUNT_NODES; i++) {
            event.addNodes(WARP_COUNT_NODES[i]);
        }
    }

    //region -- Helpers --
    private static boolean playerIsOp(ServerPlayer player) {
        return player.hasPermissions(2);
    }

    private static boolean getPermissionSafe(
            ServerPlayer player,
            PermissionNode<Boolean> node,
            boolean fallback
    ) {
        try {
            return PermissionAPI.getPermission(player, node);
        } catch (UnregisteredPermissionException ignored) {
            return fallback;
        } catch (RuntimeException e) {
            IkuServerTools.LOGGER.warn(
                    "IkuServerTools: Permission check for '{}' failed with {}: {}. Defaulting to {}",
                    node.getNodeName(),
                    e.getClass().getSimpleName(),
                    e.getMessage(),
                    fallback
            );
            return fallback;
        }
    }

    /**
     * Resolves the home limit for a player using this priority order:
     *   1. OP (level 2+)                      → maxHomesOp
     *   2. ikuservertools.homes.unlimited      → maxHomesOp
     *   3. ist.homes.count.{number} → the highest matching number
     *   4. Fallback                            → maxHomes
     */
    public static int resolveHomeLimit(ServerPlayer player) {
        // 1. OP always gets maxHomesOp
        if (player.hasPermissions(2)) {
            return Config.MAX_HOMES_OP.get();
        }

        // 2. Unlimited permission gets maxHomesOp
        if (getPermissionSafe(player, PERM_UNLIMITED_HOMES, false)) {
            return Config.MAX_HOMES_OP.get();
        }

        // 3. Check count nodes from maxHomesOp down to defaultLimit+1
        int maxToCheck = Math.min(Config.MAX_HOMES_OP.get(), MAX_HOME_COUNT_NODES);
        int defaultLimit = Config.MAX_HOMES.get();

        for (int i = maxToCheck; i > defaultLimit; i--) {
            if(getPermissionSafe(player, HOME_COUNT_NODES[i], false)) {
                return i;
            }
        }

        // 4. Default
        return defaultLimit;
    }

    /**
     * Resolves the warp limit for a player using this priority order:
     *   1. OP (level 2+)                      -> maxWarpsOp
     *   2. ikust.warp.unlimited               -> maxWarpsOp
     *   3. ikust.warp.count.{number}          -> highest matching number
     *   4. Fallback                           -> maxWarps
     */
    public static int resolveWarpLimit(ServerPlayer player) {
        // 1. OP always gets maxWarpsOp
        if (player.hasPermissions(2)) {
            return Config.MAX_WARPS_OP.get();
        }

        // 2. Unlimited permission gets maxWarpsOp
        if (getPermissionSafe(player, PERM_WARP_UNLIMITED, false)) {
            return Config.MAX_WARPS_OP.get();
        }

        // 3. Check count nodes from maxWarpsOp down to defaultLimit+1
        int maxToCheck = Math.min(Config.MAX_WARPS_OP.get(), MAX_WARP_COUNT_NODES);
        int defaultLimit = Config.MAX_WARPS.get();

        for (int i = maxToCheck; i > defaultLimit; i--) {
            if(getPermissionSafe(player, WARP_COUNT_NODES[i], false)) {
                return i;
            }
        }

        // 4. Default
        return defaultLimit;
    }

    // General
    public static boolean canBypassCombatBlock(ServerPlayer player) {
        if (shouldForceCombatCooldowns(player)) {
            return false;
        }

        return getPermissionSafe(player, PERM_BYPASS_COMBAT_BLOCK, player!=null && playerIsOp(player));
    }

    // Home
    public static boolean canUseHomeCommand(ServerPlayer player) {
        return getPermissionSafe(player, PERM_HOME, player!=null);
    }

    public static boolean canUseSetHomeCommand(ServerPlayer player) {
        return canUseHomeCommand(player);
    }

    public static boolean canUseListHomesCommand(ServerPlayer player) {
        return canUseHomeCommand(player);
    }

    public static boolean canUseDelHomeCommand(ServerPlayer player) {
        return canUseHomeCommand(player);
    }

    public static boolean canUseDelAllHomesCommand(ServerPlayer player) {
        return canUseHomeCommand(player);
    }

    public static boolean canBypassHomeWarmup(ServerPlayer player) {
        if (shouldForceWarmups(player)) {
            return false;
        }

        return getPermissionSafe(player, PERM_HOME_BYPASS_WARMUP, player!=null && playerIsOp(player));
    }

    // OP
    public static boolean canUseGodCommand(ServerPlayer player) {
        return getPermissionSafe(player, PERM_GOD, player!=null && playerIsOp(player));
    }

    public static boolean canUseGodOthers(ServerPlayer player) {
        return getPermissionSafe(player, PERM_GOD_OTHERS, player!=null && playerIsOp(player));
    }

    public static boolean canUseHealCommand(ServerPlayer player) {
        return getPermissionSafe(player, PERM_HEAL, player!=null && playerIsOp(player));
    }

    public static boolean canUseHealOthers(ServerPlayer player) {
        return getPermissionSafe(player, PERM_HEAL_OTHERS, player!=null && playerIsOp(player));
    }

    public static boolean canUseFeedCommand(ServerPlayer player) {
        return getPermissionSafe(player, PERM_FEED, player!=null && playerIsOp(player));
    }

    public static boolean canUseFeedOthers(ServerPlayer player) {
        return getPermissionSafe(player, PERM_FEED_OTHERS, player!=null && playerIsOp(player));
    }

    public static boolean canUseFlyCommand(ServerPlayer player) {
        return getPermissionSafe(player, PERM_FLY, player!=null && playerIsOp(player));
    }

    public static boolean canUseFlyOthers(ServerPlayer player) {
        return getPermissionSafe(player, PERM_FLY_OTHERS, player!=null && playerIsOp(player));
    }

    public static boolean canUseGmCommand(ServerPlayer player) {
        return getPermissionSafe(player, PERM_GM, player!=null && playerIsOp(player));
    }

    public static boolean canUseGmOthers(ServerPlayer player) {
        return getPermissionSafe(player, PERM_GM_OTHERS, player!=null && playerIsOp(player));
    }

    // Spawn
    public static boolean canUseSpawnCommand(ServerPlayer player) {
        return getPermissionSafe(player, PERM_SPAWN, player!=null);
    }

    public static boolean canBypassSpawnWarmup(ServerPlayer player) {
        if (shouldForceWarmups(player)) {
            return false;
        }

        return getPermissionSafe(player, PERM_SPAWN_BYPASS_WARMUP, player!=null && playerIsOp(player));
    }

    public static boolean canUseSetSpawnCommand(ServerPlayer player) {
        return getPermissionSafe(player, PERM_SPAWN_SET, player!=null && playerIsOp(player));
    }

    // TP
    public static boolean canUseBackCommand(ServerPlayer player) {
        return getPermissionSafe(player, PERM_BACK, player!=null && playerIsOp(player));
    }

    public static boolean canBypassBackWarmup(ServerPlayer player) {
        if (shouldForceWarmups(player)) {
            return false;
        }

        return getPermissionSafe(player, PERM_BACK_BYPASS_WARMUP, player!=null && playerIsOp(player));
    }

    public static boolean canUseTpaCommand(ServerPlayer player) {
        return getPermissionSafe(player, PERM_TPA, player!=null);
    }

    public static boolean canUseTpacceptCommand(ServerPlayer player) {
        return canUseTpaCommand(player);
    }

    public static boolean canUseTpdenyCommand(ServerPlayer player) {
        return canUseTpaCommand(player);
    }

    public static boolean canBypassTpaWarmup(ServerPlayer player) {
        if (shouldForceWarmups(player)) {
            return false;
        }

        return getPermissionSafe(player, PERM_TPA_BYPASS_WARMUP, player!=null && playerIsOp(player));
    }

    // Warp
    public static boolean canUseWarpCommand(ServerPlayer player) {
        return getPermissionSafe(player, PERM_WARP, player!=null);
    }

    public static boolean canUseListWarpsCommand(ServerPlayer player) {
        return canUseWarpCommand(player);
    }

    public static boolean canUseCreateWarpCommand(ServerPlayer player) {
        return getPermissionSafe(player, PERM_WARP_CREATE, player!=null && playerIsOp(player));
    }

    @SuppressWarnings("unused")
    public static boolean canSetUnlimitedWarps(ServerPlayer player) {
        return getPermissionSafe(player, PERM_WARP_UNLIMITED, player!=null && playerIsOp(player));
    }

    public static boolean canBypassWarpWarmup(ServerPlayer player) {
        if (shouldForceWarmups(player)) {
            return false;
        }

        return getPermissionSafe(player, PERM_WARP_BYPASS_WARMUP, player!=null && playerIsOp(player));
    }

    public static boolean canAccessPrivateWarp(ServerPlayer player, String warpName, WarpData.WarpLocation location) {
        if (player == null || location == null || warpName == null) {
            return false;
        }

        if (location.type() == WarpData.WarpType.PUBLIC) {
            return true;
        }

        UUID playerUuid = player.getUUID();
        return location.creator().equals(playerUuid) || location.allowedPlayers().contains(playerUuid);
    }

    public static boolean canManagePrivateWarp(ServerPlayer player, WarpData.WarpLocation location) {
        if (player == null || location == null || location.type() != WarpData.WarpType.PRIVATE) {
            return false;
        }

        return player.hasPermissions(2) || location.creator().equals(player.getUUID());
    }

    // Dev
    public static boolean shouldForceWarmups(ServerPlayer player) {
        return getPermissionSafe(player, PERM_FORCE_WARMUPS, false);
    }

    public static boolean shouldForceCombatCooldowns(ServerPlayer player) {
        return getPermissionSafe(player, PERM_FORCE_COMBATCD, false);
    }

    @SuppressWarnings("unused")
    public static boolean forcesCooldowns(ServerPlayer player) {
        return getPermissionSafe(player, PERM_FORCE_COOLDOWNS, false);
    }

    //endregion
}
