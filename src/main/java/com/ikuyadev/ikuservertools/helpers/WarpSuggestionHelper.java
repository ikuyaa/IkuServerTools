package com.ikuyadev.ikuservertools.helpers;

import com.ikuyadev.ikuservertools.IkuServerTools;
import com.ikuyadev.ikuservertools.data.WarpData;
import com.ikuyadev.ikuservertools.managers.PermissionsManager;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.stream.Collectors;

public final class WarpSuggestionHelper {
    private WarpSuggestionHelper() {}

    public static final SuggestionProvider<CommandSourceStack> SUGGEST_ACCESSIBLE_WARPS = (ctx, builder) -> {
        try {
            ServerPlayer player = ctx.getSource().getEntity() instanceof ServerPlayer sp ? sp : null;
            Map<String, WarpData.WarpLocation> warps = WarpData.get().getWarps();

            java.util.List<String> visibleWarps = warps.entrySet().stream()
                    .filter(entry -> canSuggestAccessibleWarpSafely(player, entry.getKey(), entry.getValue()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            return SharedSuggestionProvider.suggest(visibleWarps, builder);
        } catch (Exception e) {
            IkuServerTools.LOGGER.error("IkuServerTools: /warp suggestion provider failed", e);
            return SharedSuggestionProvider.suggest(java.util.List.of(), builder);
        }
    };

    public static final SuggestionProvider<CommandSourceStack> SUGGEST_MANAGEABLE_PRIVATE_WARPS = (ctx, builder) -> {
        try {
            ServerPlayer player = ctx.getSource().getEntity() instanceof ServerPlayer sp ? sp : null;
            Map<String, WarpData.WarpLocation> warps = WarpData.get().getWarps();
            java.util.List<String> visibleWarps = warps.entrySet().stream()
                    .filter(entry -> canSuggestManageablePrivateWarp(ctx.getSource(), player, entry.getValue()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            return SharedSuggestionProvider.suggest(visibleWarps, builder);
        } catch (Exception e) {
            IkuServerTools.LOGGER.error("IkuServerTools: /warp allow suggestion provider failed", e);
            return SharedSuggestionProvider.suggest(java.util.List.of(), builder);
        }
    };

    private static boolean canSuggestAccessibleWarpSafely(ServerPlayer player, String warpName, WarpData.WarpLocation location) {
        try {
            return canSuggestAccessibleWarp(player, warpName, location);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean canSuggestAccessibleWarp(ServerPlayer player, String warpName, WarpData.WarpLocation location) {
        if (player == null) {
            return location != null && location.type() == WarpData.WarpType.PUBLIC;
        }

        return PermissionsManager.canAccessPrivateWarp(player, warpName, location);
    }

    private static boolean canManagePrivateWarpSafely(ServerPlayer player, WarpData.WarpLocation location) {
        try {
            return PermissionsManager.canManagePrivateWarp(player, location);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean canSuggestManageablePrivateWarp(
            CommandSourceStack source,
            ServerPlayer player,
            WarpData.WarpLocation location
    ) {
        if (location == null || location.type() != WarpData.WarpType.PRIVATE) {
            return false;
        }

        if (player == null) {
            return source.hasPermission(2);
        }

        return canManagePrivateWarpSafely(player, location);
    }
}
