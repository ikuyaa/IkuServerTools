package com.ikuyadev.ikuservertools.helpers;

import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Lightweight audit logging for command actions and important events.
 * Logs to the mod's SLF4J logger which writes to logs/latest.log.
 */
public class AuditLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger("IkuServerTools-Audit");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void logCommandExecution(ServerPlayer player, String command, String result) {
        if (player == null) return;
        String timestamp = LocalDateTime.now().format(TIME_FORMAT);
        LOGGER.info("[{}] {} executed {} - {}", timestamp, player.getName().getString(), command, result);
    }

    public static void logCommandExecution(ServerPlayer player, String command) {
        logCommandExecution(player, command, "success");
    }

    public static void logTeleport(ServerPlayer player, String destination, String reason) {
        if (player == null) return;
        String timestamp = LocalDateTime.now().format(TIME_FORMAT);
        LOGGER.info("[{}] {} teleported to {} ({})", timestamp, player.getName().getString(), destination, reason);
    }

    public static void logStateChange(ServerPlayer player, String state, boolean enabled) {
        if (player == null) return;
        String timestamp = LocalDateTime.now().format(TIME_FORMAT);
        String action = enabled ? "enabled" : "disabled";
        LOGGER.info("[{}] {} {} {}", timestamp, player.getName().getString(), action, state);
    }

    public static void logError(ServerPlayer player, String command, String error) {
        if (player == null) return;
        String timestamp = LocalDateTime.now().format(TIME_FORMAT);
        LOGGER.warn("[{}] {} attempted {} but failed: {}", timestamp, player.getName().getString(), command, error);
    }
}
