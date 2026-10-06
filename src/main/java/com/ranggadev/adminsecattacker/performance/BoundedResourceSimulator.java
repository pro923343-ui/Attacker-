package com.ranggadev.adminsecattacker.performance;

/** Models bounded workload levels without allocating large objects or blocking the server. */
public final class BoundedResourceSimulator {
    private static final int MAX_RATE = 500;
    private static final int MAX_DURATION = 10;

    public ResourceSimulationResult simulate(String id, int operationsPerSecond, int durationSeconds) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id is required");
        if (operationsPerSecond < 0 || operationsPerSecond > MAX_RATE) throw new IllegalArgumentException("rate must be 0-" + MAX_RATE);
        if (durationSeconds < 1 || durationSeconds > MAX_DURATION) throw new IllegalArgumentException("duration must be 1-" + MAX_DURATION);
        long operations = (long) operationsPerSecond * durationSeconds;
        String classification = operationsPerSecond <= 20 ? "NORMAL" : operationsPerSecond <= 100 ? "ELEVATED" : "ABNORMAL";
        return new ResourceSimulationResult(id, operationsPerSecond, durationSeconds, operations, classification);
    }
}
