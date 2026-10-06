package com.ranggadev.adminsecattacker.network;

/** Result of a deterministic event-rate simulation. No packets are sent. */
public record AnomalySimulationResult(
        String id,
        String name,
        int eventsPerSecond,
        int durationSeconds,
        long totalSyntheticEvents,
        String classification,
        boolean thresholdExceeded
) {}
