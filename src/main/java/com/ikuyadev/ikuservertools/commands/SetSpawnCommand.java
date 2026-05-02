package com.ikuyadev.ikuservertools.commands;

import com.ikuyadev.ikuservertools.data.SpawnData;
import com.ikuyadev.ikuservertools.helpers.CommandHelpers;
import com.ikuyadev.ikuservertools.managers.PermissionsManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class SetSpawnCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("setspawn")
                        .requires(src -> src.isPlayer()
                                && PermissionsManager.canUseSetSpawnCommand(src.getPlayer()))
                        .executes(ctx -> execute(ctx.getSource()))
        );
    }

    private static int execute(CommandSourceStack source) {
        ServerPlayer player = CommandHelpers.requirePlayer(source);
        if(player == null) return 0;

        Vec3 pos = player.position();
        Vec2 rot = player.getRotationVector();

        SpawnData spawnData = SpawnData.get(player.getServer());
        spawnData.setSpawn(
                pos.x,
                pos.y,
                pos.z,
                rot.y,
                rot.x,
                player.level().dimension()
        );

        CommandHelpers.success(source, "Spawn point set to your current location.", true);
        return 1;
    }
}
