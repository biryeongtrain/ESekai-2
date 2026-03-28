package kim.biryeong.esekai2.impl.gametest.damage;

import kim.biryeong.esekai2.api.damage.breakdown.DamageBreakdown;
import kim.biryeong.esekai2.api.damage.breakdown.DamageType;
import kim.biryeong.esekai2.api.damage.calculation.DamageCalculationResult;
import kim.biryeong.esekai2.api.damage.calculation.DamageCalculations;
import kim.biryeong.esekai2.api.damage.calculation.HitDamageCalculation;
import kim.biryeong.esekai2.api.damage.critical.HitContext;
import kim.biryeong.esekai2.api.damage.critical.HitKind;
import kim.biryeong.esekai2.api.stat.combat.CombatStats;
import kim.biryeong.esekai2.api.stat.holder.StatHolder;
import kim.biryeong.esekai2.api.stat.holder.StatHolders;
import kim.biryeong.esekai2.impl.stat.registry.StatRegistryAccess;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.List;

public final class CriticalStrikeGameTests {
    /**
     * Verifies that a hit becomes critical when the deterministic roll is below the resolved critical strike chance.
     */
    @GameTest
    public void hitBecomesCriticalWhenRollIsBelowChance(GameTestHelper helper) {
        StatHolder attacker = newHolder(helper);
        attacker.setBaseValue(CombatStats.ATTACK_CRITICAL_STRIKE_CHANCE_INCREASED, 100.0);
        attacker.setBaseValue(CombatStats.ATTACK_CRITICAL_STRIKE_MULTIPLIER_BONUS, 50.0);

        DamageCalculationResult result = calculateAttackHit(
                helper,
                attacker,
                newHolder(helper),
                DamageBreakdown.of(DamageType.PHYSICAL, 100.0),
                new HitContext(HitKind.ATTACK, 0.0, 0.49, 25.0, 150.0)
        );

        helper.assertTrue(result.criticalStrike().criticalStrike(), "Rolls below the resolved critical strike chance should crit");
        helper.assertValueEqual(result.criticalStrike().finalCriticalStrikeChance(), 50.0, "Attack crit chance should use the attack-specific increased stat");
        helper.assertValueEqual(result.criticalStrike().finalCriticalStrikeMultiplier(), 200.0, "Attack crit multiplier should include the attack-specific bonus");
        helper.assertValueEqual(result.criticalDamage().amount(DamageType.PHYSICAL), 200.0, "Critical strikes should scale hit damage by the resolved multiplier");
        helper.succeed();
    }

    /**
     * Verifies that a hit stays non-critical when the deterministic roll is above the resolved critical strike chance.
     */
    @GameTest
    public void hitStaysNonCriticalWhenRollIsAboveChance(GameTestHelper helper) {
        StatHolder attacker = newHolder(helper);
        attacker.setBaseValue(CombatStats.ATTACK_CRITICAL_STRIKE_CHANCE_INCREASED, 100.0);

        DamageCalculationResult result = calculateAttackHit(
                helper,
                attacker,
                newHolder(helper),
                DamageBreakdown.of(DamageType.PHYSICAL, 100.0),
                new HitContext(HitKind.ATTACK, 0.0, 0.50, 25.0, 150.0)
        );

        helper.assertTrue(!result.criticalStrike().criticalStrike(), "Rolls at or above the resolved critical strike chance should not crit");
        helper.assertValueEqual(result.criticalDamage().amount(DamageType.PHYSICAL), 100.0, "Non-critical hits should preserve the scaled damage amount");
        helper.succeed();
    }

    /**
     * Verifies that attack and spell hits use separate critical strike stat axes.
     */
    @GameTest
    public void attackAndSpellHitsUseSeparateCriticalStrikeStats(GameTestHelper helper) {
        StatHolder attacker = newHolder(helper);
        attacker.setBaseValue(CombatStats.SPELL_CRITICAL_STRIKE_CHANCE_INCREASED, 100.0);

        DamageCalculationResult attack = calculateAttackHit(
                helper,
                attacker,
                newHolder(helper),
                DamageBreakdown.of(DamageType.PHYSICAL, 50.0),
                new HitContext(HitKind.ATTACK, 0.0, 0.15, 10.0, 150.0)
        );
        DamageCalculationResult spell = calculateSpellHit(
                helper,
                attacker,
                newHolder(helper),
                DamageBreakdown.of(DamageType.FIRE, 50.0),
                new HitContext(HitKind.SPELL, 0.99, 0.15, 10.0, 150.0)
        );

        helper.assertTrue(!attack.criticalStrike().criticalStrike(), "Attack hits should ignore spell crit chance stats");
        helper.assertTrue(spell.criticalStrike().criticalStrike(), "Spell hits should use spell crit chance stats");
        helper.succeed();
    }

    /**
     * Verifies that final critical strike chance is clamped to one hundred percent.
     */
    @GameTest
    public void criticalStrikeChanceClampsToOneHundred(GameTestHelper helper) {
        StatHolder attacker = newHolder(helper);
        attacker.setBaseValue(CombatStats.ATTACK_CRITICAL_STRIKE_CHANCE_INCREASED, 100.0);

        DamageCalculationResult result = calculateAttackHit(
                helper,
                attacker,
                newHolder(helper),
                DamageBreakdown.of(DamageType.PHYSICAL, 50.0),
                new HitContext(HitKind.ATTACK, 0.0, 0.99, 60.0, 150.0)
        );

        helper.assertValueEqual(result.criticalStrike().finalCriticalStrikeChance(), 100.0, "Critical strike chance should clamp at one hundred percent");
        helper.assertTrue(result.criticalStrike().criticalStrike(), "Rolls below one hundred percent should always crit after clamping");
        helper.succeed();
    }

    /**
     * Verifies that final critical strike multiplier is clamped to one hundred percent minimum.
     */
    @GameTest
    public void criticalStrikeMultiplierClampsToOneHundred(GameTestHelper helper) {
        StatHolder attacker = newHolder(helper);
        attacker.setBaseValue(CombatStats.ATTACK_CRITICAL_STRIKE_MULTIPLIER_BONUS, -80.0);

        DamageCalculationResult result = calculateAttackHit(
                helper,
                attacker,
                newHolder(helper),
                DamageBreakdown.of(DamageType.PHYSICAL, 75.0),
                new HitContext(HitKind.ATTACK, 0.0, 0.0, 100.0, 100.0)
        );

        helper.assertValueEqual(result.criticalStrike().finalCriticalStrikeMultiplier(), 100.0, "Critical strike multiplier should clamp to one hundred percent minimum");
        helper.assertValueEqual(result.criticalDamage().amount(DamageType.PHYSICAL), 75.0, "A one hundred percent critical strike multiplier should not change hit damage");
        helper.succeed();
    }

    /**
     * Verifies that critical strike multiplier scales every typed damage contribution in the hit breakdown.
     */
    @GameTest
    public void criticalStrikeMultiplierScalesEveryTypedContribution(GameTestHelper helper) {
        StatHolder attacker = newHolder(helper);
        attacker.setBaseValue(CombatStats.ATTACK_CRITICAL_STRIKE_MULTIPLIER_BONUS, 50.0);

        DamageCalculationResult result = calculateAttackHit(
                helper,
                attacker,
                newHolder(helper),
                DamageBreakdown.of(DamageType.PHYSICAL, 100.0).with(DamageType.FIRE, 50.0),
                new HitContext(HitKind.ATTACK, 0.0, 0.0, 100.0, 150.0)
        );

        helper.assertValueEqual(result.criticalDamage().amount(DamageType.PHYSICAL), 200.0, "Critical strikes should scale physical damage in the hit breakdown");
        helper.assertValueEqual(result.criticalDamage().amount(DamageType.FIRE), 100.0, "Critical strikes should scale elemental damage in the same hit breakdown");
        helper.succeed();
    }

    /**
     * Verifies that mitigation runs after critical strike scaling has already modified the hit damage.
     */
    @GameTest
    public void criticalStrikeAppliesBeforeMitigation(GameTestHelper helper) {
        StatHolder attacker = newHolder(helper);
        attacker.setBaseValue(CombatStats.ATTACK_CRITICAL_STRIKE_MULTIPLIER_BONUS, 50.0);

        StatHolder defender = newHolder(helper);
        defender.setBaseValue(CombatStats.FIRE_RESISTANCE, 50.0);

        DamageCalculationResult result = calculateAttackHit(
                helper,
                attacker,
                defender,
                DamageBreakdown.of(DamageType.FIRE, 100.0),
                new HitContext(HitKind.ATTACK, 0.0, 0.0, 100.0, 150.0)
        );

        helper.assertValueEqual(result.criticalDamage().amount(DamageType.FIRE), 200.0, "Critical strike should scale fire damage before mitigation");
        helper.assertValueEqual(result.finalDamage().amount(DamageType.FIRE), 100.0, "Mitigation should apply after the critical strike multiplier");
        helper.succeed();
    }

    /**
     * Verifies that hit context rejects hit rolls outside the supported deterministic range.
     */
    @GameTest
    public void hitContextRejectsInvalidHitRoll(GameTestHelper helper) {
        try {
            new HitContext(HitKind.ATTACK, 1.0, 0.5, 5.0, 150.0);
            throw helper.assertionException("Hit context should reject hit rolls outside the supported range");
        } catch (IllegalArgumentException expected) {
            helper.succeed();
        }
    }

    /**
     * Verifies that hit context rejects critical strike rolls outside the supported deterministic range.
     */
    @GameTest
    public void hitContextRejectsInvalidRoll(GameTestHelper helper) {
        try {
            new HitContext(HitKind.ATTACK, 0.5, 1.0, 5.0, 150.0);
            throw helper.assertionException("Hit context should reject critical strike rolls outside the supported range");
        } catch (IllegalArgumentException expected) {
            helper.succeed();
        }
    }

    /**
     * Verifies that hit context rejects invalid base critical strike chance values.
     */
    @GameTest
    public void hitContextRejectsInvalidBaseChance(GameTestHelper helper) {
        try {
            new HitContext(HitKind.ATTACK, 0.5, 0.5, 101.0, 150.0);
            throw helper.assertionException("Hit context should reject base critical strike chance above one hundred percent");
        } catch (IllegalArgumentException expected) {
            helper.succeed();
        }
    }

    /**
     * Verifies that hit context rejects base critical strike multiplier below one hundred percent.
     */
    @GameTest
    public void hitContextRejectsInvalidBaseMultiplier(GameTestHelper helper) {
        try {
            new HitContext(HitKind.ATTACK, 0.5, 0.5, 5.0, 99.0);
            throw helper.assertionException("Hit context should reject base critical strike multiplier below one hundred percent");
        } catch (IllegalArgumentException expected) {
            helper.succeed();
        }
    }

    private static DamageCalculationResult calculateAttackHit(
            GameTestHelper helper,
            StatHolder attacker,
            StatHolder defender,
            DamageBreakdown baseDamage,
            HitContext hitContext
    ) {
        return DamageCalculations.calculateHit(new HitDamageCalculation(
                baseDamage,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                attacker,
                hitContext,
                defender
        ));
    }

    private static DamageCalculationResult calculateSpellHit(
            GameTestHelper helper,
            StatHolder attacker,
            StatHolder defender,
            DamageBreakdown baseDamage,
            HitContext hitContext
    ) {
        return DamageCalculations.calculateHit(new HitDamageCalculation(
                baseDamage,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                attacker,
                hitContext,
                defender
        ));
    }

    private static StatHolder newHolder(GameTestHelper helper) {
        return StatHolders.create(StatRegistryAccess.statRegistry(helper));
    }
}
