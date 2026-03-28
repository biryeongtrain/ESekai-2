package kim.biryeong.esekai2.api.damage.calculation;

import kim.biryeong.esekai2.impl.damage.calculation.DamageCalculationCalculator;
import kim.biryeong.esekai2.impl.damage.calculation.DamageOverTimeCalculationCalculator;

/**
 * Public entry points for ESekai hit and damage-over-time calculations.
 */
public final class DamageCalculations {
    private DamageCalculations() {
    }

    /**
     * Calculates one hit damage result from base damage, explicit scaling, hit resolution, and mitigation.
     *
     * @param calculation immutable hit calculation request
     * @return calculation result containing scaled and mitigated damage views
     */
    public static DamageCalculationResult calculateHit(HitDamageCalculation calculation) {
        return DamageCalculationCalculator.calculateHit(calculation);
    }

    /**
     * Calculates one damage-over-time result from base damage, explicit scaling, and DoT mitigation.
     *
     * @param calculation immutable damage-over-time calculation request
     * @return calculation result containing scaled and mitigated damage-over-time views
     */
    public static DamageOverTimeResult calculateDamageOverTime(DamageOverTimeCalculation calculation) {
        return DamageOverTimeCalculationCalculator.calculateDamageOverTime(calculation);
    }
}
