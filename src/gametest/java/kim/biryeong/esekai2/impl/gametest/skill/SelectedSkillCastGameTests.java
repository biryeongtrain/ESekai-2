package kim.biryeong.esekai2.impl.gametest.skill;

import kim.biryeong.esekai2.api.item.socket.SocketLinkGroup;
import kim.biryeong.esekai2.api.item.socket.SocketSlotType;
import kim.biryeong.esekai2.api.item.socket.SocketedEquipmentSlot;
import kim.biryeong.esekai2.api.item.socket.SocketedSkillItemState;
import kim.biryeong.esekai2.api.item.socket.SocketedSkillRef;
import kim.biryeong.esekai2.api.item.socket.SocketedSkills;
import kim.biryeong.esekai2.api.player.resource.PlayerResources;
import kim.biryeong.esekai2.api.player.skill.PlayerActiveSkills;
import kim.biryeong.esekai2.api.player.skill.PlayerSkillBurstState;
import kim.biryeong.esekai2.api.player.skill.PlayerSkillBursts;
import kim.biryeong.esekai2.api.player.skill.PlayerSkillCharges;
import kim.biryeong.esekai2.api.player.skill.PlayerSkillCooldowns;
import kim.biryeong.esekai2.api.player.skill.SelectedActiveSkillRef;
import kim.biryeong.esekai2.api.skill.calculation.SkillCalculationDefinition;
import kim.biryeong.esekai2.api.skill.calculation.SkillCalculationLookup;
import kim.biryeong.esekai2.api.skill.execution.PreparedDamageAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedProjectileAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSandstormParticleAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSoundAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSummonAtSightAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSummonBlockAction;
import kim.biryeong.esekai2.api.skill.execution.SelectedSkillCastResult;
import kim.biryeong.esekai2.api.skill.execution.SelectedSkillUseResult;
import kim.biryeong.esekai2.api.skill.execution.SkillExecutionContext;
import kim.biryeong.esekai2.api.skill.execution.SkillExecutionHooks;
import kim.biryeong.esekai2.api.skill.execution.SkillUseContext;
import kim.biryeong.esekai2.api.skill.execution.Skills;
import kim.biryeong.esekai2.api.damage.calculation.DamageCalculationResult;
import kim.biryeong.esekai2.api.stat.combat.CombatStats;
import kim.biryeong.esekai2.api.stat.holder.StatHolder;
import kim.biryeong.esekai2.api.stat.holder.StatHolders;
import kim.biryeong.esekai2.api.stat.modifier.ConditionalStatModifier;
import kim.biryeong.esekai2.impl.skill.registry.SkillCalculationRegistryAccess;
import kim.biryeong.esekai2.impl.stat.registry.StatRegistryAccess;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Verifies the server-side selected active skill flow for socket-backed casting.
 */
public final class SelectedSkillCastGameTests {
    private static final Identifier FIREBALL_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "fireball");
    private static final Identifier BASIC_STRIKE_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "basic_strike");
    private static final Identifier BATTLE_FOCUS_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "battle_focus");
    private static final Identifier BURST_FOCUS_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "burst_focus");
    private static final Identifier BURST_STRIKE_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "burst_strike");
    private static final Identifier BURST_RESERVE_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "burst_reserve");
    private static final Identifier CHARGED_FOCUS_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "charged_focus");
    private static final Identifier CHARGED_SURGE_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "charged_surge");
    private static final Identifier OVERWORLD_BARRIER_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "overworld_barrier");
    private static final Identifier SUPPORT_CAST_ECHO_ID = Identifier.fromNamespaceAndPath("esekai2", "support_cast_echo");
    private static final Identifier SUPPORT_COST_BOOST_ID = Identifier.fromNamespaceAndPath("esekai2", "support_cost_boost");
    private static final Identifier MISSING_SUPPORT_ID = Identifier.fromNamespaceAndPath("esekai2", "missing_support");
    private static final Identifier MISSING_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "missing_skill");

    /**
     * Verifies that selected active skill state can be stored, queried, and cleared for one server player.
     */
    @GameTest
    public void playerSelectedActiveSkillStateCanBeStoredAndCleared(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        SelectedActiveSkillRef selected = new SelectedActiveSkillRef(SocketedEquipmentSlot.MAIN_HAND, FIREBALL_SKILL_ID);

        PlayerActiveSkills.select(player, selected);
        helper.assertValueEqual(PlayerActiveSkills.get(player).orElseThrow(), selected, "Stored selection should be readable from server-side state");

        PlayerActiveSkills.clear(player);
        helper.assertTrue(PlayerActiveSkills.get(player).isEmpty(), "Clearing the selected active skill should remove the saved state");
        helper.succeed();
    }

    /**
     * Verifies that the selected equipment slot disambiguates duplicate active skill ids across multiple equipped items.
     */
    @GameTest
    public void prepareSelectedUseResolvesDuplicateSkillIdsByEquipmentSlot(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemSlot(SocketedEquipmentSlot.MAIN_HAND.equipmentSlot(), fireballStack(List.of(supportRef(1, SUPPORT_COST_BOOST_ID))));
        player.setItemSlot(SocketedEquipmentSlot.OFF_HAND.equipmentSlot(), fireballStack(List.of()));

        PlayerActiveSkills.select(player, new SelectedActiveSkillRef(SocketedEquipmentSlot.MAIN_HAND, FIREBALL_SKILL_ID));
        SelectedSkillUseResult mainHandResult = Skills.prepareSelectedUse(player, skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99));

        PlayerActiveSkills.select(player, new SelectedActiveSkillRef(SocketedEquipmentSlot.OFF_HAND, FIREBALL_SKILL_ID));
        SelectedSkillUseResult offHandResult = Skills.prepareSelectedUse(player, skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99));

        helper.assertTrue(mainHandResult.success(), "Selected main-hand fireball should prepare successfully");
        helper.assertTrue(offHandResult.success(), "Selected off-hand fireball should prepare successfully");
        helper.assertValueEqual(mainHandResult.preparedUse().orElseThrow().resourceCost(), 16.0, "Main-hand linked support should increase prepared resource cost");
        helper.assertValueEqual(offHandResult.preparedUse().orElseThrow().resourceCost(), 12.0, "Off-hand duplicate skill id should resolve independently without the main-hand support");
        helper.succeed();
    }

    /**
     * Verifies that unlinked supports on the same item are ignored when preparing the selected active skill.
     */
    @GameTest
    public void prepareSelectedUseOnlyInjectsSupportsFromActiveSkillLinkGroup(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ItemStack stack = new ItemStack(Items.BLAZE_ROD);
        SocketedSkills.set(stack, new SocketedSkillItemState(
                Optional.of(FIREBALL_SKILL_ID),
                3,
                List.of(
                        new SocketLinkGroup(0, List.of(0, 1)),
                        new SocketLinkGroup(1, List.of(2))
                ),
                List.of(
                        new SocketedSkillRef(0, SocketSlotType.SKILL, FIREBALL_SKILL_ID),
                        new SocketedSkillRef(1, SocketSlotType.SUPPORT, SUPPORT_COST_BOOST_ID),
                        new SocketedSkillRef(2, SocketSlotType.SUPPORT, SUPPORT_COST_BOOST_ID)
                )
        ));
        player.setItemSlot(SocketedEquipmentSlot.MAIN_HAND.equipmentSlot(), stack);
        PlayerActiveSkills.select(player, new SelectedActiveSkillRef(SocketedEquipmentSlot.MAIN_HAND, FIREBALL_SKILL_ID));

        SelectedSkillUseResult result = Skills.prepareSelectedUse(player, skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99));

        helper.assertTrue(result.success(), "Selected skill with a valid active link group should prepare successfully");
        helper.assertValueEqual(result.preparedUse().orElseThrow().resourceCost(), 16.0, "Only the support in the active skill link group should apply");
        helper.succeed();
    }

    /**
     * Verifies that missing support definitions degrade to warnings while still preparing the selected active skill.
     */
    @GameTest
    public void prepareSelectedUseWarnsAndContinuesWhenLinkedSupportIsMissing(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemSlot(SocketedEquipmentSlot.MAIN_HAND.equipmentSlot(), fireballStack(List.of(supportRef(1, MISSING_SUPPORT_ID))));
        PlayerActiveSkills.select(player, new SelectedActiveSkillRef(SocketedEquipmentSlot.MAIN_HAND, FIREBALL_SKILL_ID));

        SelectedSkillUseResult result = Skills.prepareSelectedUse(player, skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99));

        helper.assertTrue(result.success(), "Missing support definitions should not fail selected active skill preparation");
        helper.assertValueEqual(result.preparedUse().orElseThrow().resourceCost(), 12.0, "Missing support definitions should be ignored instead of modifying the skill");
        helper.assertTrue(result.warnings().stream().anyMatch(warning -> warning.contains(MISSING_SUPPORT_ID.toString())),
                "Missing linked support should surface a warning");
        helper.succeed();
    }

    /**
     * Verifies that missing selection, stale equipment, and missing active skill definitions fail safely without throwing.
     */
    @GameTest
    public void prepareSelectedUseFailsSafelyForMissingOrStaleSelection(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();

        SelectedSkillUseResult noSelection = Skills.prepareSelectedUse(player, skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99));
        helper.assertTrue(!noSelection.success(), "Preparing without a selected active skill should fail safely");

        player.setItemSlot(SocketedEquipmentSlot.MAIN_HAND.equipmentSlot(), fireballStack(List.of()));
        PlayerActiveSkills.select(player, new SelectedActiveSkillRef(SocketedEquipmentSlot.MAIN_HAND, FIREBALL_SKILL_ID));
        player.setItemSlot(SocketedEquipmentSlot.MAIN_HAND.equipmentSlot(), ItemStack.EMPTY);

        SelectedSkillUseResult staleSelection = Skills.prepareSelectedUse(player, skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99));
        helper.assertTrue(!staleSelection.success(), "Preparing with an unequipped selected slot should fail safely");

        player.setItemSlot(SocketedEquipmentSlot.MAIN_HAND.equipmentSlot(), missingSkillStack());
        PlayerActiveSkills.select(player, new SelectedActiveSkillRef(SocketedEquipmentSlot.MAIN_HAND, MISSING_SKILL_ID));
        SelectedSkillUseResult missingActiveSkill = Skills.prepareSelectedUse(player, skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99));

        helper.assertTrue(!missingActiveSkill.success(), "Preparing with an unresolved active skill definition should fail safely");
        helper.assertTrue(missingActiveSkill.warnings().stream().anyMatch(warning -> warning.contains(MISSING_SKILL_ID.toString())),
                "Missing active skill definition should be reported in warnings");
        helper.succeed();
    }

    /**
     * Verifies that casting the selected active skill runs the normal execute-on-cast server path against the equipped socket item.
     */
    @GameTest
    public void castSelectedSkillExecutesServerCastPath(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemSlot(SocketedEquipmentSlot.MAIN_HAND.equipmentSlot(), basicStrikeStack());
        PlayerActiveSkills.select(player, new SelectedActiveSkillRef(SocketedEquipmentSlot.MAIN_HAND, BASIC_STRIKE_SKILL_ID));
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(3.0, 2.0, 3.0));
        player.snapTo(zombie.getX(), zombie.getY(), zombie.getZ() - 2.0, 0.0F, 0.0F);

        SelectedSkillCastResult result = Skills.castSelectedSkill(
                player,
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.25),
                Optional.of(zombie)
        );

        helper.assertTrue(result.success(), "Selected active skill cast should succeed when the equipped item resolves correctly");
        helper.assertValueEqual(result.executionResult().orElseThrow().executedActions(), 2, "Basic strike cast should execute the expected on-cast actions");
        helper.assertTrue(zombie.getHealth() < zombie.getMaxHealth(), "Selected active skill cast should apply its server-side hit effect");
        helper.succeed();
    }

    /**
     * Verifies that selected socket-backed casts also execute support-appended on-cast rules.
     */
    @GameTest
    public void castSelectedSkillIncludesSupportAppendedOnCastRules(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemSlot(
                SocketedEquipmentSlot.MAIN_HAND.equipmentSlot(),
                fireballStack(List.of(supportRef(1, SUPPORT_CAST_ECHO_ID)))
        );
        PlayerActiveSkills.select(player, new SelectedActiveSkillRef(SocketedEquipmentSlot.MAIN_HAND, FIREBALL_SKILL_ID));
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(3.0, 2.0, 4.0));
        player.snapTo(zombie.getX(), zombie.getY(), zombie.getZ() - 2.0, 0.0F, 0.0F);

        RecordingHooks hooks = new RecordingHooks();
        SelectedSkillCastResult result = Skills.castSelectedSkill(
                player,
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.25),
                Optional.of(zombie),
                hooks
        );

        helper.assertTrue(result.success(), "Selected fireball cast should still succeed when a rule-append support is linked");
        helper.assertValueEqual(result.executionResult().orElseThrow().executedActions(), 4, "Selected cast should include the appended HAS_TARGET support rule");
        helper.assertValueEqual(hooks.soundCount(), 2, "Selected cast should execute both the base and support-appended sound actions");
        helper.succeed();
    }

    /**
     * Verifies that selected casts spend mana and start cooldown state after a successful runtime execution.
     */
    @GameTest
    public void castSelectedSkillSpendsManaAndStartsCooldown(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemSlot(SocketedEquipmentSlot.MAIN_HAND.equipmentSlot(), battleFocusStack());
        PlayerActiveSkills.select(player, new SelectedActiveSkillRef(SocketedEquipmentSlot.MAIN_HAND, BATTLE_FOCUS_SKILL_ID));

        StatHolder attacker = newHolder(helper);
        attacker.setBaseValue(CombatStats.MANA, 20.0);
        PlayerResources.setMana(player, 20.0, 20.0);

        SelectedSkillCastResult result = Skills.castSelectedSkill(
                player,
                skillUseContext(helper, attacker, newHolder(helper), 0.0, 0.0),
                Optional.empty()
        );

        helper.assertTrue(result.success(), "Selected cast should succeed when the player has enough mana and no cooldown");
        helper.assertValueEqual(result.executionResult().orElseThrow().executedActions(), 2, "Battle focus should execute both its sound and buff actions");
        helper.assertValueEqual(PlayerResources.getMana(player, 20.0), 12.0, "Successful selected casts should spend the prepared mana cost");
        helper.assertTrue(PlayerSkillCooldowns.isOnCooldown(player, BATTLE_FOCUS_SKILL_ID, helper.getLevel().getGameTime()),
                "Successful selected casts should start the prepared cooldown");
        helper.succeed();
    }

    /**
     * Verifies that selected casts fail safely when the player's current mana is below the prepared cost.
     */
    @GameTest
    public void castSelectedSkillFailsSafelyWhenManaIsInsufficient(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemSlot(SocketedEquipmentSlot.MAIN_HAND.equipmentSlot(), fireballStack(List.of()));
        PlayerActiveSkills.select(player, new SelectedActiveSkillRef(SocketedEquipmentSlot.MAIN_HAND, FIREBALL_SKILL_ID));

        StatHolder attacker = newHolder(helper);
        attacker.setBaseValue(CombatStats.MANA, 20.0);
        PlayerResources.setMana(player, 5.0, 20.0);

        SelectedSkillCastResult result = Skills.castSelectedSkill(
                player,
                skillUseContext(helper, attacker, newHolder(helper), 0.0, 0.99),
                Optional.empty()
        );

        helper.assertTrue(result.success(), "Selected cast should still resolve selection/preparation even when mana blocks runtime execution");
        helper.assertValueEqual(result.executionResult().orElseThrow().executedActions(), 0, "Insufficient mana should block selected casts before runtime actions execute");
        helper.assertTrue(result.warnings().stream().anyMatch(warning -> warning.contains("mana")),
                "Insufficient selected-cast mana should surface a warning");
        helper.assertValueEqual(PlayerResources.getMana(player, 20.0), 5.0, "Blocked selected casts should not spend mana");
        helper.succeed();
    }

    /**
     * Verifies that selected casts fail safely when the active skill is disabled in the current dimension.
     */
    @GameTest
    public void castSelectedSkillFailsSafelyInDisabledDimension(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemSlot(SocketedEquipmentSlot.MAIN_HAND.equipmentSlot(), overworldBarrierStack());
        PlayerActiveSkills.select(player, new SelectedActiveSkillRef(SocketedEquipmentSlot.MAIN_HAND, OVERWORLD_BARRIER_SKILL_ID));

        StatHolder attacker = newHolder(helper);
        attacker.setBaseValue(CombatStats.MANA, 20.0);
        PlayerResources.setMana(player, 20.0, 20.0);

        SelectedSkillCastResult result = Skills.castSelectedSkill(
                player,
                skillUseContext(helper, attacker, newHolder(helper), 0.0, 0.0),
                Optional.empty()
        );

        helper.assertTrue(result.success(), "Selected cast should still return a resolved execution result when the skill is disabled in the current dimension");
        helper.assertValueEqual(result.executionResult().orElseThrow().executedActions(), 0, "Disabled-dimension selected casts should not execute runtime actions");
        helper.assertTrue(result.warnings().stream().anyMatch(warning -> warning.contains("dimension")),
                "Disabled-dimension selected casts should surface a warning");
        helper.assertValueEqual(PlayerResources.getMana(player, 20.0), 20.0, "Disabled-dimension selected casts should not spend mana");
        helper.succeed();
    }

    /**
     * Verifies that selected casts no-op while their player cooldown is still active and do not spend mana twice.
     */
    @GameTest
    public void castSelectedSkillBlocksWhenPlayerCooldownIsActive(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemSlot(SocketedEquipmentSlot.MAIN_HAND.equipmentSlot(), battleFocusStack());
        PlayerActiveSkills.select(player, new SelectedActiveSkillRef(SocketedEquipmentSlot.MAIN_HAND, BATTLE_FOCUS_SKILL_ID));

        StatHolder attacker = newHolder(helper);
        attacker.setBaseValue(CombatStats.MANA, 20.0);
        PlayerResources.setMana(player, 20.0, 20.0);
        SkillUseContext context = skillUseContext(helper, attacker, newHolder(helper), 0.0, 0.0);

        SelectedSkillCastResult firstCast = Skills.castSelectedSkill(player, context, Optional.empty());
        SelectedSkillCastResult secondCast = Skills.castSelectedSkill(player, context, Optional.empty());

        helper.assertValueEqual(firstCast.executionResult().orElseThrow().executedActions(), 2, "The first selected battle focus cast should execute normally");
        helper.assertTrue(secondCast.success(), "Cooldown-blocked selected casts should still return a resolved execution result");
        helper.assertValueEqual(secondCast.executionResult().orElseThrow().executedActions(), 0, "Cooldown-blocked selected casts should not execute runtime actions");
        helper.assertTrue(secondCast.warnings().stream().anyMatch(warning -> warning.contains("cooldown")),
                "Cooldown-blocked selected casts should surface a cooldown warning");
        helper.assertValueEqual(PlayerResources.getMana(player, 20.0), 12.0, "Cooldown-blocked selected casts should not spend mana again");
        helper.assertTrue(PlayerSkillCooldowns.remainingTicks(player, BATTLE_FOCUS_SKILL_ID, helper.getLevel().getGameTime()) > 0L,
                "Selected battle focus should still have remaining cooldown after an immediate recast attempt");
        helper.succeed();
    }

    /**
     * Verifies that a successful selected cast spends one stored charge from a charged skill.
     */
    @GameTest
    public void castSelectedSkillSpendsStoredChargeOnSuccessfulCast(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemSlot(SocketedEquipmentSlot.MAIN_HAND.equipmentSlot(), chargedSurgeStack());
        PlayerActiveSkills.select(player, new SelectedActiveSkillRef(SocketedEquipmentSlot.MAIN_HAND, CHARGED_SURGE_SKILL_ID));

        SelectedSkillCastResult result = Skills.castSelectedSkill(
                player,
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.0),
                Optional.empty()
        );

        helper.assertTrue(result.success(), "Selected charged cast should resolve successfully when a stored charge is available");
        helper.assertValueEqual(result.executionResult().orElseThrow().executedActions(), 1, "Charged surge should execute its single sound action");
        helper.assertValueEqual(PlayerSkillCharges.availableCharges(player, CHARGED_SURGE_SKILL_ID, 1, helper.getLevel().getGameTime()), 0,
                "A successful selected charged cast should consume one stored charge");
        helper.succeed();
    }

    /**
     * Verifies that selected casts no-op when the chosen charged skill has no stored charges remaining.
     */
    @GameTest
    public void castSelectedSkillBlocksAtZeroStoredCharges(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemSlot(SocketedEquipmentSlot.MAIN_HAND.equipmentSlot(), chargedSurgeStack());
        PlayerActiveSkills.select(player, new SelectedActiveSkillRef(SocketedEquipmentSlot.MAIN_HAND, CHARGED_SURGE_SKILL_ID));

        Skills.castSelectedSkill(
                player,
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.0),
                Optional.empty()
        );

        SelectedSkillCastResult blocked = Skills.castSelectedSkill(
                player,
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.0),
                Optional.empty()
        );

        helper.assertTrue(blocked.success(), "Zero-charge selected casts should still return a resolved cast result");
        helper.assertValueEqual(blocked.executionResult().orElseThrow().executedActions(), 0,
                "Zero-charge selected casts should not execute runtime actions");
        helper.assertTrue(blocked.warnings().stream().anyMatch(warning -> warning.contains("charge")),
                "Zero-charge selected casts should surface a charge warning");
        helper.assertValueEqual(PlayerSkillCharges.availableCharges(player, CHARGED_SURGE_SKILL_ID, 1, helper.getLevel().getGameTime()), 0,
                "Zero-charge selected casts should leave stored charges empty");
        helper.succeed();
    }

    /**
     * Verifies that a selected charged skill becomes castable again after its stored charge regenerates over time.
     */
    @GameTest
    public void castSelectedSkillRegeneratesStoredChargeAfterTimeAdvance(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemSlot(SocketedEquipmentSlot.MAIN_HAND.equipmentSlot(), chargedSurgeStack());
        PlayerActiveSkills.select(player, new SelectedActiveSkillRef(SocketedEquipmentSlot.MAIN_HAND, CHARGED_SURGE_SKILL_ID));

        Skills.castSelectedSkill(
                player,
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.0),
                Optional.empty()
        );

        helper.runAfterDelay(11, () -> {
            SelectedSkillCastResult regenerated = Skills.castSelectedSkill(
                    player,
                    skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.0),
                    Optional.empty()
            );

            helper.assertTrue(regenerated.success(), "Selected casts should still resolve after charge regeneration");
            helper.assertTrue(regenerated.executionResult().orElseThrow().executedActions() > 0,
                    "A regenerated selected charged cast should execute successfully after enough game time has elapsed");
            helper.assertValueEqual(PlayerSkillCharges.availableCharges(player, CHARGED_SURGE_SKILL_ID, 1, helper.getLevel().getGameTime()), 0,
                    "Recasting immediately after regen should consume the regenerated charge again");
            helper.succeed();
        });
    }

    /**
     * Verifies that selected casts treat stored charges and cooldown as separate gates on the same charged skill.
     */
    @GameTest
    public void castSelectedSkillKeepsRemainingChargeAcrossCooldownExpiry(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemSlot(SocketedEquipmentSlot.MAIN_HAND.equipmentSlot(), chargedFocusStack());
        PlayerActiveSkills.select(player, new SelectedActiveSkillRef(SocketedEquipmentSlot.MAIN_HAND, CHARGED_FOCUS_SKILL_ID));

        SkillUseContext context = skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.0);
        SelectedSkillCastResult first = Skills.castSelectedSkill(player, context, Optional.empty());
        SelectedSkillCastResult blockedByCooldown = Skills.castSelectedSkill(player, context, Optional.empty());

        helper.assertTrue(first.executionResult().orElseThrow().executedActions() > 0, "The first selected charged focus cast should execute successfully");
        helper.assertValueEqual(PlayerSkillCharges.availableCharges(player, CHARGED_FOCUS_SKILL_ID, 2, helper.getLevel().getGameTime()), 1,
                "The first charged focus cast should leave one stored charge available");
        helper.assertTrue(blockedByCooldown.warnings().stream().anyMatch(warning -> warning.contains("cooldown")),
                "An immediate recast should be blocked by cooldown before the remaining stored charge is used");
        helper.assertValueEqual(PlayerSkillCharges.availableCharges(player, CHARGED_FOCUS_SKILL_ID, 2, helper.getLevel().getGameTime()), 1,
                "Cooldown-blocked selected casts should not spend the remaining stored charge");

        helper.runAfterDelay(6, () -> {
            SelectedSkillCastResult afterCooldown = Skills.castSelectedSkill(player, context, Optional.empty());

            helper.assertTrue(afterCooldown.executionResult().orElseThrow().executedActions() > 0,
                    "Once cooldown expires, the next selected cast should still succeed because another stored charge remains");
            helper.assertValueEqual(PlayerSkillCharges.availableCharges(player, CHARGED_FOCUS_SKILL_ID, 2, helper.getLevel().getGameTime()), 0,
                    "The post-cooldown selected cast should consume the remaining stored charge");
            helper.succeed();
        });
    }

    /**
     * Verifies that mana-blocked selected charged casts leave stored charges unchanged.
     */
    @GameTest
    public void castSelectedSkillDoesNotSpendChargeWhenManaBlocksRuntime(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemSlot(SocketedEquipmentSlot.MAIN_HAND.equipmentSlot(), chargedFocusStack());
        PlayerActiveSkills.select(player, new SelectedActiveSkillRef(SocketedEquipmentSlot.MAIN_HAND, CHARGED_FOCUS_SKILL_ID));

        StatHolder attacker = newHolder(helper);
        attacker.setBaseValue(CombatStats.MANA, 20.0);
        PlayerResources.setMana(player, 2.0, 20.0);
        PlayerSkillCharges.setAvailableCharges(player, CHARGED_FOCUS_SKILL_ID, 2, 2, helper.getLevel().getGameTime());

        SelectedSkillCastResult blocked = Skills.castSelectedSkill(
                player,
                skillUseContext(helper, attacker, newHolder(helper), 0.0, 0.0),
                Optional.empty()
        );

        helper.assertTrue(blocked.success(), "Mana-blocked selected charged casts should still return a resolved cast result");
        helper.assertValueEqual(blocked.executionResult().orElseThrow().executedActions(), 0,
                "Mana-blocked selected charged casts should not execute runtime actions");
        helper.assertTrue(blocked.warnings().stream().anyMatch(warning -> warning.contains("mana")),
                "Mana-blocked selected charged casts should surface a mana warning");
        helper.assertValueEqual(PlayerResources.getMana(player, 20.0), 2.0,
                "Mana-blocked selected charged casts should not spend mana");
        helper.assertValueEqual(PlayerSkillCharges.availableCharges(player, CHARGED_FOCUS_SKILL_ID, 2, helper.getLevel().getGameTime()), 2,
                "Mana-blocked selected charged casts should leave stored charges unchanged");
        helper.succeed();
    }

    /**
     * Verifies that selected casts use a fixed 10-tick burst window for skills with times_to_cast=2, then reopen as a new opener after expiry.
     */
    @GameTest
    public void castSelectedSkillUsesBurstWindowForTimesToCastTwo(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemSlot(SocketedEquipmentSlot.MAIN_HAND.equipmentSlot(), burstStrikeStack());
        PlayerActiveSkills.select(player, new SelectedActiveSkillRef(SocketedEquipmentSlot.MAIN_HAND, BURST_STRIKE_SKILL_ID));
        SkillUseContext context = skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.0);

        SelectedSkillCastResult first = Skills.castSelectedSkill(player, context, Optional.empty());
        SelectedSkillCastResult second = Skills.castSelectedSkill(player, context, Optional.empty());
        SelectedSkillCastResult third = Skills.castSelectedSkill(player, context, Optional.empty());

        helper.assertTrue(first.success(), "The burst opener should resolve successfully for selected casts");
        helper.assertTrue(second.success(), "The follow-up burst cast should resolve successfully inside the active window");
        helper.assertValueEqual(first.executionResult().orElseThrow().executedActions(), 1,
                "The burst opener should execute its single on-cast action");
        helper.assertValueEqual(second.executionResult().orElseThrow().executedActions(), 1,
                "The allowed follow-up should also execute its single on-cast action");
        helper.assertValueEqual(
                PlayerSkillBursts.activeBurst(player, BURST_STRIKE_SKILL_ID, helper.getLevel().getGameTime())
                        .map(PlayerSkillBurstState.SkillBurstEntry::remainingCasts)
                        .orElse(-1),
                0,
                "Two successful casts inside one burst should exhaust the remaining follow-up count"
        );
        helper.assertValueEqual(third.executionResult().orElseThrow().executedActions(), 0,
                "A third selected cast inside the same burst window should be blocked once the burst is exhausted");
        helper.assertTrue(third.warnings().stream().anyMatch(warning -> warning.contains("burst")),
                "An exhausted selected burst should surface a burst warning");

        helper.runAfterDelay(11, () -> {
            SelectedSkillCastResult reopened = Skills.castSelectedSkill(player, context, Optional.empty());

            helper.assertValueEqual(reopened.executionResult().orElseThrow().executedActions(), 1,
                    "After the burst window expires, the next selected cast should open a fresh burst");
            helper.assertValueEqual(
                    PlayerSkillBursts.activeBurst(player, BURST_STRIKE_SKILL_ID, helper.getLevel().getGameTime())
                            .map(PlayerSkillBurstState.SkillBurstEntry::remainingCasts)
                            .orElse(-1),
                    1,
                    "A fresh selected burst opener should restore one remaining follow-up cast for times_to_cast=2"
            );
            helper.succeed();
        });
    }

    /**
     * Verifies that a successful selected cast of a different skill resets the previous burst window immediately.
     */
    @GameTest
    public void castSelectedSkillResetsBurstAfterDifferentSkillSucceeds(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemSlot(SocketedEquipmentSlot.MAIN_HAND.equipmentSlot(), burstStrikeStack());
        player.setItemSlot(SocketedEquipmentSlot.OFF_HAND.equipmentSlot(), battleFocusStack());
        SkillUseContext context = skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.0);

        PlayerActiveSkills.select(player, new SelectedActiveSkillRef(SocketedEquipmentSlot.MAIN_HAND, BURST_STRIKE_SKILL_ID));
        SelectedSkillCastResult firstBurst = Skills.castSelectedSkill(player, context, Optional.empty());
        SelectedSkillCastResult secondBurst = Skills.castSelectedSkill(player, context, Optional.empty());

        PlayerActiveSkills.select(player, new SelectedActiveSkillRef(SocketedEquipmentSlot.OFF_HAND, BATTLE_FOCUS_SKILL_ID));
        SelectedSkillCastResult otherSkill = Skills.castSelectedSkill(player, context, Optional.empty());

        PlayerActiveSkills.select(player, new SelectedActiveSkillRef(SocketedEquipmentSlot.MAIN_HAND, BURST_STRIKE_SKILL_ID));
        SelectedSkillCastResult reopened = Skills.castSelectedSkill(player, context, Optional.empty());
        SelectedSkillCastResult followUp = Skills.castSelectedSkill(player, context, Optional.empty());

        helper.assertValueEqual(firstBurst.executionResult().orElseThrow().executedActions(), 1,
                "The initial burst opener should execute before the reset check");
        helper.assertValueEqual(secondBurst.executionResult().orElseThrow().executedActions(), 1,
                "The second selected burst cast should exhaust the current burst before the reset check");
        helper.assertTrue(otherSkill.executionResult().orElseThrow().executedActions() > 0,
                "A different selected skill must execute successfully to reset the current burst window");
        helper.assertValueEqual(reopened.executionResult().orElseThrow().executedActions(), 1,
                "After another skill succeeds, the original skill should reopen its burst as a fresh opener");
        helper.assertValueEqual(followUp.executionResult().orElseThrow().executedActions(), 1,
                "The reopened selected burst should still allow its follow-up cast");
        helper.succeed();
    }

    /**
     * Verifies that cooldown-blocked selected burst follow-ups keep the remaining burst cast available and do not spend mana or charges twice.
     */
    @GameTest
    public void castSelectedSkillKeepsBurstStateWhenCooldownBlocksFollowUp(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemSlot(SocketedEquipmentSlot.MAIN_HAND.equipmentSlot(), burstReserveStack());
        PlayerActiveSkills.select(player, new SelectedActiveSkillRef(SocketedEquipmentSlot.MAIN_HAND, BURST_RESERVE_SKILL_ID));

        StatHolder attacker = newHolder(helper);
        attacker.setBaseValue(CombatStats.MANA, 20.0);
        PlayerResources.setMana(player, 20.0, 20.0);
        SkillUseContext context = skillUseContext(helper, attacker, newHolder(helper), 0.0, 0.0);

        SelectedSkillCastResult first = Skills.castSelectedSkill(player, context, Optional.empty());
        SelectedSkillCastResult blocked = Skills.castSelectedSkill(player, context, Optional.empty());

        helper.assertValueEqual(first.executionResult().orElseThrow().executedActions(), 1,
                "The first selected burst-reserve cast should execute successfully");
        helper.assertTrue(blocked.warnings().stream().anyMatch(warning -> warning.contains("cooldown")),
                "An immediate burst follow-up should still be blocked by cooldown before the remaining burst cast is consumed");
        helper.assertValueEqual(PlayerResources.getMana(player, 20.0), 16.0,
                "A cooldown-blocked selected burst follow-up should not spend mana again");
        helper.assertValueEqual(PlayerSkillCharges.availableCharges(player, BURST_RESERVE_SKILL_ID, 2, helper.getLevel().getGameTime()), 1,
                "A cooldown-blocked selected burst follow-up should not consume the remaining stored charge");
        helper.assertValueEqual(
                PlayerSkillBursts.activeBurst(player, BURST_RESERVE_SKILL_ID, helper.getLevel().getGameTime())
                        .map(PlayerSkillBurstState.SkillBurstEntry::remainingCasts)
                        .orElse(-1),
                1,
                "A cooldown-blocked selected burst follow-up should leave the remaining burst cast available"
        );

        helper.runAfterDelay(6, () -> {
            SelectedSkillCastResult followUp = Skills.castSelectedSkill(player, context, Optional.empty());

            helper.assertValueEqual(followUp.executionResult().orElseThrow().executedActions(), 1,
                    "Once cooldown expires inside the burst window, the stored follow-up cast should still execute");
            helper.assertValueEqual(PlayerResources.getMana(player, 20.0), 12.0,
                    "The delayed follow-up should spend mana only when it actually executes");
            helper.assertValueEqual(PlayerSkillCharges.availableCharges(player, BURST_RESERVE_SKILL_ID, 2, helper.getLevel().getGameTime()), 0,
                    "The delayed follow-up should consume the remaining stored charge when it executes");
            helper.assertValueEqual(
                    PlayerSkillBursts.activeBurst(player, BURST_RESERVE_SKILL_ID, helper.getLevel().getGameTime())
                            .map(PlayerSkillBurstState.SkillBurstEntry::remainingCasts)
                            .orElse(-1),
                    0,
                    "The delayed selected follow-up should exhaust the burst once it successfully executes"
            );
            helper.succeed();
        });
    }

    /**
     * Verifies that mana-blocked selected burst follow-ups keep the remaining burst cast available.
     */
    @GameTest
    public void castSelectedSkillKeepsBurstStateWhenManaBlocksFollowUp(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemSlot(SocketedEquipmentSlot.MAIN_HAND.equipmentSlot(), burstFocusStack());
        PlayerActiveSkills.select(player, new SelectedActiveSkillRef(SocketedEquipmentSlot.MAIN_HAND, BURST_FOCUS_SKILL_ID));

        StatHolder attacker = newHolder(helper);
        attacker.setBaseValue(CombatStats.MANA, 6.0);
        PlayerResources.setMana(player, 6.0, 6.0);
        SkillUseContext context = skillUseContext(helper, attacker, newHolder(helper), 0.0, 0.0);

        SelectedSkillCastResult first = Skills.castSelectedSkill(player, context, Optional.empty());
        SelectedSkillCastResult blocked = Skills.castSelectedSkill(player, context, Optional.empty());

        helper.assertValueEqual(first.executionResult().orElseThrow().executedActions(), 1,
                "The opener mana-positive selected burst cast should execute successfully");
        helper.assertValueEqual(blocked.executionResult().orElseThrow().executedActions(), 0,
                "A mana-blocked selected follow-up should not execute runtime actions");
        helper.assertTrue(blocked.warnings().stream().anyMatch(warning -> warning.contains("mana")),
                "A mana-blocked selected follow-up should surface a mana warning");
        helper.assertValueEqual(PlayerResources.getMana(player, 6.0), 2.0,
                "A mana-blocked selected follow-up should not spend mana again");
        helper.assertValueEqual(
                PlayerSkillBursts.activeBurst(player, BURST_FOCUS_SKILL_ID, helper.getLevel().getGameTime())
                        .map(PlayerSkillBurstState.SkillBurstEntry::remainingCasts)
                        .orElse(-1),
                1,
                "A mana-blocked selected follow-up should keep the remaining burst cast available"
        );

        PlayerResources.setMana(player, 6.0, 6.0);
        SelectedSkillCastResult recovered = Skills.castSelectedSkill(player, context, Optional.empty());

        helper.assertValueEqual(recovered.executionResult().orElseThrow().executedActions(), 1,
                "Restoring mana inside the burst window should allow the remaining selected follow-up cast");
        helper.assertValueEqual(
                PlayerSkillBursts.activeBurst(player, BURST_FOCUS_SKILL_ID, helper.getLevel().getGameTime())
                        .map(PlayerSkillBurstState.SkillBurstEntry::remainingCasts)
                        .orElse(-1),
                0,
                "The recovered selected follow-up should exhaust the burst when it succeeds"
        );
        helper.succeed();
    }

    private static StatHolder newHolder(GameTestHelper helper) {
        return StatHolders.create(StatRegistryAccess.statRegistry(helper));
    }

    private static SkillUseContext skillUseContext(
            GameTestHelper helper,
            StatHolder attacker,
            StatHolder defender,
            double hitRoll,
            double criticalStrikeRoll
    ) {
        return new SkillUseContext(
                attacker,
                defender,
                List.<ConditionalStatModifier>of(),
                hitRoll,
                criticalStrikeRoll,
                SkillCalculationLookup.fromRegistry(skillCalculationRegistry(helper))
        );
    }

    private static net.minecraft.core.Registry<SkillCalculationDefinition> skillCalculationRegistry(GameTestHelper helper) {
        return SkillCalculationRegistryAccess.skillCalculationRegistry(helper);
    }

    private static ItemStack fireballStack(List<SocketedSkillRef> extraSupports) {
        ItemStack stack = new ItemStack(Items.BLAZE_ROD);
        List<SocketedSkillRef> socketRefs = new java.util.ArrayList<>();
        socketRefs.add(new SocketedSkillRef(0, SocketSlotType.SKILL, FIREBALL_SKILL_ID));
        socketRefs.addAll(extraSupports);
        SocketedSkills.set(stack, new SocketedSkillItemState(
                Optional.of(FIREBALL_SKILL_ID),
                Math.max(1, socketRefs.size()),
                List.of(new SocketLinkGroup(0, socketRefs.stream().map(SocketedSkillRef::socketIndex).toList())),
                List.copyOf(socketRefs)
        ));
        return stack;
    }

    private static ItemStack basicStrikeStack() {
        ItemStack stack = new ItemStack(Items.IRON_SWORD);
        SocketedSkills.set(stack, new SocketedSkillItemState(
                Optional.of(BASIC_STRIKE_SKILL_ID),
                1,
                List.of(new SocketLinkGroup(0, List.of(0))),
                List.of(new SocketedSkillRef(0, SocketSlotType.SKILL, BASIC_STRIKE_SKILL_ID))
        ));
        return stack;
    }

    private static ItemStack battleFocusStack() {
        ItemStack stack = new ItemStack(Items.BLAZE_ROD);
        SocketedSkills.set(stack, new SocketedSkillItemState(
                Optional.of(BATTLE_FOCUS_SKILL_ID),
                1,
                List.of(new SocketLinkGroup(0, List.of(0))),
                List.of(new SocketedSkillRef(0, SocketSlotType.SKILL, BATTLE_FOCUS_SKILL_ID))
        ));
        return stack;
    }

    private static ItemStack burstStrikeStack() {
        ItemStack stack = new ItemStack(Items.IRON_SWORD);
        SocketedSkills.set(stack, new SocketedSkillItemState(
                Optional.of(BURST_STRIKE_SKILL_ID),
                1,
                List.of(new SocketLinkGroup(0, List.of(0))),
                List.of(new SocketedSkillRef(0, SocketSlotType.SKILL, BURST_STRIKE_SKILL_ID))
        ));
        return stack;
    }

    private static ItemStack burstFocusStack() {
        ItemStack stack = new ItemStack(Items.ECHO_SHARD);
        SocketedSkills.set(stack, new SocketedSkillItemState(
                Optional.of(BURST_FOCUS_SKILL_ID),
                1,
                List.of(new SocketLinkGroup(0, List.of(0))),
                List.of(new SocketedSkillRef(0, SocketSlotType.SKILL, BURST_FOCUS_SKILL_ID))
        ));
        return stack;
    }

    private static ItemStack burstReserveStack() {
        ItemStack stack = new ItemStack(Items.AMETHYST_SHARD);
        SocketedSkills.set(stack, new SocketedSkillItemState(
                Optional.of(BURST_RESERVE_SKILL_ID),
                1,
                List.of(new SocketLinkGroup(0, List.of(0))),
                List.of(new SocketedSkillRef(0, SocketSlotType.SKILL, BURST_RESERVE_SKILL_ID))
        ));
        return stack;
    }

    private static ItemStack chargedFocusStack() {
        ItemStack stack = new ItemStack(Items.AMETHYST_SHARD);
        SocketedSkills.set(stack, new SocketedSkillItemState(
                Optional.of(CHARGED_FOCUS_SKILL_ID),
                1,
                List.of(new SocketLinkGroup(0, List.of(0))),
                List.of(new SocketedSkillRef(0, SocketSlotType.SKILL, CHARGED_FOCUS_SKILL_ID))
        ));
        return stack;
    }

    private static ItemStack chargedSurgeStack() {
        ItemStack stack = new ItemStack(Items.AMETHYST_CLUSTER);
        SocketedSkills.set(stack, new SocketedSkillItemState(
                Optional.of(CHARGED_SURGE_SKILL_ID),
                1,
                List.of(new SocketLinkGroup(0, List.of(0))),
                List.of(new SocketedSkillRef(0, SocketSlotType.SKILL, CHARGED_SURGE_SKILL_ID))
        ));
        return stack;
    }

    private static ItemStack overworldBarrierStack() {
        ItemStack stack = new ItemStack(Items.AMETHYST_SHARD);
        SocketedSkills.set(stack, new SocketedSkillItemState(
                Optional.of(OVERWORLD_BARRIER_SKILL_ID),
                1,
                List.of(new SocketLinkGroup(0, List.of(0))),
                List.of(new SocketedSkillRef(0, SocketSlotType.SKILL, OVERWORLD_BARRIER_SKILL_ID))
        ));
        return stack;
    }

    private static ItemStack missingSkillStack() {
        ItemStack stack = new ItemStack(Items.STICK);
        SocketedSkills.set(stack, new SocketedSkillItemState(
                Optional.of(MISSING_SKILL_ID),
                1,
                List.of(new SocketLinkGroup(0, List.of(0))),
                List.of(new SocketedSkillRef(0, SocketSlotType.SKILL, MISSING_SKILL_ID))
        ));
        return stack;
    }

    private static SocketedSkillRef supportRef(int socketIndex, Identifier supportId) {
        return new SocketedSkillRef(socketIndex, SocketSlotType.SUPPORT, supportId);
    }

    private static final class RecordingHooks implements SkillExecutionHooks {
        private int soundCount;

        private int soundCount() {
            return soundCount;
        }

        @Override
        public boolean playSound(SkillExecutionContext context, List<Entity> targets, PreparedSoundAction action) {
            soundCount++;
            return true;
        }

        @Override
        public Optional<DamageCalculationResult> applyDamage(SkillExecutionContext context, List<Entity> targets, PreparedDamageAction action) {
            return Optional.empty();
        }

        @Override
        public Optional<Entity> spawnProjectile(SkillExecutionContext context, List<Entity> targets, PreparedProjectileAction action) {
            return Optional.of(context.source());
        }

        @Override
        public Optional<Entity> spawnSummonAtSight(SkillExecutionContext context, List<Entity> targets, PreparedSummonAtSightAction action) {
            return Optional.of(context.source());
        }

        @Override
        public boolean placeBlock(SkillExecutionContext context, List<Entity> targets, PreparedSummonBlockAction action) {
            return true;
        }

        @Override
        public boolean emitSandstormParticle(SkillExecutionContext context, List<Entity> targets, PreparedSandstormParticleAction action) {
            return true;
        }
    }
}
