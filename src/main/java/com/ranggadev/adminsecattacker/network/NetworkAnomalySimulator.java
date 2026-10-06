package com.ranggadev.adminsecattacker.network;

/**
 * Deterministic, bounded network-anomaly model. It models event pressure only;
 * it never opens sockets, sends packets, or targets another server.
 */
public final class NetworkAnomalySimulator {
    private static final int ELEVATED_THRESHOLD = 20;
    private static final int ABNORMAL_THRESHOLD = 100;

    public AnomalySimulationResult simulate(AnomalyProfile profile) {
        if (profile == null) throw new IllegalArgumentException("profile is required");
        long total = (long) profile.eventsPerSecond() * profile.durationSeconds();
        String classification;
        if (profile.eventsPerSecond() >= ABNORMAL_THRESHOLD) classification = "ABNORMAL";
        else if (profile.eventsPerSecond() >= ELEVATED_THRESHOLD) classification = "ELEVATED";
        else classification = "NORMAL";
        return new AnomalySimulationResult(profile.id(), profile.name(), profile.eventsPerSecond(),
                profile.durationSeconds(), total, classification, profile.eventsPerSecond() >= ABNORMAL_THRESHOLD);
    }
}
