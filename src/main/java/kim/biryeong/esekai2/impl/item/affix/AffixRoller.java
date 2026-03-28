package kim.biryeong.esekai2.impl.item.affix;

import kim.biryeong.esekai2.api.item.affix.AffixDefinition;
import kim.biryeong.esekai2.api.item.affix.AffixModifierDefinition;
import kim.biryeong.esekai2.api.item.affix.RolledAffix;
import kim.biryeong.esekai2.api.item.type.ItemFamily;
import kim.biryeong.esekai2.api.stat.modifier.StatModifier;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * Internal helper for rolling affix range definitions into concrete stat modifier snapshots.
 */
public final class AffixRoller {
    private AffixRoller() {
    }

    public static RolledAffix roll(
            Identifier affixId,
            ItemFamily itemFamily,
            int itemLevel,
            AffixDefinition definition,
            RandomGenerator random
    ) {
        Objects.requireNonNull(affixId, "affixId");
        Objects.requireNonNull(itemFamily, "itemFamily");
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(random, "random");

        validateRollTarget(affixId, itemFamily, itemLevel, definition);

        List<StatModifier> modifiers = new ArrayList<>(definition.modifierRanges().size());
        for (AffixModifierDefinition modifierRange : definition.modifierRanges()) {
            modifiers.add(new StatModifier(
                    modifierRange.stat(),
                    modifierRange.operation(),
                    rolledValue(modifierRange, random),
                    affixId
            ));
        }

        return new RolledAffix(affixId, itemFamily, modifiers);
    }

    public static RolledAffix roll(
            Identifier affixId,
            ItemFamily itemFamily,
            int itemLevel,
            AffixDefinition definition,
            double normalizedRoll
    ) {
        Objects.requireNonNull(affixId, "affixId");
        Objects.requireNonNull(itemFamily, "itemFamily");
        Objects.requireNonNull(definition, "definition");

        if (!Double.isFinite(normalizedRoll) || normalizedRoll < 0.0 || normalizedRoll > 1.0) {
            throw new IllegalArgumentException("normalizedRoll must be a finite number in the range [0, 1]");
        }

        validateRollTarget(affixId, itemFamily, itemLevel, definition);

        List<StatModifier> modifiers = new ArrayList<>(definition.modifierRanges().size());
        for (AffixModifierDefinition modifierRange : definition.modifierRanges()) {
            modifiers.add(new StatModifier(
                    modifierRange.stat(),
                    modifierRange.operation(),
                    interpolate(modifierRange, normalizedRoll),
                    affixId
            ));
        }

        return new RolledAffix(affixId, itemFamily, modifiers);
    }

    private static double rolledValue(AffixModifierDefinition definition, RandomGenerator random) {
        if (definition.minValue() == definition.maxValue()) {
            return definition.minValue();
        }

        return random.nextDouble(definition.minValue(), definition.maxValue());
    }

    private static double interpolate(AffixModifierDefinition definition, double normalizedRoll) {
        if (definition.minValue() == definition.maxValue()) {
            return definition.minValue();
        }

        return definition.minValue() + normalizedRoll * (definition.maxValue() - definition.minValue());
    }

    private static void validateRollTarget(
            Identifier affixId,
            ItemFamily itemFamily,
            int itemLevel,
            AffixDefinition definition
    ) {
        if (!definition.supports(itemFamily)) {
            throw new IllegalArgumentException("Affix " + affixId + " does not support item family " + itemFamily.serializedName());
        }

        if (!definition.isAvailableAtItemLevel(itemLevel)) {
            throw new IllegalArgumentException("Affix " + affixId + " requires item level " + definition.minimumItemLevel() + " or higher");
        }
    }
}
