package kim.biryeong.esekai2.impl.damage.critical;

import kim.biryeong.esekai2.api.damage.breakdown.DamageBreakdown;
import kim.biryeong.esekai2.api.damage.breakdown.DamageType;
import kim.biryeong.esekai2.api.damage.critical.CriticalStrikeResult;
import kim.biryeong.esekai2.api.damage.critical.HitContext;
import kim.biryeong.esekai2.api.damage.critical.HitKind;
import kim.biryeong.esekai2.api.stat.combat.CombatStats;
import kim.biryeong.esekai2.api.stat.definition.StatDefinition;
import kim.biryeong.esekai2.api.stat.holder.StatHolder;
import net.minecraft.resources.ResourceKey;

import java.util.Objects;

/**
 * Internal helper for resolving critical strike chance and multiplier on hit damage.
 */
public final class CriticalStrikeCalculator {
    private CriticalStrikeCalculator() {
    }

    public static CriticalStrikeApplication applyCriticalStrike(
            DamageBreakdown scaledDamage,
            StatHolder attackerStats,
            HitContext hitContext
    ) {
        Objects.requireNonNull(scaledDamage, "scaledDamage");
        Objects.requireNonNull(attackerStats, "attackerStats");
        Objects.requireNonNull(hitContext, "hitContext");

        double finalChance = resolvedCriticalStrikeChance(attackerStats, hitContext);
        double finalMultiplier = resolvedCriticalStrikeMultiplier(attackerStats, hitContext);
        boolean criticalStrike = hitContext.criticalStrikeRoll() < finalChance / 100.0;

        CriticalStrikeResult result = new CriticalStrikeResult(criticalStrike, finalChance, finalMultiplier);
        if (!criticalStrike || scaledDamage.isEmpty()) {
            return new CriticalStrikeApplication(result, scaledDamage);
        }

        double multiplier = finalMultiplier / 100.0;
        DamageBreakdown criticalDamage = DamageBreakdown.empty();
        for (DamageType type : DamageType.values()) {
            double amount = scaledDamage.amount(type);
            if (amount > 0.0) {
                criticalDamage = criticalDamage.with(type, amount * multiplier);
            }
        }

        return new CriticalStrikeApplication(result, criticalDamage);
    }

    public static CriticalStrikeResult nonCriticalResult(
            StatHolder attackerStats,
            HitContext hitContext
    ) {
        Objects.requireNonNull(attackerStats, "attackerStats");
        Objects.requireNonNull(hitContext, "hitContext");
        return new CriticalStrikeResult(
                false,
                resolvedCriticalStrikeChance(attackerStats, hitContext),
                resolvedCriticalStrikeMultiplier(attackerStats, hitContext)
        );
    }

    private static double resolvedCriticalStrikeChance(StatHolder attackerStats, HitContext hitContext) {
        double increasedChance = attackerStats.resolvedValue(criticalStrikeChanceStat(hitContext.kind()));
        return Math.max(0.0, Math.min(100.0, hitContext.baseCriticalStrikeChance() * (1.0 + increasedChance / 100.0)));
    }

    private static double resolvedCriticalStrikeMultiplier(StatHolder attackerStats, HitContext hitContext) {
        double multiplierBonus = attackerStats.resolvedValue(criticalStrikeMultiplierStat(hitContext.kind()));
        return Math.max(100.0, hitContext.baseCriticalStrikeMultiplier() + multiplierBonus);
    }

    private static ResourceKey<StatDefinition> criticalStrikeChanceStat(HitKind kind) {
        return switch (kind) {
            case ATTACK -> CombatStats.ATTACK_CRITICAL_STRIKE_CHANCE_INCREASED;
            case SPELL -> CombatStats.SPELL_CRITICAL_STRIKE_CHANCE_INCREASED;
        };
    }

    private static ResourceKey<StatDefinition> criticalStrikeMultiplierStat(HitKind kind) {
        return switch (kind) {
            case ATTACK -> CombatStats.ATTACK_CRITICAL_STRIKE_MULTIPLIER_BONUS;
            case SPELL -> CombatStats.SPELL_CRITICAL_STRIKE_MULTIPLIER_BONUS;
        };
    }

    /**
     * Internal paired output of critical strike resolution and the resulting pre-mitigation damage.
     *
     * @param result resolved critical strike outcome
     * @param criticalDamage damage breakdown after the critical strike multiplier is applied
     */
    public record CriticalStrikeApplication(
            CriticalStrikeResult result,
            DamageBreakdown criticalDamage
    ) {
        public CriticalStrikeApplication {
            Objects.requireNonNull(result, "result");
            Objects.requireNonNull(criticalDamage, "criticalDamage");
        }
    }
}
