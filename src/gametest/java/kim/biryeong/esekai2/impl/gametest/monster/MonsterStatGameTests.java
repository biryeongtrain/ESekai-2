package kim.biryeong.esekai2.impl.gametest.monster;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import kim.biryeong.esekai2.api.monster.level.MonsterScaledStatAxis;
import kim.biryeong.esekai2.api.monster.stat.MonsterRegistries;
import kim.biryeong.esekai2.api.monster.stat.MonsterStatDefinition;
import kim.biryeong.esekai2.api.monster.stat.MonsterStats;
import kim.biryeong.esekai2.api.stat.combat.CombatStats;
import kim.biryeong.esekai2.api.stat.holder.StatHolder;
import kim.biryeong.esekai2.api.stat.modifier.StatModifier;
import kim.biryeong.esekai2.api.stat.modifier.StatModifierOperation;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

import java.util.Map;
import java.util.Optional;

public final class MonsterStatGameTests {
    private static final Identifier ZOMBIE_MONSTER_STAT_ID = Identifier.fromNamespaceAndPath("esekai2", "zombie");

    /**
     * Verifies that the monster stat registry loads the zombie baseline fixture through the public codec.
     */
    @GameTest
    public void monsterStatRegistryLoadsZombieBaseline(GameTestHelper helper) {
        Registry<MonsterStatDefinition> registry = monsterRegistry(helper);

        helper.assertTrue(registry.containsKey(ZOMBIE_MONSTER_STAT_ID), "Zombie monster stat definition should be present in the monster stat registry");

        MonsterStatDefinition definition = registry.getOptional(ZOMBIE_MONSTER_STAT_ID)
                .orElseThrow(() -> helper.assertionException("Zombie monster stat definition should decode successfully"));

        helper.assertValueEqual(definition.entityType(), EntityType.ZOMBIE, "Zombie monster stat definition should target minecraft:zombie");
        helper.assertValueEqual(definition.baseStats().get(CombatStats.ARMOUR), 5.0, "Zombie monster stat definition should expose its armour baseline");
        helper.assertValueEqual(definition.scaledStats().get(CombatStats.LIFE), MonsterScaledStatAxis.LIFE, "Zombie monster stat definition should expose life as a scaled stat axis");
        helper.succeed();
    }

    /**
     * Verifies that a monster stat definition round-trips through the public codec without losing entity type or base stats.
     */
    @GameTest
    public void monsterStatDefinitionCodecRoundTrips(GameTestHelper helper) {
        MonsterStatDefinition definition = monsterRegistry(helper).getOptional(ZOMBIE_MONSTER_STAT_ID)
                .orElseThrow(() -> helper.assertionException("Zombie monster stat definition should decode successfully"));

        var encoded = MonsterStatDefinition.CODEC.encodeStart(JsonOps.INSTANCE, definition)
                .getOrThrow(message -> new IllegalStateException("Failed to encode monster stat definition: " + message));
        MonsterStatDefinition decoded = MonsterStatDefinition.CODEC.parse(JsonOps.INSTANCE, encoded)
                .getOrThrow(message -> new IllegalStateException("Failed to re-decode monster stat definition: " + message));

        helper.assertValueEqual(decoded.entityType(), EntityType.ZOMBIE, "Entity type should survive a monster stat codec round trip");
        helper.assertValueEqual(decoded.baseStats(), definition.baseStats(), "Base stats should survive a monster stat codec round trip");
        helper.assertValueEqual(decoded.scaledStats(), definition.scaledStats(), "Scaled stat axes should survive a monster stat codec round trip");
        helper.succeed();
    }

    /**
     * Verifies that monster stat definitions reject empty base stat maps.
     */
    @GameTest
    public void monsterStatDefinitionRejectsEmptyBaseStats(GameTestHelper helper) {
        try {
            new MonsterStatDefinition(EntityType.ZOMBIE, Map.of(), Map.of());
            throw helper.assertionException("Monster stat definitions should reject definitions without any base or scaled stats");
        } catch (IllegalArgumentException expected) {
            helper.succeed();
        }
    }

    /**
     * Verifies that monster stat definitions reject non-finite base stat values.
     */
    @GameTest
    public void monsterStatDefinitionRejectsNonFiniteBaseStatValue(GameTestHelper helper) {
        try {
            new MonsterStatDefinition(EntityType.ZOMBIE, Map.of(CombatStats.LIFE, Double.POSITIVE_INFINITY), Map.of());
            throw helper.assertionException("Monster stat definitions should reject non-finite base stat values");
        } catch (IllegalArgumentException expected) {
            helper.succeed();
        }
    }

    /**
     * Verifies that resolving the zombie baseline by entity type produces a fresh stat holder with the configured base values.
     */
    @GameTest
    public void resolveBaseHolderForZombieEntityType(GameTestHelper helper) {
        Optional<StatHolder> resolved = MonsterStats.resolveBaseHolder(helper.getLevel().getServer(), EntityType.ZOMBIE);

        helper.assertTrue(resolved.isPresent(), "Zombie base stat holder should resolve from the monster stat registry");

        StatHolder statHolder = resolved.orElseThrow();
        helper.assertValueEqual(statHolder.resolvedValue(CombatStats.ARMOUR), 5.0, "Resolved zombie holder should materialize its armour baseline");
        helper.assertValueEqual(statHolder.resolvedValue(CombatStats.LIFE), 20.0, "Scaled stats should retain the stat definition default during plain base-holder resolution");
        helper.assertValueEqual(statHolder.resolvedValue(CombatStats.EVADE), 0.0, "Scaled stats should retain the combat stat default during plain base-holder resolution");
        helper.assertValueEqual(statHolder.resolvedValue(CombatStats.ACCURACY), 0.0, "Scaled stats should retain the combat stat default during plain base-holder resolution");
        helper.succeed();
    }

    /**
     * Verifies that missing monster stat definitions resolve as empty optionals instead of throwing or fabricating holders.
     */
    @GameTest
    public void missingMonsterDefinitionReturnsEmptyOptional(GameTestHelper helper) {
        Optional<StatHolder> resolved = MonsterStats.resolveBaseHolder(helper.getLevel().getServer(), EntityType.CREEPER);

        helper.assertTrue(resolved.isEmpty(), "Missing monster stat definitions should resolve as empty optionals");
        helper.succeed();
    }

    /**
     * Verifies that each resolution call returns a fresh holder instead of reusing mutable runtime state.
     */
    @GameTest
    public void resolverReturnsFreshHolderPerCall(GameTestHelper helper) {
        StatHolder first = MonsterStats.resolveBaseHolder(helper.getLevel().getServer(), EntityType.ZOMBIE)
                .orElseThrow(() -> helper.assertionException("Zombie base stat holder should resolve on the first call"));
        first.setBaseValue(CombatStats.LIFE, 1.0);
        first.addModifier(new StatModifier(CombatStats.ARMOUR, StatModifierOperation.ADD, 99.0, Identifier.fromNamespaceAndPath("esekai2", "test_mutation")));

        StatHolder second = MonsterStats.resolveBaseHolder(helper.getLevel().getServer(), EntityType.ZOMBIE)
                .orElseThrow(() -> helper.assertionException("Zombie base stat holder should resolve on the second call"));

        helper.assertTrue(first != second, "Monster stat resolution should return fresh holder instances");
        helper.assertValueEqual(second.resolvedValue(CombatStats.LIFE), 20.0, "Later resolutions should not inherit mutated life values");
        helper.assertValueEqual(second.resolvedValue(CombatStats.ARMOUR), 5.0, "Later resolutions should not inherit added runtime modifiers");
        helper.succeed();
    }

    private static Registry<MonsterStatDefinition> monsterRegistry(GameTestHelper helper) {
        return helper.getLevel().getServer().registryAccess().lookupOrThrow(MonsterRegistries.MONSTER_STAT);
    }
}
