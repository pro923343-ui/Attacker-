package com.ranggadev.adminsecattacker.scenario;

/** Result contract shared by future network, inventory and performance scenarios. */
public record ScenarioResult(
        TestScenario scenario,
        String observed,
        String detection,
        String mitigation,
        String finalResult,
        long durationMs,
        TelemetrySnapshot before,
        TelemetrySnapshot during,
        TelemetrySnapshot after
) {
    public ScenarioResult {
        if (scenario == null) throw new IllegalArgumentException("Scenario is required");
        if (observed == null || observed.isBlank()) throw new IllegalArgumentException("Observed result is required");
        if (detection == null || detection.isBlank()) throw new IllegalArgumentException("Detection result is required");
        if (mitigation == null || mitigation.isBlank()) throw new IllegalArgumentException("Mitigation result is required");
        if (finalResult == null || finalResult.isBlank()) throw new IllegalArgumentException("Final result is required");
        if (durationMs < 0) throw new IllegalArgumentException("Duration cannot be negative");
        if (before == null || during == null || after == null) throw new IllegalArgumentException("Telemetry snapshots are required");
    }
}
