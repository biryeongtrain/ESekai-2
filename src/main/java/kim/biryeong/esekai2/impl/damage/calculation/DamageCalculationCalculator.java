package kim.biryeong.esekai2.impl.damage.calculation;

import kim.biryeong.esekai2.api.damage.accuracy.HitResolutionResult;
import kim.biryeong.esekai2.api.damage.breakdown.DamageBreakdown;
import kim.biryeong.esekai2.api.damage.breakdown.DamageType;
import kim.biryeong.esekai2.api.damage.calculation.DamageCalculationResult;
import kim.biryeong.esekai2.api.damage.calculation.HitDamageCalculation;
import kim.biryeong.esekai2.api.damage.critical.CriticalStrikeResult;
import kim.biryeong.esekai2.api.damage.mitigation.DamageMitigationResult;
import kim.biryeong.esekai2.api.damage.mitigation.DamageMitigations;
import kim.biryeong.esekai2.api.stat.combat.CombatStats;
import kim.biryeong.esekai2.impl.damage.accuracy.HitResolutionCalculator;
import kim.biryeong.esekai2.impl.damage.critical.CriticalStrikeCalculator;
import kim.biryeong.esekai2.impl.damage.scaling.DamageScalingCalculator;
import kim.biryeong.esekai2.impl.damage.transform.DamageTransformationCalculator;
import java.util.Map;
import java.util.Objects;

/**
 * Internal implementation of ESekai's hit damage calculation core.
 */
public final class DamageCalculationCalculator {
    private DamageCalculationCalculator() {
    }

    public static DamageCalculationResult calculateHit(HitDamageCalculation calculation) {
        Objects.requireNonNull(calculation, "calculation");

        DamageBreakdown transformedDamage = DamageTransformationCalculator.transformDamage(
                calculation.baseDamage(),
                calculation.conversions(),
                calculation.extraGains()
        );
        DamageBreakdown scaledDamage = DamageScalingCalculator.scaleDamage(transformedDamage, calculation.scaling());
        HitResolutionResult hitResolution = HitResolutionCalculator.resolveHit(
                calculation.attackerStats(),
                calculation.defenderStats(),
                calculation.hitContext()
        );
        if (!hitResolution.hitSuccessful()) {
            DamageBreakdown zeroDamage = DamageBreakdown.empty();
            CriticalStrikeResult criticalStrike = CriticalStrikeCalculator.nonCriticalResult(
                    calculation.attackerStats(),
                    calculation.hitContext()
            );
            DamageMitigationResult mitigation = new DamageMitigationResult(zeroDamage, zeroDamage, Map.of());
            return new DamageCalculationResult(
                    calculation.baseDamage(),
                    scaledDamage,
                    hitResolution,
                    zeroDamage,
                    criticalStrike,
                    mitigation
            );
        }

        CriticalStrikeCalculator.CriticalStrikeApplication criticalStrike = CriticalStrikeCalculator.applyCriticalStrike(
                scaledDamage,
                calculation.attackerStats(),
                calculation.hitContext()
        );
        DamageMitigationResult mitigation = DamageMitigations.mitigateHit(
                calculation.defenderStats(),
                criticalStrike.criticalDamage(),
                calculation.exposures(),
                calculation.penetrations()
        );
        mitigation = applyDamageTakenMore(calculation, mitigation);
        return new DamageCalculationResult(
                calculation.baseDamage(),
                scaledDamage,
                hitResolution,
                criticalStrike.criticalDamage(),
                criticalStrike.result(),
                mitigation
        );
    }

    private static DamageMitigationResult applyDamageTakenMore(
            HitDamageCalculation calculation,
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
