package kim.biryeong.esekai2.impl.gametest.level;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import kim.biryeong.esekai2.api.level.LevelProgressionDefinition;
import kim.biryeong.esekai2.api.level.LevelRegistries;
import kim.biryeong.esekai2.api.level.LevelRules;
import kim.biryeong.esekai2.api.player.level.PlayerLevelState;
import kim.biryeong.esekai2.api.player.level.PlayerLevels;
import kim.biryeong.esekai2.api.stat.combat.CombatStats;
import kim.biryeong.esekai2.api.stat.modifier.StatModifier;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;

public final class LevelProgressionGameTests {
    private static final Identifier DEFAULT_PROGRESSION_ID = Identifier.fromNamespaceAndPath("esekai2", "default");

    /**
     * Verifies that the shared level rules accept the configured bounds and reject out-of-range values.
     */
    @GameTest
    public void levelRulesValidateConfiguredBounds(GameTestHelper helper) {
        helper.assertValueEqual(LevelRules.requireValidLevel(1, "level"), 1, "Level one should be valid");
        helper.assertValueEqual(LevelRules.requireValidLevel(100, "level"), 100, "Level one hundred should be valid");

        try {
            LevelRules.requireValidLevel(0, "level");
            throw helper.assertionException("Level zero should be rejected");
        } catch (IllegalArgumentException expected) {
            helper.succeed();
        }
    }

    /**
     * Verifies that the default player progression datapack loads through the public registry.
     */
    @GameTest
    public void playerProgressionRegistryLoadsDefaultFixture(GameTestHelper helper) {
        Registry<LevelProgressionDefinition> registry = progressionRegistry(helper);

        helper.assertTrue(registry.containsKey(DEFAULT_PROGRESSION_ID), "Default player progression should be present in the level registry");
        LevelProgressionDefinition definition = registry.getOptional(DEFAULT_PROGRESSION_ID)
                .orElseThrow(() -> helper.assertionException("Default player progression should decode successfully"));

        helper.assertValueEqual(definition.entries().size(), 100, "Default player progression should contain one row for every supported level");
        helper.assertValueEqual(definition.experienceToNextLevel(1), 100L, "Level one should require the configured amount of experience");
        helper.assertValueEqual(definition.experienceToNextLevel(100), 0L, "Level one hundred should require zero additional experience");
        helper.succeed();
    }

    /**
     * Verifies that the public progression codec round-trips without losing row data.
     */
    @GameTest
    public void playerProgressionCodecRoundTrips(GameTestHelper helper) {
        LevelProgressionDefinition definition = progressionRegistry(helper).getOptional(DEFAULT_PROGRESSION_ID)
                .orElseThrow(() -> helper.assertionException("Default player progression should decode successfully"));

        var encoded = LevelProgressionDefinition.CODEC.encodeStart(JsonOps.INSTANCE, definition)
                .getOrThrow(message -> new IllegalStateException("Failed to encode player progression: " + message));
        LevelProgressionDefinition decoded = LevelProgressionDefinition.CODEC.parse(JsonOps.INSTANCE, encoded)
                .getOrThrow(message -> new IllegalStateException("Failed to decode player progression: " + message));

        helper.assertValueEqual(decoded.entries(), definition.entries(), "Progression rows should survive a codec round trip");
        helper.succeed();
    }

    /**
     * Verifies that granted_modifiers columns decode into per-level reward lists exposed by the public API.
     */
    @GameTest
    public void playerProgressionExposesGrantedModifiers(GameTestHelper helper) {
        LevelProgressionDefinition definition = progressionRegistry(helper).getOptional(DEFAULT_PROGRESSION_ID)
                .orElseThrow(() -> helper.assertionException("Default player progression should decode successfully"));

        helper.assertValueEqual(definition.grantedModifiers(1), java.util.List.of(), "Level one should not grant any configured modifiers");
        assertSingleReward(helper, definition.grantedModifiers(2), CombatStats.LIFE, 5.0, "Level two should grant the configured life reward");
        assertSingleReward(helper, definition.grantedModifiers(3), CombatStats.MANA, 10.0, "Level three should grant the configured mana reward");
        assertSingleReward(helper, definition.grantedModifiers(4), CombatStats.ACCURACY, 7.0, "Level four should grant the configured accuracy reward");
        helper.succeed();
    }

    /**
     * Verifies that progression definitions reject missing level rows.
     */
    @GameTest
    public void playerProgressionRejectsMissingLevelRow(GameTestHelper helper) {
        try {
            LevelProgressionDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                    {
                      "experience_to_next_level": [100, 0]
                    }
                    """)).getOrThrow(message -> new IllegalStateException("Expected missing-level validation to fail: " + message));
            throw helper.assertionException("Progression definitions should reject tables that do not contain every supported level");
        } catch (IllegalStateException expected) {
            helper.succeed();
        }
    }

    /**
     * Verifies that player level state values survive the public codec.
     */
    @GameTest
    public void playerLevelStateCodecRoundTrips(GameTestHelper helper) {
        PlayerLevelState state = new PlayerLevelState(12, 34L, 567L);

        var encoded = PlayerLevelState.CODEC.encodeStart(JsonOps.INSTANCE, state)
                .getOrThrow(message -> new IllegalStateException("Failed to encode player level state: " + message));
        PlayerLevelState decoded = PlayerLevelState.CODEC.parse(JsonOps.INSTANCE, encoded)
                .getOrThrow(message -> new IllegalStateException("Failed to decode player level state: " + message));

        helper.assertValueEqual(decoded, state, "Player level state should survive a codec round trip");
        helper.succeed();
    }

    /**
     * Verifies that player level state rejects negative experience values.
     */
    @GameTest
    public void playerLevelStateRejectsNegativeExperience(GameTestHelper helper) {
        try {
            new PlayerLevelState(1, -1L, 0L);
            throw helper.assertionException("Player level state should reject negative in-level experience");
        } catch (IllegalArgumentException expected) {
            helper.succeed();
        }
    }

    /**
     * Verifies that adding enough experience for one threshold promotes the player by one level.
     */
    @GameTest
    public void addExperiencePromotesOneLevel(GameTestHelper helper) {
        var player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        PlayerLevels.setExperience(player, 0L);
        PlayerLevelState state = PlayerLevels.addExperience(player, 100L);

        helper.assertValueEqual(state.level(), 2, "Adding exactly the level-one threshold should promote the player to level two");
        helper.assertValueEqual(state.experienceInLevel(), 0L, "Crossing the threshold exactly should leave zero in-level experience");
        helper.assertValueEqual(PlayerLevels.experienceToNextLevel(player), 200L, "Level two should expose its configured next-level threshold");
        helper.succeed();
    }

    /**
     * Verifies that setting a large total experience value can skip multiple levels in one resolution pass.
     */
    @GameTest
    public void setExperienceCanSkipMultipleLevels(GameTestHelper helper) {
        var player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        PlayerLevelState state = PlayerLevels.setExperience(player, 1_500L);

        helper.assertValueEqual(state.level(), 6, "One thousand five hundred total experience should resolve to level six with this default progression");
        helper.assertValueEqual(state.experienceInLevel(), 0L, "Landing exactly on a threshold should leave no remaining in-level experience");
        helper.succeed();
    }

    /**
     * Verifies that extremely large experience values clamp the resolved player level at one hundred.
     */
    @GameTest
    public void totalExperienceClampsAtMaximumLevel(GameTestHelper helper) {
        var player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        PlayerLevelState state = PlayerLevels.setExperience(player, Long.MAX_VALUE / 4L);

        helper.assertValueEqual(state.level(), 100, "Very large total experience should clamp the player level at one hundred");
        helper.assertValueEqual(state.experienceInLevel(), 0L, "Level one hundred should not retain in-level experience");
        helper.assertValueEqual(PlayerLevels.experienceToNextLevel(player), 0L, "Level one hundred should report zero remaining experience");
        helper.succeed();
    }

    private static Registry<LevelProgressionDefinition> progressionRegistry(GameTestHelper helper) {
        return helper.getLevel().getServer().registryAccess().lookupOrThrow(LevelRegistries.PLAYER_PROGRESSION);
    }

    private static void assertSingleReward(
            GameTestHelper helper,
            java.util.List<StatModifier> modifiers,
            net.minecraft.resources.ResourceKey<kim.biryeong.esekai2.api.stat.definition.StatDefinition> stat,
            double value,
            String message
    ) {
        helper.assertValueEqual(modifiers.size(), 1, message);
        StatModifier modifier = modifiers.getFirst();
        helper.assertValueEqual(modifier.stat(), stat, message);
        helper.assertValueEqual(modifier.value(), value, message);
    }
}
