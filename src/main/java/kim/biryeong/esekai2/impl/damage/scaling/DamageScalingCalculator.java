package kim.biryeong.esekai2.impl.damage.scaling;

import kim.biryeong.esekai2.api.damage.breakdown.DamageBreakdown;
import kim.biryeong.esekai2.api.damage.breakdown.DamageType;
import kim.biryeong.esekai2.api.damage.scaling.DamageScaling;
import kim.biryeong.esekai2.api.damage.scaling.DamageScalingOperation;

import java.util.List;

/**
 * Internal shared helper for applying explicit scaling entries to typed damage.
 */
public final class DamageScalingCalculator {
    private DamageScalingCalculator() {
    }

    public static DamageBreakdown scaleDamage(DamageBreakdown baseDamage, List<DamageScaling> scaling) {
        DamageBreakdown scaledDamage = DamageBreakdown.empty();

        for (DamageType type : DamageType.values()) {
            double baseAmount = baseDamage.amount(type);
            double addedAmount = summedScaling(scaling, DamageScalingOperation.ADD, type);
            double increasedAmount = summedScaling(scaling, DamageScalingOperation.INCREASED, type);
            double moreMultiplier = multipliedMoreScaling(scaling, type);

            double preScaledAmount = Math.max(0.0, baseAmount + addedAmount);
            if (preScaledAmount == 0.0) {
                continue;
            }

            double scaledAmount = preScaledAmount * Math.max(0.0, 1.0 + increasedAmount / 100.0) * moreMultiplier;
            if (scaledAmount > 0.0) {
                scaledDamage = scaledDamage.with(type, scaledAmount);
            }
        }

        return scaledDamage;
    }

    private static double summedScaling(List<DamageScaling> scaling, DamageScalingOperation operation, DamageType type) {
        double total = 0.0;

        for (DamageScaling entry : scaling) {
            if (entry.operation() == operation && entry.target().matches(type)) {
                total += entry.value();
            }
        }

        return total;
    }

    private static double multipliedMoreScaling(List<DamageScaling> scaling, DamageType type) {
        double multiplier = 1.0;

        for (DamageScaling entry : scaling) {
            if (entry.operation() == DamageScalingOperation.MORE && entry.target().matches(type)) {
                multiplier *= 1.0 + entry.value() / 100.0;
            }
        }

        return Math.max(0.0, multiplier);
    }
}
