package kim.biryeong.esekai2.api.damage.mitigation;

import kim.biryeong.esekai2.api.damage.breakdown.DamageBreakdown;
import kim.biryeong.esekai2.api.stat.holder.StatHolder;
import kim.biryeong.esekai2.impl.damage.mitigation.DamageMitigationCalculator;

import java.util.List;

/**
 * Public entry points for applying hit and damage-over-time mitigation rules.
 */
public final class DamageMitigations {
    private DamageMitigations() {
    }

    /**
     * Applies the current hit mitigation rules to the provided incoming damage breakdown.
     *
     * @param defenderStats mutable stat holder representing the defending target
     * @param incomingDamage incoming hit damage before mitigation
     * @param exposures elemental exposure entries applied before resistance caps
     * @param penetrations resistance penetration entries applied after resistance caps
     * @return mitigation result containing the original, mitigated, and delta views
     */
    public static DamageMitigationResult mitigateHit(
            StatHolder defenderStats,
            DamageBreakdown incomingDamage,
            List<ElementalExposure> exposures,
            List<ResistancePenetration> penetrations
    ) {
        return DamageMitigationCalculator.mitigateHit(defenderStats, incomingDamage, exposures, penetrations);
    }

    /**
     * Applies the current damage-over-time mitigation rules to the provided incoming damage breakdown.
     *
     * @param defenderStats mutable stat holder representing the defending target
     * @param incomingDamage incoming damage-over-time before mitigation
     * @param exposures elemental exposure entries applied before resistance caps
     * @return mitigation result containing the original, mitigated, and delta views
     */
    public static DamageMitigationResult mitigateDamageOverTime(
            StatHolder defenderStats,
            DamageBreakdown incomingDamage,
            List<ElementalExposure> exposures
    ) {
        return DamageMitigationCalculator.mitigateDamageOverTime(defenderStats, incomingDamage, exposures);
    }
}
