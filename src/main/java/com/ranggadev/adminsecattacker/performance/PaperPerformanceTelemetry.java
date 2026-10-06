package com.ranggadev.adminsecattacker.performance;

import com.ranggadev.adminsecattacker.scenario.TelemetrySnapshot;
import org.bukkit.Bukkit;

import java.lang.management.ManagementFactory;

/** Paper adapter for live server telemetry. Kept separate from the testable core telemetry contract. */
public final class PaperPerformanceTelemetry implements TelemetryProvider {
    @Override
    public TelemetrySnapshot capture() {
        Double tps = null;
        Double mspt = null;
        try {
            double[] values = Bukkit.getTPS();
            if (values.length > 0 && Double.isFinite(values[0])) tps = values[0];
        } catch (Throwable ignored) {
            // Leave unmeasured when the runtime does not expose Paper TPS telemetry.
        }
        try {
            Object server = Bukkit.getServer();
            var method = server.getClass().getMethod("getAverageTickTime");
            Object value = method.invoke(server);
            if (value instanceof Number n && Double.isFinite(n.doubleValue())) mspt = n.doubleValue();
        } catch (Throwable ignored) {
            // Leave unmeasured when the runtime does not expose the method.
        }
        long used = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
        return new TelemetrySnapshot(tps, mspt, Bukkit.getOnlinePlayers().size(), used);
    }
}
