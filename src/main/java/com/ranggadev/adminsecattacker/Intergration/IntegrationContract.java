package com.ranggadev.adminsecattacker.integration;

/**
 * Explicit boundary between AttackerLab and AdminSecurity.
 * AttackerLab may observe the target plugin and submit synthetic test input,
 * but it never invokes privileged AdminSecurity internals or executes commands.
 */
public record IntegrationContract(
        String contractVersion,
        String targetPlugin,
        boolean targetRequired,
        boolean syntheticEventsOnly,
        boolean commandExecutionAllowed,
        boolean directInternalApiCallsAllowed
) {
    public static IntegrationContract adminSecurityV1() {
        return new IntegrationContract("1.0", "AdminSecurity", true, true, false, false);
    }
}
