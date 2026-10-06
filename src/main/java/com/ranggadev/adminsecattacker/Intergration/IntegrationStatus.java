package com.ranggadev.adminsecattacker.integration;

/** Runtime-neutral integration status; no security decision is implied by presence alone. */
public record IntegrationStatus(
        boolean targetPresent,
        boolean targetEnabled,
        String targetVersion,
        IntegrationContract contract,
        String status
) {
    public IntegrationStatus {
        if (contract == null) throw new IllegalArgumentException("contract is required");
        if (status == null || status.isBlank()) throw new IllegalArgumentException("status is required");
    }
}
