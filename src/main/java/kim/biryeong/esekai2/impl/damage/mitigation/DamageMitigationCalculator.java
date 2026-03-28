package kim.biryeong.esekai2.impl.damage.mitigation;

import kim.biryeong.esekai2.api.damage.breakdown.DamageBreakdown;
import kim.biryeong.esekai2.api.damage.breakdown.DamageType;
import kim.biryeong.esekai2.api.damage.mitigation.DamageMitigationResult;
import kim.biryeong.esekai2.api.damage.mitigation.ElementalExposure;
import kim.biryeong.esekai2.api.damage.mitigation.ResistancePenetration;
import kim.biryeong.esekai2.api.stat.combat.CombatStats;
import kim.biryeong.esekai2.api.stat.holder.StatHolder;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Internal hit mitigation calculator backed by ESekai's current combat stat model.
 */
public final class DamageMitigationCalculator {
    private DamageMitigationCalculator() {
    }

    public static DamageMitigationResult mitigateHit(
            StatHolder defenderStats,
            DamageBreakdown incomingDamage,
            List<ElementalExposure> exposures,
            List<ResistancePenetration> penetrations
    ) {
        Objects.requireNonNull(defenderStats, "defenderStats");
        Objects.requireNonNull(incomingDamage, "incomingDamage");
        Objects.requireNonNull(exposures, "exposures");
        Objects.requireNonNull(penetrations, "penetrations");

        return mitigate(defenderStats, incomingDamage, exposures, penetrations, true);
    }

    public static DamageMitigationResult mitigateDamageOverTime(
            StatHolder defenderStats,
            DamageBreakdown incomingDamage,
            List<ElementalExposure> exposures
    ) {
        Objects.requireNonNull(defenderStats, "defenderStats");
        Objects.requireNonNull(incomingDamage, "incomingDamage");
        Objects.requireNonNull(exposures, "exposures");

        return mitigate(defenderStats, incomingDamage, exposures, List.of(), false);
    }

    private static DamageMitigationResult mitigate(
            StatHolder defenderStats,
            DamageBreakdown incomingDamage,
            List<ElementalExposure> exposures,
            List<ResistancePenetration> penetrations,
            boolean hit
    ) {
        Objects.requireNonNull(defenderStats, "defenderStats");
        Objects.requireNonNull(incomingDamage, "incomingDamage");
        Objects.requireNonNull(exposures, "exposures");
        Objects.requireNonNull(penetrations, "penetrations");

        DamageBreakdown mitigatedDamage = DamageBreakdown.empty();
        EnumMap<DamageType, Double> mitigationDelta = new EnumMap<>(DamageType.class);

        for (Map.Entry<DamageType, Double> entry : incomingDamage.entries().entrySet()) {
            DamageType type = entry.getKey();
            double incomingAmount = entry.getValue();
            double outgoingAmount = mitigateAmount(type, incomingAmount, defenderStats, exposures, penetrations, hit);

            mitigatedDamage = mitigatedDamage.with(type, outgoingAmount);

            double delta = incomingAmount - outgoingAmount;
            if (delta != 0.0) {
                mitigationDelta.put(type, delta);
            }
        }

        return new DamageMitigationResult(incomingDamage, mitigatedDamage, mitigationDelta);
    }

    private static double mitigateAmount(
            DamageType type,
            double incomingAmount,
            StatHolder defenderStats,
            List<ElementalExposure> exposures,
            List<ResistancePenetration> penetrations,
            boolean hit
    ) {
        if (incomingAmount == 0.0 || type.bypassesMitigation()) {
            return incomingAmount;
        }

        return switch (type) {
            case PHYSICAL -> hit
                    ? mitigatePhysicalHit(incomingAmount, defenderStats.resolvedValue(CombatStats.ARMOUR))
                    : incomingAmount;
            case FIRE -> mitigateByResistance(
                    type,
                    incomingAmount,
                    defenderStats.resolvedValue(CombatStats.FIRE_RESISTANCE),
                    defenderStats.resolvedValue(CombatStats.MAX_FIRE_RESISTANCE),
                    exposures,
                    penetrations,
                    hit
            );
            case COLD -> mitigateByResistance(
                    type,
                    incomingAmount,
                    defenderStats.resolvedValue(CombatStats.COLD_RESISTANCE),
                    defenderStats.resolvedValue(CombatStats.MAX_COLD_RESISTANCE),
                    exposures,
                    penetrations,
                    hit
            );
            case LIGHTNING -> mitigateByResistance(
                    type,
                    incomingAmount,
                    defenderStats.resolvedValue(CombatStats.LIGHTNING_RESISTANCE),
                    defenderStats.resolvedValue(CombatStats.MAX_LIGHTNING_RESISTANCE),
                    exposures,
                    penetrations,
                    hit
            );
            case CHAOS -> mitigateByResistance(
                    type,
                    incomingAmount,
                    defenderStats.resolvedValue(CombatStats.CHAOS_RESISTANCE),
                    defenderStats.resolvedValue(CombatStats.MAX_CHAOS_RESISTANCE),
                    exposures,
                    penetrations,
                    hit
            );
            case FIXED -> incomingAmount;
        };
    }

    private static double mitigatePhysicalHit(double incomingAmount, double armour) {
        if (incomingAmount <= 0.0 || armour <= 0.0) {
            return incomingAmount;
        }

        double reduction = armour / (armour + 5.0 * incomingAmount);
        double clampedReduction = Math.max(0.0, Math.min(0.9, reduction));
        return incomingAmount * (1.0 - clampedReduction);
    }

    private static double mitigateByResistance(
            DamageType type,
            double incomingAmount,
            double resistance,
            double maxResistance,
            List<ElementalExposure> exposures,
            List<ResistancePenetration> penetrations,
            boolean hit
    ) {
        double exposedResistance = resistance - summedExposure(exposures, type);
        double cappedResistance = Math.min(exposedResistance, maxResistance);
        double effectiveResistance = hit
                ? cappedResistance - summedPenetration(penetrations, type)
                : cappedResistance;
        return incomingAmount * (1.0 - effectiveResistance / 100.0);
    }

    private static double summedExposure(List<ElementalExposure> exposures, DamageType type) {
        double total = 0.0;

        for (ElementalExposure exposure : exposures) {
            if (exposure.appliesTo(type)) {
                total += exposure.value();
            }
        }

        return total;
    }

    private static double summedPenetration(List<ResistancePenetration> penetrations, DamageType type) {
        double total = 0.0;

        for (ResistancePenetration penetration : penetrations) {
            if (penetration.appliesTo(type)) {
                total += penetration.value();
            }
        }

        return total;
    }
}
