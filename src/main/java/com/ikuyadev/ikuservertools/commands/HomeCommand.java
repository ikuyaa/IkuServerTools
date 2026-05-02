package com.ikuyadev.ikuservertools.commands;

import com.ikuyadev.ikuservertools.Config;
import com.ikuyadev.ikuservertools.data.HomeData;
import com.ikuyadev.ikuservertools.data.WarmupData;
import com.ikuyadev.ikuservertools.enums.CooldownSource;
import com.ikuyadev.ikuservertools.helpers.CommandHelpers;
import com.ikuyadev.ikuservertools.helpers.PlayerHelpers;
import com.ikuyadev.ikuservertools.managers.PermissionsManager;
import com.ikuyadev.ikuservertools.managers.WarmupManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.Optional;

public class HomeCommand {

    private record WarmupPayload (
            String homeName,
            ServerLevel targetLevel,
            HomeData.HomeLocation loc
    ) {}

    private static final WarmupManager<WarmupData<WarmupPayload>> WARMUP_MANAGER = new WarmupManager<>(
            WarmupData::endTimeMs,
            WarmupData::startPos,
            WarmupData::lastAnnouncedSecond,
            WarmupData::withLastAnnouncedSecond,
            (player, data) -> {
                WarmupPayload payload = data.payload();
                doTeleport(player, payload.targetLevel(), payload.loc(), payload.homeName());
            },
            (player, data) -> {
                player.sendSystemMessage(teleportTargetMessage(data.payload().homeName(), data.lastAnnouncedSecond()));
                player.playNotifySound(
                        SoundEvents.NOTE_BLOCK_PLING.value(),
                        SoundSource.PLAYERS,
                        0.7f,
                        1.0f
                );
            }
    );

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_HOMES = (ctx, builder) -> {
        ServerPlayer player = ctx.getSource().getPlayer();
        if(player == null) return builder.buildFuture();

        return SharedSuggestionProvider.suggest(
                HomeData.get()
                        .getHomes(player.getUUID())
                        .keySet(),
                builder
        );
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("home")
                        .requires(src -> src.isPlayer()
                            && PermissionsManager.canUseHomeCommand(src.getPlayer()))
                        .executes(ctx -> execute(ctx.getSource(), "home")) // Default home. come back to this to return the 1st home in the home list
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(SUGGEST_HOMES)
                                .executes(ctx ->
                                        execute(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                        )
        );
    }

    private static int execute(CommandSourceStack source, String homeName) {
        ServerPlayer player = CommandHelpers.requirePlayer(source);
        if(player == null) return 0;

        // Already in warmup?
        if(WARMUP_MANAGER.hasPending(player)) {
            CommandHelpers.failure(source, "You are already waiting to go home!");
            return 0;
        }

        // Combat check
        if(CommandHelpers.isUserInCombat(player, "/home")) return 0;

        // Cooldown check
        if(CommandHelpers.isCommandOnCooldown(player, CooldownSource.HOME)) return 0;

        // Checking for homes
        final Map<String, HomeData.HomeLocation> homes = HomeData.get().getHomes(player.getUUID());
        if(homes.isEmpty()) {
            CommandHelpers.failure(source, Component.literal("You do not have any homes set! Use ")
                    .withStyle(ChatFormatting.RED)
                    .append(Component.literal(("/sethome <name>")).withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE))
                    .append(Component.literal(" to set a home!").withStyle(ChatFormatting.RED))
            );

            return 0;
        }

        // Looking up the home
        Optional<HomeData.HomeLocation> opt = Optional.ofNullable(homes.get(homeName));
        if(opt.isEmpty()) {
            CommandHelpers.failure(source, Component.literal("Home ")
                    .withStyle(ChatFormatting.RED)
                    .append(Component.literal(homeName)
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE))
                    .append(" not found.")
                    .withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        // Getting the home's location
        HomeData.HomeLocation loc = opt.get();

        // Dimension lookup
        ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(loc.dimension()));
        ServerLevel targetLevel = source.getServer().getLevel(dimKey);
        if(targetLevel == null) {
            CommandHelpers.failure(source, Component.literal("The dimension for home ")
                    .withStyle(ChatFormatting.RED)
                    .append(Component.literal(loc.dimension())
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE))
                    .append(" is not available on this server.")
                    .withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        // Cross-dimension enable check
        if(!Config.HOME_ALLOW_CROSS_DIMENSION.get() && !targetLevel.equals(player.serverLevel())) {
            CommandHelpers.failure(source, "Cross-dimension teleportation for homes are disabled on this server.");
            return 0;
        }

        // Start warmup, or instant teleport
        int homeTpWarmup = Config.HOME_WARMUP.get();
        if(homeTpWarmup > 0 && !PermissionsManager.canBypassHomeWarmup(player)) {
            startWarmup(player, homeTpWarmup, targetLevel, loc, homeName);
        } else {
            doTeleport(player, targetLevel, loc, homeName);
        }

        return 1;
    }

    private static void startWarmup(ServerPlayer player, int seconds, ServerLevel targetLevel, HomeData.HomeLocation loc, String homeName) {
        WarmupPayload payload = new WarmupPayload(homeName, targetLevel, loc);
        WarmupData.createOptional(seconds, player.position(), true, "home " + homeName, payload)
                .ifPresent(data -> WARMUP_MANAGER.start(player, data.withLastAnnouncedSecond(seconds)));

        player.sendSystemMessage(teleportTargetMessage(homeName, seconds));
        player.playNotifySound(
                SoundEvents.NOTE_BLOCK_PLING.value(),
                SoundSource.PLAYERS,
                0.7f,
                1.0f
        );
    }

    private static void doTeleport(ServerPlayer player, ServerLevel targetLevel, HomeData.HomeLocation loc, String homeName) {
        PlayerHelpers.teleportPlayer(player, targetLevel, loc, true);
        int cooldownSecs = Config.HOME_COOLDOWN.get();
        if (CommandHelpers.setCommandCooldown(player, CooldownSource.HOME, cooldownSecs) && player.getServer() != null) {
            // Resaving the data to ensure the cooldown is saved
            player.getServer().overworld().getDataStorage().save();
        }
        player.sendSystemMessage(teleportTargetCompleteMessage(homeName));
    }

    private static Component teleportTargetMessage(String homeName, int seconds) {
        return CommandHelpers.teleportWarmupMessage(
                homeDestinationComponent(homeName),
                seconds
        );
    }

    private static Component teleportTargetCompleteMessage(String homeName) {
        return CommandHelpers.teleportCompleteMessage(
                homeDestinationComponent(homeName)
        );
    }

    private static Component homeDestinationComponent(String homeName) {
        return Component.literal("home ")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(homeName)
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE));
    }

    public static void tickWarmups(MinecraftServer server) {
        WARMUP_MANAGER.tick(server);
    }
}
