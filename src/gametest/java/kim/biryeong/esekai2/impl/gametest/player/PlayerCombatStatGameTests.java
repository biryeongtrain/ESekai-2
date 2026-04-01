package kim.biryeong.esekai2.impl.gametest.player;

import kim.biryeong.esekai2.api.item.affix.AffixDefinition;
import kim.biryeong.esekai2.api.item.affix.AffixRegistries;
import kim.biryeong.esekai2.api.item.affix.Affixes;
import kim.biryeong.esekai2.api.item.affix.ItemAffixState;
import kim.biryeong.esekai2.api.item.affix.ItemAffixes;
import kim.biryeong.esekai2.api.item.affix.RolledAffix;
import kim.biryeong.esekai2.api.item.type.ItemFamily;
import kim.biryeong.esekai2.api.item.socket.SocketedEquipmentSlot;
import kim.biryeong.esekai2.api.level.LevelProgressionDefinition;
import kim.biryeong.esekai2.api.level.LevelRegistries;
import kim.biryeong.esekai2.api.player.level.PlayerLevels;
import kim.biryeong.esekai2.api.player.stat.PlayerCombatStats;
import kim.biryeong.esekai2.api.stat.combat.CombatStats;
import kim.biryeong.esekai2.api.stat.definition.StatDefinition;
import kim.biryeong.esekai2.api.stat.holder.StatHolder;
import kim.biryeong.esekai2.api.stat.holder.StatHolders;
import kim.biryeong.esekai2.api.stat.modifier.StatModifier;
import kim.biryeong.esekai2.impl.stat.registry.StatRegistryAccess;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Verifies that player combat stats lazily rebuild from progression and equipped affix sources.
 */
public final class PlayerCombatStatGameTests {
    private static final Identifier DEFAULT_PROGRESSION_ID = Identifier.fromNamespaceAndPath("esekai2", "default");
    private static final Identifier TRINKET_LIFE_T1_AFFIX_ID = Identifier.fromNamespaceAndPath("esekai2", "trinket_life_t1");
    private static final Identifier TRINKET_LIFE_T2_AFFIX_ID = Identifier.fromNamespaceAndPath("esekai2", "trinket_life_t2");
    private static final Identifier WEAPON_ACCURACY_T1_AFFIX_ID = Identifier.fromNamespaceAndPath("esekai2", "weapon_accuracy_t1");

    /**
     * Verifies that level rewards defined in player_progression accumulate into the player's combat stat holder.
     */
    @GameTest
    public void playerCombatStatsAccumulateProgressionRewards(GameTestHelper helper) {
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        PlayerLevels.setLevel(player, 3);

        StatHolder expected = expectedProgressionHolder(helper, 3);

        assertMatchingResolved(helper, PlayerCombatStats.get(player), expected, CombatStats.LIFE, "Life should include cumulative progression rewards");
        assertMatchingResolved(helper, PlayerCombatStats.get(player), expected, CombatStats.MANA,
                "Mana should include cumulative progression rewards");
        helper.succeed();
    }

    /**
     * Verifies that equipped GLOBAL affixes contribute to player combat stats while LOCAL affixes stay item-local.
     */
    @GameTest
    public void playerCombatStatsProjectOnlyGlobalEquippedAffixes(GameTestHelper helper) {
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        player.setItemSlot(SocketedEquipmentSlot.MAIN_HAND.equipmentSlot(), new ItemStack(Items.STICK));
        player.setItemSlot(SocketedEquipmentSlot.OFF_HAND.equipmentSlot(), new ItemStack(Items.WOODEN_SWORD));
        ItemStack mainHand = player.getItemBySlot(SocketedEquipmentSlot.MAIN_HAND.equipmentSlot());
        ItemStack offHand = player.getItemBySlot(SocketedEquipmentSlot.OFF_HAND.equipmentSlot());

        RolledAffix globalLife = roll(helper, TRINKET_LIFE_T1_AFFIX_ID, ItemFamily.TRINKET, 1.0);
        RolledAffix localAccuracy = roll(helper, WEAPON_ACCURACY_T1_AFFIX_ID, ItemFamily.WEAPON, 1.0);
        ItemAffixes.set(mainHand, new ItemAffixState(List.of(globalLife)));
        ItemAffixes.set(offHand, new ItemAffixState(List.of(localAccuracy)));

        StatHolder holder = PlayerCombatStats.get(player);
        StatHolder expected = StatHolders.create(StatRegistryAccess.statRegistry(helper));
        for (StatModifier modifier : globalLife.modifiers()) {
            expected.addModifier(modifier);
        }

        assertMatchingResolved(helper, holder, expected, CombatStats.LIFE, "Global equipped affixes should project into player life");
        helper.assertValueEqual(holder.resolvedValue(CombatStats.ACCURACY), 0.0,
                "Local affixes should remain item-local and not project into the player combat holder");
        helper.succeed();
    }

    /**
     * Verifies that manual runtime base overrides survive source rebuilds triggered by equipment changes.
     */
    @GameTest
    public void manualBaseOverridesSurviveEquipmentDrivenRebuilds(GameTestHelper helper) {
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        player.setItemSlot(SocketedEquipmentSlot.MAIN_HAND.equipmentSlot(), new ItemStack(Items.STICK));
        ItemStack mainHand = player.getItemBySlot(SocketedEquipmentSlot.MAIN_HAND.equipmentSlot());
        ItemAffixes.set(mainHand, new ItemAffixState(List.of(roll(helper, TRINKET_LIFE_T2_AFFIX_ID, ItemFamily.TRINKET, 0.0))));

        StatHolder holder = PlayerCombatStats.get(player);
        holder.setBaseValue(CombatStats.MANA, 20.0);
        double firstLife = holder.resolvedValue(CombatStats.LIFE);

        ItemAffixes.set(mainHand, new ItemAffixState(List.of(roll(helper, TRINKET_LIFE_T1_AFFIX_ID, ItemFamily.TRINKET, 1.0))));
        double rebuiltLife = holder.resolvedValue(CombatStats.LIFE);

        helper.assertValueEqual(holder.resolvedValue(CombatStats.MANA), 20.0,
                "Manual runtime base overrides should survive lazy rebuilds");
        helper.assertTrue(rebuiltLife > firstLife,
                "Changing equipped affix state should trigger a rebuild that updates projected life");
        helper.succeed();
    }

    /**
     * Verifies that mutating player progression dirties an existing holder and updates the same runtime facade on reuse.
     */
    @GameTest
    public void playerCombatStatsRebuildWhenProgressionChanges(GameTestHelper helper) {
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        StatHolder holder = PlayerCombatStats.get(player);
        double baselineLife = holder.resolvedValue(CombatStats.LIFE);

        PlayerLevels.addExperience(player, 100L);

        helper.assertValueEqual(holder.resolvedValue(CombatStats.LIFE), baselineLife + 5.0,
                "Crossing the level two threshold should dirty and rebuild the existing combat stat holder");
        helper.succeed();
    }

    /**
     * Verifies that progression rewards and equipped global affixes accumulate into the same rebuilt player stat holder.
     */
    @GameTest
    public void playerCombatStatsAccumulateProgressionAndEquipmentSources(GameTestHelper helper) {
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        PlayerLevels.setLevel(player, 3);
        player.setItemSlot(SocketedEquipmentSlot.MAIN_HAND.equipmentSlot(), new ItemStack(Items.STICK));
        ItemStack mainHand = player.getItemBySlot(SocketedEquipmentSlot.MAIN_HAND.equipmentSlot());

        RolledAffix globalLife = roll(helper, TRINKET_LIFE_T1_AFFIX_ID, ItemFamily.TRINKET, 1.0);
        ItemAffixes.set(mainHand, new ItemAffixState(List.of(globalLife)));

        StatHolder expected = expectedProgressionHolder(helper, 3);
        for (StatModifier modifier : globalLife.modifiers()) {
            expected.addModifier(modifier);
        }

        assertMatchingResolved(helper, PlayerCombatStats.get(player), expected, CombatStats.LIFE,
                "Life should include both accumulated progression rewards and equipped global affixes");
        assertMatchingResolved(helper, PlayerCombatStats.get(player), expected, CombatStats.MANA,
                "Mana should continue to include progression rewards alongside equipment contributions");
        helper.succeed();
    }

    private static StatHolder expectedProgressionHolder(GameTestHelper helper, int level) {
        StatHolder holder = StatHolders.create(StatRegistryAccess.statRegistry(helper));
        LevelProgressionDefinition progression = progressionRegistry(helper).getOptional(DEFAULT_PROGRESSION_ID)
                .orElseThrow(() -> helper.assertionException("Default progression should decode successfully"));
        for (int currentLevel = 1; currentLevel <= level; currentLevel++) {
            for (StatModifier modifier : progression.grantedModifiers(currentLevel)) {
                holder.addModifier(modifier);
            }
        }
        return holder;
    }

    private static RolledAffix roll(GameTestHelper helper, Identifier affixId, ItemFamily family, double normalizedRoll) {
        AffixDefinition definition = affixRegistry(helper).getOptional(affixId)
                .orElseThrow(() -> helper.assertionException("Affix should decode successfully: " + affixId));
        return Affixes.roll(affixId, family, 20, definition, normalizedRoll);
    }

    private static void assertMatchingResolved(
            GameTestHelper helper,
            StatHolder actual,
            StatHolder expected,
            ResourceKey<StatDefinition> stat,
            String message
    ) {
        helper.assertValueEqual(actual.resolvedValue(stat), expected.resolvedValue(stat), message);
    }

    private static Registry<AffixDefinition> affixRegistry(GameTestHelper helper) {
        return helper.getLevel().getServer().registryAccess().lookupOrThrow(AffixRegistries.AFFIX);
    }

    private static Registry<LevelProgressionDefinition> progressionRegistry(GameTestHelper helper) {
        return helper.getLevel().getServer().registryAccess().lookupOrThrow(LevelRegistries.PLAYER_PROGRESSION);
    }
}
