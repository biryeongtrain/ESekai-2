package kim.biryeong.esekai2.impl.gametest.stat;

import kim.biryeong.esekai2.api.stat.definition.StatDefinition;
import kim.biryeong.esekai2.api.stat.definition.StatRegistries;
import kim.biryeong.esekai2.api.stat.holder.StatHolder;
import kim.biryeong.esekai2.api.stat.holder.StatHolders;
import kim.biryeong.esekai2.api.stat.modifier.StatModifier;
import kim.biryeong.esekai2.api.stat.modifier.StatModifierOperation;
import kim.biryeong.esekai2.impl.stat.registry.StatRegistryAccess;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public final class StatHolderGameTests {
    private static final Identifier TEST_STAT_ID = Identifier.fromNamespaceAndPath("esekai-gametest", "test_stat");
    private static final Identifier SECONDARY_STAT_ID = Identifier.fromNamespaceAndPath("esekai-gametest", "secondary_stat");

    /**
     * Verifies that a stat holder materializes missing stat instances lazily from the registry defaults.
     */
    @GameTest
    public void statHolderLazilyCreatesMaterializedInstances(GameTestHelper helper) {
        StatHolder holder = newHolder(helper);

        helper.assertTrue(holder.snapshot().isEmpty(), "Stat holder should start empty before any stat is resolved");
        helper.assertValueEqual(holder.resolvedValue(testStatKey()), 10.0, "Stat holder should lazily create the first stat with its definition default value");
        helper.assertTrue(holder.snapshot().containsKey(testStatKey()), "Stat holder snapshot should contain the lazily created test stat");
        helper.succeed();
    }

    /**
     * Verifies that multiple materialized stats remain independent when one receives a modifier.
     */
    @GameTest
    public void statHolderKeepsMultipleStatsIndependent(GameTestHelper helper) {
        StatHolder holder = newHolder(helper);
        StatModifier modifier = new StatModifier(
                testStatKey(),
                StatModifierOperation.ADD,
                5.0,
                Identifier.fromNamespaceAndPath("esekai-gametest", "holder_independence")
        );

        holder.addModifier(modifier);

        helper.assertValueEqual(holder.resolvedValue(testStatKey()), 15.0, "Primary stat should include the appended modifier");
        helper.assertValueEqual(holder.resolvedValue(secondaryStatKey()), 3.0, "Secondary stat should stay at its own default value");
        helper.assertValueEqual(holder.snapshot().size(), 2, "Stat holder snapshot should contain both materialized stats");
        helper.succeed();
    }

    /**
     * Verifies that removing the exact modifier instance restores the affected stat to its previous value.
     */
    @GameTest
    public void statHolderRemovesExactModifier(GameTestHelper helper) {
        StatHolder holder = newHolder(helper);
        StatModifier modifier = new StatModifier(
                testStatKey(),
                StatModifierOperation.INCREASED,
                50.0,
                Identifier.fromNamespaceAndPath("esekai-gametest", "holder_remove")
        );

        holder.addModifier(modifier);
        helper.assertValueEqual(holder.resolvedValue(testStatKey()), 15.0, "Stat holder should apply the modifier before removal");
        helper.assertTrue(holder.removeModifier(modifier), "Stat holder should remove the exact modifier instance");
        helper.assertValueEqual(holder.resolvedValue(testStatKey()), 10.0, "Stat holder should restore the original value after removal");
        helper.succeed();
    }

    /**
     * Verifies that replacing a stat base value updates both the resolved value and the materialized instance.
     */
    @GameTest
    public void statHolderUpdatesBaseValue(GameTestHelper helper) {
        StatHolder holder = newHolder(helper);

        holder.setBaseValue(secondaryStatKey(), 12.0);

        helper.assertValueEqual(holder.resolvedValue(secondaryStatKey()), 12.0, "Stat holder should use the replaced base value");
        helper.assertValueEqual(holder.stat(secondaryStatKey()).baseValue(), 12.0, "Materialized stat instance should store the updated base value");
        helper.succeed();
    }

    private static StatHolder newHolder(GameTestHelper helper) {
        return StatHolders.create(StatRegistryAccess.statRegistry(helper));
    }

    private static ResourceKey<StatDefinition> testStatKey() {
        return ResourceKey.create(StatRegistries.STAT, TEST_STAT_ID);
    }

    private static ResourceKey<StatDefinition> secondaryStatKey() {
        return ResourceKey.create(StatRegistries.STAT, SECONDARY_STAT_ID);
    }
}
