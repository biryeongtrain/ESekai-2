package kim.biryeong.esekai2.api.item.affix;

import kim.biryeong.esekai2.api.item.type.ItemFamily;
import kim.biryeong.esekai2.api.stat.modifier.StatModifier;
import kim.biryeong.esekai2.impl.item.affix.AffixModifierResolver;
import kim.biryeong.esekai2.impl.item.affix.AffixRoller;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Public entry points for rolling affixes and projecting them into stat modifiers.
 */
public final class Affixes {
    private Affixes() {
    }

    /**
     * Rolls the provided affix definition for the requested item family.
     *
     * @param affixId stable identifier of the affix definition being rolled
     * @param itemFamily target item family the affix should be rolled for
     * @param itemLevel item level used to validate affix eligibility
     * @param definition affix definition loaded from datapacks or fixtures
     * @param random deterministic or nondeterministic random source used to roll ranged values
     * @return rolled affix snapshot containing concrete stat modifiers
     */
    public static RolledAffix roll(
            Identifier affixId,
            ItemFamily itemFamily,
            int itemLevel,
            AffixDefinition definition,
            RandomGenerator random
    ) {
        return AffixRoller.roll(affixId, itemFamily, itemLevel, definition, random);
    }

    /**
     * Rolls the provided affix definition from a normalized deterministic roll value.
     *
     * @param affixId stable identifier of the affix definition being rolled
     * @param itemFamily target item family the affix should be rolled for
     * @param itemLevel item level used to validate affix eligibility
     * @param definition affix definition loaded from datapacks or fixtures
     * @param normalizedRoll deterministic normalized roll in the range {@code [0, 1]}
     * @return rolled affix snapshot containing concrete stat modifiers
     */
    public static RolledAffix roll(
            Identifier affixId,
            ItemFamily itemFamily,
            int itemLevel,
            AffixDefinition definition,
            double normalizedRoll
    ) {
        return AffixRoller.roll(affixId, itemFamily, itemLevel, definition, normalizedRoll);
    }

    /**
     * Returns the concrete stat modifiers carried by the provided rolled affix.
     *
     * @param rolledAffix rolled affix snapshot to inspect
     * @return immutable concrete stat modifier list
     */
    public static List<StatModifier> toModifiers(RolledAffix rolledAffix) {
        return AffixModifierResolver.toModifiers(rolledAffix);
    }
}
