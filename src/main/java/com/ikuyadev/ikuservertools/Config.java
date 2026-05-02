package com.ikuyadev.ikuservertools;

import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // =========================================================
    //  General
    // =========================================================
    static {
        BUILDER.comment("General").push("general");
    }

    public static final ModConfigSpec.IntValue COMBAT_COOLDOWN = BUILDER
            .comment("Cooldown before certain commands can be used after combat in seconds. 0 = no combat cooldown.")
            .defineInRange("combatCooldown", 30, 0, 3600);

    static {
        BUILDER.pop();
    }

    // =========================================================
    //  Homes
    // =========================================================
    static {
        BUILDER.comment("Home Settings").push("homes");
    }

    public static final ModConfigSpec.IntValue MAX_HOMES = BUILDER
            .comment("\nDefault maximum number of homes a player can set.")
            .defineInRange("maxHomes", 3, 1, 100);

    public static final ModConfigSpec.IntValue MAX_HOMES_OP = BUILDER
            .comment("\nMaximum homes for operators / players with ikuservertools.homes.unlimited.")
            .defineInRange("maxHomesOp", 50, 1, 100);

    public static final ModConfigSpec.IntValue HOME_COOLDOWN = BUILDER
            .comment("\nCooldown between /home uses in seconds. 0 = no cooldown.")
            .defineInRange("homeCooldown", 10, 0, 3600);

    public static final ModConfigSpec.IntValue HOME_WARMUP = BUILDER
            .comment("\nWarmup delay before teleporting home in seconds. 0 = instant.")
            .defineInRange("homeTeleportWarmup", 3, 0, 30);

    public static final ModConfigSpec.BooleanValue HOME_ALLOW_CROSS_DIMENSION = BUILDER
            .comment("\nAllow teleporting to homes in different dimensions.")
            .define("homeAllowCrossDimension", true);

    static {
        BUILDER.pop();
    }

    // =========================================================
    //  Homes
    // =========================================================
    static {
        BUILDER.comment("Spawn Settings").push("spawn");
    }

    public static final ModConfigSpec.IntValue SPAWN_WARMUP_TIME = BUILDER
            .comment("\nWarmup delay before teleporting to spawn in seconds. 0 = instant.")
            .defineInRange("spawnWarmupTime", 3, 0, 30);

    public static final ModConfigSpec.IntValue SPAWN_COOLDOWN = BUILDER
            .comment("\nCooldown between /spawn uses in seconds. 0 = no cooldown.")
            .defineInRange("spawnCooldown", 60, 0, 3600);

    static {
        BUILDER.pop();
    }

    // =========================================================
    //  TP
    // =========================================================

    static {
        BUILDER.comment("TP Settings").push("tp");
    }

    public static final ModConfigSpec.IntValue BACK_WARMUP = BUILDER
            .comment("\nWarmup delay before teleporting back in seconds. 0 = instant.")
            .defineInRange("backTeleportWarmup", 3, 0, 30);

    public static final ModConfigSpec.IntValue BACK_COOLDOWN = BUILDER
            .comment("\nCooldown between /back uses in seconds. 0 = no cooldown.")
            .defineInRange("backTeleportCooldown", 10, 0, 3600);

    public static final ModConfigSpec.IntValue TPA_WARMUP = BUILDER
            .comment("\nWarmup delay before teleporting with /tpa in seconds. 0 = instant.")
            .defineInRange("tpaWarmup", 3, 0, 30);

    public static final ModConfigSpec.IntValue TPA_TELEPORT_COOLDOWN = BUILDER
            .comment("\nCooldown between teleporting with /tpa in seconds. 0 = no cooldown.")
            .defineInRange("tpaTeleportCooldown", 10, 0, 3600);
    public static final ModConfigSpec.IntValue TPA_REQUEST_EXPIRE_TIME = BUILDER
            .comment("\nTime before a /tpa request expires in seconds.")
            .defineInRange("tpaRequestExpireTime", 60, 10, 3600);

    static {
        BUILDER.pop();
    }

    // =========================================================
    //  Warps
    // =========================================================

    static {
        BUILDER.comment("Warp Settings").push("warps");
    }

    public static final ModConfigSpec.IntValue MAX_WARPS = BUILDER
            .comment("\nDefault maximum number of warps a player can create without a permission override.")
            .defineInRange("maxWarps", 5, 1, 100);

    public static final ModConfigSpec.IntValue MAX_WARPS_OP = BUILDER
            .comment("\nMaximum warps for operators / players with ikust.warp.unlimited.")
            .defineInRange("maxWarpsOp", 50, 1, 100);

    public static final ModConfigSpec.IntValue WARP_WARMUP = BUILDER
            .comment("\nWarmup delay before teleporting to a warp in seconds. 0 = instant.")
            .defineInRange("warpWarmup", 3, 0, 30);

    public static final ModConfigSpec.IntValue WARP_COOLDOWN = BUILDER
            .comment("\nCooldown between teleporting to a warp in seconds. 0 = no cooldown.")
            .defineInRange("warpTeleportCooldown", 10, 0, 3600);

    public static final ModConfigSpec.BooleanValue WARP_ALLOW_CROSS_DIMENSION = BUILDER
            .comment("\nAllow teleporting to warps in different dimensions.")
            .define("warpAllowCrossDimension", true);

    static {
        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();
}
