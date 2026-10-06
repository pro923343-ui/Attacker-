package com.ranggadev.adminsecattacker.performance;

import com.ranggadev.adminsecattacker.scenario.TelemetrySnapshot;

import java.lang.management.ManagementFactory;

/** Runtime-neutral performance telemetry using JVM memory only. */
public final class PerformanceTelemetry implements TelemetryProvider {
    @Override
    public TelemetrySnapshot capture() {
        long used = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
        return new TelemetrySnapshot(null, null, null, used);
    }
}
