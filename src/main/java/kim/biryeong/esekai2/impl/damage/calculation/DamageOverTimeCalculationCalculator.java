package kim.biryeong.esekai2.impl.damage.calculation;

import kim.biryeong.esekai2.api.damage.breakdown.DamageBreakdown;
import kim.biryeong.esekai2.api.damage.breakdown.DamageType;
import kim.biryeong.esekai2.api.damage.calculation.DamageOverTimeCalculation;
import kim.biryeong.esekai2.api.damage.calculation.DamageOverTimeResult;
import kim.biryeong.esekai2.api.damage.mitigation.DamageMitigationResult;
import kim.biryeong.esekai2.api.damage.mitigation.DamageMitigations;
import kim.biryeong.esekai2.api.stat.combat.CombatStats;
import kim.biryeong.esekai2.impl.damage.scaling.DamageScalingCalculator;
import kim.biryeong.esekai2.impl.damage.transform.DamageTransformationCalculator;

import java.util.Map;
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
        mitigation = applyDamageTakenMore(calculation, mitigation);
        return new DamageOverTimeResult(calculation.baseDamage(), scaledDamage, mitigation);
    }

    private static DamageMitigationResult applyDamageTakenMore(
            DamageOverTimeCalculation calculation,
            DamageMitigationResult mitigation
    ) {
        double damageTakenMore = calculation.defenderStats().resolvedValue(CombatStats.DAMAGE_TAKEN_MORE);
        if (!Double.isFinite(damageTakenMore) || damageTakenMore == 0.0) {
            return mitigation;
        }

        double multiplier = 1.0 + damageTakenMore / 100.0;
        DamageBreakdown amplified = DamageBreakdown.empty();
        for (Map.Entry<DamageType, Double> entry : mitigation.mitigatedDamage().entries().entrySet()) {
            amplified = amplified.with(entry.getKey(), Math.max(0.0, entry.getValue() * multiplier));
        }

        Map<DamageType, Double> mitigationDelta = new java.util.EnumMap<>(DamageType.class);
        for (Map.Entry<DamageType, Double> entry : mitigation.incomingDamage().entries().entrySet()) {
            mitigationDelta.put(entry.getKey(), entry.getValue() - amplified.amount(entry.getKey()));
        }

        return new DamageMitigationResult(
                mitigation.incomingDamage(),
                amplified,
                Map.copyOf(mitigationDelta)
        );
    }
}
