package com.ranggadev.adminsecattacker.simulation;

/** Result of comparing an expected synthetic inventory delta with an observed state. */
public record StateSimulationResult(
        String transactionId,
        String itemType,
        int beforeAmount,
        int expectedAfterAmount,
        int observedAfterAmount,
        int observedDelta,
        boolean consistent,
        String classification
) {
    public StateSimulationResult {
        if (transactionId == null || transactionId.isBlank()) throw new IllegalArgumentException("transactionId is required");
        if (itemType == null || itemType.isBlank()) throw new IllegalArgumentException("itemType is required");
        if (beforeAmount < 0 || expectedAfterAmount < 0 || observedAfterAmount < 0) {
            throw new IllegalArgumentException("inventory amounts cannot be negative");
        }
    }
}
