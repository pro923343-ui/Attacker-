package com.ranggadev.adminsecattacker.simulation;

/** Minimal synthetic item state. It is not a Bukkit ItemStack and never touches a live inventory. */
public record ItemStackState(String itemType, int amount) {
    public ItemStackState {
        if (itemType == null || itemType.isBlank()) throw new IllegalArgumentException("itemType is required");
        if (amount < 0) throw new IllegalArgumentException("amount cannot be negative");
    }

    public ItemStackState withAmount(int newAmount) {
        return new ItemStackState(itemType, newAmount);
    }
}
