package com.ikuyadev.ikuservertools.data;

import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public record WarmupData<T>(
        Vec3 startPos,
        long endTimeMs,
        int lastAnnouncedSecond,
        boolean cancelOnMove,
        String actionLabel,
        T payload
){
    public static <T> Optional<WarmupData<T>> createOptional(
            int seconds,
            Vec3 startPos,
            boolean cancelOnMove,
            String actionLabel,
            T payload
    ) {
        if (seconds <= 0) {
            return Optional.empty();
        }

        long endTime = System.currentTimeMillis() + (seconds * 1000L);
        return Optional.of(new WarmupData<>(
                startPos,
                endTime,
                seconds,
                cancelOnMove,
                actionLabel,
                payload
        ));
    }

    public WarmupData<T> withLastAnnouncedSecond(int second) {
        return new WarmupData<>(
                startPos,
                endTimeMs,
                second,
                cancelOnMove,
                actionLabel,
                payload
        );
    }
}
