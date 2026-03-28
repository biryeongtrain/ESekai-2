package kim.biryeong.esekai2.impl.damage.transform;

import kim.biryeong.esekai2.api.damage.breakdown.DamageBreakdown;
import kim.biryeong.esekai2.api.damage.breakdown.DamageType;
import kim.biryeong.esekai2.api.damage.transform.DamageConversion;
import kim.biryeong.esekai2.api.damage.transform.ExtraDamageGain;

import java.util.List;

/**
 * Internal shared helper for applying typed damage conversions and gain-as-extra rules.
 */
public final class DamageTransformationCalculator {
    private DamageTransformationCalculator() {
    }

    public static DamageBreakdown transformDamage(
            DamageBreakdown baseDamage,
            List<DamageConversion> conversions,
            List<ExtraDamageGain> extraGains
    ) {
        DamageBreakdown convertedDamage = applyConversions(baseDamage, conversions);
        return applyExtraDamageGains(convertedDamage, extraGains);
    }

    private static DamageBreakdown applyConversions(DamageBreakdown baseDamage, List<DamageConversion> conversions) {
        DamageBreakdown convertedDamage = DamageBreakdown.empty();

        for (DamageType sourceType : DamageType.values()) {
            double sourceAmount = baseDamage.amount(sourceType);
            if (sourceAmount == 0.0) {
                continue;
            }

            double totalPercent = 0.0;
            for (DamageConversion conversion : conversions) {
                if (conversion.from() == sourceType) {
                    totalPercent += conversion.valuePercent();
                }
            }

            double normalizationFactor = totalPercent > 100.0 ? 100.0 / totalPercent : 1.0;
            double convertedAmount = 0.0;

            for (DamageConversion conversion : conversions) {
                if (conversion.from() != sourceType) {
                    continue;
                }

                double appliedPercent = conversion.valuePercent() * normalizationFactor;
                if (appliedPercent == 0.0) {
                    continue;
                }

                double gainedAmount = sourceAmount * appliedPercent / 100.0;
                convertedAmount += gainedAmount;
                convertedDamage = convertedDamage.with(conversion.to(), convertedDamage.amount(conversion.to()) + gainedAmount);
            }

            double remainingAmount = Math.max(0.0, sourceAmount - convertedAmount);
            if (remainingAmount > 0.0) {
                convertedDamage = convertedDamage.with(sourceType, convertedDamage.amount(sourceType) + remainingAmount);
            }
        }

        return convertedDamage;
    }

    private static DamageBreakdown applyExtraDamageGains(DamageBreakdown convertedDamage, List<ExtraDamageGain> extraGains) {
        DamageBreakdown transformedDamage = convertedDamage;

        for (ExtraDamageGain gain : extraGains) {
            double sourceAmount = convertedDamage.amount(gain.source());
            if (sourceAmount == 0.0 || gain.valuePercent() == 0.0) {
                continue;
            }

            double gainedAmount = sourceAmount * gain.valuePercent() / 100.0;
            transformedDamage = transformedDamage.with(
                    gain.gainedType(),
                    transformedDamage.amount(gain.gainedType()) + gainedAmount
            );
        }

        return transformedDamage;
    }
}
