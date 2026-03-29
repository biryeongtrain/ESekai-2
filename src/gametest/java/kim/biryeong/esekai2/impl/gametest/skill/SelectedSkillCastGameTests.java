package kim.biryeong.esekai2.impl.gametest.skill;

import kim.biryeong.esekai2.api.item.socket.SocketLinkGroup;
import kim.biryeong.esekai2.api.item.socket.SocketSlotType;
import kim.biryeong.esekai2.api.item.socket.SocketedEquipmentSlot;
import kim.biryeong.esekai2.api.item.socket.SocketedSkillItemState;
import kim.biryeong.esekai2.api.item.socket.SocketedSkillRef;
import kim.biryeong.esekai2.api.item.socket.SocketedSkills;
import kim.biryeong.esekai2.api.player.skill.PlayerActiveSkills;
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
