# IkuServerTools
This is my first Minecraft mod :D
**This project is a WIP, but what is documented in the readme should work fine**
Currently only for `NeoForge 1.21.1`. I may port to more versions in the future.

## <u>Important Notes</u>:
- If you use [LuckPerms](https://luckperms.net/) you must use the version [5.4.150](https://modrinth.com/plugin/luckperms/version/v5.4.150-neoforge) for Minecraft `1.21.4`. **Yes**, it will still work even though the mod is for a different version of Minecraft. This is the only version I've tested it with, but I know that is at least the minimum version. 

## <u>Commands</u>:
- `/home {name}`
- `/sethome {name}`
- `/listhomes`
- `/delhome {name}`
- `/delallhomes`
- `/spawn`
- `/setspawn`
- `/tpa {player}`
- `/tpaccept {player}`
- `/tpdeny {player}`
- `/back`
- `/heal {player}`
- `/feed {player}`
- `/fly {player}`
- `/god {player}`
- `/gm {gamemode} {player}`
- `/warp {name}`
- `/setwarp {name} {public|private}`
- `/warp allow {warp} {user}` - `Allow a user to your private warp`
- `/delwarp {name}`
- `/listwarps`

## <u>Permissions</u>:
- `ikust.home` - `Access to all base home commands (Default: everyone)`
- `ikust.home.bypasscombatblock` - `Bypass the combat block for /home (Default: OP)`
- `ikust.home.unlimited` - `Allows the player to set the max amount of homes (100) (Default: OP)`
- `ikust.home.count.{number}` - `Allows the player to set a certain amount of homes (max: 100)`
- `ikust.spawn` - `Access to /spawn (Default: everyone)`
- `ikust.spawn.set` - `Access to /setspawn (Default: OP)`
- `ikust.spawn.bypasswarmup` - `Bypass the /spawn warmup time (Default: OP)`
- `ikust.back` - `Access to /back (Default: OP)`
- `ikust.back.bypasswarmup` - `Bypass the /back warmup time (Default: OP)`
- `ikust.tpa` - `Access to all base TPA commands (Default: everyone)`
- `ikust.tpa.bypasswarmup` - `Bypass the /tpaccept warmup (Default: OP)`
- `ikust.heal` - `Access to use /heal on self (Default: OP)`
- `ikust.heal.others` - `Access to use /heal {player} (Default: OP)`
- `ikust.feed` - `Access to use /feed on self (Default: OP)`
- `ikust.feed.others` - `Access to use /feed {player} (Default: OP)`
- `ikust.fly` - `Access to use /fly on self (Default: OP)`
- `ikust.fly.others` - `Allows the user to use /fly {player} (Default: OP)`
- `ikust.god` - `Allows the user to use /god on self (Default: OP)`
- `ikust.god.others` - `Allows the user to use /god {player} (Default: OP)`
- `ikust.gm` - `Allows the user to use /gm on self (Default: OP)`
- `ikust.gm.others` - `Allows the user to use /gm {mode} {player} (Default: OP)`
- `ikust.bypass.combatblock` - `Bypasses combat block for commands (Default: OP)`
- `ikust.warp` - `Allows the use of /warp and /listwarps (Default: Everyone)` 
- `warp.create` - `Allows the use of /setwarp (Default: OP)`
- `warp.count.{number}` - `Set the amount of warps a player can set. 1-100`
- `warp.unlimited` - `Set unlimited number of homes (Max 100) (Default: OP)`



## <u>Base Config</u>:
``` toml
#General
[general]
    #Cooldown before certain commands can be used after combat in seconds. 0 = no combat cooldown.
    # Default: 30
    # Range: 0 ~ 3600
    combatCooldown = 30

#Home Settings
[homes]
    #
    #Default maximum number of homes a player can set.
    # Default: 3
    # Range: 1 ~ 100
    maxHomes = 3
    #
    #Maximum homes for operators / players with ikuservertools.homes.unlimited.
    # Default: 50
    # Range: 1 ~ 100
    maxHomesOp = 50
    #
    #Cooldown between /home uses in seconds. 0 = no cooldown.
    # Default: 10
    # Range: 0 ~ 3600
    homeCooldown = 10
    #
    #Warmup delay before teleporting home in seconds. 0 = instant.
    # Default: 3
    # Range: 0 ~ 30
    homeTeleportWarmup = 3
    #
    #Allow teleporting to homes in different dimensions.
    homeAllowCrossDimension = true

#Spawn Settings
[spawn]
    #
    #Warmup delay before teleporting to spawn in seconds. 0 = instant.
    # Default: 3
    # Range: 0 ~ 30
    spawnWarmupTime = 3
    #
    #Cooldown between /spawn uses in seconds. 0 = no cooldown.
    # Default: 60
    # Range: 0 ~ 3600
    spawnCooldown = 60

#TP Settings
[tp]
    #
    #Warmup delay before teleporting back in seconds. 0 = instant.
    # Default: 3
    # Range: 0 ~ 30
    backTeleportWarmup = 3
    #
    #Cooldown between /back uses in seconds. 0 = no cooldown.
    # Default: 10
    # Range: 0 ~ 3600
    backTeleportCooldown = 10
    #
    #Warmup delay before teleporting with /tpa in seconds. 0 = instant.
    # Default: 3
    # Range: 0 ~ 30
    tpaWarmup = 3
    #
    #Cooldown between teleporting with /tpa in seconds. 0 = no cooldown.
    # Default: 10
    # Range: 0 ~ 3600
    tpaTeleportCooldown = 10
    #
    #Time before a /tpa request expires in seconds.
    # Default: 60
    # Range: 10 ~ 3600
    tpaRequestExpireTime = 60

#Warp Settings
[warps]
    #
    #Default maximum number of warps a player can create without a permission override.
    # Default: 5
    # Range: 1 ~ 100
    maxWarps = 5
    #
    #Maximum warps for operators / players with ikust.warp.unlimited.
    # Default: 50
    # Range: 1 ~ 100
    maxWarpsOp = 50
    #
    #Warmup delay before teleporting to a warp in seconds. 0 = instant.
    # Default: 3
    # Range: 0 ~ 30
    warpWarmup = 3
    #
    #Cooldown between teleporting to a warp in seconds. 0 = no cooldown.
    # Default: 10
    # Range: 0 ~ 3600
    warpTeleportCooldown = 10
    #
    #Allow teleporting to warps in different dimensions.
    warpAllowCrossDimension = true


```

