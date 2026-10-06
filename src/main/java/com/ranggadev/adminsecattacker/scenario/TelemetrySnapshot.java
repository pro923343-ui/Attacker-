package com.ranggadev.adminsecattacker.scenario;

/**
 * Explicit telemetry contract. Null values mean the metric was not measured,
 * never that an assumed value was substituted.
 */
public record TelemetrySnapshot(
        Double tps,
        Double mspt,
        Integer onlinePlayers,
        Long memoryUsedBytes
) {
    public static TelemetrySnapshot notMeasured() {
        return new TelemetrySnapshot(null, null, null, null);
    }
}
