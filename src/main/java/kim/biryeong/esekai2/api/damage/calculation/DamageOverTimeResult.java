package kim.biryeong.esekai2.api.damage.calculation;

import kim.biryeong.esekai2.api.damage.breakdown.DamageBreakdown;
import kim.biryeong.esekai2.api.damage.mitigation.DamageMitigationResult;
import java.util.Objects;

/**
 * Captures the result of running the ESekai damage-over-time calculation core.
 *
 * @param baseDamage base typed damage before scaling
 * @param scaledDamage damage after explicit scaling has been applied but before mitigation
 * @param mitigation mitigation output derived from the scaled damage-over-time breakdown
 */
public record DamageOverTimeResult(
        DamageBreakdown baseDamage,
        DamageBreakdown scaledDamage,
        DamageMitigationResult mitigation
) {
    public DamageOverTimeResult {
        Objects.requireNonNull(baseDamage, "baseDamage");
        Objects.requireNonNull(scaledDamage, "scaledDamage");
        Objects.requireNonNull(mitigation, "mitigation");
    }

    /**
     * Returns the post-mitigation damage-over-time breakdown that later systems should apply.
     *
     * @return final typed damage-over-time after scaling and mitigation
     */
    public DamageBreakdown finalDamage() {
        return mitigation.mitigatedDamage();
    }
}
