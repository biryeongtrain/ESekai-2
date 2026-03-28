package kim.biryeong.esekai2.api.damage.critical;

import java.util.Objects;

/**
 * Runtime hit context used to resolve hit chance and critical strike behavior.
 *
 * @param kind hit kind used to select the matching critical strike stat axis
 * @param hitRoll deterministic roll compared against the final hit chance for attack hits
 * @param criticalStrikeRoll deterministic roll compared against the final critical strike chance
 * @param baseCriticalStrikeChance base critical strike chance supplied by the hit source
 * @param baseCriticalStrikeMultiplier base critical strike multiplier supplied by the hit source
 */
public record HitContext(
        HitKind kind,
        double hitRoll,
        double criticalStrikeRoll,
        double baseCriticalStrikeChance,
        double baseCriticalStrikeMultiplier
) {
    public HitContext {
        Objects.requireNonNull(kind, "kind");

        if (!Double.isFinite(hitRoll) || hitRoll < 0.0 || hitRoll >= 1.0) {
            throw new IllegalArgumentException("hitRoll must be a finite number in the range [0, 1)");
        }

        if (!Double.isFinite(criticalStrikeRoll) || criticalStrikeRoll < 0.0 || criticalStrikeRoll >= 1.0) {
            throw new IllegalArgumentException("criticalStrikeRoll must be a finite number in the range [0, 1)");
        }

        if (!Double.isFinite(baseCriticalStrikeChance) || baseCriticalStrikeChance < 0.0 || baseCriticalStrikeChance > 100.0) {
            throw new IllegalArgumentException("baseCriticalStrikeChance must be a finite number in the range [0, 100]");
        }

        if (!Double.isFinite(baseCriticalStrikeMultiplier) || baseCriticalStrikeMultiplier < 100.0) {
            throw new IllegalArgumentException("baseCriticalStrikeMultiplier must be a finite number greater than or equal to 100");
        }
    }
}
