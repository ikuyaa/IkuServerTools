package com.ikuyadev.ikuservertools.commands;

import com.ikuyadev.ikuservertools.data.HomeData;
import com.ikuyadev.ikuservertools.helpers.CommandHelpers;
import com.ikuyadev.ikuservertools.managers.PermissionsManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class SetHomeCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        PermissionsManager PermissionHelper;
        dispatcher.register(
                Commands.literal("sethome")
                        .requires(src -> src.isPlayer()
                                && PermissionsManager.canUseSetHomeCommand(src.getPlayer()))
                        .executes(ctx -> execute(ctx.getSource(), "home"))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> execute(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name")
                                ))
                        )
        );
    }

    private static int execute(CommandSourceStack source, String homeName) {
        ServerPlayer player = CommandHelpers.requirePlayer(source);
        if (player == null) return 0;

        // Validate coordinates
        if (!CommandHelpers.isValidTeleportCoordinate(player.getY())) {
            CommandHelpers.failure(source, "Cannot set home: You are at an invalid Y coordinate. " +
                    "Valid range is -64 to 320.");
            return 0;
        }

        // Getting the player's max homes based on permissions
        int maxHomes = PermissionsManager.resolveHomeLimit(player);
        int currentHomeCount = HomeData.get().getHomeCount(player.getUUID());
        boolean homeExists = HomeData.get().getHome(player.getUUID(), homeName).isPresent();

        if(!homeExists && currentHomeCount >= maxHomes) {
            //TODO: Make the maxHomes gold and underlined
            CommandHelpers.failure(source,
                    "You have reached your home limit (" + maxHomes + ")." +
                            " Delete a home first with /delhome.");
            return 0;
        }

        String dimension = player.serverLevel().dimension().location().toString();

        HomeData.HomeLocation loc = new HomeData.HomeLocation(
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYRot(),
                player.getXRot(),
                dimension
        );
        HomeData.get().setHome(player.getUUID(), homeName, loc);
        CommandHelpers.success(source, () -> Component.literal("Home ")
                .append(Component.literal(homeName).withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE))
                .append(homeExists ? " updated!" : " set!").withStyle(ChatFormatting.GREEN)
        );

        return 1;
    }
}
