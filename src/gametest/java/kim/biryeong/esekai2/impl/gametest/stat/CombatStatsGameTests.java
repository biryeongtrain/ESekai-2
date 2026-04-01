package kim.biryeong.esekai2.impl.gametest.stat;

import kim.biryeong.esekai2.api.stat.combat.CombatStats;
import kim.biryeong.esekai2.api.stat.definition.StatDefinition;
import kim.biryeong.esekai2.api.stat.holder.StatHolder;
import kim.biryeong.esekai2.api.stat.holder.StatHolders;
import kim.biryeong.esekai2.api.stat.value.StatInstance;
import kim.biryeong.esekai2.impl.stat.registry.StatRegistryAccess;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.Optional;

public final class CombatStatsGameTests {
    /**
     * Verifies that resource and defense combat stats load with the expected built-in default definitions.
     */
    @GameTest
    public void resourceAndDefenseCombatStatsLoadWithExpectedDefaults(GameTestHelper helper) {
        StatHolder holder = newHolder(helper);

        assertDefinition(helper, holder.stat(CombatStats.LIFE), 20.0, Optional.of(1.0), Optional.empty(), "Life");
        assertDefinition(helper, holder.stat(CombatStats.MANA), 0.0, Optional.of(0.0), Optional.empty(), "Mana");
        assertDefinition(helper, holder.stat(CombatStats.MANA_REGENERATION_PER_SECOND), 0.0, Optional.of(0.0), Optional.empty(), "Mana regeneration per second");
        assertDefinition(helper, holder.stat(CombatStats.ENERGY_SHIELD), 0.0, Optional.of(0.0), Optional.empty(), "Energy shield");
        assertDefinition(helper, holder.stat(CombatStats.ARMOUR), 0.0, Optional.of(0.0), Optional.empty(), "Armour");
        assertDefinition(helper, holder.stat(CombatStats.EVADE), 0.0, Optional.of(0.0), Optional.empty(), "Evade");
        assertDefinition(helper, holder.stat(CombatStats.ACCURACY), 0.0, Optional.of(0.0), Optional.empty(), "Accuracy");
        helper.succeed();
    }

    /**
     * Verifies that elemental and chaos resistance stats use ninety as their current definition maximum.
     */
    @GameTest
    public void resistanceCombatStatsUseNinetyAsDefinitionMaximum(GameTestHelper helper) {
        StatHolder holder = newHolder(helper);

        assertDefinition(helper, holder.stat(CombatStats.FIRE_RESISTANCE), 0.0, Optional.empty(), Optional.of(90.0), "Fire resistance");
        assertDefinition(helper, holder.stat(CombatStats.COLD_RESISTANCE), 0.0, Optional.empty(), Optional.of(90.0), "Cold resistance");
        assertDefinition(helper, holder.stat(CombatStats.LIGHTNING_RESISTANCE), 0.0, Optional.empty(), Optional.of(90.0), "Lightning resistance");
        assertDefinition(helper, holder.stat(CombatStats.CHAOS_RESISTANCE), 0.0, Optional.empty(), Optional.of(90.0), "Chaos resistance");
        helper.succeed();
    }

    /**
     * Verifies that maximum resistance stats default to seventy-five and retain a zero lower bound.
     */
    @GameTest
    public void maximumResistanceCombatStatsDefaultToSeventyFive(GameTestHelper helper) {
        StatHolder holder = newHolder(helper);

        assertDefinition(helper, holder.stat(CombatStats.MAX_FIRE_RESISTANCE), 75.0, Optional.of(0.0), Optional.empty(), "Max fire resistance");
        assertDefinition(helper, holder.stat(CombatStats.MAX_COLD_RESISTANCE), 75.0, Optional.of(0.0), Optional.empty(), "Max cold resistance");
        assertDefinition(helper, holder.stat(CombatStats.MAX_LIGHTNING_RESISTANCE), 75.0, Optional.of(0.0), Optional.empty(), "Max lightning resistance");
        assertDefinition(helper, holder.stat(CombatStats.MAX_CHAOS_RESISTANCE), 75.0, Optional.of(0.0), Optional.empty(), "Max chaos resistance");
        helper.succeed();
    }

    /**
     * Verifies that combat stats materialize through a stat holder with their expected default values.
     */
    @GameTest
    public void combatStatsMaterializeThroughStatHolder(GameTestHelper helper) {
        StatHolder holder = newHolder(helper);

        helper.assertTrue(holder.snapshot().isEmpty(), "Combat stat holder should start empty before any stat is materialized");
        helper.assertValueEqual(holder.resolvedValue(CombatStats.EVADE), 0.0, "Evade should materialize with its default value");
        helper.assertValueEqual(holder.resolvedValue(CombatStats.FIRE_RESISTANCE), 0.0, "Fire resistance should materialize with its default value");
        helper.assertValueEqual(holder.resolvedValue(CombatStats.MAX_FIRE_RESISTANCE), 75.0, "Max fire resistance should materialize with its default value");
        helper.assertValueEqual(holder.snapshot().size(), 3, "Holder snapshot should contain the materialized combat stats");
        helper.succeed();
    }

    /**
     * Verifies that attack and spell critical strike stats load with zero defaults and no explicit bounds.
     */
    @GameTest
    public void criticalStrikeCombatStatsLoadWithZeroDefaults(GameTestHelper helper) {
        StatHolder holder = newHolder(helper);

        assertDefinition(helper, holder.stat(CombatStats.ATTACK_CRITICAL_STRIKE_CHANCE_INCREASED), 0.0, Optional.empty(), Optional.empty(), "Attack critical strike chance increased");
        assertDefinition(helper, holder.stat(CombatStats.SPELL_CRITICAL_STRIKE_CHANCE_INCREASED), 0.0, Optional.empty(), Optional.empty(), "Spell critical strike chance increased");
        assertDefinition(helper, holder.stat(CombatStats.ATTACK_CRITICAL_STRIKE_MULTIPLIER_BONUS), 0.0, Optional.empty(), Optional.empty(), "Attack critical strike multiplier bonus");
        assertDefinition(helper, holder.stat(CombatStats.SPELL_CRITICAL_STRIKE_MULTIPLIER_BONUS), 0.0, Optional.empty(), Optional.empty(), "Spell critical strike multiplier bonus");
        helper.succeed();
    }

    private static StatHolder newHolder(GameTestHelper helper) {
        return StatHolders.create(StatRegistryAccess.statRegistry(helper));
    }

    private static void assertDefinition(
            GameTestHelper helper,
            StatInstance instance,
            double expectedDefaultValue,
            Optional<Double> expectedMinValue,
            Optional<Double> expectedMaxValue,
            String label
    ) {
        StatDefinition definition = instance.definition();

        helper.assertValueEqual(definition.defaultValue(), expectedDefaultValue, label + " default value should match the built-in combat stat definition");
        helper.assertTrue(definition.minValue().equals(expectedMinValue), label + " min value should match the built-in combat stat definition");
        helper.assertTrue(definition.maxValue().equals(expectedMaxValue), label + " max value should match the built-in combat stat definition");
    }
}
