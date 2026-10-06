package com.ranggadev.adminsecattacker.simulation;

/**
 * Safe, deterministic state simulator. It never accesses Bukkit inventories and never mutates a player.
 */
public final class InventoryStateSimulator {
    public StateSimulationResult compare(InventoryState before, TransactionState transaction, InventoryState observedAfter) {
        if (before == null || transaction == null || observedAfter == null) {
            throw new IllegalArgumentException("before, transaction and observedAfter are required");
        }
        int beforeAmount = before.amountOf(transaction.itemType());
        int expectedAfter = beforeAmount + transaction.expectedDelta();
        int observedAfterAmount = observedAfter.amountOf(transaction.itemType());
        int observedDelta = observedAfterAmount - beforeAmount;
        boolean consistent = expectedAfter == observedAfterAmount;
        String classification = consistent ? "CONSISTENT" : classify(transaction.expectedDelta(), observedDelta);
        return new StateSimulationResult(transaction.id(), transaction.itemType(), beforeAmount,
                expectedAfter, observedAfterAmount, observedDelta, consistent, classification);
    }

    private String classify(int expectedDelta, int observedDelta) {
        if (observedDelta > expectedDelta) return "UNEXPECTED_ITEM_GAIN";
        if (observedDelta < expectedDelta) return "UNEXPECTED_ITEM_LOSS";
        return "STATE_MISMATCH";
    }
}
