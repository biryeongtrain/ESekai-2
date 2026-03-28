package kim.biryeong.esekai2.impl.gametest.core;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import kim.biryeong.esekai2.Esekai2;
import kim.biryeong.esekai2.api.stat.definition.StatDefinition;
import kim.biryeong.esekai2.api.stat.definition.StatRegistries;
import kim.biryeong.esekai2.api.stat.modifier.StatModifier;
import kim.biryeong.esekai2.api.stat.modifier.StatModifierOperation;
import kim.biryeong.esekai2.api.stat.value.StatInstance;
import kim.biryeong.esekai2.impl.stat.registry.StatRegistryAccess;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.io.IOException;

public final class SmokeGameTests {
    private static final Identifier TEST_STAT_ID = Identifier.fromNamespaceAndPath("esekai-gametest", "test_stat");
    private static final Identifier TEST_MODIFIER_RESOURCE_ID = Identifier.fromNamespaceAndPath("esekai-gametest", "gametest/stat_modifier/test_modifier.json");

    /**
     * Verifies that the ESekai mod and its primary logger initialize inside the GameTest environment.
     */
    @GameTest
    public void modLoads(GameTestHelper helper) {
        helper.assertTrue(FabricLoader.getInstance().isModLoaded(Esekai2.MOD_ID), "ESekai main mod should be loaded during GameTest");
        helper.assertTrue(Esekai2.LOGGER != null, "ESekai logger should be initialized during GameTest");
        helper.succeed();
    }

    /**
     * Verifies that a test stat definition is loaded into the dynamic registry with the expected datapack values.
     */
    @GameTest
    public void statDefinitionLoads(GameTestHelper helper) {
        var registry = StatRegistryAccess.statRegistry(helper);
        helper.assertTrue(registry.containsKey(TEST_STAT_ID), "Test stat definition should be present in the dynamic registry");

        StatDefinition definition = registry.getOptional(TEST_STAT_ID)
                .orElseThrow(() -> helper.assertionException("Test stat definition should decode successfully"));

        helper.assertValueEqual(definition.translationKey(), "stat.esekai_gametest.test_stat", "Stat translation key should match the datapack definition");
        helper.assertValueEqual(definition.defaultValue(), 10.0, "Stat default value should match the datapack definition");
        helper.assertTrue(definition.minValue().isPresent(), "Stat min value should be present");
        helper.assertTrue(definition.maxValue().isPresent(), "Stat max value should be present");
        helper.assertValueEqual(definition.minValue().orElseThrow(), 0.0, "Stat min value should match the datapack definition");
        helper.assertValueEqual(definition.maxValue().orElseThrow(), 100.0, "Stat max value should match the datapack definition");
        helper.succeed();
    }

    /**
     * Verifies that a stat modifier fixture decodes through the public stat modifier codec.
     */
    @GameTest
    public void statModifierDecodes(GameTestHelper helper) {
        StatModifier modifier;

        try (var reader = helper.getLevel().getServer().getResourceManager().openAsReader(TEST_MODIFIER_RESOURCE_ID)) {
            modifier = StatModifier.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader))
                    .getOrThrow(message -> new IllegalStateException("Failed to decode stat modifier fixture: " + message));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load stat modifier fixture", exception);
        }

        helper.assertValueEqual(
                modifier.stat(),
                ResourceKey.create(StatRegistries.STAT, TEST_STAT_ID),
                "Stat modifier should target the test stat"
        );
        helper.assertValueEqual(modifier.operation(), StatModifierOperation.INCREASED, "Stat modifier operation should match the fixture");
        helper.assertValueEqual(modifier.value(), 15.0, "Stat modifier value should match the fixture");
        helper.assertValueEqual(
                modifier.sourceId(),
                Identifier.fromNamespaceAndPath("esekai-gametest", "test_fixture"),
                "Stat modifier source id should match the fixture"
        );
        helper.succeed();
    }

    /**
     * Verifies that a stat instance uses its definition default value when no modifiers are present.
     */
    @GameTest
    public void statInstanceUsesDefinitionDefaultValue(GameTestHelper helper) {
        StatInstance instance = StatInstance.fromDefinition(testStatKey(), testStatDefinition(helper));

        helper.assertValueEqual(instance.baseValue(), 10.0, "Stat instance base value should default to the stat definition");
        helper.assertValueEqual(instance.resolvedValue(), 10.0, "Stat instance resolved value should match the default base value without modifiers");
        helper.succeed();
    }

    /**
     * Verifies that stat instance calculations apply ADD, INCREASED, and MORE in the expected order.
     */
    @GameTest
    public void statInstanceAppliesModifierBucketsInOrder(GameTestHelper helper) {
        StatInstance instance = StatInstance.fromDefinition(testStatKey(), testStatDefinition(helper))
                .withModifier(new StatModifier(testStatKey(), StatModifierOperation.ADD, 5.0, Identifier.fromNamespaceAndPath("esekai-gametest", "flat_bonus")))
                .withModifier(new StatModifier(testStatKey(), StatModifierOperation.INCREASED, 50.0, Identifier.fromNamespaceAndPath("esekai-gametest", "increased_bonus")))
                .withModifier(new StatModifier(testStatKey(), StatModifierOperation.MORE, 20.0, Identifier.fromNamespaceAndPath("esekai-gametest", "more_bonus")));

        helper.assertTrue(
                Math.abs(instance.unclampedValue() - 27.0) < 1.0E-9,
                "Stat instance should apply ADD, then INCREASED, then MORE buckets"
        );
        helper.assertTrue(
                Math.abs(instance.resolvedValue() - 27.0) < 1.0E-9,
                "Stat instance resolved value should match the unclamped value when it is inside the stat bounds"
        );
        helper.succeed();
    }

    /**
     * Verifies that stat instance resolution preserves the unclamped value and then clamps to definition bounds.
     */
    @GameTest
    public void statInstanceClampsToDefinitionBounds(GameTestHelper helper) {
        StatInstance instance = StatInstance.fromDefinition(testStatKey(), testStatDefinition(helper))
                .withBaseValue(90.0)
                .withModifier(new StatModifier(testStatKey(), StatModifierOperation.ADD, 20.0, Identifier.fromNamespaceAndPath("esekai-gametest", "overflow_bonus")));

        helper.assertTrue(
                Math.abs(instance.unclampedValue() - 110.0) < 1.0E-9,
                "Stat instance unclamped value should preserve the pre-clamp result"
        );
        helper.assertValueEqual(instance.resolvedValue(), 100.0, "Stat instance resolved value should clamp to the definition max value");
        helper.succeed();
    }

    private static ResourceKey<StatDefinition> testStatKey() {
        return ResourceKey.create(StatRegistries.STAT, TEST_STAT_ID);
    }

    private static StatDefinition testStatDefinition(GameTestHelper helper) {
        return StatRegistryAccess.statRegistry(helper).getOptional(TEST_STAT_ID)
                .orElseThrow(() -> helper.assertionException("Test stat definition should decode successfully"));
    }
}
