package com.ranggadev.adminsecattacker.integration;

/**
 * Deterministic validation for the Phase 6 integration boundary.
 *
 * This class validates the safety contract itself. It does not inspect or invoke
 * AdminSecurity internals and does not execute Minecraft commands.
 */
public final class IntegrationContractValidator {
    private IntegrationContractValidator() {}

    public static ValidationResult validate(IntegrationContract contract, String targetVersion) {
        if (contract == null) {
            return new ValidationResult(false, "CONTRACT_MISSING");
        }
        if (!"AdminSecurity".equals(contract.targetPlugin())) {
            return new ValidationResult(false, "TARGET_PLUGIN_MISMATCH");
        }
        if (!"1.0".equals(contract.contractVersion())) {
            return new ValidationResult(false, "UNSUPPORTED_CONTRACT_VERSION");
        }
        if (!contract.targetRequired()) {
            return new ValidationResult(false, "TARGET_MUST_BE_REQUIRED");
        }
        if (!contract.syntheticEventsOnly()) {
            return new ValidationResult(false, "SYNTHETIC_ONLY_REQUIRED");
        }
        if (contract.commandExecutionAllowed()) {
            return new ValidationResult(false, "COMMAND_EXECUTION_MUST_BE_DISABLED");
        }
        if (contract.directInternalApiCallsAllowed()) {
            return new ValidationResult(false, "DIRECT_INTERNAL_API_CALLS_MUST_BE_DISABLED");
        }
        if (targetVersion == null || targetVersion.isBlank()) {
            return new ValidationResult(false, "TARGET_VERSION_MISSING");
        }

        // The supplied AdminSecurity source of truth is 4.2.0. Keep the
        // integration contract compatible with the 4.2.x line without locking
        // AttackerLab to a patch release.
        if (!targetVersion.matches("4\\.2(?:\\.\\d+)?")) {
            return new ValidationResult(false, "UNSUPPORTED_TARGET_VERSION:" + targetVersion);
        }

        return new ValidationResult(true, "CONTRACT_VALID");
    }

    public record ValidationResult(boolean valid, String reason) {}
}
