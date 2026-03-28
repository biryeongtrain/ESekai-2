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

public final class AccuracyEvasionGameTests {
    /**
     * Verifies that attack hits succeed when the deterministic hit roll is below the resolved PoE-style hit chance.
     */
    @GameTest
    public void attackHitsWhenRollIsBelowResolvedHitChance(GameTestHelper helper) {
        StatHolder attacker = newHolder(helper);
        attacker.setBaseValue(CombatStats.ACCURACY, 1000.0);

        StatHolder defender = newHolder(helper);
        defender.setBaseValue(CombatStats.EVADE, 100.0);

        DamageCalculationResult result = calculateHit(attacker, defender, new HitContext(HitKind.ATTACK, 0.90, 0.99, 0.0, 150.0));

        helper.assertTrue(result.hitResolution().hitSuccessful(), "Attack hit roll below the resolved hit chance should hit");
        helper.assertTrue(!result.hitResolution().bypassedAccuracyCheck(), "Attack hits should not bypass accuracy and evasion");
        helper.assertTrue(result.finalDamage().amount(DamageType.PHYSICAL) > 0.0, "Successful attack hits should continue into damage application");
        helper.succeed();
    }

    /**
     * Verifies that attack hits miss when the deterministic hit roll is above the resolved PoE-style hit chance.
     */
    @GameTest
    public void attackMissesWhenRollIsAboveResolvedHitChance(GameTestHelper helper) {
        StatHolder attacker = newHolder(helper);
        attacker.setBaseValue(CombatStats.ACCURACY, 100.0);

        StatHolder defender = newHolder(helper);
        defender.setBaseValue(CombatStats.EVADE, 10_000.0);

        DamageCalculationResult result = calculateHit(attacker, defender, new HitContext(HitKind.ATTACK, 0.80, 0.0, 100.0, 150.0));

        helper.assertTrue(!result.hitResolution().hitSuccessful(), "Attack hit roll above the resolved hit chance should miss");
        helper.assertValueEqual(result.finalDamage().totalAmount(), 0.0, "Missed attacks should deal no final damage");
        helper.assertTrue(result.criticalDamage().isEmpty(), "Missed attacks should not proceed into the critical damage layer");
        helper.succeed();
    }

    /**
     * Verifies that attack hit chance clamps to five percent minimum.
     */
    @GameTest
    public void attackHitChanceClampsToFivePercentMinimum(GameTestHelper helper) {
        StatHolder attacker = newHolder(helper);
        StatHolder defender = newHolder(helper);
        defender.setBaseValue(CombatStats.EVADE, 10_000.0);

        DamageCalculationResult result = calculateHit(attacker, defender, new HitContext(HitKind.ATTACK, 0.049, 0.99, 0.0, 150.0));

        helper.assertValueEqual(result.hitResolution().finalHitChance(), 5.0, "Attack hit chance should clamp to five percent minimum");
        helper.assertTrue(result.hitResolution().hitSuccessful(), "Rolls below the clamped five percent hit chance should still hit");
        helper.succeed();
    }

    /**
     * Verifies that attack hit chance clamps to one hundred percent maximum.
     */
    @GameTest
    public void attackHitChanceClampsToOneHundredPercentMaximum(GameTestHelper helper) {
        StatHolder attacker = newHolder(helper);
        attacker.setBaseValue(CombatStats.ACCURACY, 1000.0);

        DamageCalculationResult result = calculateHit(attacker, newHolder(helper), new HitContext(HitKind.ATTACK, 0.999, 0.99, 0.0, 150.0));

        helper.assertValueEqual(result.hitResolution().finalHitChance(), 100.0, "Attack hit chance should clamp to one hundred percent maximum");
        helper.assertTrue(result.hitResolution().hitSuccessful(), "Rolls below one hundred percent should always hit after clamping");
        helper.succeed();
    }

    /**
     * Verifies that spell hits bypass accuracy and evasion and always report a full hit chance.
     */
    @GameTest
    public void spellHitsBypassAccuracyAndEvasion(GameTestHelper helper) {
        StatHolder defender = newHolder(helper);
        defender.setBaseValue(CombatStats.EVADE, 10_000.0);

        DamageCalculationResult result = calculateHit(newHolder(helper), defender, new HitContext(HitKind.SPELL, 0.999, 0.99, 0.0, 150.0));

        helper.assertTrue(result.hitResolution().hitSuccessful(), "Spell hits should always hit in the current phase");
        helper.assertTrue(result.hitResolution().bypassedAccuracyCheck(), "Spell hits should report that they bypassed accuracy and evasion");
        helper.assertValueEqual(result.hitResolution().finalHitChance(), 100.0, "Spell hits should report a full hit chance");
        helper.succeed();
    }

    /**
     * Verifies that attack misses skip critical strike application and return a zero mitigation result.
     */
    @GameTest
    public void attackMissReturnsZeroDamageAndNoCriticalStrike(GameTestHelper helper) {
        StatHolder attacker = newHolder(helper);
        attacker.setBaseValue(CombatStats.ACCURACY, 100.0);
        attacker.setBaseValue(CombatStats.ATTACK_CRITICAL_STRIKE_CHANCE_INCREASED, 500.0);

        StatHolder defender = newHolder(helper);
        defender.setBaseValue(CombatStats.EVADE, 10_000.0);

        DamageCalculationResult result = calculateHit(attacker, defender, new HitContext(HitKind.ATTACK, 0.80, 0.0, 50.0, 150.0));

        helper.assertTrue(!result.criticalStrike().criticalStrike(), "Missed attacks should never resolve as critical strikes");
        helper.assertTrue(result.criticalDamage().isEmpty(), "Missed attacks should not produce critical damage");
        helper.assertTrue(result.mitigation().incomingDamage().isEmpty(), "Missed attacks should feed zero incoming damage into mitigation");
        helper.assertTrue(result.finalDamage().isEmpty(), "Missed attacks should end with zero final damage");
        helper.succeed();
    }

    /**
     * Verifies that successful attack hits continue into the critical strike and mitigation layers after hit resolution.
     */
    @GameTest
    public void successfulAttackHitsContinueIntoCriticalStrikeAndMitigation(GameTestHelper helper) {
        StatHolder attacker = newHolder(helper);
        attacker.setBaseValue(CombatStats.ACCURACY, 1000.0);
        attacker.setBaseValue(CombatStats.ATTACK_CRITICAL_STRIKE_MULTIPLIER_BONUS, 50.0);

        StatHolder defender = newHolder(helper);
        defender.setBaseValue(CombatStats.FIRE_RESISTANCE, 50.0);

        DamageCalculationResult result = DamageCalculations.calculateHit(new HitDamageCalculation(
                DamageBreakdown.of(DamageType.FIRE, 100.0),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                attacker,
                new HitContext(HitKind.ATTACK, 0.50, 0.0, 100.0, 150.0),
                defender
        ));

        helper.assertTrue(result.hitResolution().hitSuccessful(), "Successful attacks should pass hit resolution");
        helper.assertTrue(result.criticalStrike().criticalStrike(), "Successful attacks should still resolve critical strikes");
        helper.assertValueEqual(result.criticalDamage().amount(DamageType.FIRE), 200.0, "Successful attacks should still scale damage through the critical strike layer");
        helper.assertValueEqual(result.finalDamage().amount(DamageType.FIRE), 100.0, "Successful attacks should still enter mitigation after the critical strike layer");
        helper.succeed();
    }

    /**
     * Verifies that hit context rejects hit rolls outside the supported deterministic range.
     */
    @GameTest
    public void hitContextRejectsInvalidHitRoll(GameTestHelper helper) {
        try {
            new HitContext(HitKind.ATTACK, 1.0, 0.0, 5.0, 150.0);
            throw helper.assertionException("Hit context should reject hit rolls outside the supported range");
        } catch (IllegalArgumentException expected) {
            helper.succeed();
        }
    }

    private static DamageCalculationResult calculateHit(
            StatHolder attacker,
            StatHolder defender,
            HitContext hitContext
    ) {
        return DamageCalculations.calculateHit(new HitDamageCalculation(
                DamageBreakdown.of(DamageType.PHYSICAL, 100.0),
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
