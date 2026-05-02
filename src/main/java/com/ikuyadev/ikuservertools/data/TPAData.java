package com.ikuyadev.ikuservertools.data;

import com.ikuyadev.ikuservertools.Config;

import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TPAData {
    // Map is target player UUID -> requester player UUID
    private static final Map<UUID, Map<UUID, Date>> pendingTPARequests = new ConcurrentHashMap<>();

    public static void addTPARequest(UUID requester, UUID target) {
        pendingTPARequests.computeIfAbsent(target, k -> new ConcurrentHashMap<>())
                .put(requester, new Date());
    }

    public static boolean hasPendingRequest(UUID requester, UUID target) {
        Map<UUID, Date> targetRequests = pendingTPARequests.get(target);
        return targetRequests != null && targetRequests.containsKey(requester);
    }

    public static void removeTPARequest(UUID requester, UUID target) {
        Map<UUID, Date> targetRequests = pendingTPARequests.get(target);
        if(targetRequests != null) {
            targetRequests.remove(requester);
            if(targetRequests.isEmpty()) {
                pendingTPARequests.remove(target);
            }
        }
    }

    public static Map<UUID, Date> getPendingRequestsForTarget(UUID target) {
        return pendingTPARequests.getOrDefault(target, Map.of());
    }

    public static void clearExpiredTPARequests() {
        long now = System.currentTimeMillis();
        long expireTime = Config.TPA_REQUEST_EXPIRE_TIME.get() * 1000L;
        if(expireTime > 0) {
            pendingTPARequests.forEach((target, requests) -> {
                requests.entrySet().removeIf(entry -> now - entry.getValue().getTime() > Config.TPA_REQUEST_EXPIRE_TIME.get() * 1000L);
                if(requests.isEmpty()) {
                    pendingTPARequests.remove(target);
                }
            });
        }
    }
}
