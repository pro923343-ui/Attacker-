package com.ranggadev.adminsecattacker.performance;

import com.ranggadev.adminsecattacker.scenario.TelemetrySnapshot;

/** Runtime-neutral telemetry contract so core tests do not depend on Bukkit/Paper classes. */
public interface TelemetryProvider {
    TelemetrySnapshot capture();
}
