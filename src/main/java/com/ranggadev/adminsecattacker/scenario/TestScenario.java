package com.ranggadev.adminsecattacker.scenario;

/** Immutable contract for a safe AttackerLab scenario. */
public record TestScenario(
        String id,
        String name,
        String category,
        String purpose,
        String setup,
        String input,
        String expected
) {
    public TestScenario {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Scenario id is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Scenario name is required");
        if (category == null || category.isBlank()) throw new IllegalArgumentException("Scenario category is required");
        if (purpose == null || purpose.isBlank()) throw new IllegalArgumentException("Scenario purpose is required");
        if (setup == null || setup.isBlank()) throw new IllegalArgumentException("Scenario setup is required");
        if (input == null || input.isBlank()) throw new IllegalArgumentException("Scenario input is required");
        if (expected == null || expected.isBlank()) throw new IllegalArgumentException("Scenario expected result is required");
    }
}
