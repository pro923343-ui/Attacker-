package com.ranggadev.adminsecattacker.simulation;

/** Expected state transition for a safe synthetic transaction test. */
public record TransactionState(String id, String itemType, int expectedDelta) {
    public TransactionState {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("transaction id is required");
        if (itemType == null || itemType.isBlank()) throw new IllegalArgumentException("itemType is required");
    }
}
