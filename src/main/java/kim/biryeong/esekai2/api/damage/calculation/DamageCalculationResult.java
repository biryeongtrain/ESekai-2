package kim.biryeong.esekai2.api.damage.calculation;

import kim.biryeong.esekai2.api.damage.accuracy.HitResolutionResult;
import kim.biryeong.esekai2.api.damage.breakdown.DamageBreakdown;
import kim.biryeong.esekai2.api.damage.critical.CriticalStrikeResult;
import kim.biryeong.esekai2.api.damage.mitigation.DamageMitigationResult;
import java.util.Objects;

/**
 * Captures the result of running the ESekai hit damage calculation core.
 *
 * @param baseDamage base typed damage before scaling
 * @param scaledDamage damage after explicit scaling has been applied but before critical strikes
 * @param hitResolution resolved hit or miss outcome before critical strikes
 * @param criticalDamage damage after the critical strike multiplier has been applied but before mitigation
 * @param criticalStrike resolved critical strike outcome for the hit
 * @param mitigation mitigation output derived from the critical damage
 */
public record DamageCalculationResult(
        DamageBreakdown baseDamage,
        DamageBreakdown scaledDamage,
        HitResolutionResult hitResolution,
        DamageBreakdown criticalDamage,
        CriticalStrikeResult criticalStrike,
        DamageMitigationResult mitigation
) {
    public DamageCalculationResult {
        Objects.requireNonNull(baseDamage, "baseDamage");
        Objects.requireNonNull(scaledDamage, "scaledDamage");
        Objects.requireNonNull(hitResolution, "hitResolution");
        Objects.requireNonNull(criticalDamage, "criticalDamage");
        Objects.requireNonNull(criticalStrike, "criticalStrike");
        Objects.requireNonNull(mitigation, "mitigation");
    }

    /**
     * Returns the post-mitigation damage that should be applied by later systems.
     *
     * @return final typed damage after scaling and mitigation
     */
    public DamageBreakdown finalDamage() {
        return mitigation.mitigatedDamage();
    }
}
