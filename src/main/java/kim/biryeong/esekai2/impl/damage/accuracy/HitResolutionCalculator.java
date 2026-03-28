package kim.biryeong.esekai2.impl.damage.accuracy;

import kim.biryeong.esekai2.api.damage.accuracy.HitResolutionResult;
import kim.biryeong.esekai2.api.damage.critical.HitContext;
import kim.biryeong.esekai2.api.damage.critical.HitKind;
import kim.biryeong.esekai2.api.stat.combat.CombatStats;
import kim.biryeong.esekai2.api.stat.holder.StatHolder;

import java.util.Objects;

/**
 * Internal helper for resolving hit success from the current accuracy and evasion model.
 */
public final class HitResolutionCalculator {
    private static final double MIN_HIT_CHANCE = 5.0;
    private static final double MAX_HIT_CHANCE = 100.0;

    private HitResolutionCalculator() {
    }

    public static HitResolutionResult resolveHit(
            StatHolder attackerStats,
            StatHolder defenderStats,
            HitContext hitContext
    ) {
        Objects.requireNonNull(attackerStats, "attackerStats");
        Objects.requireNonNull(defenderStats, "defenderStats");
        Objects.requireNonNull(hitContext, "hitContext");

        if (hitContext.kind() == HitKind.SPELL) {
            return new HitResolutionResult(true, 100.0, true);
        }

        double accuracy = attackerStats.resolvedValue(CombatStats.ACCURACY);
        double evasion = defenderStats.resolvedValue(CombatStats.EVADE);
        double finalHitChance = finalHitChance(accuracy, evasion);
        boolean hitSuccessful = hitContext.hitRoll() < finalHitChance / 100.0;
        return new HitResolutionResult(hitSuccessful, finalHitChance, false);
    }

    private static double finalHitChance(double accuracy, double evasion) {
        double rawHitChance = accuracy <= 0.0
                ? 0.0
                : accuracy / (accuracy + Math.pow(evasion / 4.0, 0.8)) * 100.0;
        return Math.max(MIN_HIT_CHANCE, Math.min(MAX_HIT_CHANCE, rawHitChance));
    }
}
