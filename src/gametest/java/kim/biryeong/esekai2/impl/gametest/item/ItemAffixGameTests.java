package kim.biryeong.esekai2.impl.gametest.item;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import kim.biryeong.esekai2.api.item.affix.AffixDefinition;
import kim.biryeong.esekai2.api.item.affix.AffixKind;
import kim.biryeong.esekai2.api.item.affix.AffixRegistries;
import kim.biryeong.esekai2.api.item.affix.AffixScope;
import kim.biryeong.esekai2.api.item.affix.Affixes;
import kim.biryeong.esekai2.api.item.affix.ItemAffixState;
import kim.biryeong.esekai2.api.item.affix.ItemAffixes;
import kim.biryeong.esekai2.api.item.affix.RolledAffix;
import kim.biryeong.esekai2.api.item.type.ItemFamily;
import kim.biryeong.esekai2.api.stat.definition.StatRegistries;
import kim.biryeong.esekai2.api.stat.modifier.StatModifier;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public final class ItemAffixGameTests {
    private static final Identifier TRINKET_LIFE_T1_AFFIX_ID = Identifier.fromNamespaceAndPath("esekai2", "trinket_life_t1");
    private static final Identifier TRINKET_LIFE_T2_AFFIX_ID = Identifier.fromNamespaceAndPath("esekai2", "trinket_life_t2");
    private static final Identifier WEAPON_ACCURACY_T1_AFFIX_ID = Identifier.fromNamespaceAndPath("esekai2", "weapon_accuracy_t1");
    private static final Identifier TRINKET_LIFE_GROUP_ID = Identifier.fromNamespaceAndPath("esekai2", "trinket_life");

    /**
     * Verifies that the affix dynamic registry loads the sample classification fixtures used by the item layer.
     */
    @GameTest
    public void affixRegistryLoadsSampleFixtures(GameTestHelper helper) {
        Registry<AffixDefinition> registry = affixRegistry(helper);

        helper.assertTrue(registry.containsKey(TRINKET_LIFE_T1_AFFIX_ID), "Trinket life tier one affix should be present in the affix registry");
        helper.assertTrue(registry.containsKey(TRINKET_LIFE_T2_AFFIX_ID), "Trinket life tier two affix should be present in the affix registry");
        helper.assertTrue(registry.containsKey(WEAPON_ACCURACY_T1_AFFIX_ID), "Weapon accuracy tier one affix should be present in the affix registry");
        helper.succeed();
    }

    /**
     * Verifies that an affix definition round-trips its classification metadata through the public codec.
     */
    @GameTest
    public void affixDefinitionCodecRoundTrips(GameTestHelper helper) {
        AffixDefinition definition = affixRegistry(helper).getOptional(TRINKET_LIFE_T2_AFFIX_ID)
                .orElseThrow(() -> helper.assertionException("Trinket life tier two affix should decode successfully"));

        var encoded = AffixDefinition.CODEC.encodeStart(JsonOps.INSTANCE, definition)
                .getOrThrow(message -> new IllegalStateException("Failed to encode affix definition: " + message));
        AffixDefinition decoded = AffixDefinition.CODEC.parse(JsonOps.INSTANCE, encoded)
                .getOrThrow(message -> new IllegalStateException("Failed to re-decode affix definition: " + message));

        helper.assertValueEqual(decoded.translationKey(), "affix.esekai2.trinket_life_t2", "Affix translation key should survive a codec round trip");
        helper.assertValueEqual(decoded.kind(), AffixKind.PREFIX, "Affix kind should survive a codec round trip");
        helper.assertValueEqual(decoded.groupId(), TRINKET_LIFE_GROUP_ID, "Affix group id should survive a codec round trip");
        helper.assertValueEqual(decoded.tier(), 2, "Affix tier should survive a codec round trip");
        helper.assertValueEqual(decoded.minimumItemLevel(), 10, "Affix minimum item level should survive a codec round trip");
        helper.assertValueEqual(decoded.scope(), AffixScope.GLOBAL, "Affix scope should survive a codec round trip");
        helper.assertTrue(decoded.itemFamilies().contains(ItemFamily.TRINKET), "Affix families should survive a codec round trip");
        helper.assertValueEqual(decoded.modifierRanges().size(), 1, "Affix modifier ranges should survive a codec round trip");
        helper.succeed();
    }

    /**
     * Verifies that affix definitions reject an empty item family list during codec validation.
     */
    @GameTest
    public void affixDefinitionRejectsEmptyItemFamilies(GameTestHelper helper) {
        try {
            AffixDefinition.CODEC.parse(
                    JsonOps.INSTANCE,
                    JsonParser.parseString("""
                            {
                              "translation_key": "affix.esekai2.invalid_empty_families",
                              "kind": "prefix",
                              "group_id": "esekai2:invalid_group",
                              "tier": 1,
                              "minimum_item_level": 1,
                              "scope": "global",
                              "item_families": [],
                              "modifier_ranges": [
                                {
                                  "stat": "esekai2:life",
                                  "operation": "add",
                                  "min_value": 1.0,
                                  "max_value": 2.0
                                }
                              ]
                            }
                            """)
            ).getOrThrow(message -> new IllegalStateException("Expected empty item family validation to fail: " + message));
            throw helper.assertionException("Affix definition should reject an empty item family list");
        } catch (IllegalStateException expected) {
            helper.succeed();
        }
    }

    /**
     * Verifies that a modifier range with an inverted minimum and maximum is rejected.
     */
    @GameTest
    public void affixDefinitionRejectsInvertedModifierRange(GameTestHelper helper) {
        try {
            AffixDefinition.CODEC.parse(
                    JsonOps.INSTANCE,
                    JsonParser.parseString("""
                            {
                              "translation_key": "affix.esekai2.invalid_range",
                              "kind": "prefix",
                              "group_id": "esekai2:invalid_group",
                              "tier": 1,
                              "minimum_item_level": 1,
                              "scope": "global",
                              "item_families": ["trinket"],
                              "modifier_ranges": [
                                {
                                  "stat": "esekai2:life",
                                  "operation": "add",
                                  "min_value": 10.0,
                                  "max_value": 1.0
                                }
                              ]
                            }
                            """)
            ).getOrThrow(message -> new IllegalStateException("Expected inverted modifier range validation to fail: " + message));
            throw helper.assertionException("Affix definition should reject inverted modifier ranges");
        } catch (IllegalStateException expected) {
            helper.succeed();
        }
    }

    /**
     * Verifies that affix definitions reject non-positive tier values during codec validation.
     */
    @GameTest
    public void affixDefinitionRejectsNonPositiveTier(GameTestHelper helper) {
        try {
            AffixDefinition.CODEC.parse(
                    JsonOps.INSTANCE,
                    JsonParser.parseString("""
                            {
                              "translation_key": "affix.esekai2.invalid_tier",
                              "kind": "prefix",
                              "group_id": "esekai2:invalid_group",
                              "tier": 0,
                              "minimum_item_level": 1,
                              "scope": "global",
                              "item_families": ["trinket"],
                              "modifier_ranges": [
                                {
                                  "stat": "esekai2:life",
                                  "operation": "add",
                                  "min_value": 1.0,
                                  "max_value": 2.0
                                }
                              ]
                            }
                            """)
            ).getOrThrow(message -> new IllegalStateException("Expected non-positive tier validation to fail: " + message));
            throw helper.assertionException("Affix definition should reject non-positive tiers");
        } catch (IllegalStateException expected) {
            helper.succeed();
        }
    }

    /**
     * Verifies that affix definitions reject item levels below one during codec validation.
     */
    @GameTest
    public void affixDefinitionRejectsInvalidMinimumItemLevel(GameTestHelper helper) {
        try {
            AffixDefinition.CODEC.parse(
                    JsonOps.INSTANCE,
                    JsonParser.parseString("""
                            {
                              "translation_key": "affix.esekai2.invalid_item_level",
                              "kind": "prefix",
                              "group_id": "esekai2:invalid_group",
                              "tier": 1,
                              "minimum_item_level": 0,
                              "scope": "global",
                              "item_families": ["trinket"],
                              "modifier_ranges": [
                                {
                                  "stat": "esekai2:life",
                                  "operation": "add",
                                  "min_value": 1.0,
                                  "max_value": 2.0
                                }
                              ]
                            }
                            """)
            ).getOrThrow(message -> new IllegalStateException("Expected invalid minimum item level validation to fail: " + message));
            throw helper.assertionException("Affix definition should reject minimum item levels below one");
        } catch (IllegalStateException expected) {
            helper.succeed();
        }
    }

    /**
     * Verifies that tiered affixes can share a group id while still exposing distinct tier numbers.
     */
    @GameTest
    public void tieredAffixesShareGroupAndExposeDistinctTiers(GameTestHelper helper) {
        AffixDefinition tierOne = affixRegistry(helper).getOptional(TRINKET_LIFE_T1_AFFIX_ID)
                .orElseThrow(() -> helper.assertionException("Trinket life tier one affix should decode successfully"));
        AffixDefinition tierTwo = affixRegistry(helper).getOptional(TRINKET_LIFE_T2_AFFIX_ID)
                .orElseThrow(() -> helper.assertionException("Trinket life tier two affix should decode successfully"));

        helper.assertTrue(tierOne.belongsToGroup(TRINKET_LIFE_GROUP_ID), "Tier one affix should report its shared group id");
        helper.assertTrue(tierTwo.belongsToGroup(TRINKET_LIFE_GROUP_ID), "Tier two affix should report its shared group id");
        helper.assertValueEqual(tierOne.tier(), 1, "Tier one affix should expose the stronger tier number");
        helper.assertValueEqual(tierTwo.tier(), 2, "Tier two affix should expose the weaker tier number");
        helper.succeed();
    }

    /**
     * Verifies that affix availability helper follows the configured minimum item level.
     */
    @GameTest
    public void affixAvailabilityHelperUsesMinimumItemLevel(GameTestHelper helper) {
        AffixDefinition definition = affixRegistry(helper).getOptional(TRINKET_LIFE_T2_AFFIX_ID)
                .orElseThrow(() -> helper.assertionException("Trinket life tier two affix should decode successfully"));

        helper.assertTrue(!definition.isAvailableAtItemLevel(9), "Item levels below the configured minimum should be unavailable");
        helper.assertTrue(definition.isAvailableAtItemLevel(10), "The configured minimum item level should be available");
        helper.assertTrue(definition.isAvailableAtItemLevel(20), "Higher item levels should remain available");
        helper.succeed();
    }

    /**
     * Verifies that sample affixes expose both prefix/global and suffix/local classifications.
     */
    @GameTest
    public void affixSamplesExposeClassificationMetadata(GameTestHelper helper) {
        AffixDefinition trinket = affixRegistry(helper).getOptional(TRINKET_LIFE_T2_AFFIX_ID)
                .orElseThrow(() -> helper.assertionException("Trinket life tier two affix should decode successfully"));
        AffixDefinition weapon = affixRegistry(helper).getOptional(WEAPON_ACCURACY_T1_AFFIX_ID)
                .orElseThrow(() -> helper.assertionException("Weapon accuracy tier one affix should decode successfully"));

        helper.assertValueEqual(trinket.kind(), AffixKind.PREFIX, "Trinket life affix should decode as a prefix");
        helper.assertValueEqual(trinket.scope(), AffixScope.GLOBAL, "Trinket life affix should decode as a global affix");
        helper.assertValueEqual(weapon.kind(), AffixKind.SUFFIX, "Weapon accuracy affix should decode as a suffix");
        helper.assertValueEqual(weapon.scope(), AffixScope.LOCAL, "Weapon accuracy affix should decode as a local affix");
        helper.succeed();
    }

    /**
     * Verifies that rolling an affix materializes a stable modifier snapshot within the requested range.
     */
    @GameTest
    public void affixRollsModifierSnapshotWithinRange(GameTestHelper helper) {
        AffixDefinition definition = affixRegistry(helper).getOptional(TRINKET_LIFE_T2_AFFIX_ID)
                .orElseThrow(() -> helper.assertionException("Trinket life tier two affix should decode successfully"));

        RolledAffix rolled = Affixes.roll(TRINKET_LIFE_T2_AFFIX_ID, ItemFamily.TRINKET, 10, definition, 0.5);

        helper.assertValueEqual(rolled.affixId(), TRINKET_LIFE_T2_AFFIX_ID, "Rolled affix should preserve the source affix id");
        helper.assertValueEqual(rolled.itemFamily(), ItemFamily.TRINKET, "Rolled affix should preserve the requested item family");
        helper.assertValueEqual(rolled.modifiers().size(), 1, "Rolled affix should snapshot one modifier");

        StatModifier modifier = rolled.modifiers().get(0);
        helper.assertValueEqual(
                modifier.stat(),
                ResourceKey.create(StatRegistries.STAT, Identifier.fromNamespaceAndPath("esekai2", "life")),
                "Rolled modifier should target the life stat"
        );
        helper.assertValueEqual(modifier.value(), 7.5, "Midpoint roll should resolve the expected snapshot value");
        helper.assertValueEqual(modifier.sourceId(), TRINKET_LIFE_T2_AFFIX_ID, "Rolled modifier should use the affix id as its source id");
        helper.succeed();
    }

    /**
     * Verifies that different roll inputs produce different resolved modifier snapshots from the same affix definition.
     */
    @GameTest
    public void affixRollsDifferentSnapshotsForDifferentRolls(GameTestHelper helper) {
        AffixDefinition definition = affixRegistry(helper).getOptional(TRINKET_LIFE_T2_AFFIX_ID)
                .orElseThrow(() -> helper.assertionException("Trinket life tier two affix should decode successfully"));

        RolledAffix minimumRoll = Affixes.roll(TRINKET_LIFE_T2_AFFIX_ID, ItemFamily.TRINKET, 10, definition, 0.0);
        RolledAffix maximumRoll = Affixes.roll(TRINKET_LIFE_T2_AFFIX_ID, ItemFamily.TRINKET, 10, definition, 1.0);

        helper.assertValueEqual(minimumRoll.modifiers().get(0).value(), 5.0, "Minimum roll should materialize the modifier minimum");
        helper.assertValueEqual(maximumRoll.modifiers().get(0).value(), 10.0, "Maximum roll should materialize the modifier maximum");
        helper.succeed();
    }

    /**
     * Verifies that persisted item affix state survives the public codec without losing rolled affix snapshots.
     */
    @GameTest
    public void itemAffixStateCodecRoundTrips(GameTestHelper helper) {
        AffixDefinition definition = affixRegistry(helper).getOptional(TRINKET_LIFE_T1_AFFIX_ID)
                .orElseThrow(() -> helper.assertionException("Trinket life tier one affix should decode successfully"));
        ItemAffixState state = new ItemAffixState(List.of(Affixes.roll(
                TRINKET_LIFE_T1_AFFIX_ID,
                ItemFamily.TRINKET,
                20,
                definition,
                0.25
        )));

        var encoded = ItemAffixState.CODEC.encodeStart(JsonOps.INSTANCE, state)
                .getOrThrow(message -> new IllegalStateException("Failed to encode item affix state: " + message));
        ItemAffixState decoded = ItemAffixState.CODEC.parse(JsonOps.INSTANCE, encoded)
                .getOrThrow(message -> new IllegalStateException("Failed to decode item affix state: " + message));

        helper.assertValueEqual(decoded, state, "Item affix state should survive a codec round trip");
        helper.succeed();
    }

    /**
     * Verifies that the public item affix facade stores and clears persisted rolled affix state on item stacks.
     */
    @GameTest
    public void itemAffixFacadeStoresAndClearsState(GameTestHelper helper) {
        AffixDefinition definition = affixRegistry(helper).getOptional(TRINKET_LIFE_T1_AFFIX_ID)
                .orElseThrow(() -> helper.assertionException("Trinket life tier one affix should decode successfully"));
        ItemAffixState state = new ItemAffixState(List.of(Affixes.roll(
                TRINKET_LIFE_T1_AFFIX_ID,
                ItemFamily.TRINKET,
                20,
                definition,
                1.0
        )));
        ItemStack stack = new ItemStack(Items.STICK);

        ItemAffixes.set(stack, state);
        helper.assertValueEqual(ItemAffixes.get(stack), state, "Stored item affix state should be readable through the public facade");
        helper.assertValueEqual(ItemAffixes.getRolledAffixes(stack), state.rolledAffixes(),
                "Stored rolled affixes should be exposed through the convenience accessor");

        ItemAffixes.clear(stack);
        helper.assertValueEqual(ItemAffixes.get(stack), ItemAffixState.EMPTY, "Clearing item affix state should restore the stable empty value");
        helper.succeed();
    }

    /**
     * Verifies that an affix rejects item families outside its declared family set.
     */
    @GameTest
    public void affixRejectsUnsupportedItemFamily(GameTestHelper helper) {
        AffixDefinition definition = affixRegistry(helper).getOptional(TRINKET_LIFE_T2_AFFIX_ID)
                .orElseThrow(() -> helper.assertionException("Trinket life tier two affix should decode successfully"));

        try {
            Affixes.roll(TRINKET_LIFE_T2_AFFIX_ID, ItemFamily.WEAPON, 10, definition, 0.5);
            throw helper.assertionException("Trinket-only affix should reject weapon item families");
        } catch (IllegalArgumentException expected) {
            helper.succeed();
        }
    }

    /**
     * Verifies that affixes reject item levels below their configured minimum before rolling.
     */
    @GameTest
    public void affixRejectsInsufficientItemLevel(GameTestHelper helper) {
        AffixDefinition definition = affixRegistry(helper).getOptional(TRINKET_LIFE_T1_AFFIX_ID)
                .orElseThrow(() -> helper.assertionException("Trinket life tier one affix should decode successfully"));

        try {
            Affixes.roll(TRINKET_LIFE_T1_AFFIX_ID, ItemFamily.TRINKET, 19, definition, 0.5);
            throw helper.assertionException("Tier one trinket life affix should reject insufficient item level");
        } catch (IllegalArgumentException expected) {
            helper.succeed();
        }
    }

    private static Registry<AffixDefinition> affixRegistry(GameTestHelper helper) {
        return helper.getLevel().getServer().registryAccess().lookupOrThrow(AffixRegistries.AFFIX);
    }
}
