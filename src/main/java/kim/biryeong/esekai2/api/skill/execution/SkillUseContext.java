package kim.biryeong.esekai2.api.skill.execution;

import kim.biryeong.esekai2.api.stat.holder.StatHolder;
import kim.biryeong.esekai2.api.stat.modifier.ConditionalStatModifier;

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
 */
public record SkillUseContext(
        StatHolder attackerStats,
        StatHolder defenderStats,
        List<ConditionalStatModifier> conditionalModifiers,
        double hitRoll,
        double criticalStrikeRoll
) {
    public SkillUseContext {
        Objects.requireNonNull(attackerStats, "attackerStats");
        Objects.requireNonNull(defenderStats, "defenderStats");
        Objects.requireNonNull(conditionalModifiers, "conditionalModifiers");

        conditionalModifiers = List.copyOf(conditionalModifiers);
        for (ConditionalStatModifier modifier : conditionalModifiers) {
            Objects.requireNonNull(modifier, "conditionalModifier entry");
        }

        if (!Double.isFinite(hitRoll) || hitRoll < 0.0 || hitRoll >= 1.0) {
            throw new IllegalArgumentException("hitRoll must be a finite number in the range [0, 1)");
        }

        if (!Double.isFinite(criticalStrikeRoll) || criticalStrikeRoll < 0.0 || criticalStrikeRoll >= 1.0) {
            throw new IllegalArgumentException("criticalStrikeRoll must be a finite number in the range [0, 1)");
        }
    }
}
