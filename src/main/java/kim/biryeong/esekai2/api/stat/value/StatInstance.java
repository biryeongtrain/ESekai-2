package kim.biryeong.esekai2.api.stat.value;

import kim.biryeong.esekai2.api.stat.definition.StatDefinition;
import kim.biryeong.esekai2.api.stat.modifier.StatModifier;
import net.minecraft.resources.ResourceKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents the runtime value of a single stat after applying a base value and a list of modifiers.
 *
 * <p>The current calculation pipeline is intentionally minimal and fixed:
 * {@code (base + sum(add)) * (1 + sum(increased) / 100) * product(1 + more / 100)}.
 * The final result is then clamped against the target {@link StatDefinition}'s optional bounds.</p>
 *
 * @param stat stat targeted by this instance
 * @param definition stat definition backing this instance
 * @param baseValue base numeric value before modifiers are applied
 * @param modifiers immutable list of modifiers targeting the same stat
 */
public record StatInstance(
        ResourceKey<StatDefinition> stat,
        StatDefinition definition,
        double baseValue,
        List<StatModifier> modifiers
) {
    public StatInstance {
        Objects.requireNonNull(stat, "stat");
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(modifiers, "modifiers");

        if (!Double.isFinite(baseValue)) {
            throw new IllegalArgumentException("baseValue must be a finite number");
        }

        modifiers = List.copyOf(modifiers);

        for (StatModifier modifier : modifiers) {
            validateModifierTarget(stat, modifier);
        }
    }

    /**
     * Creates an instance using the target definition's default base value and no modifiers.
     *
     * @param stat stat targeted by the created instance
     * @param definition stat definition backing the created instance
     * @return new stat instance initialized from the definition
     */
    public static StatInstance fromDefinition(ResourceKey<StatDefinition> stat, StatDefinition definition) {
        return new StatInstance(stat, definition, definition.defaultValue(), List.of());
    }

    /**
     * Returns a new instance with the provided base value.
     *
     * @param baseValue base numeric value to apply before modifiers
     * @return copied instance with the new base value
     */
    public StatInstance withBaseValue(double baseValue) {
        return new StatInstance(stat, definition, baseValue, modifiers);
    }

    /**
     * Returns a new instance with one additional modifier appended to the current list.
     *
     * @param modifier modifier to append
     * @return copied instance including the modifier
     */
    public StatInstance withModifier(StatModifier modifier) {
        validateModifierTarget(stat, modifier);

        ArrayList<StatModifier> updatedModifiers = new ArrayList<>(modifiers);
        updatedModifiers.add(modifier);
        return new StatInstance(stat, definition, baseValue, updatedModifiers);
    }

    /**
     * Calculates the stat's numeric value before the definition's optional bounds are applied.
     *
     * @return unclamped numeric result produced by the current modifier pipeline
     */
    public double unclampedValue() {
        double addedValue = 0.0;
        double increasedValue = 0.0;
        double moreMultiplier = 1.0;

        for (StatModifier modifier : modifiers) {
            switch (modifier.operation()) {
                case ADD -> addedValue += modifier.value();
                case INCREASED -> increasedValue += modifier.value();
                case MORE -> moreMultiplier *= 1.0 + modifier.value() / 100.0;
            }
        }

        return (baseValue + addedValue) * (1.0 + increasedValue / 100.0) * moreMultiplier;
    }

    /**
     * Calculates the stat's final numeric value after applying the modifier pipeline and optional bounds.
     *
     * @return resolved numeric value for this stat
     */
    public double resolvedValue() {
        double value = unclampedValue();

        if (definition.minValue().isPresent()) {
            value = Math.max(definition.minValue().orElseThrow(), value);
        }

        if (definition.maxValue().isPresent()) {
            value = Math.min(definition.maxValue().orElseThrow(), value);
        }

        return value;
    }

    private static void validateModifierTarget(ResourceKey<StatDefinition> stat, StatModifier modifier) {
        Objects.requireNonNull(modifier, "modifier");

        if (!modifier.stat().equals(stat)) {
            throw new IllegalArgumentException("modifier targets a different stat: " + modifier.stat());
        }
    }
}
