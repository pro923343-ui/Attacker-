package com.ranggadev.adminsecattacker.simulation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Immutable synthetic inventory snapshot used only by the AttackerLab. */
public record InventoryState(Map<String, ItemStackState> items) {
    public InventoryState {
        if (items == null) throw new IllegalArgumentException("items are required");
        Map<String, ItemStackState> copy = new HashMap<>(items);
        copy.forEach((key, value) -> {
            if (key == null || key.isBlank()) throw new IllegalArgumentException("item key is required");
            if (value == null) throw new IllegalArgumentException("item state is required");
        });
        items = Collections.unmodifiableMap(copy);
    }

    public int amountOf(String itemType) {
        ItemStackState state = items.get(itemType);
        return state == null ? 0 : state.amount();
    }
}
