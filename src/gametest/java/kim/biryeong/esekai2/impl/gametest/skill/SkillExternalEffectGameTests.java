package kim.biryeong.esekai2.impl.gametest.skill;

import kim.biryeong.esekai2.api.ailment.AilmentType;
import kim.biryeong.esekai2.api.ailment.Ailments;
import kim.biryeong.esekai2.api.item.socket.SocketLinkGroup;
import kim.biryeong.esekai2.api.item.socket.SocketSlotType;
import kim.biryeong.esekai2.api.item.socket.SocketedEquipmentSlot;
import kim.biryeong.esekai2.api.item.socket.SocketedSkillItemState;
import kim.biryeong.esekai2.api.item.socket.SocketedSkillRef;
import kim.biryeong.esekai2.api.item.socket.SocketedSkills;
import kim.biryeong.esekai2.api.monster.stat.MonsterStats;
import kim.biryeong.esekai2.api.player.skill.PlayerActiveSkills;
import kim.biryeong.esekai2.api.player.skill.SelectedActiveSkillRef;
import kim.biryeong.esekai2.api.skill.calculation.SkillCalculationDefinition;
import kim.biryeong.esekai2.api.skill.calculation.SkillCalculationLookup;
import kim.biryeong.esekai2.api.skill.definition.SkillAttached;
import kim.biryeong.esekai2.api.skill.definition.SkillConfig;
import kim.biryeong.esekai2.api.skill.definition.SkillDefinition;
import kim.biryeong.esekai2.api.skill.definition.SkillRegistries;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillAction;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillActionType;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillRule;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillTargetSelector;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillTargetType;
import kim.biryeong.esekai2.api.skill.effect.MobEffectRefreshPolicy;
import kim.biryeong.esekai2.api.skill.execution.PreparedApplyBuffAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedApplyDotAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedRemoveEffectAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSkillAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSkillUse;
import kim.biryeong.esekai2.api.skill.execution.SelectedSkillCastResult;
import kim.biryeong.esekai2.api.skill.execution.SelectedSkillUseResult;
import kim.biryeong.esekai2.api.skill.execution.SkillExecutionContext;
import kim.biryeong.esekai2.api.skill.execution.SkillExecutionResult;
import kim.biryeong.esekai2.api.skill.execution.SkillUseContext;
import kim.biryeong.esekai2.api.skill.execution.Skills;
import kim.biryeong.esekai2.api.skill.support.SkillSupportDefinition;
import kim.biryeong.esekai2.api.stat.holder.StatHolder;
import kim.biryeong.esekai2.api.stat.holder.StatHolders;
import kim.biryeong.esekai2.api.stat.modifier.ConditionalStatModifier;
import kim.biryeong.esekai2.impl.skill.registry.SkillCalculationRegistryAccess;
import kim.biryeong.esekai2.impl.stat.registry.StatRegistryAccess;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Verifies buff and generic damage-over-time skill effects on the typed skill graph.
 */
public final class SkillExternalEffectGameTests {
    private static final Identifier BATTLE_FOCUS_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "battle_focus");
    private static final Identifier CRIPPLING_HEX_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "crippling_hex");
    private static final Identifier CLEANSE_FOCUS_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "cleanse_focus");
    private static final Identifier PURGING_HEX_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "purging_hex");
    private static final Identifier VENOM_STRIKE_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "venom_strike");
    private static final Identifier SEARING_BRAND_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "searing_brand");
    private static final Identifier SUPPORT_LASTING_HEX_ID = Identifier.fromNamespaceAndPath("esekai2", "support_lasting_hex");
    private static final Identifier SUPPORT_QUICK_FOCUS_ID = Identifier.fromNamespaceAndPath("esekai2", "support_quick_focus");
    private static final Identifier SUPPORT_TOXIC_CLEANSE_ID = Identifier.fromNamespaceAndPath("esekai2", "support_toxic_cleanse");
    private static final Identifier SUPPORT_LINGERING_BRAND_ID = Identifier.fromNamespaceAndPath("esekai2", "support_lingering_brand");
    private static final Identifier BATTLE_FOCUS_EFFECT_ID = Identifier.fromNamespaceAndPath("minecraft", "speed");
    private static final Identifier CRIPPLING_HEX_EFFECT_ID = Identifier.fromNamespaceAndPath("minecraft", "slowness");
    private static final Identifier POISON_EFFECT_ID = AilmentType.POISON.effectId();

    /**
     * Verifies that the sample MobEffect, cleanse, dot skill, and support fixtures load into their registries.
     */
    @GameTest
    public void registryLoadsBuffDotAndSupportFixtures(GameTestHelper helper) {
        helper.assertTrue(skillRegistry(helper).containsKey(BATTLE_FOCUS_SKILL_ID), "Battle focus should load into the skill registry");
        helper.assertTrue(skillRegistry(helper).containsKey(CRIPPLING_HEX_SKILL_ID), "Crippling hex should load into the skill registry");
        helper.assertTrue(skillRegistry(helper).containsKey(CLEANSE_FOCUS_SKILL_ID), "Cleanse focus should load into the skill registry");
        helper.assertTrue(skillRegistry(helper).containsKey(PURGING_HEX_SKILL_ID), "Purging hex should load into the skill registry");
        helper.assertTrue(skillRegistry(helper).containsKey(SEARING_BRAND_SKILL_ID), "Searing brand should load into the skill registry");
        helper.assertTrue(supportRegistry(helper).containsKey(SUPPORT_LASTING_HEX_ID), "Lasting hex support should load into the support registry");
        helper.assertTrue(supportRegistry(helper).containsKey(SUPPORT_QUICK_FOCUS_ID), "Quick focus support should load into the support registry");
        helper.assertTrue(supportRegistry(helper).containsKey(SUPPORT_TOXIC_CLEANSE_ID), "Toxic cleanse support should load into the support registry");
        helper.assertTrue(supportRegistry(helper).containsKey(SUPPORT_LINGERING_BRAND_ID), "Lingering brand support should load into the support registry");
        helper.succeed();
    }

    /**
     * Verifies that legacy buff, generic effect, and dot actions decode into concrete prepared action payloads.
     */
    @GameTest
    public void prepareUseBuildsEffectAndDotActionsFromConfig(GameTestHelper helper) {
        PreparedSkillUse buffPrepared = Skills.prepareUse(
                battleFocus(helper),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );
        PreparedSkillUse debuffPrepared = Skills.prepareUse(
                cripplingHex(helper),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );
        PreparedSkillUse dotPrepared = Skills.prepareUse(
                searingBrand(helper),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );

        PreparedApplyBuffAction buffAction = (PreparedApplyBuffAction) buffPrepared.onCastActions().stream()
                .filter(PreparedApplyBuffAction.class::isInstance)
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Battle focus should prepare one apply_buff action"));
        PreparedApplyDotAction dotAction = (PreparedApplyDotAction) dotPrepared.onCastActions().stream()
                .filter(PreparedApplyDotAction.class::isInstance)
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Searing brand should prepare one apply_dot action"));
        PreparedApplyBuffAction debuffAction = (PreparedApplyBuffAction) debuffPrepared.onCastActions().stream()
                .filter(PreparedApplyBuffAction.class::isInstance)
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Crippling hex should prepare one apply_effect action"));

        helper.assertValueEqual(buffAction.effectId(), BATTLE_FOCUS_EFFECT_ID, "Prepared buff action should preserve the mob effect id");
        helper.assertValueEqual(buffAction.actionType(), "apply_buff", "Legacy buff action should preserve its prepared action type");
        helper.assertValueEqual(buffAction.durationTicks(), 40, "Prepared buff action should resolve duration_ticks");
        helper.assertValueEqual(debuffAction.effectId(), CRIPPLING_HEX_EFFECT_ID, "Prepared effect action should preserve the mob effect id");
        helper.assertValueEqual(debuffAction.actionType(), "apply_effect", "Prepared generic effect action should expose the shared effect runtime type");
        helper.assertValueEqual(debuffAction.refreshPolicy(), MobEffectRefreshPolicy.ADD_DURATION, "Prepared effect action should preserve refresh_policy");
        helper.assertValueEqual(dotAction.dotId(), "searing_brand", "Prepared dot action should preserve the stable dot id");
        helper.assertValueEqual(dotAction.durationTicks(), 8, "Prepared dot action should resolve duration_ticks");
        helper.assertValueEqual(dotAction.tickIntervalTicks(), 2, "Prepared dot action should resolve tick_interval");
        helper.succeed();
    }

    /**
     * Verifies that remove_effect actions decode into concrete prepared remove payloads and preserve their action type.
     */
    @GameTest
    public void prepareUseBuildsRemoveEffectActionsFromConfig(GameTestHelper helper) {
        PreparedSkillUse selfCleansePrepared = Skills.prepareUse(
                cleanseFocus(helper),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );
        PreparedSkillUse targetCleansePrepared = Skills.prepareUse(
                purgingHex(helper),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );

        PreparedRemoveEffectAction selfCleanseAction = (PreparedRemoveEffectAction) selfCleansePrepared.onCastActions().stream()
                .filter(PreparedRemoveEffectAction.class::isInstance)
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Cleanse focus should prepare one remove_effect action"));
        PreparedRemoveEffectAction targetCleanseAction = (PreparedRemoveEffectAction) targetCleansePrepared.onCastActions().stream()
                .filter(PreparedRemoveEffectAction.class::isInstance)
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Purging hex should prepare one remove_effect action"));

        helper.assertValueEqual(selfCleanseAction.effectId(), BATTLE_FOCUS_EFFECT_ID, "Prepared self-cleanse action should preserve its effect id");
        helper.assertValueEqual(selfCleanseAction.actionType(), "remove_effect", "Prepared self-cleanse action should preserve remove_effect type");
        helper.assertValueEqual(targetCleanseAction.effectId(), CRIPPLING_HEX_EFFECT_ID, "Prepared target-cleanse action should preserve its effect id");
        helper.assertValueEqual(targetCleanseAction.actionType(), "remove_effect", "Prepared target-cleanse action should preserve remove_effect type");
        helper.succeed();
    }

    /**
     * Verifies that executing the sample self-buff skill applies the configured mob effect to the caster.
     */
    @GameTest
    public void executeOnCastAppliesBuffMobEffectToCaster(GameTestHelper helper) {
        Player caster = helper.makeMockPlayer(GameType.CREATIVE);
        PreparedSkillUse prepared = Skills.prepareUse(
                battleFocus(helper),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );

        SkillExecutionResult result = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.empty())
        );

        MobEffectInstance effectInstance = caster.getActiveEffects().stream()
                .filter(instance -> BATTLE_FOCUS_EFFECT_ID.equals(BuiltInRegistries.MOB_EFFECT.getKey(instance.getEffect().value())))
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Battle focus should apply the configured mob effect to the caster"));

        helper.assertValueEqual(result.executedActions(), 2, "Battle focus should execute both sound and buff actions");
        helper.assertValueEqual(effectInstance.getAmplifier(), 1, "Battle focus should apply the configured amplifier");
        helper.assertTrue(effectInstance.getDuration() > 0, "Battle focus should apply a positive duration");
        helper.succeed();
    }

    /**
     * Verifies that generic apply_effect actions can apply a debuff to the current target.
     */
    @GameTest
    public void executeOnCastAppliesDebuffMobEffectToTarget(GameTestHelper helper) {
        Player caster = helper.makeMockPlayer(GameType.CREATIVE);
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(3.0, 2.0, 3.0));
        PreparedSkillUse prepared = Skills.prepareUse(
                cripplingHex(helper),
                skillUseContext(helper, newHolder(helper), MonsterStats.resolveBaseHolder(zombie).orElse(newHolder(helper)), 0.0, 0.99)
        );

        SkillExecutionResult result = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.of(zombie))
        );

        MobEffectInstance effectInstance = zombie.getActiveEffects().stream()
                .filter(instance -> CRIPPLING_HEX_EFFECT_ID.equals(BuiltInRegistries.MOB_EFFECT.getKey(instance.getEffect().value())))
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Crippling hex should apply the configured mob effect to the target"));

        helper.assertValueEqual(result.executedActions(), 2, "Crippling hex should execute both sound and apply_effect actions");
        helper.assertValueEqual(effectInstance.getAmplifier(), 0, "Crippling hex should apply the configured amplifier");
        helper.assertTrue(effectInstance.getDuration() > 0, "Crippling hex should apply a positive duration");
        helper.succeed();
    }

    /**
     * Verifies that executing the sample self-cleanse skill removes the configured MobEffect from the caster.
     */
    @GameTest
    public void executeOnCastRemovesBuffMobEffectFromCaster(GameTestHelper helper) {
        Player caster = helper.makeMockPlayer(GameType.CREATIVE);
        caster.addEffect(new MobEffectInstance(
                BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, BATTLE_FOCUS_EFFECT_ID)),
                80,
                1
        ));
        PreparedSkillUse prepared = Skills.prepareUse(
                cleanseFocus(helper),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );

        SkillExecutionResult result = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.empty())
        );

        helper.assertValueEqual(result.executedActions(), 2, "Cleanse focus should execute both sound and remove_effect actions when the caster has speed");
        helper.assertTrue(!caster.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, BATTLE_FOCUS_EFFECT_ID))),
                "Cleanse focus should remove the configured self-buff effect from the caster");
        helper.succeed();
    }

    /**
     * Verifies that executing the sample target-cleanse skill removes the configured MobEffect from the target.
     */
    @GameTest
    public void executeOnCastRemovesDebuffMobEffectFromTarget(GameTestHelper helper) {
        Player caster = helper.makeMockPlayer(GameType.CREATIVE);
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(3.0, 2.0, 3.0));
        zombie.addEffect(new MobEffectInstance(
                BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, CRIPPLING_HEX_EFFECT_ID)),
                80,
                0
        ));
        PreparedSkillUse prepared = Skills.prepareUse(
                purgingHex(helper),
                skillUseContext(helper, newHolder(helper), MonsterStats.resolveBaseHolder(zombie).orElse(newHolder(helper)), 0.0, 0.99)
        );

        SkillExecutionResult result = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.of(zombie))
        );

        helper.assertValueEqual(result.executedActions(), 2, "Purging hex should execute both sound and remove_effect actions when the target has slowness");
        helper.assertTrue(!zombie.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, CRIPPLING_HEX_EFFECT_ID))),
                "Purging hex should remove the configured debuff effect from the target");
        helper.succeed();
    }

    /**
     * Verifies that remove_effect safely no-ops when the configured effect is absent on the target.
     */
    @GameTest
    public void executeOnCastNoopsWhenRemoveEffectTargetIsMissing(GameTestHelper helper) {
        Player caster = helper.makeMockPlayer(GameType.CREATIVE);
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(3.0, 2.0, 3.0));
        PreparedSkillUse prepared = Skills.prepareUse(
                purgingHex(helper),
                skillUseContext(helper, newHolder(helper), MonsterStats.resolveBaseHolder(zombie).orElse(newHolder(helper)), 0.0, 0.99)
        );

        SkillExecutionResult result = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.of(zombie))
        );

        helper.assertValueEqual(result.executedActions(), 1, "Purging hex should only execute its sound action when the target lacks the effect");
        helper.assertValueEqual(result.skippedActions(), 1, "Purging hex should treat the missing remove_effect target as a skipped action");
        helper.assertTrue(!zombie.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, CRIPPLING_HEX_EFFECT_ID))),
                "Purging hex should leave the target unchanged when the configured effect is absent");
        helper.succeed();
    }

    /**
     * Verifies that add_duration refresh policy stacks the remaining duration of the same MobEffect.
     */
    @GameTest
    public void reapplyingSameEffectAddsDuration(GameTestHelper helper) {
        Player caster = helper.makeMockPlayer(GameType.CREATIVE);
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(3.0, 2.0, 3.0));
        PreparedSkillUse prepared = Skills.prepareUse(
                cripplingHex(helper),
                skillUseContext(helper, newHolder(helper), MonsterStats.resolveBaseHolder(zombie).orElse(newHolder(helper)), 0.0, 0.99)
        );

        Skills.executeOnCast(SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.of(zombie)));
        int firstDuration = activeEffectDuration(zombie, CRIPPLING_HEX_EFFECT_ID);

        Skills.executeOnCast(SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.of(zombie)));
        int stackedDuration = activeEffectDuration(zombie, CRIPPLING_HEX_EFFECT_ID);

        helper.assertTrue(stackedDuration >= firstDuration + 9, "Reapplying add_duration effect should add nearly a full second duration window");
        helper.succeed();
    }

    /**
     * Verifies that longer_only does not replace a stronger-or-longer existing MobEffect with a shorter reapply.
     */
    @GameTest
    public void reapplyingLongerOnlyEffectKeepsExistingLongerDuration(GameTestHelper helper) {
        Player caster = helper.makeMockPlayer(GameType.CREATIVE);
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(3.0, 2.0, 3.0));
        PreparedSkillUse longPrepared = Skills.prepareUse(
                testEffectSkill("esekai2:test_longer_only_long", 20, MobEffectRefreshPolicy.LONGER_ONLY),
                skillUseContext(helper, newHolder(helper), MonsterStats.resolveBaseHolder(zombie).orElse(newHolder(helper)), 0.0, 0.99)
        );
        PreparedSkillUse shortPrepared = Skills.prepareUse(
                testEffectSkill("esekai2:test_longer_only_short", 10, MobEffectRefreshPolicy.LONGER_ONLY),
                skillUseContext(helper, newHolder(helper), MonsterStats.resolveBaseHolder(zombie).orElse(newHolder(helper)), 0.0, 0.99)
        );

        Skills.executeOnCast(SkillExecutionContext.forCast(longPrepared, helper.getLevel(), caster, Optional.of(zombie)));
        int firstDuration = activeEffectDuration(zombie, CRIPPLING_HEX_EFFECT_ID);

        Skills.executeOnCast(SkillExecutionContext.forCast(shortPrepared, helper.getLevel(), caster, Optional.of(zombie)));
        int secondDuration = activeEffectDuration(zombie, CRIPPLING_HEX_EFFECT_ID);

        helper.assertValueEqual(secondDuration, firstDuration, "longer_only should keep the existing longer duration when a shorter effect reapplies");
        helper.succeed();
    }

    /**
     * Verifies that legacy apply_buff support overrides can change apply_effect field values on the selected cast path.
     */
    @GameTest
    public void prepareSelectedUseAppliesEffectSupportOverrides(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemSlot(
                SocketedEquipmentSlot.MAIN_HAND.equipmentSlot(),
                socketedSkillStack(Items.STICK, CRIPPLING_HEX_SKILL_ID, List.of(supportRef(1, SUPPORT_LASTING_HEX_ID)))
        );
        PlayerActiveSkills.select(player, new SelectedActiveSkillRef(SocketedEquipmentSlot.MAIN_HAND, CRIPPLING_HEX_SKILL_ID));

        SelectedSkillUseResult result = Skills.prepareSelectedUse(
                player,
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );

        PreparedApplyBuffAction effectAction = (PreparedApplyBuffAction) result.preparedUse().orElseThrow().onCastActions().stream()
                .filter(PreparedApplyBuffAction.class::isInstance)
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Selected crippling hex should still expose one apply_effect action"));

        helper.assertTrue(result.success(), "Selected crippling hex should prepare successfully");
        helper.assertValueEqual(effectAction.durationTicks(), 40, "Legacy apply_buff support should override apply_effect duration_ticks");
        helper.assertValueEqual(effectAction.refreshPolicy(), MobEffectRefreshPolicy.ADD_DURATION, "Selected apply_effect should preserve refresh_policy through support merge");
        helper.succeed();
    }

    /**
     * Verifies that apply_effect support selectors can still override legacy apply_buff actions.
     */
    @GameTest
    public void prepareSelectedUseAppliesEffectAliasOverrideToLegacyBuff(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemSlot(
                SocketedEquipmentSlot.MAIN_HAND.equipmentSlot(),
                socketedSkillStack(Items.STICK, BATTLE_FOCUS_SKILL_ID, List.of(supportRef(1, SUPPORT_QUICK_FOCUS_ID)))
        );
        PlayerActiveSkills.select(player, new SelectedActiveSkillRef(SocketedEquipmentSlot.MAIN_HAND, BATTLE_FOCUS_SKILL_ID));

        SelectedSkillUseResult result = Skills.prepareSelectedUse(
                player,
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );

        PreparedApplyBuffAction effectAction = (PreparedApplyBuffAction) result.preparedUse().orElseThrow().onCastActions().stream()
                .filter(PreparedApplyBuffAction.class::isInstance)
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Selected battle focus should still expose one apply_buff action"));

        helper.assertTrue(result.success(), "Selected battle focus should prepare successfully");
        helper.assertValueEqual(effectAction.actionType(), "apply_buff", "Legacy battle focus should keep apply_buff prepared action type");
        helper.assertValueEqual(effectAction.durationTicks(), 12, "apply_effect support override should match legacy apply_buff actions");
        helper.succeed();
    }

    /**
     * Verifies that selected socket-backed preparation can override remove_effect.effect_id through a linked support.
     */
    @GameTest
    public void prepareSelectedUseAppliesRemoveEffectSupportOverrides(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemSlot(
                SocketedEquipmentSlot.MAIN_HAND.equipmentSlot(),
                socketedSkillStack(Items.STICK, CLEANSE_FOCUS_SKILL_ID, List.of(supportRef(1, SUPPORT_TOXIC_CLEANSE_ID)))
        );
        PlayerActiveSkills.select(player, new SelectedActiveSkillRef(SocketedEquipmentSlot.MAIN_HAND, CLEANSE_FOCUS_SKILL_ID));

        SelectedSkillUseResult result = Skills.prepareSelectedUse(
                player,
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );

        PreparedRemoveEffectAction removeEffectAction = (PreparedRemoveEffectAction) result.preparedUse().orElseThrow().onCastActions().stream()
                .filter(PreparedRemoveEffectAction.class::isInstance)
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Selected cleanse focus should still expose one remove_effect action"));

        helper.assertTrue(result.success(), "Selected cleanse focus should prepare successfully");
        helper.assertValueEqual(removeEffectAction.actionType(), "remove_effect", "Selected cleanse focus should preserve remove_effect prepared action type");
        helper.assertValueEqual(removeEffectAction.effectId(), POISON_EFFECT_ID, "Linked support should override remove_effect.effect_id on the selected cast path");
        helper.succeed();
    }

    /**
     * Verifies that selected remove_effect casts clear both the poison MobEffect identity and attached ailment payload.
     */
    @GameTest
    public void castSelectedSkillRemovesPoisonAilmentEffectAndAttachment(GameTestHelper helper) {
        Player caster = helper.makeMockPlayer(GameType.CREATIVE);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        PreparedSkillUse poisonPrepared = Skills.prepareUse(
                venomStrike(helper),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );

        Skills.executeOnCast(
                SkillExecutionContext.forCast(poisonPrepared, helper.getLevel(), caster, Optional.of(player))
        );
        helper.assertTrue(player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, POISON_EFFECT_ID))),
                "Poison setup should apply the poison MobEffect identity before the cleanse cast");
        helper.assertTrue(Ailments.get(player).flatMap(state -> state.get(AilmentType.POISON)).isPresent(),
                "Poison setup should also attach a poison payload before the cleanse cast");

        player.setItemSlot(
                SocketedEquipmentSlot.MAIN_HAND.equipmentSlot(),
                socketedSkillStack(Items.STICK, CLEANSE_FOCUS_SKILL_ID, List.of(supportRef(1, SUPPORT_TOXIC_CLEANSE_ID)))
        );
        PlayerActiveSkills.select(player, new SelectedActiveSkillRef(SocketedEquipmentSlot.MAIN_HAND, CLEANSE_FOCUS_SKILL_ID));

        SelectedSkillCastResult result = Skills.castSelectedSkill(
                player,
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99),
                Optional.empty()
        );

        helper.assertTrue(result.success(), "Selected cleanse focus cast should succeed");
        helper.assertValueEqual(result.executionResult().orElseThrow().executedActions(), 2, "Selected cleanse focus should execute both sound and remove_effect actions when poison is present");
        helper.assertTrue(!player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, POISON_EFFECT_ID))),
                "Selected cleanse focus should remove the poison MobEffect identity");
        helper.assertTrue(Ailments.get(player).flatMap(state -> state.get(AilmentType.POISON)).isEmpty(),
                "Selected cleanse focus should clear the poison attachment payload");
        helper.succeed();
    }

    /**
     * Verifies that the selected cast path executes generic apply_effect actions against the chosen target.
     */
    @GameTest
    public void castSelectedSkillAppliesEffectToTarget(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(3.0, 2.0, 3.0));
        player.setItemSlot(
                SocketedEquipmentSlot.MAIN_HAND.equipmentSlot(),
                socketedSkillStack(Items.STICK, CRIPPLING_HEX_SKILL_ID, List.of(supportRef(1, SUPPORT_LASTING_HEX_ID)))
        );
        PlayerActiveSkills.select(player, new SelectedActiveSkillRef(SocketedEquipmentSlot.MAIN_HAND, CRIPPLING_HEX_SKILL_ID));

        SelectedSkillCastResult result = Skills.castSelectedSkill(
                player,
                skillUseContext(helper, newHolder(helper), MonsterStats.resolveBaseHolder(zombie).orElse(newHolder(helper)), 0.0, 0.99),
                Optional.of(zombie)
        );

        helper.assertTrue(result.success(), "Selected crippling hex cast should succeed");
        helper.assertValueEqual(activeEffectDuration(zombie, CRIPPLING_HEX_EFFECT_ID), 40, "Selected cast support override should carry into runtime effect duration");
        helper.succeed();
    }

    /**
     * Verifies that linked support overrides can change apply_dot field values on the selected cast path.
     */
    @GameTest
    public void prepareSelectedUseAppliesDotSupportOverrides(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemSlot(
                SocketedEquipmentSlot.MAIN_HAND.equipmentSlot(),
                socketedSkillStack(Items.BLAZE_ROD, SEARING_BRAND_SKILL_ID, List.of(supportRef(1, SUPPORT_LINGERING_BRAND_ID)))
        );
        PlayerActiveSkills.select(player, new SelectedActiveSkillRef(SocketedEquipmentSlot.MAIN_HAND, SEARING_BRAND_SKILL_ID));

        SelectedSkillUseResult result = Skills.prepareSelectedUse(
                player,
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );

        PreparedApplyDotAction dotAction = (PreparedApplyDotAction) result.preparedUse().orElseThrow().onCastActions().stream()
                .filter(PreparedApplyDotAction.class::isInstance)
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Selected searing brand should still expose one apply_dot action"));

        helper.assertTrue(result.success(), "Selected searing brand should prepare successfully");
        helper.assertValueEqual(dotAction.durationTicks(), 16, "Linked support should override apply_dot duration_ticks");
        helper.succeed();
    }

    /**
     * Verifies that executing the sample dot skill damages the target over time and stops once the duration expires.
     */
    @GameTest
    public void executeOnCastAppliesDotDamageAndExpires(GameTestHelper helper) {
        Player caster = helper.makeMockPlayer(GameType.CREATIVE);
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(3.0, 2.0, 3.0));
        PreparedSkillUse prepared = Skills.prepareUse(
                searingBrand(helper),
                skillUseContext(
                        helper,
                        newHolder(helper),
                        MonsterStats.resolveBaseHolder(zombie).orElse(newHolder(helper)),
                        0.0,
                        0.99
                )
        );

        SkillExecutionResult result = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.of(zombie))
        );

        float initialHealth = zombie.getHealth();
        float[] settledHealth = new float[1];

        helper.assertValueEqual(result.executedActions(), 2, "Searing brand should execute both sound and dot actions");
        helper.runAfterDelay(3, () ->
                helper.assertTrue(zombie.getHealth() < initialHealth, "The first DoT tick should damage the target shortly after cast")
        );
        helper.runAfterDelay(12, () -> settledHealth[0] = zombie.getHealth());
        helper.runAfterDelay(18, () -> {
            helper.assertValueEqual(zombie.getHealth(), settledHealth[0], "The generic DoT should stop dealing damage once its duration expires");
            helper.succeed();
        });
    }

    /**
     * Verifies that reapplying the same dot id refreshes duration instead of expiring on the original timer.
     */
    @GameTest
    public void reapplyingSameDotIdRefreshesDuration(GameTestHelper helper) {
        Player caster = helper.makeMockPlayer(GameType.CREATIVE);
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(3.0, 2.0, 3.0));
        PreparedSkillUse prepared = Skills.prepareUse(
                searingBrand(helper),
                skillUseContext(
                        helper,
                        newHolder(helper),
                        MonsterStats.resolveBaseHolder(zombie).orElse(newHolder(helper)),
                        0.0,
                        0.99
                )
        );

        Skills.executeOnCast(SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.of(zombie)));
        helper.runAfterDelay(7, () ->
                Skills.executeOnCast(SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.of(zombie)))
        );

        float[] healthAtOriginalExpiry = new float[1];
        helper.runAfterDelay(10, () -> healthAtOriginalExpiry[0] = zombie.getHealth());
        helper.runAfterDelay(13, () -> {
            helper.assertTrue(
                    zombie.getHealth() < healthAtOriginalExpiry[0],
                    "Reapplying the same dot id should refresh duration and continue ticking past the original expiry window"
            );
            helper.succeed();
        });
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

    private static Registry<SkillCalculationDefinition> skillCalculationRegistry(GameTestHelper helper) {
        return SkillCalculationRegistryAccess.skillCalculationRegistry(helper);
    }

    private static Registry<SkillDefinition> skillRegistry(GameTestHelper helper) {
        return helper.getLevel().getServer().registryAccess().lookupOrThrow(SkillRegistries.SKILL);
    }

    private static Registry<SkillSupportDefinition> supportRegistry(GameTestHelper helper) {
        return helper.getLevel().getServer().registryAccess().lookupOrThrow(SkillRegistries.SKILL_SUPPORT);
    }

    private static SkillDefinition battleFocus(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(BATTLE_FOCUS_SKILL_ID)
                .orElseThrow(() -> helper.assertionException("Battle focus should decode successfully"));
    }

    private static SkillDefinition cleanseFocus(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(CLEANSE_FOCUS_SKILL_ID)
                .orElseThrow(() -> helper.assertionException("Cleanse focus should decode successfully"));
    }

    private static SkillDefinition purgingHex(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(PURGING_HEX_SKILL_ID)
                .orElseThrow(() -> helper.assertionException("Purging hex should decode successfully"));
    }

    private static SkillDefinition searingBrand(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(SEARING_BRAND_SKILL_ID)
                .orElseThrow(() -> helper.assertionException("Searing brand should decode successfully"));
    }

    private static SkillDefinition cripplingHex(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(CRIPPLING_HEX_SKILL_ID)
                .orElseThrow(() -> helper.assertionException("Crippling hex should decode successfully"));
    }

    private static SkillDefinition venomStrike(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(VENOM_STRIKE_SKILL_ID)
                .orElseThrow(() -> helper.assertionException("Venom strike should decode successfully"));
    }

    private static net.minecraft.world.effect.MobEffect effect(GameTestHelper helper, Identifier effectId) {
        return BuiltInRegistries.MOB_EFFECT.getOptional(effectId)
                .orElseThrow(() -> helper.assertionException("Missing mob effect fixture: " + effectId));
    }

    private static int activeEffectDuration(LivingEntity entity, Identifier effectId) {
        return entity.getActiveEffects().stream()
                .filter(instance -> effectId.equals(BuiltInRegistries.MOB_EFFECT.getKey(instance.getEffect().value())))
                .findFirst()
                .map(MobEffectInstance::getDuration)
                .orElse(0);
    }

    private static ItemStack socketedSkillStack(net.minecraft.world.item.Item item, Identifier activeSkillId, List<SocketedSkillRef> extraSupports) {
        ItemStack stack = new ItemStack(item);
        List<SocketedSkillRef> socketRefs = new java.util.ArrayList<>();
        socketRefs.add(new SocketedSkillRef(0, SocketSlotType.SKILL, activeSkillId));
        socketRefs.addAll(extraSupports);
        SocketedSkills.set(stack, new SocketedSkillItemState(
                Optional.of(activeSkillId),
                Math.max(1, socketRefs.size()),
                List.of(new SocketLinkGroup(0, socketRefs.stream().map(SocketedSkillRef::socketIndex).toList())),
                List.copyOf(socketRefs)
        ));
        return stack;
    }

    private static SocketedSkillRef supportRef(int socketIndex, Identifier supportId) {
        return new SocketedSkillRef(socketIndex, SocketSlotType.SUPPORT, supportId);
    }

    private static SkillDefinition testEffectSkill(String identifier, int durationTicks, MobEffectRefreshPolicy refreshPolicy) {
        return new SkillDefinition(
                identifier,
                SkillConfig.DEFAULT,
                new SkillAttached(
                        List.of(new SkillRule(
                                Set.of(new SkillTargetSelector(SkillTargetType.TARGET, Map.of())),
                                List.of(new SkillAction(SkillActionType.APPLY_EFFECT, Map.of(
                                        "effect_id", CRIPPLING_HEX_EFFECT_ID.toString(),
                                        "duration_ticks", Integer.toString(durationTicks),
                                        "refresh_policy", refreshPolicy.serializedName()
                                ))),
                                List.of(),
                                List.of()
                        )),
                        Map.of()
                ),
                "",
                Set.of(),
                ""
        );
    }
}
