package kim.biryeong.esekai2.impl.gametest.player;

import kim.biryeong.esekai2.api.player.resource.PlayerResources;
import kim.biryeong.esekai2.api.player.resource.PlayerResourceIds;
import kim.biryeong.esekai2.api.player.stat.PlayerCombatStats;
import kim.biryeong.esekai2.api.stat.combat.CombatStats;
import kim.biryeong.esekai2.api.stat.definition.StatDefinition;
import kim.biryeong.esekai2.api.stat.definition.StatRegistries;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;

/**
 * Verifies persistent player mana resource runtime behavior.
 */
public final class PlayerResourceGameTests {
    private static final String GUARD_RESOURCE = "guard";
    private static final String FOCUS_RESOURCE = "examplemod:focus";
    private static final ResourceKey<StatDefinition> GUARD_STAT = ResourceKey.create(
            StatRegistries.STAT,
            Identifier.fromNamespaceAndPath("esekai2", "guard")
    );
    private static final ResourceKey<StatDefinition> GUARD_REGENERATION_PER_SECOND_STAT = ResourceKey.create(
            StatRegistries.STAT,
            Identifier.fromNamespaceAndPath("esekai2", "guard_regeneration_per_second")
    );
    private static final ResourceKey<StatDefinition> FOCUS_STAT = ResourceKey.create(
            StatRegistries.STAT,
            Identifier.fromNamespaceAndPath("examplemod", "focus")
    );
    private static final ResourceKey<StatDefinition> FOCUS_REGENERATION_PER_SECOND_STAT = ResourceKey.create(
            StatRegistries.STAT,
            Identifier.fromNamespaceAndPath("examplemod", "focus_regeneration_per_second")
    );

    /**
     * Verifies that positive mana regeneration restores persistent mana over server ticks.
     */
    @GameTest
    public void manaRegenerationRestoresPersistentManaOverTime(GameTestHelper helper) {
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        PlayerCombatStats.get(player).setBaseValue(CombatStats.MANA, 20.0);
        PlayerCombatStats.get(player).setBaseValue(CombatStats.MANA_REGENERATION_PER_SECOND, 4.0);
        PlayerResources.setMana(player, 5.0, 20.0);

        helper.runAfterDelay(10, () -> {
            double currentMana = PlayerResources.getMana(player, 20.0);
            helper.assertTrue(Math.abs(currentMana - 7.0) <= 1.0E-6,
                    "Ten server ticks at four mana per second should restore two mana");
            helper.succeed();
        });
    }

    /**
     * Verifies that mana regeneration never restores past the player's current maximum mana.
     */
    @GameTest
    public void manaRegenerationClampsAtMaximumMana(GameTestHelper helper) {
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        PlayerCombatStats.get(player).setBaseValue(CombatStats.MANA, 6.0);
        PlayerCombatStats.get(player).setBaseValue(CombatStats.MANA_REGENERATION_PER_SECOND, 10.0);
        PlayerResources.setMana(player, 5.5, 6.0);

        helper.runAfterDelay(10, () -> {
            double currentMana = PlayerResources.getMana(player, 6.0);
            helper.assertTrue(Math.abs(currentMana - 6.0) <= 1.0E-6,
                    "Mana regeneration should clamp to the current maximum mana");
            helper.succeed();
        });
    }

    /**
     * Verifies that zero max mana or zero regeneration do not mutate persistent mana state.
     */
    @GameTest
    public void manaRegenerationSkipsPlayersWithoutManaPoolOrRegen(GameTestHelper helper) {
        ServerPlayer noRegenPlayer = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        PlayerCombatStats.get(noRegenPlayer).setBaseValue(CombatStats.MANA, 20.0);
        PlayerCombatStats.get(noRegenPlayer).setBaseValue(CombatStats.MANA_REGENERATION_PER_SECOND, 0.0);
        PlayerResources.setMana(noRegenPlayer, 5.0, 20.0);

        ServerPlayer noManaPlayer = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        PlayerCombatStats.get(noManaPlayer).setBaseValue(CombatStats.MANA, 0.0);
        PlayerCombatStats.get(noManaPlayer).setBaseValue(CombatStats.MANA_REGENERATION_PER_SECOND, 5.0);
        PlayerResources.setMana(noManaPlayer, 0.0, 0.0);

        helper.runAfterDelay(10, () -> {
            double noRegenMana = PlayerResources.getMana(noRegenPlayer, 20.0);
            double noManaPoolMana = PlayerResources.getMana(noManaPlayer, 0.0);
            helper.assertTrue(Math.abs(noRegenMana - 5.0) <= 1.0E-6,
                    "Players without mana regeneration should keep their current mana");
            helper.assertTrue(Math.abs(noManaPoolMana - 0.0) <= 1.0E-6,
                    "Players without a positive mana pool should not regenerate mana");
            helper.succeed();
        });
    }

    /**
     * Verifies that registered non-mana resources regenerate through the shared registry-backed runtime loop.
     */
    @GameTest
    public void registeredNonManaResourceRegenerationUsesRegistryDefinition(GameTestHelper helper) {
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        PlayerCombatStats.get(player).setBaseValue(GUARD_STAT, 10.0);
        PlayerCombatStats.get(player).setBaseValue(GUARD_REGENERATION_PER_SECOND_STAT, 4.0);
        PlayerResources.set(player, GUARD_RESOURCE, 5.0);

        helper.runAfterDelay(10, () -> {
            double currentGuard = PlayerResources.getAmount(player, GUARD_RESOURCE);
            helper.assertTrue(Math.abs(currentGuard - 7.0) <= 1.0E-6,
                    "Ten server ticks at four resource per second should restore two guard");
            helper.succeed();
        });
    }

    /**
     * Verifies that registered non-esekai2 resources keep their full namespaced id during regeneration.
     */
    @GameTest
    public void namespacedRegisteredResourceRegenerationUsesFullResourceId(GameTestHelper helper) {
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        PlayerCombatStats.get(player).setBaseValue(FOCUS_STAT, 10.0);
        PlayerCombatStats.get(player).setBaseValue(FOCUS_REGENERATION_PER_SECOND_STAT, 4.0);
        PlayerResources.set(player, FOCUS_RESOURCE, 5.0);

        helper.runAfterDelay(10, () -> {
            double currentFocus = PlayerResources.getAmount(player, FOCUS_RESOURCE);
            helper.assertTrue(Math.abs(currentFocus - 7.0) <= 1.0E-6,
                    "Ten server ticks at four focus per second should restore two focus on the full namespaced id");
            helper.succeed();
        });
    }

    /**
     * Verifies that registry-backed namespaced resources remain distinct from explicit-max compatibility buckets.
     */
    @GameTest
    public void namespacedRegisteredResourceDoesNotAliasShortPathBuckets(GameTestHelper helper) {
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        PlayerCombatStats.get(player).setBaseValue(FOCUS_STAT, 10.0);

        PlayerResources.set(player, "focus", 1.0, 10.0);
        PlayerResources.set(player, FOCUS_RESOURCE, 5.0);

        helper.assertValueEqual(PlayerResources.getAmount(player, "focus", 10.0), 1.0,
                "Compatibility buckets should keep their explicit short-path state");
        helper.assertValueEqual(PlayerResources.getAmount(player, FOCUS_RESOURCE), 5.0,
                "Registered namespaced resources should keep their own canonical runtime state");
        helper.succeed();
    }

    /**
     * Verifies that supports() only reports registry-backed runtime resources while explicit-max buckets remain available.
     */
    @GameTest
    public void supportsOnlyReportsRegistryBackedRuntimeResources(GameTestHelper helper) {
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);

        helper.assertTrue(PlayerResources.supports(PlayerResourceIds.MANA),
                "The registered mana resource should be reported as supported by the runtime");
        helper.assertTrue(PlayerResources.supports(FOCUS_RESOURCE),
                "Registered namespaced resources should be reported as supported by the runtime");
        helper.assertFalse(PlayerResources.supports("rage"),
                "Ad-hoc compatibility buckets should not be reported as supported runtime resources");

        PlayerResources.set(player, "rage", 3.0, 10.0);
        helper.assertValueEqual(PlayerResources.getAmount(player, "rage", 10.0), 3.0,
                "Explicit-max compatibility buckets should remain readable and writable even when supports() is false");
        helper.succeed();
    }

    /**
     * Verifies that named resource storage keeps independent resource tracks per player.
     */
    @GameTest
    public void namedResourcesStayIndependent(GameTestHelper helper) {
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);

        PlayerResources.set(player, "rage", 3.0, 10.0);
        PlayerResources.setMana(player, 5.0, 20.0);
        PlayerResources.add(player, "rage", 4.0, 10.0);

        helper.assertValueEqual(PlayerResources.getAmount(player, "rage", 10.0), 7.0,
                "Generic named resource state should update independently");
        helper.assertValueEqual(PlayerResources.getMana(player, 20.0), 5.0,
                "Updating a different named resource should not mutate mana");

        PlayerResources.spend(player, "rage", 2.0, 10.0)
                .orElseThrow(() -> helper.assertionException("Spending the named resource should succeed"));

        helper.assertValueEqual(PlayerResources.getAmount(player, "rage", 10.0), 5.0,
                "Named resource spend should reduce only that resource track");
        helper.assertValueEqual(PlayerResources.getMana(player, 20.0), 5.0,
                "Named resource spend should not affect mana");
        helper.succeed();
    }

    /**
     * Verifies that the legacy mana facade reads and writes through the generic mana resource key.
     */
    @GameTest
    public void manaFacadeUsesNamedManaResource(GameTestHelper helper) {
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);

        PlayerResources.set(player, PlayerResourceIds.MANA, 9.0, 20.0);
        helper.assertValueEqual(PlayerResources.getMana(player, 20.0), 9.0,
                "Legacy mana reads should resolve from the generic mana resource track");

        PlayerResources.addMana(player, 3.0, 20.0);
        helper.assertValueEqual(PlayerResources.getAmount(player, PlayerResourceIds.MANA, 20.0), 12.0,
                "Legacy mana writes should update the generic mana resource track");
        helper.succeed();
    }
}
