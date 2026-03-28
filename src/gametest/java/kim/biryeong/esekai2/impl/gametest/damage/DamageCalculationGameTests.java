package kim.biryeong.esekai2.impl.gametest.damage;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import kim.biryeong.esekai2.api.damage.breakdown.DamageBreakdown;
import kim.biryeong.esekai2.api.damage.breakdown.DamageType;
import kim.biryeong.esekai2.api.damage.calculation.DamageCalculationResult;
import kim.biryeong.esekai2.api.damage.calculation.DamageCalculations;
import kim.biryeong.esekai2.api.damage.calculation.HitDamageCalculation;
import kim.biryeong.esekai2.api.damage.critical.HitContext;
import kim.biryeong.esekai2.api.damage.critical.HitKind;
import kim.biryeong.esekai2.api.damage.mitigation.ElementalExposure;
import kim.biryeong.esekai2.api.damage.mitigation.ResistancePenetration;
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

public final class DamageCalculationGameTests {
    /**
     * Verifies that flat typed ADD scaling can introduce a new damage type alongside the base damage.
     */
    @GameTest
    public void flatTypedAddIntroducesNewDamageType(GameTestHelper helper) {
        DamageCalculationResult result = DamageCalculations.calculateHit(new HitDamageCalculation(
                DamageBreakdown.of(DamageType.PHYSICAL, 10.0),
                List.of(),
                List.of(),
                List.of(new DamageScaling(DamageScalingTarget.of(DamageType.FIRE), DamageScalingOperation.ADD, 5.0)),
                List.of(),
                List.of(),
                newHolder(helper),
                nonCriticalHitContext(),
                newHolder(helper)
        ));

        helper.assertValueEqual(result.scaledDamage().amount(DamageType.PHYSICAL), 10.0, "Physical base damage should remain in the scaled breakdown");
        helper.assertValueEqual(result.scaledDamage().amount(DamageType.FIRE), 5.0, "Flat ADD scaling should introduce a new fire damage component");
        helper.assertValueEqual(result.finalDamage().amount(DamageType.FIRE), 5.0, "Fire damage should survive unchanged without mitigation stats");
        helper.succeed();
    }

    /**
     * Verifies that all-target INCREASED scaling applies to every typed component in the breakdown.
     */
    @GameTest
    public void allIncreasedScalesEveryDamageType(GameTestHelper helper) {
        DamageCalculationResult result = DamageCalculations.calculateHit(new HitDamageCalculation(
                DamageBreakdown.of(DamageType.PHYSICAL, 10.0).with(DamageType.FIRE, 20.0),
                List.of(),
                List.of(),
                List.of(new DamageScaling(DamageScalingTarget.all(), DamageScalingOperation.INCREASED, 50.0)),
                List.of(),
                List.of(),
                newHolder(helper),
                nonCriticalHitContext(),
                newHolder(helper)
        ));

        helper.assertValueEqual(result.scaledDamage().amount(DamageType.PHYSICAL), 15.0, "All-target increased scaling should affect physical damage");
        helper.assertValueEqual(result.scaledDamage().amount(DamageType.FIRE), 30.0, "All-target increased scaling should affect fire damage");
        helper.succeed();
    }

    /**
     * Verifies that specific MORE scaling only affects the targeted damage type.
     */
    @GameTest
    public void specificMoreScalesOnlyMatchingType(GameTestHelper helper) {
        DamageCalculationResult result = DamageCalculations.calculateHit(new HitDamageCalculation(
                DamageBreakdown.of(DamageType.PHYSICAL, 10.0).with(DamageType.FIRE, 20.0),
                List.of(),
                List.of(),
                List.of(new DamageScaling(DamageScalingTarget.of(DamageType.FIRE), DamageScalingOperation.MORE, 50.0)),
                List.of(),
                List.of(),
                newHolder(helper),
                nonCriticalHitContext(),
                newHolder(helper)
        ));

        helper.assertValueEqual(result.scaledDamage().amount(DamageType.PHYSICAL), 10.0, "Fire-specific MORE scaling should not affect physical damage");
        helper.assertValueEqual(result.scaledDamage().amount(DamageType.FIRE), 30.0, "Fire-specific MORE scaling should only affect fire damage");
        helper.succeed();
    }

    /**
     * Verifies that hit calculations apply mitigation only after the explicit scaling pipeline completes.
     */
    @GameTest
    public void calculationAppliesMitigationAfterScaling(GameTestHelper helper) {
        StatHolder holder = newHolder(helper);
        holder.setBaseValue(CombatStats.FIRE_RESISTANCE, 50.0);

        DamageCalculationResult result = DamageCalculations.calculateHit(new HitDamageCalculation(
                DamageBreakdown.of(DamageType.FIRE, 100.0),
                List.of(),
                List.of(),
                List.of(new DamageScaling(DamageScalingTarget.of(DamageType.FIRE), DamageScalingOperation.INCREASED, 50.0)),
                List.of(),
                List.of(),
                newHolder(helper),
                nonCriticalHitContext(),
                holder
        ));

        helper.assertValueEqual(result.scaledDamage().amount(DamageType.FIRE), 150.0, "Scaling should produce the pre-mitigation fire damage amount");
        helper.assertValueEqual(result.finalDamage().amount(DamageType.FIRE), 75.0, "Mitigation should apply after scaling has finished");
        helper.assertValueEqual(result.mitigation().mitigationDelta(DamageType.FIRE), 75.0, "Mitigation delta should reflect the resisted amount after scaling");
        helper.succeed();
    }

    /**
     * Verifies that fixed damage still bypasses mitigation after the scaling pipeline modifies it.
     */
    @GameTest
    public void fixedDamageBypassesMitigationAfterScaling(GameTestHelper helper) {
        StatHolder holder = newHolder(helper);
        holder.setBaseValue(CombatStats.ARMOUR, 9999.0);
        holder.setBaseValue(CombatStats.FIRE_RESISTANCE, 90.0);

        DamageCalculationResult result = DamageCalculations.calculateHit(new HitDamageCalculation(
                DamageBreakdown.of(DamageType.FIXED, 20.0),
                List.of(),
                List.of(),
                List.of(new DamageScaling(DamageScalingTarget.of(DamageType.FIXED), DamageScalingOperation.MORE, 50.0)),
                List.of(),
                List.of(),
                newHolder(helper),
                nonCriticalHitContext(),
                holder
        ));

        helper.assertValueEqual(result.scaledDamage().amount(DamageType.FIXED), 30.0, "Scaling should modify fixed damage before mitigation");
        helper.assertValueEqual(result.finalDamage().amount(DamageType.FIXED), 30.0, "Fixed damage should bypass mitigation even after scaling");
        helper.succeed();
    }

    /**
     * Verifies that invalid all-target ADD scaling is rejected by the public scaling model.
     */
    @GameTest
    public void allTargetAddScalingIsRejected(GameTestHelper helper) {
        try {
            new DamageScaling(DamageScalingTarget.all(), DamageScalingOperation.ADD, 5.0);
            throw helper.assertionException("All-target ADD scaling should be rejected");
        } catch (IllegalArgumentException expected) {
            helper.succeed();
        }
    }

    /**
     * Verifies that the damage scaling codec round-trips a concrete typed scaling entry.
     */
    @GameTest
    public void damageScalingCodecRoundTrips(GameTestHelper helper) {
        DamageScaling decoded = DamageScaling.CODEC.parse(
                        JsonOps.INSTANCE,
                        JsonParser.parseString("""
                                {
                                  "target": "fire",
                                  "operation": "add",
                                  "value": 5.0
                                }
                                """)
                )
                .getOrThrow(message -> new IllegalStateException("Failed to decode damage scaling fixture: " + message));

        helper.assertTrue(decoded.target().damageType().isPresent(), "Decoded scaling target should keep its concrete damage type");
        helper.assertValueEqual(decoded.target().damageType().orElseThrow(), DamageType.FIRE, "Decoded scaling target should match the fixture");
        helper.assertValueEqual(decoded.operation(), DamageScalingOperation.ADD, "Decoded scaling operation should match the fixture");
        helper.assertValueEqual(decoded.value(), 5.0, "Decoded scaling value should match the fixture");
        helper.succeed();
    }

    /**
     * Verifies that an empty hit calculation stays empty through scaling and mitigation.
     */
    @GameTest
    public void emptyHitCalculationProducesEmptyResult(GameTestHelper helper) {
        DamageCalculationResult result = DamageCalculations.calculateHit(new HitDamageCalculation(
                DamageBreakdown.empty(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                newHolder(helper),
                nonCriticalHitContext(),
                newHolder(helper)
        ));

        helper.assertTrue(result.baseDamage().isEmpty(), "Base damage should stay empty");
        helper.assertTrue(result.scaledDamage().isEmpty(), "Scaled damage should stay empty");
        helper.assertValueEqual(result.hitResolution().finalHitChance(), 5.0, "Attack hit resolution should still apply its minimum hit chance clamp");
        helper.assertTrue(result.criticalDamage().isEmpty(), "Critical damage should stay empty");
        helper.assertTrue(!result.criticalStrike().criticalStrike(), "Empty hit calculation should not resolve as a critical strike");
        helper.assertTrue(result.finalDamage().isEmpty(), "Final damage should stay empty");
        helper.succeed();
    }

    /**
     * Verifies that hit calculations apply exposure, caps, and penetration only after the explicit scaling pipeline completes.
     */
    @GameTest
    public void calculationAppliesExposureAndPenetrationAfterScaling(GameTestHelper helper) {
        StatHolder holder = newHolder(helper);
        holder.setBaseValue(CombatStats.FIRE_RESISTANCE, 90.0);
        holder.setBaseValue(CombatStats.MAX_FIRE_RESISTANCE, 75.0);

        DamageCalculationResult result = DamageCalculations.calculateHit(new HitDamageCalculation(
                DamageBreakdown.of(DamageType.FIRE, 100.0),
                List.of(),
                List.of(),
                List.of(new DamageScaling(DamageScalingTarget.of(DamageType.FIRE), DamageScalingOperation.INCREASED, 50.0)),
                List.of(new ElementalExposure(DamageType.FIRE, 10.0)),
                List.of(new ResistancePenetration(DamageType.FIRE, 20.0)),
                newHolder(helper),
                nonCriticalHitContext(),
                holder
        ));

        helper.assertValueEqual(result.scaledDamage().amount(DamageType.FIRE), 150.0, "Scaling should complete before exposure and penetration are applied");
        helper.assertValueEqual(result.finalDamage().amount(DamageType.FIRE), 67.5, "Mitigation should apply exposure, cap, and penetration after scaling");
        helper.assertValueEqual(result.mitigation().mitigationDelta(DamageType.FIRE), 82.5, "Mitigation delta should reflect the post-scaling exposure and penetration result");
        helper.succeed();
    }

    private static StatHolder newHolder(GameTestHelper helper) {
        return StatHolders.create(StatRegistryAccess.statRegistry(helper));
    }

    private static HitContext nonCriticalHitContext() {
        return new HitContext(HitKind.ATTACK, 0.0, 0.99, 0.0, 150.0);
    }
}
