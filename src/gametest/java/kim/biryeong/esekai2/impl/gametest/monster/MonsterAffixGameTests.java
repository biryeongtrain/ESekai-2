package kim.biryeong.esekai2.impl.gametest.monster;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import kim.biryeong.esekai2.api.config.monster.MonsterAffixConfig;
import kim.biryeong.esekai2.api.config.monster.MonsterAffixCountConfig;
import kim.biryeong.esekai2.api.monster.affix.MonsterAffixDefinition;
import kim.biryeong.esekai2.api.monster.affix.MonsterAffixKind;
import kim.biryeong.esekai2.api.monster.affix.MonsterAffixPoolDefinition;
import kim.biryeong.esekai2.api.monster.affix.MonsterAffixRegistries;
import kim.biryeong.esekai2.api.monster.affix.MonsterAffixState;
import kim.biryeong.esekai2.api.monster.affix.MonsterAffixes;
import kim.biryeong.esekai2.api.monster.level.MonsterLevelContext;
import kim.biryeong.esekai2.api.monster.level.MonsterLevelProfile;
import kim.biryeong.esekai2.api.monster.level.MonsterLevels;
import kim.biryeong.esekai2.api.monster.level.MonsterRarity;
import kim.biryeong.esekai2.api.monster.stat.MonsterStats;
import kim.biryeong.esekai2.api.stat.combat.CombatStats;
import kim.biryeong.esekai2.api.stat.holder.StatHolder;
import kim.biryeong.esekai2.impl.config.monster.MonsterAffixConfigManager;
import kim.biryeong.esekai2.impl.monster.affix.MonsterAffixRuntimeInitializer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Random;

public final class MonsterAffixGameTests {
    private static final Identifier BRUTAL_PREFIX_ID = Identifier.fromNamespaceAndPath("esekai2", "zombie_brutal_life_prefix");
    private static final Identifier HARDENED_PREFIX_ID = Identifier.fromNamespaceAndPath("esekai2", "zombie_hardened_armour_prefix");
    private static final Identifier FETID_SUFFIX_ID = Identifier.fromNamespaceAndPath("esekai2", "zombie_fetid_chaos_suffix");
    private static final Identifier SKULKING_SUFFIX_ID = Identifier.fromNamespaceAndPath("esekai2", "zombie_skulking_evasion_suffix");
    private static final Identifier ZOMBIE_POOL_ID = Identifier.fromNamespaceAndPath("esekai2", "zombie");

    /**
     * Verifies that the monster affix registry loads the sample affix fixtures used by the monster runtime.
     */
    @GameTest
    public void monsterAffixRegistryLoadsSampleFixtures(GameTestHelper helper) {
        Registry<MonsterAffixDefinition> registry = affixRegistry(helper);

        helper.assertTrue(registry.containsKey(BRUTAL_PREFIX_ID), "Brutal life prefix should be present in the monster affix registry");
        helper.assertTrue(registry.containsKey(HARDENED_PREFIX_ID), "Hardened armour prefix should be present in the monster affix registry");
        helper.assertTrue(registry.containsKey(FETID_SUFFIX_ID), "Fetid chaos suffix should be present in the monster affix registry");
        helper.assertTrue(registry.containsKey(SKULKING_SUFFIX_ID), "Skulking evasion suffix should be present in the monster affix registry");
        helper.succeed();
    }

    /**
     * Verifies that a monster affix definition round-trips through the public codec without losing rarity or modifier metadata.
     */
    @GameTest
    public void monsterAffixDefinitionCodecRoundTrips(GameTestHelper helper) {
        MonsterAffixDefinition definition = affixRegistry(helper).getOptional(BRUTAL_PREFIX_ID)
                .orElseThrow(() -> helper.assertionException("Brutal life prefix should decode successfully"));

        var encoded = MonsterAffixDefinition.CODEC.encodeStart(JsonOps.INSTANCE, definition)
                .getOrThrow(message -> new IllegalStateException("Failed to encode monster affix definition: " + message));
        MonsterAffixDefinition decoded = MonsterAffixDefinition.CODEC.parse(JsonOps.INSTANCE, encoded)
                .getOrThrow(message -> new IllegalStateException("Failed to decode monster affix definition: " + message));

        helper.assertValueEqual(decoded.kind(), MonsterAffixKind.PREFIX, "Monster affix kind should survive a codec round trip");
        helper.assertTrue(decoded.allowedRarities().contains(MonsterRarity.MAGIC), "Allowed rarities should survive a codec round trip");
        helper.assertValueEqual(decoded.modifierRanges().size(), 1, "Modifier ranges should survive a codec round trip");
        helper.succeed();
    }

    /**
     * Verifies that a monster affix pool definition round-trips through the public codec without losing entity and candidate metadata.
     */
    @GameTest
    public void monsterAffixPoolDefinitionCodecRoundTrips(GameTestHelper helper) {
        MonsterAffixPoolDefinition definition = poolRegistry(helper).getOptional(ZOMBIE_POOL_ID)
                .orElseThrow(() -> helper.assertionException("Zombie monster affix pool should decode successfully"));

        var encoded = MonsterAffixPoolDefinition.CODEC.encodeStart(JsonOps.INSTANCE, definition)
                .getOrThrow(message -> new IllegalStateException("Failed to encode monster affix pool definition: " + message));
        MonsterAffixPoolDefinition decoded = MonsterAffixPoolDefinition.CODEC.parse(JsonOps.INSTANCE, encoded)
                .getOrThrow(message -> new IllegalStateException("Failed to decode monster affix pool definition: " + message));

        helper.assertValueEqual(decoded.entityType(), EntityType.ZOMBIE, "Pool entity type should survive a codec round trip");
        helper.assertValueEqual(decoded.candidateAffixIds().size(), 4, "Candidate affix ids should survive a codec round trip");
        helper.assertValueEqual(decoded.defaultSpawnContext().rarity(), MonsterRarity.MAGIC, "Default spawn context should survive a codec round trip");
        helper.succeed();
    }

    /**
     * Verifies that monster affix definitions reject non-positive weights during codec validation.
     */
    @GameTest
    public void monsterAffixDefinitionRejectsNonPositiveWeight(GameTestHelper helper) {
        try {
            MonsterAffixDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                    {
                      "translation_key": "monster_affix.esekai2.invalid_weight",
                      "kind": "prefix",
                      "weight": 0,
                      "minimum_monster_level": 1,
                      "allowed_rarities": ["magic"],
                      "modifier_ranges": [
                        {
                          "stat": "esekai2:life",
                          "operation": "add",
                          "min_value": 1.0,
                          "max_value": 2.0
                        }
                      ]
                    }
                    """)).getOrThrow(message -> new IllegalStateException("Expected non-positive weight validation to fail: " + message));
            throw helper.assertionException("Monster affix definitions should reject non-positive weights");
        } catch (IllegalStateException expected) {
            helper.succeed();
        }
    }

    /**
     * Verifies that monster affix definitions reject empty rarity gates during codec validation.
     */
    @GameTest
    public void monsterAffixDefinitionRejectsEmptyAllowedRarities(GameTestHelper helper) {
        try {
            MonsterAffixDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                    {
                      "translation_key": "monster_affix.esekai2.invalid_rarities",
                      "kind": "prefix",
                      "weight": 1,
                      "minimum_monster_level": 1,
                      "allowed_rarities": [],
                      "modifier_ranges": [
                        {
                          "stat": "esekai2:life",
                          "operation": "add",
                          "min_value": 1.0,
                          "max_value": 2.0
                        }
                      ]
                    }
                    """)).getOrThrow(message -> new IllegalStateException("Expected empty rarity validation to fail: " + message));
            throw helper.assertionException("Monster affix definitions should reject empty rarity gates");
        } catch (IllegalStateException expected) {
            helper.succeed();
        }
    }

    /**
     * Verifies that monster affix pools reject empty candidate lists during codec validation.
     */
    @GameTest
    public void monsterAffixPoolRejectsEmptyCandidates(GameTestHelper helper) {
        try {
            MonsterAffixPoolDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                    {
                      "entity_type": "minecraft:zombie",
                      "default_spawn_context": {
                        "level": 68,
                        "rarity": "magic"
                      },
                      "candidate_affix_ids": []
                    }
                    """)).getOrThrow(message -> new IllegalStateException("Expected empty candidate validation to fail: " + message));
            throw helper.assertionException("Monster affix pools should reject empty candidate lists");
        } catch (IllegalStateException expected) {
            helper.succeed();
        }
    }

    /**
     * Verifies that the default rarity-based affix count config matches the current agreed baseline.
     */
    @GameTest
    public void defaultMonsterAffixCountConfigMatchesBaseline(GameTestHelper helper) {
        MonsterAffixCountConfig counts = MonsterAffixCountConfig.DEFAULT;

        helper.assertValueEqual(counts.profile(MonsterRarity.NORMAL).prefixCount(), 0, "Normal monsters should default to zero prefixes");
        helper.assertValueEqual(counts.profile(MonsterRarity.NORMAL).suffixCount(), 0, "Normal monsters should default to zero suffixes");
        helper.assertValueEqual(counts.profile(MonsterRarity.MAGIC).prefixCount(), 1, "Magic monsters should default to one prefix");
        helper.assertValueEqual(counts.profile(MonsterRarity.MAGIC).suffixCount(), 1, "Magic monsters should default to one suffix");
        helper.assertValueEqual(counts.profile(MonsterRarity.RARE).prefixCount(), 1, "Rare monsters should default to one prefix");
        helper.assertValueEqual(counts.profile(MonsterRarity.RARE).suffixCount(), 1, "Rare monsters should default to one suffix");
        helper.assertValueEqual(counts.profile(MonsterRarity.UNIQUE).prefixCount(), 0, "Unique monsters should default to zero prefixes");
        helper.assertValueEqual(counts.profile(MonsterRarity.UNIQUE).suffixCount(), 0, "Unique monsters should default to zero suffixes");
        helper.succeed();
    }

    /**
     * Verifies that missing config files fall back to defaults and write a default file.
     */
    @GameTest
    public void missingMonsterAffixConfigFallsBackToDefaults(GameTestHelper helper) throws Exception {
        Path tempDir = Files.createTempDirectory("esekai2-monster-affix-config");
        Path configPath = tempDir.resolve("esekai2-server.json");

        MonsterAffixConfig loaded = MonsterAffixConfigManager.load(configPath);

        helper.assertValueEqual(loaded, MonsterAffixConfig.DEFAULT, "Missing config files should fall back to the default monster affix config");
        helper.assertTrue(Files.exists(configPath), "Missing config files should be materialized with defaults");
        helper.succeed();
    }

    /**
     * Verifies that invalid config files fall back to defaults instead of exposing negative affix counts.
     */
    @GameTest
    public void invalidMonsterAffixConfigFallsBackToDefaults(GameTestHelper helper) throws Exception {
        Path tempDir = Files.createTempDirectory("esekai2-monster-affix-invalid");
        Path configPath = tempDir.resolve("esekai2-server.json");
        Files.writeString(configPath, """
                {
                  "monster_affix_counts": {
                    "magic_prefixes": -1
                  }
                }
                """);

        MonsterAffixConfig loaded = MonsterAffixConfigManager.load(configPath);

        helper.assertValueEqual(loaded, MonsterAffixConfig.DEFAULT, "Invalid config files should fall back to default monster affix counts");
        helper.succeed();
    }

    /**
     * Verifies that current config overrides change the number of affixes rolled for a rarity.
     */
    @GameTest
    public void configOverrideChangesRolledAffixCounts(GameTestHelper helper) {
        MonsterAffixConfigManager.setCurrentForTesting(new MonsterAffixConfig(new MonsterAffixCountConfig(0, 0, 0, 1, 1, 1, 0, 0)));
        try {
            MonsterAffixState state = MonsterAffixes.rollState(
                    helper.getLevel().getServer(),
                    EntityType.ZOMBIE,
                    new MonsterLevelContext(68, MonsterRarity.MAGIC, false, false),
                    new Random(0L)
            ).orElseThrow(() -> helper.assertionException("Zombie monster affix state should roll when a pool exists"));

            helper.assertValueEqual(state.rolledAffixes().size(), 1, "Magic override should reduce the zombie roll to one suffix only");
            helper.assertValueEqual(state.rolledAffixes().getFirst().kind(), MonsterAffixKind.SUFFIX, "The remaining rolled affix should be a suffix");
            helper.succeed();
        } finally {
            MonsterAffixConfigManager.setCurrentForTesting(MonsterAffixConfig.DEFAULT);
        }
    }

    /**
     * Verifies that rarity gating excludes rare-only affixes when rolling a magic zombie.
     */
    @GameTest
    public void magicZombieRollSkipsRareOnlyAffixes(GameTestHelper helper) {
        MonsterAffixState state = MonsterAffixes.rollState(
                helper.getLevel().getServer(),
                EntityType.ZOMBIE,
                new MonsterLevelContext(68, MonsterRarity.MAGIC, false, false),
                new Random(0L)
        ).orElseThrow(() -> helper.assertionException("Zombie monster affix state should roll when a pool exists"));

        helper.assertValueEqual(state.rolledAffixes().size(), 2, "Magic zombies should roll one prefix and one suffix with the default config");
        helper.assertValueEqual(state.rolledAffixes().stream().filter(affix -> affix.affixId().equals(HARDENED_PREFIX_ID)).count(), 0L,
                "Rare-only prefixes should be excluded from magic rolls");
        helper.assertValueEqual(state.rolledAffixes().stream().filter(affix -> affix.affixId().equals(SKULKING_SUFFIX_ID)).count(), 0L,
                "Rare-only suffixes should be excluded from magic rolls");
        helper.succeed();
    }

    /**
     * Verifies that live zombie spawns receive an attached affix state during finalizeSpawn.
     */
    @GameTest
    public void zombieSpawnReceivesAttachedMonsterAffixState(GameTestHelper helper) {
        Mob zombie = (Mob) helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(1.0, 2.0, 1.0));

        Optional<MonsterAffixState> state = MonsterAffixes.get(zombie);

        helper.assertTrue(state.isPresent(), "Spawned zombies should receive attached monster affix state");
        helper.assertValueEqual(state.orElseThrow().levelContext().rarity(), MonsterRarity.MAGIC, "Zombie pool default spawn rarity should be attached to live zombies");
        helper.assertValueEqual(state.orElseThrow().rolledAffixes().size(), 2, "Zombie pool default spawn should roll one prefix and one suffix");
        helper.succeed();
    }

    /**
     * Verifies that repeated initialization does not reroll or duplicate already attached monster affixes.
     */
    @GameTest
    public void repeatedInitializationDoesNotRerollAttachedAffixes(GameTestHelper helper) {
        Mob zombie = (Mob) helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(1.0, 2.0, 2.0));
        MonsterAffixState first = MonsterAffixes.get(zombie)
                .orElseThrow(() -> helper.assertionException("Spawned zombies should receive attached monster affix state"));

        MonsterAffixRuntimeInitializer.initialize(zombie, new Random(1234L));
        MonsterAffixState second = MonsterAffixes.get(zombie)
                .orElseThrow(() -> helper.assertionException("Attached state should remain present after repeated initialization"));

        helper.assertValueEqual(second, first, "Repeated initialization should preserve the existing attached monster affix state");
        helper.succeed();
    }

    /**
     * Verifies that runtime holder resolution combines scaled baseline stats with attached affix modifiers.
     */
    @GameTest
    public void resolveRuntimeHolderAppliesAttachedMonsterAffixes(GameTestHelper helper) {
        Mob zombie = (Mob) helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(1.0, 2.0, 3.0));

        StatHolder holder = MonsterStats.resolveRuntimeHolder(zombie)
                .orElseThrow(() -> helper.assertionException("Spawned zombies with affix state should resolve a runtime holder"));
        MonsterLevelProfile profile = MonsterLevels.resolveProfile(helper.getLevel().getServer(), new MonsterLevelContext(68, MonsterRarity.MAGIC, false, false));

        helper.assertValueEqual(holder.resolvedValue(CombatStats.LIFE), profile.baseLife() * profile.effectiveLifeMultiplier() + 200.0,
                "Runtime holder should include both scaled life and the brutal life prefix");
        helper.assertValueEqual(holder.resolvedValue(CombatStats.CHAOS_RESISTANCE), 17.0,
                "Runtime holder should include the magic-eligible chaos suffix");
        helper.assertValueEqual(holder.resolvedValue(CombatStats.ARMOUR), 5.0,
                "Rare-only armour affixes should not affect magic zombie runtime holders");
        helper.succeed();
    }

    /**
     * Verifies that monsters without an affix pool do not fabricate runtime holder state.
     */
    @GameTest
    public void runtimeHolderIsEmptyWithoutMonsterAffixState(GameTestHelper helper) {
        Mob creeper = (Mob) helper.spawnWithNoFreeWill(EntityType.CREEPER, new Vec3(1.0, 2.0, 4.0));

        helper.assertTrue(MonsterAffixes.get(creeper).isEmpty(), "Creatures without a monster affix pool should not receive attached affix state");
        helper.assertTrue(MonsterStats.resolveRuntimeHolder(creeper).isEmpty(), "Creatures without monster affix state should not resolve runtime holders");
        helper.succeed();
    }

    private static Registry<MonsterAffixDefinition> affixRegistry(GameTestHelper helper) {
        return helper.getLevel().getServer().registryAccess().lookupOrThrow(MonsterAffixRegistries.MONSTER_AFFIX);
    }

    private static Registry<MonsterAffixPoolDefinition> poolRegistry(GameTestHelper helper) {
        return helper.getLevel().getServer().registryAccess().lookupOrThrow(MonsterAffixRegistries.MONSTER_AFFIX_POOL);
    }
}
