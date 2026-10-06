package com.ranggadev.adminsecattacker;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import com.ranggadev.adminsecattacker.simulation.InventoryState;
import com.ranggadev.adminsecattacker.simulation.InventoryStateSimulator;
import com.ranggadev.adminsecattacker.simulation.ItemStackState;
import com.ranggadev.adminsecattacker.simulation.StateSimulationResult;
import com.ranggadev.adminsecattacker.simulation.TransactionState;
import com.ranggadev.adminsecattacker.integration.IntegrationContract;
import com.ranggadev.adminsecattacker.integration.IntegrationStatus;
import com.ranggadev.adminsecattacker.auth.AuthorizationContract;
import com.ranggadev.adminsecattacker.network.AnomalyProfile;
import com.ranggadev.adminsecattacker.network.AnomalySimulationResult;
import com.ranggadev.adminsecattacker.network.NetworkAnomalySimulator;
import com.ranggadev.adminsecattacker.performance.BoundedResourceSimulator;
import com.ranggadev.adminsecattacker.performance.PaperPerformanceTelemetry;
import com.ranggadev.adminsecattacker.performance.ResourceSimulationResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * AdminSecurity-specific security test harness.
 *
 * IMPORTANT: this plugin NEVER dispatches the tested command. It only fires
 * PlayerCommandPreprocessEvent directly, allowing AdminSecurity to inspect it.
 * If the event is not cancelled, that is reported as a test failure.
 */
public final class AttackerLabPlugin extends JavaPlugin implements Listener {
    private final List<Result> results = new CopyOnWriteArrayList<>();
    private Path reportFile;
    private volatile boolean running;

    private record TestCase(String id, String command, String reason) {}
    private record Result(
            String id,
            String category,
            String name,
            String input,
            String expected,
            String observed,
            boolean cancelled,
            String status,
            String reason,
            long durationMs
    ) {}

    @Override public void onEnable() {
        reportFile = getDataFolder().toPath().resolve("attackerlab-report.json");
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("AdminSecurity-AttackerLab enabled. Synthetic-event mode only; no tested command is executed.");
    }

    @Override public void onDisable() { writeReport(); }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("asattacker")) return false;
        if (!sender.hasPermission("adminsec.attackerlab.run")) { sender.sendMessage(ChatColor.RED + "No permission."); return true; }
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "status" -> status(sender);
            case "run" -> run(sender);
            case "integrity" -> integrity(sender);
            case "state" -> state(sender);
            case "network" -> network(sender);
            case "performance" -> performance(sender);
            case "integration" -> integration(sender);
            case "authz" -> authz(sender);
            case "report" -> report(sender);
            case "help" -> help(sender);
            default -> help(sender);
        }
        return true;
    }

    private void status(CommandSender sender) {
        Plugin target = Bukkit.getPluginManager().getPlugin("AdminSecurity");
        sender.sendMessage(ChatColor.GOLD + "[AttackerLab] Target: " +
                (target == null ? ChatColor.RED + "NOT FOUND" : ChatColor.GREEN + "FOUND " + target.getDescription().getVersion()));
        sender.sendMessage(ChatColor.GRAY + "Mode: synthetic PlayerCommandPreprocessEvent only");
        sender.sendMessage(ChatColor.GRAY + "No command is dispatched/executed by this plugin.");
        if (sender instanceof Player p) {
            sender.sendMessage(ChatColor.GRAY + "Tester: " + p.getName() + " | op=" + p.isOp());
            if (p.isOp()) sender.sendMessage(ChatColor.YELLOW + "WARNING: use a dedicated non-privileged test player for meaningful authorization results.");
        }
    }

    private void run(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Run this from the dedicated test player, not console.");
            return;
        }
        Plugin target = Bukkit.getPluginManager().getPlugin("AdminSecurity");
        if (target == null || !target.isEnabled()) { sender.sendMessage(ChatColor.RED + "AdminSecurity is not enabled."); return; }
        if (running) { sender.sendMessage(ChatColor.YELLOW + "A test run is already active."); return; }
        if (player.isOp()) { sender.sendMessage(ChatColor.RED + "STOP: remove OP from the test player first. AttackerLab requires a non-OP account."); return; }

        running = true; results.clear();
        sender.sendMessage(ChatColor.GOLD + "=== AdminSecurity AttackerLab ===");
        sender.sendMessage(ChatColor.GRAY + "Safe mode: commands are NEVER executed; only security events are simulated.");

        List<TestCase> tests = List.of(
            new TestCase("AUTH-001", "/op __ADMINSEC_TEST__", "direct privilege command"),
            new TestCase("AUTH-002", "/deop __ADMINSEC_TEST__", "privilege command alias path"),
            new TestCase("AUTH-003", "/lp user __ADMINSEC_TEST__ permission set adminsec.admin true", "LuckPerms privilege escalation probe"),
            new TestCase("AUTH-004", "/luckperms user __ADMINSEC_TEST__ permission set adminsec.admin true", "LuckPerms alternate root"),
            new TestCase("AUTH-005", "/pex user __ADMINSEC_TEST__ add adminsec.admin", "legacy permission escalation probe"),
            new TestCase("AUTH-006", "/permissions user __ADMINSEC_TEST__ set adminsec.admin true", "permission-system escalation probe"),
            new TestCase("AUTH-007", "/minecraft:op __ADMINSEC_TEST__", "namespaced command bypass"),
            new TestCase("AUTH-008", "/bukkit:op __ADMINSEC_TEST__", "alternate namespaced bypass"),
            new TestCase("AUTH-009", "/minecraft:stop", "namespaced server-control bypass"),
            new TestCase("AUTH-010", "/adminsec server stop", "AdminSecurity server control without permission"),
            new TestCase("AUTH-011", "/adminsec panic hard", "AdminSecurity emergency control without permission"),
            new TestCase("AUTH-012", "/adminsec backup restore", "backup restore without permission"),
            new TestCase("AUTH-013", "/adminsec commandprotect enable", "security policy modification without permission"),
            new TestCase("AUTH-014", "/adminsec discord setup", "Discord security administration without owner"),
            new TestCase("DET-001", "/adminsecurity bypass owner", "forensic escalation keyword detection"),
            new TestCase("DET-002", "/lp group default permission set * true", "wildcard permission escalation detection"),
            new TestCase("MANIP-001", "/adminsec antitamper status", "integrity status boundary (synthetic only)"),
            new TestCase("MANIP-002", "/adminsec antitamper rebaseline", "rebaseline command boundary (synthetic only)")
        );

        for (TestCase test : tests) {
            long started = System.nanoTime();
            PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(player, test.command());
            Bukkit.getPluginManager().callEvent(event);
            long durationMs = Math.max(0L, (System.nanoTime() - started) / 1_000_000L);
            boolean cancelled = event.isCancelled();
            String status;
            if (test.id().startsWith("DET-") || test.id().startsWith("MANIP-")) status = cancelled ? "PASS_BLOCKED" : "OBSERVED_NOT_BLOCKED";
            else status = cancelled ? "PASS_BLOCKED" : "FAIL_NOT_BLOCKED";
            String category = test.id().split("-", 2)[0];
            String expected = cancelled ? "Synthetic event cancelled by target security boundary" : "Synthetic event cancellation expected";
            String observed = cancelled ? "Event cancelled" : "Event not cancelled";
            results.add(new Result(test.id(), category, test.reason(), test.command(), expected, observed, cancelled, status, test.reason(), durationMs));
            sender.sendMessage(format(results.get(results.size() - 1)));
        }

        running = false;
        writeReport();
        long failed = results.stream().filter(r -> r.status().equals("FAIL_NOT_BLOCKED")).count();
        sender.sendMessage(ChatColor.GOLD + "=== Result ===");
        sender.sendMessage(failed == 0 ? ChatColor.GREEN + "No authorization test failed." : ChatColor.RED + failed + " authorization test(s) were NOT blocked.");
        sender.sendMessage(ChatColor.GRAY + "Report: " + reportFile.toAbsolutePath());
        sender.sendMessage(ChatColor.GRAY + "IMPORTANT: FAIL means 'security handler did not cancel the synthetic event'; it does not mean a real command was executed.");
    }

    private String format(Result r) {
        ChatColor c = r.status().equals("FAIL_NOT_BLOCKED") ? ChatColor.RED : (r.status().equals("PASS_BLOCKED") ? ChatColor.GREEN : ChatColor.YELLOW);
        return c + "[" + r.status() + "] " + r.id() + " " + ChatColor.GRAY + r.command();
    }


    private void integration(CommandSender sender) {
        Plugin target = Bukkit.getPluginManager().getPlugin("AdminSecurity");
        IntegrationContract contract = IntegrationContract.adminSecurityV1();
        IntegrationStatus status = new IntegrationStatus(
                target != null, target != null && target.isEnabled(),
                target == null ? null : target.getDescription().getVersion(),
                contract, target != null && target.isEnabled() ? "TARGET_AVAILABLE" : "TARGET_NOT_AVAILABLE"
        );
        sender.sendMessage(ChatColor.GOLD + "=== AdminSecurity Integration Contract ===");
        sender.sendMessage(ChatColor.GRAY + "Contract: " + status.contract().contractVersion());
        sender.sendMessage(ChatColor.GRAY + "Target: " + status.contract().targetPlugin());
        sender.sendMessage(ChatColor.GRAY + "Status: " + (status.targetEnabled() ? ChatColor.GREEN + status.status() : ChatColor.YELLOW + status.status()));
        sender.sendMessage(ChatColor.GRAY + "Version: " + (status.targetVersion() == null ? "NOT_AVAILABLE" : status.targetVersion()));
        sender.sendMessage(ChatColor.GRAY + "Synthetic events only: " + status.contract().syntheticEventsOnly());
        sender.sendMessage(ChatColor.GRAY + "Command execution allowed: " + status.contract().commandExecutionAllowed());
        sender.sendMessage(ChatColor.GRAY + "Direct AdminSecurity internals: " + status.contract().directInternalApiCallsAllowed());

        IntegrationContractValidator.ValidationResult validation =
                IntegrationContractValidator.validate(status.contract(), status.targetVersion());
        sender.sendMessage(ChatColor.GRAY + "Contract validation: "
                + (validation.valid() ? ChatColor.GREEN + "PASS" : ChatColor.RED + "FAIL")
                + ChatColor.GRAY + " (" + validation.reason() + ")");

        if (!validation.valid()) {
            sender.sendMessage(ChatColor.RED + "Phase 6 integration contract is not valid for this target.");
        } else {
            sender.sendMessage(ChatColor.GREEN + "Phase 6 integration boundary is valid for AdminSecurity 4.2.x.");
        }
        sender.sendMessage(ChatColor.YELLOW + "This only inspects the boundary. Use /asattacker run for synthetic authorization tests.");
    }

    private void authz(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Phase 7 Authorization Contract Self-Test ===");
        int pass = 0;
        int total = 0;

        pass += checkAuthz(sender, "AUTH-001", AuthorizationContract.Decision.DENY,
                AuthorizationContract.command(false, AuthorizationContract.Role.OWNER, true, "adminsec")); total++;
        pass += checkAuthz(sender, "AUTH-002", AuthorizationContract.Decision.ALLOW,
                AuthorizationContract.command(true, AuthorizationContract.Role.ADMIN, true, "adminsec")); total++;
        pass += checkAuthz(sender, "AUTH-003", AuthorizationContract.Decision.DENY,
                AuthorizationContract.command(true, AuthorizationContract.Role.ADMIN, true, "op")); total++;
        pass += checkAuthz(sender, "AUTH-004", AuthorizationContract.Decision.ALLOW,
                AuthorizationContract.command(true, AuthorizationContract.Role.OWNER, false, "op")); total++;
        pass += checkAuthz(sender, "AUTH-005", AuthorizationContract.Decision.DENY,
                AuthorizationContract.targetAction(true, AuthorizationContract.Role.ADMIN, AuthorizationContract.Role.ADMIN, true, false, "role.manage")); total++;
        pass += checkAuthz(sender, "AUTH-006", AuthorizationContract.Decision.DENY,
                AuthorizationContract.targetAction(true, AuthorizationContract.Role.ADMIN, AuthorizationContract.Role.ADMIN, true, true, "role.manage")); total++;
        pass += checkAuthz(sender, "AUTH-007", AuthorizationContract.Decision.ALLOW,
                AuthorizationContract.targetAction(true, AuthorizationContract.Role.OWNER, AuthorizationContract.Role.ADMIN, true, false, "role.manage")); total++;
        pass += checkAuthz(sender, "AUTH-008", AuthorizationContract.Decision.DENY,
                AuthorizationContract.targetAction(true, AuthorizationContract.Role.STAFF, AuthorizationContract.Role.OWNER, true, false, "profile.kick")); total++;
        pass += checkAuthz(sender, "AUTH-009", AuthorizationContract.Decision.DENY,
                AuthorizationContract.targetAction(false, AuthorizationContract.Role.OWNER, AuthorizationContract.Role.ADMIN, true, false, "authentication.manage")); total++;

        sender.sendMessage(pass == total
                ? ChatColor.GREEN + "PHASE7_AUTHZ_SELFTEST=PASS (" + pass + "/" + total + ")"
                : ChatColor.RED + "PHASE7_AUTHZ_SELFTEST=FAIL (" + pass + "/" + total + ")");
        sender.sendMessage(ChatColor.YELLOW + "Synthetic contract test only; it does not execute Minecraft commands or prove live runtime authorization.");
    }

    private int checkAuthz(CommandSender sender, String id, AuthorizationContract.Decision expected,
                           AuthorizationContract.Decision observed) {
        boolean ok = expected == observed;
        sender.sendMessage((ok ? ChatColor.GREEN + "[PASS] " : ChatColor.RED + "[FAIL] ")
                + id + ChatColor.GRAY + " expected=" + expected + " observed=" + observed);
        return ok ? 1 : 0;
    }

    private void integrity(CommandSender sender) {
        Plugin target = Bukkit.getPluginManager().getPlugin("AdminSecurity");
        if (target == null || !target.isEnabled()) {
            sender.sendMessage(ChatColor.RED + "AdminSecurity is not enabled.");
            return;
        }
        sender.sendMessage(ChatColor.GOLD + "=== Integrity / Tamper Test ===");
        sender.sendMessage(ChatColor.GRAY + "This check is non-destructive: AttackerLab does not modify the target JAR, config, baseline, or forensic files.");
        sender.sendMessage(ChatColor.GRAY + "Use /adminsec antitamper status on the target server to inspect the live integrity lock.");
        sender.sendMessage(ChatColor.GRAY + "The full tamper workflow is intentionally not simulated by writing to the target files while the server is running.");
        sender.sendMessage(ChatColor.YELLOW + "Expected: tamper detection remains fail-closed; recovery requires the legitimate Owner.");
    }

    private void state(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Safe Inventory/Transaction State Simulation ===");
        sender.sendMessage(ChatColor.GRAY + "Synthetic state only: no live player inventory or container is touched.");

        InventoryState before = new InventoryState(java.util.Map.of(
                "DIAMOND", new ItemStackState("DIAMOND", 10)));
        TransactionState valid = new TransactionState("STATE-001", "DIAMOND", -1);
        InventoryState validAfter = new InventoryState(java.util.Map.of(
                "DIAMOND", new ItemStackState("DIAMOND", 9)));

        InventoryStateSimulator simulator = new InventoryStateSimulator();
        StateSimulationResult validResult = simulator.compare(before, valid, validAfter);
        printStateResult(sender, validResult);

        TransactionState suspicious = new TransactionState("STATE-002", "DIAMOND", -1);
        InventoryState anomalousAfter = new InventoryState(java.util.Map.of(
                "DIAMOND", new ItemStackState("DIAMOND", 11)));
        StateSimulationResult suspiciousResult = simulator.compare(before, suspicious, anomalousAfter);
        printStateResult(sender, suspiciousResult);

        sender.sendMessage(ChatColor.GRAY + "Expected: STATE-001=CONSISTENT; STATE-002=UNEXPECTED_ITEM_GAIN.");
        sender.sendMessage(ChatColor.YELLOW + "This simulator detects state inconsistency only; it does not punish, rollback, or modify a server.");
    }

    private void network(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Safe Network/Event Anomaly Simulation ===");
        sender.sendMessage(ChatColor.GRAY + "Synthetic event-rate model only: no packets, sockets, or external targets are used.");

        NetworkAnomalySimulator simulator = new NetworkAnomalySimulator();
        List<AnomalyProfile> profiles = List.of(
                new AnomalyProfile("NET-001", "NORMAL", 5, 3),
                new AnomalyProfile("NET-002", "ELEVATED", 25, 3),
                new AnomalyProfile("NET-003", "ABNORMAL", 120, 3)
        );
        for (AnomalyProfile profile : profiles) {
            AnomalySimulationResult result = simulator.simulate(profile);
            ChatColor color = result.thresholdExceeded() ? ChatColor.RED
                    : (result.classification().equals("ELEVATED") ? ChatColor.YELLOW : ChatColor.GREEN);
            sender.sendMessage(color + "[" + result.classification() + "] " + result.id()
                    + ChatColor.GRAY + " rate=" + result.eventsPerSecond() + "/s duration="
                    + result.durationSeconds() + "s syntheticEvents=" + result.totalSyntheticEvents());
        }
        sender.sendMessage(ChatColor.GRAY + "Expected: NORMAL → ELEVATED → ABNORMAL.");
        sender.sendMessage(ChatColor.YELLOW + "This is a model for detection testing; it does not stress the Minecraft server.");
    }

    private void performance(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Performance Telemetry / Bounded Resource Simulation ===");
        PaperPerformanceTelemetry telemetry = new PaperPerformanceTelemetry();
        var before = telemetry.capture();
        sendTelemetry(sender, "BEFORE", before);

        BoundedResourceSimulator simulator = new BoundedResourceSimulator();
        List<ResourceSimulationResult> scenarios = List.of(
                simulator.simulate("PERF-001", 10, 3),
                simulator.simulate("PERF-002", 50, 3),
                simulator.simulate("PERF-003", 150, 3)
        );
        for (ResourceSimulationResult result : scenarios) {
            ChatColor color = result.classification().equals("ABNORMAL") ? ChatColor.RED
                    : result.classification().equals("ELEVATED") ? ChatColor.YELLOW : ChatColor.GREEN;
            sender.sendMessage(color + "[" + result.classification() + "] " + result.id()
                    + ChatColor.GRAY + " rate=" + result.operationsPerSecond() + "/s duration="
                    + result.durationSeconds() + "s syntheticOperations=" + result.syntheticOperations());
        }

        var after = telemetry.capture();
        sendTelemetry(sender, "AFTER", after);
        sender.sendMessage(ChatColor.YELLOW + "No deliberate CPU/memory exhaustion is performed. Telemetry values are runtime measurements when supported by Paper.");
    }

    private void sendTelemetry(CommandSender sender, String label, com.ranggadev.adminsecattacker.scenario.TelemetrySnapshot snapshot) {
        sender.sendMessage(ChatColor.GRAY + label + " TPS=" + formatMetric(snapshot.tps())
                + " MSPT=" + formatMetric(snapshot.mspt())
                + " players=" + value(snapshot.onlinePlayers())
                + " heapUsedBytes=" + value(snapshot.memoryUsedBytes()));
    }

    private String formatMetric(Double value) {
        return value == null ? "NOT_MEASURED" : String.format(Locale.ROOT, "%.2f", value);
    }

    private String value(Object value) {
        return value == null ? "NOT_MEASURED" : String.valueOf(value);
    }

    private void printStateResult(CommandSender sender, StateSimulationResult result) {
        ChatColor color = result.consistent() ? ChatColor.GREEN : ChatColor.RED;
        sender.sendMessage(color + "[" + result.classification() + "] " + result.transactionId()
                + ChatColor.GRAY + " item=" + result.itemType()
                + " before=" + result.beforeAmount()
                + " expectedAfter=" + result.expectedAfterAmount()
                + " observedAfter=" + result.observedAfterAmount()
                + " delta=" + result.observedDelta());
    }

    private void report(CommandSender sender) {
        if (results.isEmpty()) { sender.sendMessage(ChatColor.YELLOW + "No test result in memory. Run /asattacker run first."); return; }
        for (Result r : results) sender.sendMessage(format(r));
        sender.sendMessage(ChatColor.GRAY + "Saved report: " + reportFile.toAbsolutePath());
    }

    private void help(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "/asattacker status" + ChatColor.GRAY + " - verify target and test account");
        sender.sendMessage(ChatColor.GOLD + "/asattacker run" + ChatColor.GRAY + " - run safe AdminSecurity authorization tests");
        sender.sendMessage(ChatColor.GOLD + "/asattacker integrity" + ChatColor.GRAY + " - show safe anti-tamper test guidance");
        sender.sendMessage(ChatColor.GOLD + "/asattacker state" + ChatColor.GRAY + " - run synthetic inventory/transaction state tests");
        sender.sendMessage(ChatColor.GOLD + "/asattacker network" + ChatColor.GRAY + " - run bounded synthetic network/event-rate scenarios");
        sender.sendMessage(ChatColor.GOLD + "/asattacker performance" + ChatColor.GRAY + " - capture runtime telemetry and bounded workload models");
        sender.sendMessage(ChatColor.GOLD + "/asattacker integration" + ChatColor.GRAY + " - inspect the safe AdminSecurity integration contract");
        sender.sendMessage(ChatColor.GOLD + "/asattacker authz" + ChatColor.GRAY + " - run deterministic Phase 7 authorization-contract tests");
        sender.sendMessage(ChatColor.GOLD + "/asattacker report" + ChatColor.GRAY + " - show last test results");
    }

    private void writeReport() {
        if (reportFile == null) return;
        try {
            Files.createDirectories(reportFile.getParent());
            StringBuilder json = new StringBuilder();
            json.append("{\"generatedAt\":\"").append(Instant.now()).append("\",\"syntheticOnly\":true,\"results\":[");
            for (int i = 0; i < results.size(); i++) {
                if (i > 0) json.append(',');
                Result r = results.get(i);
                json.append("{\"id\":\"").append(esc(r.id()))
                    .append("\",\"category\":\"").append(esc(r.category()))
                    .append("\",\"name\":\"").append(esc(r.name()))
                    .append("\",\"input\":\"").append(esc(r.input()))
                    .append("\",\"expected\":\"").append(esc(r.expected()))
                    .append("\",\"observed\":\"").append(esc(r.observed()))
                    .append("\",\"cancelled\":").append(r.cancelled())
                    .append(",\"status\":\"").append(esc(r.status()))
                    .append("\",\"reason\":\"").append(esc(r.reason()))
                    .append("\",\"durationMs\":").append(r.durationMs())
                    .append(",\"telemetry\":{\"tps\":null,\"mspt\":null,\"onlinePlayers\":null,\"memoryUsedBytes\":null}}");
            }
            json.append("]}");
            Files.writeString(reportFile, json.toString(), StandardCharsets.UTF_8);
        } catch (IOException ex) { getLogger().warning("Could not write report: " + ex.getClass().getSimpleName()); }
    }

    private static String esc(String s) { return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " "); }
}
