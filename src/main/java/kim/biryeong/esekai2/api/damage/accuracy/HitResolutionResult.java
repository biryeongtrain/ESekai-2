package kim.biryeong.esekai2.api.damage.accuracy;

/**
 * Captures the resolved hit or miss outcome before later hit-only damage layers continue.
 *
 * @param hitSuccessful whether the hit passed the current accuracy or bypass rule
 * @param finalHitChance resolved final hit chance as a percentage
 * @param bypassedAccuracyCheck whether the hit skipped accuracy and evasion entirely
 */
public record HitResolutionResult(
        boolean hitSuccessful,
        double finalHitChance,
        boolean bypassedAccuracyCheck
) {
    public HitResolutionResult {
        if (!Double.isFinite(finalHitChance) || finalHitChance < 0.0 || finalHitChance > 100.0) {
            throw new IllegalArgumentException("finalHitChance must be a finite number in the range [0, 100]");
        }
    }
}
