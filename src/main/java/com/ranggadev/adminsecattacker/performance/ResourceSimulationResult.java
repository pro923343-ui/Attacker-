package com.ranggadev.adminsecattacker.performance;

/** Deterministic workload model; it does not intentionally consume server resources. */
public record ResourceSimulationResult(String id, int operationsPerSecond, int durationSeconds,
                                       long syntheticOperations, String classification) {}
