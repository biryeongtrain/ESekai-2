package kim.biryeong.esekai2.impl.gametest.damage;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import kim.biryeong.esekai2.api.damage.breakdown.DamageBreakdown;
import kim.biryeong.esekai2.api.damage.breakdown.DamageType;
import kim.biryeong.esekai2.api.damage.mitigation.DamageMitigationResult;
import kim.biryeong.esekai2.api.damage.mitigation.DamageMitigations;
import kim.biryeong.esekai2.api.damage.mitigation.ElementalExposure;
import kim.biryeong.esekai2.api.damage.mitigation.ResistancePenetration;
import kim.biryeong.esekai2.api.stat.combat.CombatStats;
import kim.biryeong.esekai2.api.stat.holder.StatHolder;
import kim.biryeong.esekai2.api.stat.holder.StatHolders;
import kim.biryeong.esekai2.impl.stat.registry.StatRegistryAccess;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.List;

public final class DamageMitigationGameTests {
    /**
     * Verifies that elemental mitigation respects both the current resistance and the matching maximum resistance cap.
     */
    @GameTest
    public void elementalMitigationUsesMaximumResistanceCap(GameTestHelper helper) {
        StatHolder holder = newHolder(helper);
        holder.setBaseValue(CombatStats.FIRE_RESISTANCE, 80.0);
        holder.setBaseValue(CombatStats.MAX_FIRE_RESISTANCE, 75.0);

        DamageMitigationResult result = DamageMitigations.mitigateHit(holder, DamageBreakdown.of(DamageType.FIRE, 100.0), List.of(), List.of());

        helper.assertValueEqual(result.mitigatedDamage().amount(DamageType.FIRE), 25.0, "Fire damage should be reduced by the capped maximum resistance");
        helper.assertValueEqual(result.mitigationDelta(DamageType.FIRE), 75.0, "Mitigation delta should record the prevented fire damage");
        helper.succeed();
    }

    /**
     * Verifies that negative chaos resistance amplifies the final damage and produces a negative mitigation delta.
     */
    @GameTest
    public void negativeChaosResistanceAmplifiesDamage(GameTestHelper helper) {
        StatHolder holder = newHolder(helper);
        holder.setBaseValue(CombatStats.CHAOS_RESISTANCE, -20.0);
        holder.setBaseValue(CombatStats.MAX_CHAOS_RESISTANCE, 75.0);

        DamageMitigationResult result = DamageMitigations.mitigateHit(holder, DamageBreakdown.of(DamageType.CHAOS, 100.0), List.of(), List.of());

        helper.assertValueEqual(result.mitigatedDamage().amount(DamageType.CHAOS), 120.0, "Negative chaos resistance should amplify incoming chaos damage");
        helper.assertValueEqual(result.mitigationDelta(DamageType.CHAOS), -20.0, "Mitigation delta should be negative when mitigation amplifies damage");
        helper.succeed();
    }

    /**
     * Verifies that physical hit mitigation uses the Path of Exile armour formula.
     */
    @GameTest
    public void physicalMitigationUsesPoeArmourFormula(GameTestHelper helper) {
        StatHolder holder = newHolder(helper);
        holder.setBaseValue(CombatStats.ARMOUR, 100.0);

        DamageMitigationResult result = DamageMitigations.mitigateHit(holder, DamageBreakdown.of(DamageType.PHYSICAL, 50.0), List.of(), List.of());

        helper.assertTrue(
                Math.abs(result.mitigatedDamage().amount(DamageType.PHYSICAL) - 35.714285714285715) < 1.0E-9,
                "Physical damage should be reduced using the PoE armour formula"
        );
        helper.assertTrue(
                Math.abs(result.mitigationDelta(DamageType.PHYSICAL) - 14.285714285714285) < 1.0E-9,
                "Mitigation delta should record the armour-prevented physical damage"
        );
        helper.succeed();
    }

    /**
     * Verifies that fixed damage bypasses the mitigation layer even when other mitigation stats are present.
     */
    @GameTest
    public void fixedDamageBypassesMitigationLayer(GameTestHelper helper) {
        StatHolder holder = newHolder(helper);
        holder.setBaseValue(CombatStats.ARMOUR, 1000.0);
        holder.setBaseValue(CombatStats.FIRE_RESISTANCE, 90.0);

        DamageMitigationResult result = DamageMitigations.mitigateHit(
                holder,
                DamageBreakdown.of(DamageType.FIXED, 40.0),
                List.of(new ElementalExposure(DamageType.FIRE, 20.0)),
                List.of(new ResistancePenetration(DamageType.CHAOS, 15.0))
        );

        helper.assertValueEqual(result.mitigatedDamage().amount(DamageType.FIXED), 40.0, "Fixed damage should bypass the mitigation layer");
        helper.assertValueEqual(result.mitigationDelta(DamageType.FIXED), 0.0, "Fixed damage should not record any mitigation delta");
        helper.succeed();
    }

    /**
     * Verifies that mixed typed damage resolves each mitigation branch independently inside one result.
     */
    @GameTest
    public void mixedDamageBreakdownMitigatesEachTypeIndependently(GameTestHelper helper) {
        StatHolder holder = newHolder(helper);
        holder.setBaseValue(CombatStats.ARMOUR, 100.0);
        holder.setBaseValue(CombatStats.FIRE_RESISTANCE, 80.0);
        holder.setBaseValue(CombatStats.MAX_FIRE_RESISTANCE, 75.0);

        DamageBreakdown incoming = DamageBreakdown.empty()
                .with(DamageType.PHYSICAL, 50.0)
                .with(DamageType.FIRE, 100.0)
                .with(DamageType.FIXED, 10.0);

        DamageMitigationResult result = DamageMitigations.mitigateHit(holder, incoming, List.of(), List.of());

        helper.assertTrue(
                Math.abs(result.mitigatedDamage().amount(DamageType.PHYSICAL) - 35.714285714285715) < 1.0E-9,
                "Physical portion should use armour mitigation"
        );
        helper.assertValueEqual(result.mitigatedDamage().amount(DamageType.FIRE), 25.0, "Fire portion should use elemental resistance mitigation");
        helper.assertValueEqual(result.mitigatedDamage().amount(DamageType.FIXED), 10.0, "Fixed portion should bypass mitigation");
        helper.assertTrue(result.incomingDamage().entries().equals(incoming.entries()), "Result should preserve the original incoming damage breakdown");
        helper.succeed();
    }

    /**
     * Verifies that an empty incoming damage breakdown produces an empty mitigation result.
     */
    @GameTest
    public void emptyDamageBreakdownProducesEmptyMitigationResult(GameTestHelper helper) {
        DamageMitigationResult result = DamageMitigations.mitigateHit(newHolder(helper), DamageBreakdown.empty(), List.of(), List.of());

        helper.assertTrue(result.incomingDamage().isEmpty(), "Incoming empty damage should stay empty");
        helper.assertTrue(result.mitigatedDamage().isEmpty(), "Mitigated empty damage should stay empty");
        helper.assertTrue(result.mitigationDelta().isEmpty(), "Mitigation delta should stay empty for empty damage");
        helper.succeed();
    }

    /**
     * Verifies that elemental exposure lowers the target elemental resistance before mitigation is applied.
     */
    @GameTest
    public void elementalExposureLowersHitResistance(GameTestHelper helper) {
        StatHolder holder = newHolder(helper);
        holder.setBaseValue(CombatStats.FIRE_RESISTANCE, 50.0);

        DamageMitigationResult result = DamageMitigations.mitigateHit(
                holder,
                DamageBreakdown.of(DamageType.FIRE, 100.0),
                List.of(new ElementalExposure(DamageType.FIRE, 20.0)),
                List.of()
        );

        helper.assertValueEqual(result.mitigatedDamage().amount(DamageType.FIRE), 70.0, "Exposure should reduce the effective fire resistance before mitigation");
        helper.assertValueEqual(result.mitigationDelta(DamageType.FIRE), 30.0, "Mitigation delta should reflect the lowered effective resistance");
        helper.succeed();
    }

    /**
     * Verifies that elemental exposure also applies to the damage-over-time resistance path.
     */
    @GameTest
    public void elementalExposureAppliesToDamageOverTime(GameTestHelper helper) {
        StatHolder holder = newHolder(helper);
        holder.setBaseValue(CombatStats.FIRE_RESISTANCE, 50.0);

        DamageMitigationResult result = DamageMitigations.mitigateDamageOverTime(
                holder,
                DamageBreakdown.of(DamageType.FIRE, 100.0),
                List.of(new ElementalExposure(DamageType.FIRE, 20.0))
        );

        helper.assertValueEqual(result.mitigatedDamage().amount(DamageType.FIRE), 70.0, "Exposure should reduce fire resistance for damage over time as well");
        helper.assertValueEqual(result.mitigationDelta(DamageType.FIRE), 30.0, "DoT mitigation delta should reflect the exposure-adjusted resistance");
        helper.succeed();
    }

    /**
     * Verifies that hit penetration applies after the resistance cap has been enforced.
     */
    @GameTest
    public void penetrationAppliesAfterResistanceCap(GameTestHelper helper) {
        StatHolder holder = newHolder(helper);
        holder.setBaseValue(CombatStats.FIRE_RESISTANCE, 90.0);
        holder.setBaseValue(CombatStats.MAX_FIRE_RESISTANCE, 75.0);

        DamageMitigationResult result = DamageMitigations.mitigateHit(
                holder,
                DamageBreakdown.of(DamageType.FIRE, 100.0),
                List.of(new ElementalExposure(DamageType.FIRE, 10.0)),
                List.of(new ResistancePenetration(DamageType.FIRE, 20.0))
        );

        helper.assertTrue(
                Math.abs(result.mitigatedDamage().amount(DamageType.FIRE) - 45.0) < 1.0E-9,
                "Penetration should lower the capped fire resistance during hit mitigation"
        );
        helper.assertTrue(
                Math.abs(result.mitigationDelta(DamageType.FIRE) - 55.0) < 1.0E-9,
                "Mitigation delta should reflect the cap-then-penetration order"
        );
        helper.succeed();
    }

    /**
     * Verifies that chaos penetration can push negative resistance further below zero on hit damage.
     */
    @GameTest
    public void chaosPenetrationAmplifiesNegativeResistance(GameTestHelper helper) {
        StatHolder holder = newHolder(helper);
        holder.setBaseValue(CombatStats.CHAOS_RESISTANCE, -20.0);

        DamageMitigationResult result = DamageMitigations.mitigateHit(
                holder,
                DamageBreakdown.of(DamageType.CHAOS, 100.0),
                List.of(),
                List.of(new ResistancePenetration(DamageType.CHAOS, 30.0))
        );

        helper.assertValueEqual(result.mitigatedDamage().amount(DamageType.CHAOS), 150.0, "Chaos penetration should amplify hit damage when chaos resistance is already negative");
        helper.assertValueEqual(result.mitigationDelta(DamageType.CHAOS), -50.0, "Mitigation delta should go further negative after chaos penetration");
        helper.succeed();
    }

    /**
     * Verifies that the elemental exposure codec round-trips a valid elemental entry.
     */
    @GameTest
    public void elementalExposureCodecRoundTrips(GameTestHelper helper) {
        ElementalExposure decoded = ElementalExposure.CODEC.parse(
                        JsonOps.INSTANCE,
                        JsonParser.parseString("""
                                {
                                  "type": "fire",
                                  "value": 15.0
                                }
                                """)
                )
                .getOrThrow(message -> new IllegalStateException("Failed to decode elemental exposure fixture: " + message));

        helper.assertValueEqual(decoded.type(), DamageType.FIRE, "Decoded exposure type should match the fixture");
        helper.assertValueEqual(decoded.value(), 15.0, "Decoded exposure value should match the fixture");
        helper.succeed();
    }

    /**
     * Verifies that the resistance penetration codec round-trips a valid resistable type entry.
     */
    @GameTest
    public void resistancePenetrationCodecRoundTrips(GameTestHelper helper) {
        ResistancePenetration decoded = ResistancePenetration.CODEC.parse(
                        JsonOps.INSTANCE,
                        JsonParser.parseString("""
                                {
                                  "type": "chaos",
                                  "value": 12.0
                                }
                                """)
                )
                .getOrThrow(message -> new IllegalStateException("Failed to decode resistance penetration fixture: " + message));

        helper.assertValueEqual(decoded.type(), DamageType.CHAOS, "Decoded penetration type should match the fixture");
        helper.assertValueEqual(decoded.value(), 12.0, "Decoded penetration value should match the fixture");
        helper.succeed();
    }

    /**
     * Verifies that elemental exposure rejects non-elemental damage types.
     */
    @GameTest
    public void elementalExposureRejectsNonElementalType(GameTestHelper helper) {
        try {
            new ElementalExposure(DamageType.CHAOS, 10.0);
            throw helper.assertionException("Non-elemental exposure should be rejected");
        } catch (IllegalArgumentException expected) {
            helper.succeed();
        }
    }

    /**
     * Verifies that resistance penetration rejects non-resistance damage types.
     */
    @GameTest
    public void resistancePenetrationRejectsNonResistableType(GameTestHelper helper) {
        try {
            new ResistancePenetration(DamageType.PHYSICAL, 10.0);
            throw helper.assertionException("Physical penetration should be rejected");
        } catch (IllegalArgumentException expected) {
            helper.succeed();
        }
    }

    private static StatHolder newHolder(GameTestHelper helper) {
        return StatHolders.create(StatRegistryAccess.statRegistry(helper));
    }
}
