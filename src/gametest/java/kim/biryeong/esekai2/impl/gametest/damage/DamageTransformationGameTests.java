package kim.biryeong.esekai2.impl.gametest.damage;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import kim.biryeong.esekai2.api.damage.breakdown.DamageBreakdown;
import kim.biryeong.esekai2.api.damage.breakdown.DamageType;
import kim.biryeong.esekai2.api.damage.calculation.DamageCalculations;
import kim.biryeong.esekai2.api.damage.calculation.DamageOverTimeCalculation;
import kim.biryeong.esekai2.api.damage.calculation.HitDamageCalculation;
import kim.biryeong.esekai2.api.damage.critical.HitContext;
import kim.biryeong.esekai2.api.damage.critical.HitKind;
import kim.biryeong.esekai2.api.damage.transform.DamageConversion;
import kim.biryeong.esekai2.api.damage.transform.ExtraDamageGain;
import kim.biryeong.esekai2.api.stat.combat.CombatStats;
import kim.biryeong.esekai2.api.stat.holder.StatHolder;
import kim.biryeong.esekai2.api.stat.holder.StatHolders;
import kim.biryeong.esekai2.impl.stat.registry.StatRegistryAccess;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.List;

public final class DamageTransformationGameTests {
    /**
     * Verifies that one conversion moves the configured percentage from the source type into the target type.
     */
    @GameTest
    public void conversionMovesDamageBetweenTypes(GameTestHelper helper) {
        var result = DamageCalculations.calculateHit(new HitDamageCalculation(
                DamageBreakdown.of(DamageType.PHYSICAL, 100.0),
                List.of(new DamageConversion(DamageType.PHYSICAL, DamageType.FIRE, 50.0)),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                newHolder(helper),
                nonCriticalHitContext(),
                newHolder(helper)
        ));

        helper.assertValueEqual(result.scaledDamage().amount(DamageType.PHYSICAL), 50.0, "Half of the physical damage should remain after conversion");
        helper.assertValueEqual(result.scaledDamage().amount(DamageType.FIRE), 50.0, "Half of the physical damage should become fire damage after conversion");
        helper.succeed();
    }

    /**
     * Verifies that over-allocated conversions are normalized so the source damage never exceeds 100 percent conversion.
     */
    @GameTest
    public void conversionNormalizesWhenTotalPercentExceedsOneHundred(GameTestHelper helper) {
        var result = DamageCalculations.calculateHit(new HitDamageCalculation(
                DamageBreakdown.of(DamageType.PHYSICAL, 120.0),
                List.of(
                        new DamageConversion(DamageType.PHYSICAL, DamageType.FIRE, 80.0),
                        new DamageConversion(DamageType.PHYSICAL, DamageType.COLD, 70.0)
                ),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                newHolder(helper),
                nonCriticalHitContext(),
                newHolder(helper)
        ));

        helper.assertValueEqual(result.scaledDamage().amount(DamageType.PHYSICAL), 0.0, "Normalized conversion should consume the full physical source amount");
        helper.assertTrue(Math.abs(result.scaledDamage().amount(DamageType.FIRE) - 64.0) < 1.0E-9, "Fire conversion should keep its normalized share of the source damage");
        helper.assertTrue(Math.abs(result.scaledDamage().amount(DamageType.COLD) - 56.0) < 1.0E-9, "Cold conversion should keep its normalized share of the source damage");
        helper.succeed();
    }

    /**
     * Verifies that gain-as-extra samples the post-conversion damage and adds new damage without consuming the source type.
     */
    @GameTest
    public void gainAsExtraUsesConvertedBreakdownWithoutConsumingSource(GameTestHelper helper) {
        var result = DamageCalculations.calculateHit(new HitDamageCalculation(
                DamageBreakdown.of(DamageType.PHYSICAL, 100.0),
                List.of(new DamageConversion(DamageType.PHYSICAL, DamageType.FIRE, 50.0)),
                List.of(new ExtraDamageGain(DamageType.FIRE, DamageType.COLD, 20.0)),
                List.of(),
                List.of(),
                List.of(),
                newHolder(helper),
                nonCriticalHitContext(),
                newHolder(helper)
        ));

        helper.assertValueEqual(result.scaledDamage().amount(DamageType.PHYSICAL), 50.0, "Conversion should leave the expected physical remainder");
        helper.assertValueEqual(result.scaledDamage().amount(DamageType.FIRE), 50.0, "Gain-as-extra should not consume the converted fire damage");
        helper.assertValueEqual(result.scaledDamage().amount(DamageType.COLD), 10.0, "Gain-as-extra should add cold damage from the converted fire amount");
        helper.succeed();
    }

    /**
     * Verifies that hit and damage-over-time share the same transformed breakdown before each mitigation path diverges.
     */
    @GameTest
    public void hitAndDamageOverTimeShareTransformationStage(GameTestHelper helper) {
        StatHolder holder = newHolder(helper);
        holder.setBaseValue(CombatStats.ARMOUR, 100.0);

        List<DamageConversion> conversions = List.of(new DamageConversion(DamageType.PHYSICAL, DamageType.FIRE, 50.0));
        List<ExtraDamageGain> extraGains = List.of(new ExtraDamageGain(DamageType.FIRE, DamageType.COLD, 20.0));

        var hit = DamageCalculations.calculateHit(new HitDamageCalculation(
                DamageBreakdown.of(DamageType.PHYSICAL, 100.0),
                conversions,
                extraGains,
                List.of(),
                List.of(),
                List.of(),
                newHolder(helper),
                nonCriticalHitContext(),
                holder
        ));
        var dot = DamageCalculations.calculateDamageOverTime(new DamageOverTimeCalculation(
                DamageBreakdown.of(DamageType.PHYSICAL, 100.0),
                conversions,
                extraGains,
                List.of(),
                List.of(),
                holder
        ));

        helper.assertTrue(hit.scaledDamage().entries().equals(dot.scaledDamage().entries()), "Hit and DoT should share the same transformed pre-mitigation breakdown");
        helper.assertTrue(hit.finalDamage().amount(DamageType.PHYSICAL) < dot.finalDamage().amount(DamageType.PHYSICAL), "Physical mitigation should still diverge between hit and DoT");
        helper.succeed();
    }

    /**
     * Verifies that the damage conversion codec round-trips a valid typed conversion entry.
     */
    @GameTest
    public void damageConversionCodecRoundTrips(GameTestHelper helper) {
        DamageConversion decoded = DamageConversion.CODEC.parse(
                        JsonOps.INSTANCE,
                        JsonParser.parseString("""
                                {
                                  "from": "physical",
                                  "to": "fire",
                                  "value_percent": 50.0
                                }
                                """)
                )
                .getOrThrow(message -> new IllegalStateException("Failed to decode damage conversion fixture: " + message));

        helper.assertValueEqual(decoded.from(), DamageType.PHYSICAL, "Decoded conversion source should match the fixture");
        helper.assertValueEqual(decoded.to(), DamageType.FIRE, "Decoded conversion target should match the fixture");
        helper.assertValueEqual(decoded.valuePercent(), 50.0, "Decoded conversion percentage should match the fixture");
        helper.succeed();
    }

    /**
     * Verifies that the gain-as-extra codec round-trips a valid extra damage entry.
     */
    @GameTest
    public void extraDamageGainCodecRoundTrips(GameTestHelper helper) {
        ExtraDamageGain decoded = ExtraDamageGain.CODEC.parse(
                        JsonOps.INSTANCE,
                        JsonParser.parseString("""
                                {
                                  "source": "fire",
                                  "gained_type": "cold",
                                  "value_percent": 25.0
                                }
                                """)
                )
                .getOrThrow(message -> new IllegalStateException("Failed to decode extra damage gain fixture: " + message));

        helper.assertValueEqual(decoded.source(), DamageType.FIRE, "Decoded extra gain source should match the fixture");
        helper.assertValueEqual(decoded.gainedType(), DamageType.COLD, "Decoded extra gain target should match the fixture");
        helper.assertValueEqual(decoded.valuePercent(), 25.0, "Decoded extra gain percentage should match the fixture");
        helper.succeed();
    }

    /**
     * Verifies that self-targeted conversions are rejected by the public conversion model.
     */
    @GameTest
    public void selfConversionIsRejected(GameTestHelper helper) {
        try {
            new DamageConversion(DamageType.FIRE, DamageType.FIRE, 10.0);
            throw helper.assertionException("Self-targeted conversion should be rejected");
        } catch (IllegalArgumentException expected) {
            helper.succeed();
        }
    }

    /**
     * Verifies that self-targeted gain-as-extra rules are rejected by the public extra damage model.
     */
    @GameTest
    public void selfExtraDamageGainIsRejected(GameTestHelper helper) {
        try {
            new ExtraDamageGain(DamageType.COLD, DamageType.COLD, 10.0);
            throw helper.assertionException("Self-targeted extra gain should be rejected");
        } catch (IllegalArgumentException expected) {
            helper.succeed();
        }
    }

    private static StatHolder newHolder(GameTestHelper helper) {
        return StatHolders.create(StatRegistryAccess.statRegistry(helper));
    }

    private static HitContext nonCriticalHitContext() {
        return new HitContext(HitKind.ATTACK, 0.0, 0.99, 0.0, 150.0);
    }
}
