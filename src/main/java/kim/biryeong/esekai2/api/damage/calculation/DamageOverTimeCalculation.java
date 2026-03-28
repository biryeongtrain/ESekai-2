package kim.biryeong.esekai2.api.damage.calculation;

import kim.biryeong.esekai2.api.damage.breakdown.DamageBreakdown;
import kim.biryeong.esekai2.api.damage.mitigation.ElementalExposure;
import kim.biryeong.esekai2.api.damage.scaling.DamageScaling;
import kim.biryeong.esekai2.api.damage.transform.DamageConversion;
import kim.biryeong.esekai2.api.damage.transform.ExtraDamageGain;
import kim.biryeong.esekai2.api.stat.holder.StatHolder;

import java.util.List;
import java.util.Objects;

/**
 * Represents one damage-over-time calculation request before mitigation is applied.
 *
 * @param baseDamage base typed damage before scaling
 * @param conversions typed conversion entries applied before scaling
 * @param extraGains gain-as-extra entries applied after conversion but before scaling
 * @param scaling explicit scaling entries applied to the base damage
 * @param exposures elemental exposure entries applied during mitigation
 * @param defenderStats defender stat holder used by the damage-over-time mitigation layer
 */
public record DamageOverTimeCalculation(
        DamageBreakdown baseDamage,
        List<DamageConversion> conversions,
        List<ExtraDamageGain> extraGains,
        List<DamageScaling> scaling,
        List<ElementalExposure> exposures,
        StatHolder defenderStats
) {
    public DamageOverTimeCalculation {
        Objects.requireNonNull(baseDamage, "baseDamage");
        Objects.requireNonNull(conversions, "conversions");
        Objects.requireNonNull(extraGains, "extraGains");
        Objects.requireNonNull(scaling, "scaling");
        Objects.requireNonNull(exposures, "exposures");
        Objects.requireNonNull(defenderStats, "defenderStats");

        conversions = List.copyOf(conversions);
        for (DamageConversion entry : conversions) {
            Objects.requireNonNull(entry, "conversion entry");
        }

        extraGains = List.copyOf(extraGains);
        for (ExtraDamageGain entry : extraGains) {
            Objects.requireNonNull(entry, "extra gain entry");
        }

        scaling = List.copyOf(scaling);
        for (DamageScaling entry : scaling) {
            Objects.requireNonNull(entry, "scaling entry");
        }

        exposures = List.copyOf(exposures);
        for (ElementalExposure entry : exposures) {
            Objects.requireNonNull(entry, "exposure entry");
        }
    }
}
