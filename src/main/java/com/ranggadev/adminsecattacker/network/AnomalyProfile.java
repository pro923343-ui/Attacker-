package com.ranggadev.adminsecattacker.network;

/** Safe synthetic event-rate profile; it never creates network traffic. */
public record AnomalyProfile(String id, String name, int eventsPerSecond, int durationSeconds) {
    public AnomalyProfile {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (eventsPerSecond < 0 || eventsPerSecond > 1000) throw new IllegalArgumentException("eventsPerSecond must be 0..1000");
        if (durationSeconds < 1 || durationSeconds > 10) throw new IllegalArgumentException("durationSeconds must be 1..10");
    }
}
