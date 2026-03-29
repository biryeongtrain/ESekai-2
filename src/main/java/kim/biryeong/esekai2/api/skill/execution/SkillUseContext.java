package kim.biryeong.esekai2.api.skill.execution;

import kim.biryeong.esekai2.api.skill.calculation.SkillCalculationLookup;
import kim.biryeong.esekai2.api.skill.support.SkillSupportDefinition;
import kim.biryeong.esekai2.api.skill.value.SkillValueLookup;
import kim.biryeong.esekai2.api.stat.holder.StatHolder;
import kim.biryeong.esekai2.api.stat.modifier.ConditionalStatModifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Runtime context used to prepare one skill hit from a static skill definition.
 *
 * @param attackerStats attacker stat holder used for runtime skill stat and hit calculations
 * @param defenderStats defender stat holder passed through to later mitigation layers
 * @param conditionalModifiers conditional stat modifiers that may apply to this skill at runtime
 * @param hitRoll deterministic hit roll forwarded into the hit context
 * @param criticalStrikeRoll deterministic critical strike roll forwarded into the hit context
 * @param calculationLookup optional lookup used to resolve datapack-backed calculation references
 * @param valueLookup optional lookup used to resolve datapack-backed value references
 * @param linkedSupports support definitions linked to the active skill for this preparation
 */
public record SkillUseContext(
        StatHolder attackerStats,
        StatHolder defenderStats,
        List<ConditionalStatModifier> conditionalModifiers,
        double hitRoll,
        double criticalStrikeRoll,
        SkillCalculationLookup calculationLookup,
        SkillValueLookup valueLookup,
        List<SkillSupportDefinition> linkedSupports
) {
    public SkillUseContext {
        Objects.requireNonNull(attackerStats, "attackerStats");
        Objects.requireNonNull(defenderStats, "defenderStats");
        Objects.requireNonNull(conditionalModifiers, "conditionalModifiers");
        Objects.requireNonNull(calculationLookup, "calculationLookup");
        Objects.requireNonNull(valueLookup, "valueLookup");
        Objects.requireNonNull(linkedSupports, "linkedSupports");

        conditionalModifiers = List.copyOf(conditionalModifiers);
        for (ConditionalStatModifier modifier : conditionalModifiers) {
            Objects.requireNonNull(modifier, "conditionalModifier entry");
        }

        linkedSupports = List.copyOf(linkedSupports);
        for (SkillSupportDefinition support : linkedSupports) {
            Objects.requireNonNull(support, "linkedSupports entry");
        }

        if (!Double.isFinite(hitRoll) || hitRoll < 0.0 || hitRoll >= 1.0) {
            throw new IllegalArgumentException("hitRoll must be a finite number in the range [0, 1)");
        }

        if (!Double.isFinite(criticalStrikeRoll) || criticalStrikeRoll < 0.0 || criticalStrikeRoll >= 1.0) {
            throw new IllegalArgumentException("criticalStrikeRoll must be a finite number in the range [0, 1)");
        }
    }

    /**
     * Creates a use context without any datapack-backed calculation lookup.
     *
     * @param attackerStats attacker stat holder used for runtime skill stat and hit calculations
     * @param defenderStats defender stat holder passed through to later mitigation layers
     * @param conditionalModifiers conditional stat modifiers that may apply to this skill at runtime
     * @param hitRoll deterministic hit roll forwarded into the hit context
     * @param criticalStrikeRoll deterministic critical strike roll forwarded into the hit context
     */
    public SkillUseContext(
            StatHolder attackerStats,
            StatHolder defenderStats,
            List<ConditionalStatModifier> conditionalModifiers,
            double hitRoll,
            double criticalStrikeRoll
    ) {
        this(
                attackerStats,
                defenderStats,
                conditionalModifiers,
                hitRoll,
                criticalStrikeRoll,
                SkillCalculationLookup.empty(),
                SkillValueLookup.empty(),
                List.of()
        );
    }

    /**
     * Creates a use context with a calculation lookup but without linked supports.
     *
     * @param attackerStats attacker stat holder used for runtime skill stat and hit calculations
     * @param defenderStats defender stat holder passed through to later mitigation layers
     * @param conditionalModifiers conditional stat modifiers that may apply to this skill at runtime
     * @param hitRoll deterministic hit roll forwarded into the hit context
     * @param criticalStrikeRoll deterministic critical strike roll forwarded into the hit context
     * @param calculationLookup lookup used to resolve datapack-backed calculation references
     */
    public SkillUseContext(
            StatHolder attackerStats,
            StatHolder defenderStats,
            List<ConditionalStatModifier> conditionalModifiers,
            double hitRoll,
            double criticalStrikeRoll,
            SkillCalculationLookup calculationLookup
    ) {
        this(attackerStats, defenderStats, conditionalModifiers, hitRoll, criticalStrikeRoll, calculationLookup, SkillValueLookup.empty(), List.of());
    }

    /**
     * Creates a use context with a calculation lookup and linked supports but without a value lookup.
     *
     * @param attackerStats attacker stat holder used for runtime skill stat and hit calculations
     * @param defenderStats defender stat holder passed through to later mitigation layers
     * @param conditionalModifiers conditional stat modifiers that may apply to this skill at runtime
     * @param hitRoll deterministic hit roll forwarded into the hit context
     * @param criticalStrikeRoll deterministic critical strike roll forwarded into the hit context
     * @param calculationLookup lookup used to resolve datapack-backed calculation references
     * @param linkedSupports support definitions linked to the active skill for this preparation
     */
    public SkillUseContext(
            StatHolder attackerStats,
            StatHolder defenderStats,
            List<ConditionalStatModifier> conditionalModifiers,
            double hitRoll,
            double criticalStrikeRoll,
            SkillCalculationLookup calculationLookup,
            List<SkillSupportDefinition> linkedSupports
    ) {
        this(attackerStats, defenderStats, conditionalModifiers, hitRoll, criticalStrikeRoll, calculationLookup, SkillValueLookup.empty(), linkedSupports);
    }

    /**
     * Creates a use context with calculation and value lookups but without linked supports.
     *
     * @param attackerStats attacker stat holder used for runtime skill stat and hit calculations
     * @param defenderStats defender stat holder passed through to later mitigation layers
     * @param conditionalModifiers conditional stat modifiers that may apply to this skill at runtime
     * @param hitRoll deterministic hit roll forwarded into the hit context
     * @param criticalStrikeRoll deterministic critical strike roll forwarded into the hit context
     * @param calculationLookup lookup used to resolve datapack-backed calculation references
     * @param valueLookup lookup used to resolve datapack-backed value references
     */
    public SkillUseContext(
            StatHolder attackerStats,
            StatHolder defenderStats,
            List<ConditionalStatModifier> conditionalModifiers,
            double hitRoll,
            double criticalStrikeRoll,
            SkillCalculationLookup calculationLookup,
            SkillValueLookup valueLookup
    ) {
        this(attackerStats, defenderStats, conditionalModifiers, hitRoll, criticalStrikeRoll, calculationLookup, valueLookup, List.of());
    }

    /**
     * Returns a copy of this context with additional conditional modifiers appended.
     *
     * @param additionalModifiers modifiers to append
     * @return copied context with appended modifiers
     */
    public SkillUseContext withAdditionalConditionalModifiers(List<ConditionalStatModifier> additionalModifiers) {
        Objects.requireNonNull(additionalModifiers, "additionalModifiers");
        if (additionalModifiers.isEmpty()) {
            return this;
        }

        List<ConditionalStatModifier> merged = new ArrayList<>(conditionalModifiers);
        merged.addAll(additionalModifiers);
        return new SkillUseContext(
                attackerStats,
                defenderStats,
                List.copyOf(merged),
                hitRoll,
                criticalStrikeRoll,
                calculationLookup,
                valueLookup,
                linkedSupports
        );
    }

    /**
     * Returns a copy of this context with linked supports replaced.
     *
     * @param supports support definitions that should be linked for preparation
     * @return copied context with replaced support list
     */
    public SkillUseContext withLinkedSupports(List<SkillSupportDefinition> supports) {
        Objects.requireNonNull(supports, "supports");
        return new SkillUseContext(
                attackerStats,
                defenderStats,
                conditionalModifiers,
                hitRoll,
                criticalStrikeRoll,
                calculationLookup,
                valueLookup,
                supports
        );
    }

    /**
     * Returns a copy of this context with the value lookup replaced.
     *
     * @param lookup lookup used to resolve reusable value expressions
     * @return copied context with replaced value lookup
     */
    public SkillUseContext withValueLookup(SkillValueLookup lookup) {
        Objects.requireNonNull(lookup, "lookup");
        return new SkillUseContext(
                attackerStats,
                defenderStats,
                conditionalModifiers,
                hitRoll,
                criticalStrikeRoll,
                calculationLookup,
                lookup,
                linkedSupports
        );
    }
}
