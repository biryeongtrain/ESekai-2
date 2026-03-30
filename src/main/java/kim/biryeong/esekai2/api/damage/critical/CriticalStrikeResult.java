package kim.biryeong.esekai2.api.damage.critical;
/**
 * Captures the resolved outcome of one critical strike evaluation.
 *
 * @param criticalStrike whether the hit resolved as a critical strike
 * @param finalCriticalStrikeChance resolved final critical strike chance after attacker stats
 * @param finalCriticalStrikeMultiplier resolved final critical strike multiplier after attacker stats
 */
public record CriticalStrikeResult(
        boolean criticalStrike,
        double finalCriticalStrikeChance,
        double finalCriticalStrikeMultiplier
) {
    public CriticalStrikeResult {
        if (!Double.isFinite(finalCriticalStrikeChance) || finalCriticalStrikeChance < 0.0 || finalCriticalStrikeChance > 100.0) {
            throw new IllegalArgumentException("finalCriticalStrikeChance must be a finite number in the range [0, 100]");
        }

        if (!Double.isFinite(finalCriticalStrikeMultiplier) || finalCriticalStrikeMultiplier < 100.0) {
            throw new IllegalArgumentException("finalCriticalStrikeMultiplier must be a finite number greater than or equal to 100");
        }
    }
}
