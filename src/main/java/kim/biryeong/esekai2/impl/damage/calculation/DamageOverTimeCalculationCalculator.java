package kim.biryeong.esekai2.impl.damage.calculation;

import kim.biryeong.esekai2.api.damage.breakdown.DamageBreakdown;
import kim.biryeong.esekai2.api.damage.calculation.DamageOverTimeCalculation;
import kim.biryeong.esekai2.api.damage.calculation.DamageOverTimeResult;
import kim.biryeong.esekai2.api.damage.mitigation.DamageMitigationResult;
import kim.biryeong.esekai2.api.damage.mitigation.DamageMitigations;
import kim.biryeong.esekai2.impl.damage.scaling.DamageScalingCalculator;
import kim.biryeong.esekai2.impl.damage.transform.DamageTransformationCalculator;

import java.util.Objects;

/**
 * Internal implementation of ESekai's damage-over-time calculation core.
 */
public final class DamageOverTimeCalculationCalculator {
    private DamageOverTimeCalculationCalculator() {
    }

    public static DamageOverTimeResult calculateDamageOverTime(DamageOverTimeCalculation calculation) {
        Objects.requireNonNull(calculation, "calculation");

        DamageBreakdown transformedDamage = DamageTransformationCalculator.transformDamage(
                calculation.baseDamage(),
                calculation.conversions(),
                calculation.extraGains()
        );
        DamageBreakdown scaledDamage = DamageScalingCalculator.scaleDamage(transformedDamage, calculation.scaling());
        DamageMitigationResult mitigation = DamageMitigations.mitigateDamageOverTime(
                calculation.defenderStats(),
                scaledDamage,
                calculation.exposures()
        );
        return new DamageOverTimeResult(calculation.baseDamage(), scaledDamage, mitigation);
    }
}
