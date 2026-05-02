package com.ikuyadev.ikuservertools.commands;

import com.ikuyadev.ikuservertools.data.WarpData;
import com.ikuyadev.ikuservertools.helpers.CommandHelpers;
import com.ikuyadev.ikuservertools.managers.PermissionsManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.Set;

public class WarpCreateCommand {

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
				Commands.literal("setwarp")
						.requires(src -> src.isPlayer()
								&& PermissionsManager.canUseCreateWarpCommand(src.getPlayer()))
						.executes(ctx -> {
							CommandHelpers.failure(ctx.getSource(), "Usage: /setwarp <name> [public|private]");
							return 0;
						})
						.then(Commands.argument("name", StringArgumentType.word())
								.executes(ctx -> execute(
										ctx.getSource(),
										StringArgumentType.getString(ctx, "name"),
										WarpData.WarpType.PUBLIC
								))
								.then(Commands.literal("public")
										.executes(ctx -> execute(
												ctx.getSource(),
												StringArgumentType.getString(ctx, "name"),
												WarpData.WarpType.PUBLIC
										))
								)
								.then(Commands.literal("private")
										.executes(ctx -> execute(
											 ctx.getSource(),
											 StringArgumentType.getString(ctx, "name"),
											 WarpData.WarpType.PRIVATE
										))
								)
						)
		);
	}

	private static int execute(CommandSourceStack source, String warpName, WarpData.WarpType type) {
		ServerPlayer player = CommandHelpers.requirePlayer(source);
		if (player == null) return 0;

		WarpData warpData = WarpData.get();
		Optional<WarpData.WarpLocation> existingWarp = warpData.getWarp(warpName);
		boolean warpExists = existingWarp.isPresent();

		if (warpExists && !player.hasPermissions(2)
				&& !existingWarp.get().creator().equals(player.getUUID())) {
			CommandHelpers.failure(source, "You can only update warps that you created.");
			return 0;
		}

		if (!warpExists) {
			int maxWarps = PermissionsManager.resolveWarpLimit(player);
			int currentWarps = warpData.getWarpCountByCreator(player.getUUID());
			if (currentWarps >= maxWarps) {
				CommandHelpers.failure(source,
						"You have reached your warp limit (" + maxWarps + "). Delete a warp first.");
				return 0;
			}
		}

		Set<java.util.UUID> allowedPlayers = Set.of();
		if (type == WarpData.WarpType.PRIVATE && warpExists) {
			WarpData.WarpLocation current = existingWarp.get();
			if (current.type() == WarpData.WarpType.PRIVATE) {
				allowedPlayers = current.allowedPlayers();
			}
		}

		java.util.UUID creator = warpExists
				? existingWarp.get().creator()
				: player.getUUID();

		String dimension = player.serverLevel().dimension().location().toString();
		WarpData.WarpLocation location = new WarpData.WarpLocation(
				player.getX(),
				player.getY(),
				player.getZ(),
				player.getYRot(),
				player.getXRot(),
				dimension,
				type,
				allowedPlayers,
				creator
		);
		warpData.setWarp(warpName, location);
		if (player.getServer() != null) {
			player.getServer().overworld().getDataStorage().save();
		}

		CommandHelpers.success(source, () -> Component.literal("Warp ")
				.withStyle(ChatFormatting.GREEN)
				.append(Component.literal(warpName)
						.withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE))
				.append(Component.literal(warpExists ? " updated as " : " set as ").withStyle(ChatFormatting.GREEN))
				.append(Component.literal(type.name().toLowerCase())
						.withStyle(ChatFormatting.AQUA))
				.append(Component.literal(".").withStyle(ChatFormatting.GREEN))
		);

		return 1;
	}

}
