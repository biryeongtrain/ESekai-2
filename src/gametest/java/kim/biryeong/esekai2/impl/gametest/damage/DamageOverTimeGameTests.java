package kim.biryeong.esekai2.impl.gametest.damage;

import kim.biryeong.esekai2.api.damage.breakdown.DamageBreakdown;
import kim.biryeong.esekai2.api.damage.breakdown.DamageType;
import kim.biryeong.esekai2.api.damage.calculation.DamageCalculations;
import kim.biryeong.esekai2.api.damage.calculation.DamageOverTimeCalculation;
import kim.biryeong.esekai2.api.damage.calculation.DamageOverTimeResult;
import kim.biryeong.esekai2.api.damage.calculation.HitDamageCalculation;
import kim.biryeong.esekai2.api.damage.critical.HitContext;
import kim.biryeong.esekai2.api.damage.critical.HitKind;
import kim.biryeong.esekai2.api.damage.mitigation.ElementalExposure;
import kim.biryeong.esekai2.api.damage.scaling.DamageScaling;
import kim.biryeong.esekai2.api.damage.scaling.DamageScalingOperation;
import kim.biryeong.esekai2.api.damage.scaling.DamageScalingTarget;
import kim.biryeong.esekai2.api.stat.combat.CombatStats;
import kim.biryeong.esekai2.api.stat.holder.StatHolder;
import kim.biryeong.esekai2.api.stat.holder.StatHolders;
import kim.biryeong.esekai2.impl.stat.registry.StatRegistryAccess;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.List;

public final class DamageOverTimeGameTests {
    /**
     * Verifies that physical damage over time ignores armour while the hit path still uses armour mitigation.
     */
    @GameTest
    public void physicalDamageOverTimeIgnoresArmour(GameTestHelper helper) {
        StatHolder holder = newHolder(helper);
        holder.setBaseValue(CombatStats.ARMOUR, 100.0);

        DamageOverTimeResult dot = DamageCalculations.calculateDamageOverTime(new DamageOverTimeCalculation(
                DamageBreakdown.of(DamageType.PHYSICAL, 50.0),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                holder
        ));

        helper.assertValueEqual(dot.scaledDamage().amount(DamageType.PHYSICAL), 50.0, "Physical DoT should preserve the pre-mitigation physical amount");
        helper.assertValueEqual(dot.finalDamage().amount(DamageType.PHYSICAL), 50.0, "Physical DoT should ignore armour");
        helper.succeed();
    }

    /**
     * Verifies that elemental damage over time still uses elemental resistance and maximum resistance.
     */
    @GameTest
    public void elementalDamageOverTimeUsesResistance(GameTestHelper helper) {
        StatHolder holder = newHolder(helper);
        holder.setBaseValue(CombatStats.FIRE_RESISTANCE, 80.0);
        holder.setBaseValue(CombatStats.MAX_FIRE_RESISTANCE, 75.0);

        DamageOverTimeResult dot = DamageCalculations.calculateDamageOverTime(new DamageOverTimeCalculation(
                DamageBreakdown.of(DamageType.FIRE, 100.0),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                holder
        ));

        helper.assertValueEqual(dot.finalDamage().amount(DamageType.FIRE), 25.0, "Fire DoT should be reduced by the capped fire resistance");
        helper.assertValueEqual(dot.mitigation().mitigationDelta(DamageType.FIRE), 75.0, "DoT mitigation delta should record the resisted fire damage");
        helper.succeed();
    }

    /**
     * Verifies that negative chaos resistance amplifies chaos damage over time.
     */
    @GameTest
    public void negativeChaosResistanceAmplifiesDamageOverTime(GameTestHelper helper) {
        StatHolder holder = newHolder(helper);
        holder.setBaseValue(CombatStats.CHAOS_RESISTANCE, -20.0);

        DamageOverTimeResult dot = DamageCalculations.calculateDamageOverTime(new DamageOverTimeCalculation(
                DamageBreakdown.of(DamageType.CHAOS, 100.0),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                holder
        ));

        helper.assertValueEqual(dot.finalDamage().amount(DamageType.CHAOS), 120.0, "Negative chaos resistance should amplify chaos DoT");
        helper.assertValueEqual(dot.mitigation().mitigationDelta(DamageType.CHAOS), -20.0, "DoT mitigation delta should go negative when resistance amplifies damage");
        helper.succeed();
    }

    /**
     * Verifies that fixed damage over time continues to bypass the mitigation layer.
     */
    @GameTest
    public void fixedDamageOverTimeBypassesMitigation(GameTestHelper helper) {
        StatHolder holder = newHolder(helper);
        holder.setBaseValue(CombatStats.ARMOUR, 1000.0);
        holder.setBaseValue(CombatStats.FIRE_RESISTANCE, 90.0);

        DamageOverTimeResult dot = DamageCalculations.calculateDamageOverTime(new DamageOverTimeCalculation(
                DamageBreakdown.of(DamageType.FIXED, 40.0),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                holder
        ));

        helper.assertValueEqual(dot.finalDamage().amount(DamageType.FIXED), 40.0, "Fixed DoT should bypass mitigation");
        helper.assertValueEqual(dot.mitigation().mitigationDelta(DamageType.FIXED), 0.0, "Fixed DoT should not record a mitigation delta");
        helper.succeed();
    }

    /**
     * Verifies that hit and damage-over-time share the same scaling result but diverge at physical mitigation.
     */
    @GameTest
    public void hitAndDamageOverTimeDivergeOnlyAtMitigation(GameTestHelper helper) {
        StatHolder holder = newHolder(helper);
        holder.setBaseValue(CombatStats.ARMOUR, 100.0);

        List<DamageScaling> scaling = List.of(
                new DamageScaling(DamageScalingTarget.of(DamageType.PHYSICAL), DamageScalingOperation.ADD, 10.0),
                new DamageScaling(DamageScalingTarget.all(), DamageScalingOperation.INCREASED, 50.0)
        );

        var hit = DamageCalculations.calculateHit(new HitDamageCalculation(
                DamageBreakdown.of(DamageType.PHYSICAL, 50.0),
                List.of(),
                List.of(),
                scaling,
                List.of(),
                List.of(),
                newHolder(helper),
                nonCriticalHitContext(),
                holder
        ));
        var dot = DamageCalculations.calculateDamageOverTime(new DamageOverTimeCalculation(
                DamageBreakdown.of(DamageType.PHYSICAL, 50.0),
                List.of(),
                List.of(),
                scaling,
                List.of(),
                holder
        ));

        helper.assertValueEqual(hit.scaledDamage().amount(DamageType.PHYSICAL), 90.0, "Hit scaling should produce the expected pre-mitigation physical damage");
        helper.assertValueEqual(dot.scaledDamage().amount(DamageType.PHYSICAL), 90.0, "DoT scaling should match the hit scaling result");
        helper.assertTrue(hit.finalDamage().amount(DamageType.PHYSICAL) < dot.finalDamage().amount(DamageType.PHYSICAL), "Physical hit damage should be lower than physical DoT after mitigation");
        helper.succeed();
    }

    /**
     * Verifies that an empty damage-over-time calculation remains empty through scaling and mitigation.
     */
    @GameTest
    public void emptyDamageOverTimeCalculationProducesEmptyResult(GameTestHelper helper) {
        DamageOverTimeResult dot = DamageCalculations.calculateDamageOverTime(new DamageOverTimeCalculation(
                DamageBreakdown.empty(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                newHolder(helper)
        ));

        helper.assertTrue(dot.baseDamage().isEmpty(), "Empty DoT base damage should stay empty");
        helper.assertTrue(dot.scaledDamage().isEmpty(), "Empty DoT scaled damage should stay empty");
        helper.assertTrue(dot.finalDamage().isEmpty(), "Empty DoT final damage should stay empty");
        helper.succeed();
    }

    /**
     * Verifies that damage-over-time calculations apply elemental exposure through the mitigation path.
     */
    @GameTest
    public void damageOverTimeAppliesExposure(GameTestHelper helper) {
        StatHolder holder = newHolder(helper);
        holder.setBaseValue(CombatStats.FIRE_RESISTANCE, 50.0);

        DamageOverTimeResult dot = DamageCalculations.calculateDamageOverTime(new DamageOverTimeCalculation(
                DamageBreakdown.of(DamageType.FIRE, 100.0),
                List.of(),
                List.of(),
                List.of(),
                List.of(new ElementalExposure(DamageType.FIRE, 20.0)),
                holder
        ));

        helper.assertValueEqual(dot.finalDamage().amount(DamageType.FIRE), 70.0, "Exposure should lower effective fire resistance for damage over time");
        helper.assertValueEqual(dot.mitigation().mitigationDelta(DamageType.FIRE), 30.0, "DoT mitigation delta should reflect the exposure-adjusted fire resistance");
        helper.succeed();
    }

    private static StatHolder newHolder(GameTestHelper helper) {
        return StatHolders.create(StatRegistryAccess.statRegistry(helper));
    }

    private static HitContext nonCriticalHitContext() {
        return new HitContext(HitKind.ATTACK, 0.0, 0.99, 0.0, 150.0);
    }
}
