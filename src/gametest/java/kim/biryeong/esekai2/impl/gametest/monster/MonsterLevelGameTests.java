package kim.biryeong.esekai2.impl.gametest.monster;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import kim.biryeong.esekai2.api.monster.level.MonsterLevelContext;
import kim.biryeong.esekai2.api.monster.level.MonsterLevelDefinition;
import kim.biryeong.esekai2.api.monster.level.MonsterLevelEntry;
import kim.biryeong.esekai2.api.monster.level.MonsterLevelProfile;
import kim.biryeong.esekai2.api.monster.level.MonsterLevelRegistries;
import kim.biryeong.esekai2.api.monster.level.MonsterLevels;
import kim.biryeong.esekai2.api.monster.level.MonsterRarity;
import kim.biryeong.esekai2.api.monster.stat.MonsterStats;
import kim.biryeong.esekai2.api.stat.combat.CombatStats;
import kim.biryeong.esekai2.api.stat.holder.StatHolder;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

public final class MonsterLevelGameTests {
    private static final Identifier DEFAULT_MONSTER_LEVEL_ID = Identifier.fromNamespaceAndPath("esekai2", "default");

    /**
     * Verifies that the default monster level datapack loads through the public registry.
     */
    @GameTest
    public void monsterLevelRegistryLoadsDefaultFixture(GameTestHelper helper) {
        Registry<MonsterLevelDefinition> registry = monsterLevelRegistry(helper);

        helper.assertTrue(registry.containsKey(DEFAULT_MONSTER_LEVEL_ID), "Default monster level definition should be present in the level registry");
        MonsterLevelDefinition definition = registry.getOptional(DEFAULT_MONSTER_LEVEL_ID)
                .orElseThrow(() -> helper.assertionException("Default monster level definition should decode successfully"));

        helper.assertValueEqual(definition.entries().size(), 100, "Monster level definition should contain one row for every supported level");
        helper.assertValueEqual(definition.entry(68).life(), 6127.0, "Level sixty-eight should expose its configured base life value");
        helper.succeed();
    }

    /**
     * Verifies that the public monster level codec round-trips without losing row data.
     */
    @GameTest
    public void monsterLevelCodecRoundTrips(GameTestHelper helper) {
        MonsterLevelDefinition definition = monsterLevelRegistry(helper).getOptional(DEFAULT_MONSTER_LEVEL_ID)
                .orElseThrow(() -> helper.assertionException("Default monster level definition should decode successfully"));

        var encoded = MonsterLevelDefinition.CODEC.encodeStart(JsonOps.INSTANCE, definition)
                .getOrThrow(message -> new IllegalStateException("Failed to encode monster level definition: " + message));
        MonsterLevelDefinition decoded = MonsterLevelDefinition.CODEC.parse(JsonOps.INSTANCE, encoded)
                .getOrThrow(message -> new IllegalStateException("Failed to decode monster level definition: " + message));

        helper.assertValueEqual(decoded.entries(), definition.entries(), "Monster level rows should survive a codec round trip");
        helper.succeed();
    }

    /**
     * Verifies that monster level definitions reject tables with missing levels.
     */
    @GameTest
    public void monsterLevelDefinitionRejectsMissingLevel(GameTestHelper helper) {
        try {
            MonsterLevelDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                    {
                      "damage": [1.0],
                      "evasion_rating": [1.0],
                      "accuracy_rating": [1.0],
                      "experience_points": [1.0],
                      "life": [1.0],
                      "summon_life": [1.0],
                      "magic_life_bonus_percent": [0.0],
                      "rare_life_bonus_percent": [0.0],
                      "map_life_bonus_percent": [0.0],
                      "map_damage_bonus_percent": [0.0],
                      "boss_life_bonus_percent": [0.0],
                      "boss_damage_bonus_percent": [0.0],
                      "boss_item_quantity_bonus_percent": [0.0],
                      "boss_item_rarity_bonus_percent": [0.0]
                    }
                    """)).getOrThrow(message -> new IllegalStateException("Expected missing-level validation to fail: " + message));
            throw helper.assertionException("Monster level definitions should reject incomplete tables");
        } catch (IllegalStateException expected) {
            helper.succeed();
        }
    }

    /**
     * Verifies that monster level rows reject negative bonus values.
     */
    @GameTest
    public void monsterLevelEntryRejectsNegativeBonus(GameTestHelper helper) {
        try {
            new MonsterLevelEntry(1, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
            throw helper.assertionException("Monster level rows should reject negative bonus values");
        } catch (IllegalArgumentException expected) {
            helper.succeed();
        }
    }

    /**
     * Verifies that rarity offsets derive dropped item level using the current PoE-style rule.
     */
    @GameTest
    public void droppedItemLevelUsesMonsterRarityOffsets(GameTestHelper helper) {
        helper.assertValueEqual(MonsterLevels.resolveDroppedItemLevel(new MonsterLevelContext(68, MonsterRarity.NORMAL, false, false)), 68, "Normal monsters should drop items of their own level");
        helper.assertValueEqual(MonsterLevels.resolveDroppedItemLevel(new MonsterLevelContext(68, MonsterRarity.MAGIC, false, false)), 69, "Magic monsters should add one item level");
        helper.assertValueEqual(MonsterLevels.resolveDroppedItemLevel(new MonsterLevelContext(68, MonsterRarity.RARE, false, false)), 70, "Rare monsters should add two item levels");
        helper.assertValueEqual(MonsterLevels.resolveDroppedItemLevel(new MonsterLevelContext(100, MonsterRarity.UNIQUE, false, false)), 100, "Derived item level should clamp at one hundred");
        helper.succeed();
    }

    /**
     * Verifies that rarity life bonuses affect resolved monster profiles.
     */
    @GameTest
    public void monsterProfileAppliesRarityLifeMultiplier(GameTestHelper helper) {
        MonsterLevelProfile profile = MonsterLevels.resolveProfile(helper.getLevel().getServer(), new MonsterLevelContext(68, MonsterRarity.MAGIC, false, false));

        helper.assertValueEqual(profile.baseLife(), 6127.0, "Level sixty-eight should expose its base life row value");
        helper.assertValueEqual(profile.effectiveLifeMultiplier(), 1.75, "Magic monsters should apply the configured rarity life multiplier");
        helper.assertValueEqual(profile.baseAccuracy(), 290.0, "The resolved profile should expose the level-row accuracy value");
        helper.succeed();
    }

    /**
     * Verifies that map and boss bonuses stack into the resolved life multiplier.
     */
    @GameTest
    public void monsterProfileAppliesMapAndBossLifeBonuses(GameTestHelper helper) {
        MonsterLevelProfile profile = MonsterLevels.resolveProfile(helper.getLevel().getServer(), new MonsterLevelContext(66, MonsterRarity.RARE, true, true));

        helper.assertTrue(Math.abs(profile.effectiveLifeMultiplier() - 2.9997) < 0.0000001, "Rare map bosses should apply rarity, map, and boss life bonuses multiplicatively");
        helper.assertValueEqual(profile.effectiveDamageMultiplier(), 1.0, "Level sixty-six should not apply any configured map damage bonus");
        helper.succeed();
    }

    /**
     * Verifies that scaled monster stat holder resolution projects level rows into configured stat axes.
     */
    @GameTest
    public void resolveScaledHolderUsesMonsterLevelAxes(GameTestHelper helper) {
        StatHolder holder = MonsterStats.resolveScaledHolder(helper.getLevel().getServer(), EntityType.ZOMBIE, new MonsterLevelContext(68, MonsterRarity.NORMAL, false, false))
                .orElseThrow(() -> helper.assertionException("Scaled zombie stat holder should resolve when the monster baseline exists"));

        helper.assertValueEqual(holder.resolvedValue(CombatStats.LIFE), 6127.0, "Life should come from the monster level table for scaled zombie holders");
        helper.assertValueEqual(holder.resolvedValue(CombatStats.ACCURACY), 290.0, "Accuracy should come from the monster level table for scaled zombie holders");
        helper.assertValueEqual(holder.resolvedValue(CombatStats.EVADE), 4739.0, "Evade should come from the monster level table for scaled zombie holders");
        helper.assertValueEqual(holder.resolvedValue(CombatStats.ARMOUR), 5.0, "Static baseline armour should remain intact on scaled zombie holders");
        helper.succeed();
    }

    private static Registry<MonsterLevelDefinition> monsterLevelRegistry(GameTestHelper helper) {
        return helper.getLevel().getServer().registryAccess().lookupOrThrow(MonsterLevelRegistries.MONSTER_LEVEL);
    }
}
