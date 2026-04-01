package kim.biryeong.esekai2.impl.gametest.skill;

import kim.biryeong.esekai2.api.ailment.AilmentPayload;
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
import kim.biryeong.esekai2.api.player.stat.PlayerCombatStats;
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
import kim.biryeong.esekai2.api.skill.effect.SkillAilmentRefreshPolicy;
import kim.biryeong.esekai2.api.skill.execution.PreparedSkillUse;
import kim.biryeong.esekai2.api.skill.execution.SelectedSkillCastResult;
import kim.biryeong.esekai2.api.skill.execution.SkillExecutionContext;
import kim.biryeong.esekai2.api.skill.execution.SkillExecutionResult;
import kim.biryeong.esekai2.api.skill.execution.SkillUseContext;
import kim.biryeong.esekai2.api.skill.execution.Skills;
import kim.biryeong.esekai2.api.skill.support.SkillSupportDefinition;
import kim.biryeong.esekai2.api.skill.tag.SkillTag;
import kim.biryeong.esekai2.api.stat.combat.CombatStats;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Verifies non-DoT control ailments and their cast-blocking runtime semantics.
 */
public final class AilmentControlGameTests {
    private static final Identifier ICE_PRISON_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "ice_prison");
    private static final Identifier DEEP_FREEZE_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "deep_freeze");
    private static final Identifier DAZING_TAP_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "dazing_tap");
    private static final Identifier CONCUSSIVE_SLAM_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "concussive_slam");
    private static final Identifier BASIC_STRIKE_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "basic_strike");
    private static final Identifier NEGATIVE_PURGE_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "negative_purge_inline");

    /**
     * Verifies that freeze and stun fixture skills load and register their MobEffect identities.
     */
    @GameTest
    public void registryLoadsFreezeAndStunFixturesAndEffects(GameTestHelper helper) {
        helper.assertTrue(skillRegistry(helper).containsKey(ICE_PRISON_SKILL_ID), "Ice prison should load into the skill registry");
        helper.assertTrue(skillRegistry(helper).containsKey(DEEP_FREEZE_SKILL_ID), "Deep freeze should load into the skill registry");
        helper.assertTrue(skillRegistry(helper).containsKey(DAZING_TAP_SKILL_ID), "Dazing tap should load into the skill registry");
        helper.assertTrue(skillRegistry(helper).containsKey(CONCUSSIVE_SLAM_SKILL_ID), "Concussive slam should load into the skill registry");
        helper.assertTrue(BuiltInRegistries.MOB_EFFECT.containsKey(AilmentType.FREEZE.effectId()), "Freeze effect should be registered");
        helper.assertTrue(BuiltInRegistries.MOB_EFFECT.containsKey(AilmentType.STUN.effectId()), "Stun effect should be registered");
        helper.succeed();
    }

    /**
     * Verifies that stun applies both an attachment payload and a MobEffect identity, then only refreshes when a longer payload arrives.
     */
    @GameTest
    public void stunAppliesAttachmentAndRefreshesLongerOnly(GameTestHelper helper) {
        ServerPlayer caster = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        ServerPlayer target = controlTarget(helper, new Vec3(3.0, 2.0, 3.0));
        PreparedSkillUse shortStun = Skills.prepareUse(
                dazingTap(helper),
                skillUseContext(helper, newHolder(helper), PlayerCombatStats.get(target), 0.0, 0.99)
        );
        PreparedSkillUse longStun = Skills.prepareUse(
                concussiveSlam(helper),
                skillUseContext(helper, newHolder(helper), PlayerCombatStats.get(target), 0.0, 0.99)
        );

        Skills.executeOnCast(SkillExecutionContext.forCast(longStun, helper.getLevel(), caster, Optional.of(target)));
        AilmentPayload baseline = payload(helper, target, AilmentType.STUN);
        Skills.executeOnCast(SkillExecutionContext.forCast(shortStun, helper.getLevel(), caster, Optional.of(target)));
        AilmentPayload afterShorter = payload(helper, target, AilmentType.STUN);

        helper.assertTrue(target.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(AilmentType.STUN))),
                "Stun should be represented by a MobEffect identity");
        helper.assertValueEqual(afterShorter, baseline,
                "A shorter stun reapplication should not replace the current attachment payload");
        helper.succeed();
    }

    /**
     * Verifies that freeze reduces movement speed to zero through its MobEffect attribute modifier and stores attachment state.
     */
    @GameTest
    public void freezeAppliesAttachmentAndZeroesMovementSpeed(GameTestHelper helper) {
        ServerPlayer caster = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        ServerPlayer target = controlTarget(helper, new Vec3(3.0, 2.0, 3.0));
        double baselineSpeed = target.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        PreparedSkillUse prepared = Skills.prepareUse(
                icePrison(helper),
                skillUseContext(helper, newHolder(helper), PlayerCombatStats.get(target), 0.0, 0.99)
        );

        Skills.executeOnCast(SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.of(target)));

        helper.assertTrue(Ailments.get(target).flatMap(state -> state.get(AilmentType.FREEZE)).isPresent(),
                "Freeze should store an attachment payload");
        helper.assertTrue(target.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(AilmentType.FREEZE))),
                "Freeze should be represented by a MobEffect identity");
        helper.assertTrue(target.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) < baselineSpeed,
                "Freeze should lower movement speed through the MobEffect attribute modifier");
        helper.assertValueEqual(target.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED), 0.0,
                "Freeze should clamp movement speed to zero");
        helper.succeed();
    }

    /**
     * Verifies that freeze effective duration grows with higher dealt damage after threshold scaling.
     */
    @GameTest
    public void freezeDurationScalesWithDamageAfterThreshold(GameTestHelper helper) {
        ServerPlayer caster = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        PreparedSkillUse lighterFreeze = Skills.prepareUse(
                controlAilmentSkill("esekai2:test_freeze_lighter_formula", AilmentType.FREEZE, 6.0, 10, SkillAilmentRefreshPolicy.LONGER_ONLY),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );
        PreparedSkillUse heavierFreeze = Skills.prepareUse(
                controlAilmentSkill("esekai2:test_freeze_heavier_formula", AilmentType.FREEZE, 14.0, 10, SkillAilmentRefreshPolicy.LONGER_ONLY),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );
        ServerPlayer lighterTarget = controlTarget(helper, new Vec3(3.0, 2.0, 3.0));
        ServerPlayer heavierTarget = controlTarget(helper, new Vec3(5.0, 2.0, 3.0));

        Skills.executeOnCast(SkillExecutionContext.forCast(lighterFreeze, helper.getLevel(), caster, Optional.of(lighterTarget)));
        Skills.executeOnCast(SkillExecutionContext.forCast(heavierFreeze, helper.getLevel(), caster, Optional.of(heavierTarget)));

        int lighterDuration = payload(helper, lighterTarget, AilmentType.FREEZE).durationTicks();
        int heavierDuration = payload(helper, heavierTarget, AilmentType.FREEZE).durationTicks();

        helper.assertTrue(heavierDuration > lighterDuration,
                "Higher freeze hit damage should produce a longer effective freeze duration");
        helper.succeed();
    }

    /**
     * Verifies that a zero ailment-threshold stat falls back to the target's life value when scaling freeze duration.
     */
    @GameTest
    public void freezeDurationFallsBackToLifeWhenAilmentThresholdIsZero(GameTestHelper helper) {
        ServerPlayer caster = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        ServerPlayer target = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        target.snapTo(helper.absoluteVec(new Vec3(3.0, 2.0, 3.0)));
        var targetStats = kim.biryeong.esekai2.api.player.stat.PlayerCombatStats.get(target);
        targetStats.setBaseValue(CombatStats.LIFE, 40.0);
        targetStats.setBaseValue(CombatStats.AILMENT_THRESHOLD, 0.0);

        PreparedSkillUse prepared = Skills.prepareUse(
                controlAilmentSkill("esekai2:test_freeze_life_threshold_fallback", AilmentType.FREEZE, 20.0, 10, SkillAilmentRefreshPolicy.LONGER_ONLY),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );

        Skills.executeOnCast(SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.of(target)));

        helper.assertValueEqual(payload(helper, target, AilmentType.FREEZE).durationTicks(), 5,
                "Zero ailment threshold should fall back to life when computing effective freeze duration");
        helper.succeed();
    }

    /**
     * Verifies that stun effective duration grows with higher dealt damage after threshold scaling.
     */
    @GameTest
    public void stunDurationScalesWithDamageAfterThreshold(GameTestHelper helper) {
        ServerPlayer caster = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        PreparedSkillUse lighterStun = Skills.prepareUse(
                controlAilmentSkill("esekai2:test_stun_lighter_formula", AilmentType.STUN, 7.0, 8, SkillAilmentRefreshPolicy.LONGER_ONLY),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );
        PreparedSkillUse heavierStun = Skills.prepareUse(
                controlAilmentSkill("esekai2:test_stun_heavier_formula", AilmentType.STUN, 16.0, 8, SkillAilmentRefreshPolicy.LONGER_ONLY),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );
        ServerPlayer lighterTarget = controlTarget(helper, new Vec3(3.0, 2.0, 3.0));
        ServerPlayer heavierTarget = controlTarget(helper, new Vec3(5.0, 2.0, 3.0));

        Skills.executeOnCast(SkillExecutionContext.forCast(lighterStun, helper.getLevel(), caster, Optional.of(lighterTarget)));
        Skills.executeOnCast(SkillExecutionContext.forCast(heavierStun, helper.getLevel(), caster, Optional.of(heavierTarget)));

        int lighterDuration = payload(helper, lighterTarget, AilmentType.STUN).durationTicks();
        int heavierDuration = payload(helper, heavierTarget, AilmentType.STUN).durationTicks();

        helper.assertTrue(heavierDuration > lighterDuration,
                "Higher stun hit damage should produce a longer effective stun duration");
        helper.succeed();
    }

    /**
     * Verifies that freeze-duration-increased attacker stats extend the final effective freeze duration.
     */
    @GameTest
    public void freezeDurationIncreasedStatExtendsEffectiveDuration(GameTestHelper helper) {
        ServerPlayer caster = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        StatHolder baselineAttacker = newHolder(helper);
        StatHolder boostedAttacker = newHolder(helper);
        boostedAttacker.setBaseValue(CombatStats.FREEZE_DURATION_INCREASED, 100.0);
        PreparedSkillUse baselineFreeze = Skills.prepareUse(
                controlAilmentSkill("esekai2:test_freeze_duration_stat_baseline", AilmentType.FREEZE, 10.0, 10, SkillAilmentRefreshPolicy.LONGER_ONLY),
                skillUseContext(helper, baselineAttacker, newHolder(helper), 0.0, 0.99)
        );
        PreparedSkillUse boostedFreeze = Skills.prepareUse(
                controlAilmentSkill("esekai2:test_freeze_duration_stat_boosted", AilmentType.FREEZE, 10.0, 10, SkillAilmentRefreshPolicy.LONGER_ONLY),
                skillUseContext(helper, boostedAttacker, newHolder(helper), 0.0, 0.99)
        );
        ServerPlayer baselineTarget = controlTarget(helper, new Vec3(3.0, 2.0, 3.0));
        ServerPlayer boostedTarget = controlTarget(helper, new Vec3(5.0, 2.0, 3.0));

        Skills.executeOnCast(SkillExecutionContext.forCast(baselineFreeze, helper.getLevel(), caster, Optional.of(baselineTarget)));
        Skills.executeOnCast(SkillExecutionContext.forCast(boostedFreeze, helper.getLevel(), caster, Optional.of(boostedTarget)));

        int baselineDuration = payload(helper, baselineTarget, AilmentType.FREEZE).durationTicks();
        int boostedDuration = payload(helper, boostedTarget, AilmentType.FREEZE).durationTicks();

        helper.assertTrue(boostedDuration > baselineDuration,
                "freeze_duration_increased should extend the final effective freeze duration");
        helper.succeed();
    }

    /**
     * Verifies that stun-duration-increased attacker stats extend the final effective stun duration.
     */
    @GameTest
    public void stunDurationIncreasedStatExtendsEffectiveDuration(GameTestHelper helper) {
        ServerPlayer caster = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        StatHolder baselineAttacker = newHolder(helper);
        StatHolder boostedAttacker = newHolder(helper);
        boostedAttacker.setBaseValue(CombatStats.STUN_DURATION_INCREASED, 100.0);
        PreparedSkillUse baselineStun = Skills.prepareUse(
                controlAilmentSkill("esekai2:test_stun_duration_stat_baseline", AilmentType.STUN, 10.0, 10, SkillAilmentRefreshPolicy.LONGER_ONLY),
                skillUseContext(helper, baselineAttacker, newHolder(helper), 0.0, 0.99)
        );
        PreparedSkillUse boostedStun = Skills.prepareUse(
                controlAilmentSkill("esekai2:test_stun_duration_stat_boosted", AilmentType.STUN, 10.0, 10, SkillAilmentRefreshPolicy.LONGER_ONLY),
                skillUseContext(helper, boostedAttacker, newHolder(helper), 0.0, 0.99)
        );
        ServerPlayer baselineTarget = controlTarget(helper, new Vec3(3.0, 2.0, 3.0));
        ServerPlayer boostedTarget = controlTarget(helper, new Vec3(5.0, 2.0, 3.0));

        Skills.executeOnCast(SkillExecutionContext.forCast(baselineStun, helper.getLevel(), caster, Optional.of(baselineTarget)));
        Skills.executeOnCast(SkillExecutionContext.forCast(boostedStun, helper.getLevel(), caster, Optional.of(boostedTarget)));

        int baselineDuration = payload(helper, baselineTarget, AilmentType.STUN).durationTicks();
        int boostedDuration = payload(helper, boostedTarget, AilmentType.STUN).durationTicks();

        helper.assertTrue(boostedDuration > baselineDuration,
                "stun_duration_increased should extend the final effective stun duration");
        helper.succeed();
    }

    /**
     * Verifies that control ailments safely no-op when threshold scaling reduces their effective duration to zero.
     */
    @GameTest
    public void freezeAndStunNoopWhenEffectiveDurationRoundsToZero(GameTestHelper helper) {
        ServerPlayer caster = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        LivingEntity freezeTarget = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(3.0, 2.0, 3.0));
        LivingEntity stunTarget = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(5.0, 2.0, 3.0));
        PreparedSkillUse tinyFreeze = Skills.prepareUse(
                controlAilmentSkill("esekai2:test_freeze_zero_duration_formula", AilmentType.FREEZE, 0.25, 6, SkillAilmentRefreshPolicy.LONGER_ONLY),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );
        PreparedSkillUse tinyStun = Skills.prepareUse(
                controlAilmentSkill("esekai2:test_stun_zero_duration_formula", AilmentType.STUN, 0.25, 6, SkillAilmentRefreshPolicy.LONGER_ONLY),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );

        SkillExecutionResult freezeResult = Skills.executeOnCast(
                SkillExecutionContext.forCast(tinyFreeze, helper.getLevel(), caster, Optional.of(freezeTarget))
        );
        SkillExecutionResult stunResult = Skills.executeOnCast(
                SkillExecutionContext.forCast(tinyStun, helper.getLevel(), caster, Optional.of(stunTarget))
        );

        helper.assertValueEqual(freezeResult.executedActions(), 2,
                "Zero-effective freeze should leave only sound and damage as executed actions");
        helper.assertValueEqual(stunResult.executedActions(), 2,
                "Zero-effective stun should leave only sound and damage as executed actions");
        helper.assertTrue(Ailments.get(freezeTarget).flatMap(state -> state.get(AilmentType.FREEZE)).isEmpty(),
                "Zero-effective freeze should not attach a freeze payload");
        helper.assertTrue(Ailments.get(stunTarget).flatMap(state -> state.get(AilmentType.STUN)).isEmpty(),
                "Zero-effective stun should not attach a stun payload");
        helper.assertTrue(!freezeTarget.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(AilmentType.FREEZE))),
                "Zero-effective freeze should not add a freeze MobEffect identity");
        helper.assertTrue(!stunTarget.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(AilmentType.STUN))),
                "Zero-effective stun should not add a stun MobEffect identity");
        helper.succeed();
    }

    /**
     * Verifies that longer_only refresh compares formula-scaled freeze durations instead of raw datapack duration.
     */
    @GameTest
    public void longerOnlyRefreshComparesEffectiveFreezeDurationAfterFormula(GameTestHelper helper) {
        ServerPlayer caster = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        PreparedSkillUse longerEffectiveFreeze = Skills.prepareUse(
                controlAilmentSkill("esekai2:test_freeze_refresh_longer_effective", AilmentType.FREEZE, 14.0, 8, SkillAilmentRefreshPolicy.LONGER_ONLY),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );
        PreparedSkillUse shorterEffectiveFreeze = Skills.prepareUse(
                controlAilmentSkill("esekai2:test_freeze_refresh_shorter_effective", AilmentType.FREEZE, 6.0, 14, SkillAilmentRefreshPolicy.LONGER_ONLY),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );
        ServerPlayer longerBaselineTarget = controlTarget(helper, new Vec3(3.0, 2.0, 3.0));
        ServerPlayer shorterBaselineTarget = controlTarget(helper, new Vec3(5.0, 2.0, 3.0));
        ServerPlayer refreshTarget = controlTarget(helper, new Vec3(7.0, 2.0, 3.0));

        Skills.executeOnCast(SkillExecutionContext.forCast(longerEffectiveFreeze, helper.getLevel(), caster, Optional.of(longerBaselineTarget)));
        Skills.executeOnCast(SkillExecutionContext.forCast(shorterEffectiveFreeze, helper.getLevel(), caster, Optional.of(shorterBaselineTarget)));
        int longerEffectiveDuration = payload(helper, longerBaselineTarget, AilmentType.FREEZE).durationTicks();
        int shorterEffectiveDuration = payload(helper, shorterBaselineTarget, AilmentType.FREEZE).durationTicks();

        helper.assertTrue(longerEffectiveDuration > shorterEffectiveDuration,
                "Test setup should produce a longer effective freeze before comparing refresh policy behavior");

        Skills.executeOnCast(SkillExecutionContext.forCast(longerEffectiveFreeze, helper.getLevel(), caster, Optional.of(refreshTarget)));
        Skills.executeOnCast(SkillExecutionContext.forCast(shorterEffectiveFreeze, helper.getLevel(), caster, Optional.of(refreshTarget)));

        helper.assertValueEqual(payload(helper, refreshTarget, AilmentType.FREEZE).durationTicks(), longerEffectiveDuration,
                "longer_only should keep the existing longer freeze duration after formula scaling");
        helper.succeed();
    }

    /**
     * Verifies that overwrite refresh replaces the current stun payload with the incoming formula-scaled duration.
     */
    @GameTest
    public void overwriteRefreshUsesIncomingEffectiveStunDurationAfterFormula(GameTestHelper helper) {
        ServerPlayer caster = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        PreparedSkillUse firstStun = Skills.prepareUse(
                controlAilmentSkill("esekai2:test_stun_refresh_first_overwrite", AilmentType.STUN, 15.0, 8, SkillAilmentRefreshPolicy.OVERWRITE),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );
        PreparedSkillUse secondStun = Skills.prepareUse(
                controlAilmentSkill("esekai2:test_stun_refresh_second_overwrite", AilmentType.STUN, 7.0, 14, SkillAilmentRefreshPolicy.OVERWRITE),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );
        ServerPlayer firstBaselineTarget = controlTarget(helper, new Vec3(3.0, 2.0, 3.0));
        ServerPlayer secondBaselineTarget = controlTarget(helper, new Vec3(5.0, 2.0, 3.0));
        ServerPlayer refreshTarget = controlTarget(helper, new Vec3(7.0, 2.0, 3.0));

        Skills.executeOnCast(SkillExecutionContext.forCast(firstStun, helper.getLevel(), caster, Optional.of(firstBaselineTarget)));
        Skills.executeOnCast(SkillExecutionContext.forCast(secondStun, helper.getLevel(), caster, Optional.of(secondBaselineTarget)));
        int firstEffectiveDuration = payload(helper, firstBaselineTarget, AilmentType.STUN).durationTicks();
        int secondEffectiveDuration = payload(helper, secondBaselineTarget, AilmentType.STUN).durationTicks();

        helper.assertTrue(firstEffectiveDuration != secondEffectiveDuration,
                "Test setup should produce distinct effective stun durations before comparing overwrite behavior");

        Skills.executeOnCast(SkillExecutionContext.forCast(firstStun, helper.getLevel(), caster, Optional.of(refreshTarget)));
        Skills.executeOnCast(SkillExecutionContext.forCast(secondStun, helper.getLevel(), caster, Optional.of(refreshTarget)));

        helper.assertValueEqual(payload(helper, refreshTarget, AilmentType.STUN).durationTicks(), secondEffectiveDuration,
                "overwrite should replace the current stun duration with the incoming formula-scaled result");
        helper.succeed();
    }

    /**
     * Verifies that targeted negative purge removes both the stun attachment payload and MobEffect identity.
     */
    @GameTest
    public void targetedNegativePurgeRemovesStunAttachmentAndIdentity(GameTestHelper helper) {
        ServerPlayer applier = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        ServerPlayer cleanser = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        ServerPlayer target = controlTarget(helper, new Vec3(3.0, 2.0, 3.0));
        PreparedSkillUse stunPrepared = Skills.prepareUse(
                concussiveSlam(helper),
                skillUseContext(helper, newHolder(helper), PlayerCombatStats.get(target), 0.0, 0.99)
        );
        PreparedSkillUse purgePrepared = Skills.prepareUse(
                targetedNegativePurge(),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );

        Skills.executeOnCast(SkillExecutionContext.forCast(stunPrepared, helper.getLevel(), applier, Optional.of(target)));
        helper.assertTrue(Ailments.get(target).flatMap(state -> state.get(AilmentType.STUN)).isPresent(),
                "Stun setup should attach a stun payload before the purge");

        SkillExecutionResult purgeResult = Skills.executeOnCast(
                SkillExecutionContext.forCast(purgePrepared, helper.getLevel(), cleanser, Optional.of(target))
        );

        helper.assertValueEqual(purgeResult.executedActions(), 1, "Targeted negative purge should execute its remove_effect action");
        helper.assertTrue(Ailments.get(target).flatMap(state -> state.get(AilmentType.STUN)).isEmpty(),
                "Negative purge should remove the stun attachment payload");
        helper.assertTrue(!target.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(AilmentType.STUN))),
                "Negative purge should remove the stun MobEffect identity");
        helper.succeed();
    }

    /**
     * Verifies that executeOnCast no-ops with a warning when the source is frozen.
     */
    @GameTest
    public void freezeBlocksExecuteOnCastOnSource(GameTestHelper helper) {
        ServerPlayer controller = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        ServerPlayer frozenCaster = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(3.0, 2.0, 3.0));
        PreparedSkillUse freezePrepared = Skills.prepareUse(
                icePrison(helper),
                skillUseContext(helper, newHolder(helper), MonsterStats.resolveBaseHolder(frozenCaster).orElse(newHolder(helper)), 0.0, 0.99)
        );
        PreparedSkillUse strikePrepared = Skills.prepareUse(
                basicStrike(helper),
                skillUseContext(helper, newHolder(helper), MonsterStats.resolveBaseHolder(zombie).orElse(newHolder(helper)), 0.0, 0.25)
        );

        Skills.executeOnCast(SkillExecutionContext.forCast(freezePrepared, helper.getLevel(), controller, Optional.of(frozenCaster)));
        float startingHealth = zombie.getHealth();
        SkillExecutionResult result = Skills.executeOnCast(
                SkillExecutionContext.forCast(strikePrepared, helper.getLevel(), frozenCaster, Optional.of(zombie))
        );

        helper.assertValueEqual(result.executedActions(), 0, "Frozen sources should not execute on-cast actions");
        helper.assertTrue(result.skippedActions() >= 2, "Frozen sources should report skipped actions");
        helper.assertTrue(result.warnings().stream().anyMatch(warning -> warning.contains("freeze")),
                "Frozen cast attempts should surface a freeze warning");
        helper.assertValueEqual(zombie.getHealth(), startingHealth, "Frozen cast attempts should not damage the target");
        helper.succeed();
    }

    /**
     * Verifies that selected active skill casts also no-op with a warning when the player is stunned.
     */
    @GameTest
    public void castSelectedSkillNoopsWhenPlayerIsStunned(GameTestHelper helper) {
        ServerPlayer controller = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        ServerPlayer stunnedPlayer = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(3.0, 2.0, 3.0));
        stunnedPlayer.setItemSlot(SocketedEquipmentSlot.MAIN_HAND.equipmentSlot(), basicStrikeStack());
        PlayerActiveSkills.select(stunnedPlayer, new SelectedActiveSkillRef(SocketedEquipmentSlot.MAIN_HAND, BASIC_STRIKE_SKILL_ID));
        PreparedSkillUse stunPrepared = Skills.prepareUse(
                dazingTap(helper),
                skillUseContext(helper, newHolder(helper), MonsterStats.resolveBaseHolder(stunnedPlayer).orElse(newHolder(helper)), 0.0, 0.99)
        );

        Skills.executeOnCast(SkillExecutionContext.forCast(stunPrepared, helper.getLevel(), controller, Optional.of(stunnedPlayer)));
        float startingHealth = zombie.getHealth();
        SelectedSkillCastResult result = Skills.castSelectedSkill(
                stunnedPlayer,
                skillUseContext(helper, newHolder(helper), MonsterStats.resolveBaseHolder(zombie).orElse(newHolder(helper)), 0.0, 0.25),
                Optional.of(zombie)
        );

        helper.assertTrue(result.success(), "Selected casts should fail-safe into a runtime no-op instead of hard failing when stunned");
        helper.assertValueEqual(result.executionResult().orElseThrow().executedActions(), 0,
                "Stunned selected casts should execute no actions");
        helper.assertTrue(result.warnings().stream().anyMatch(warning -> warning.contains("stun")),
                "Stunned selected casts should surface a stun warning");
        helper.assertValueEqual(zombie.getHealth(), startingHealth, "Stunned selected casts should not damage the target");
        helper.succeed();
    }

    private static StatHolder newHolder(GameTestHelper helper) {
        return StatHolders.create(StatRegistryAccess.statRegistry(helper));
    }

    private static ServerPlayer controlTarget(GameTestHelper helper, Vec3 position) {
        ServerPlayer target = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        target.snapTo(helper.absoluteVec(position));
        PlayerCombatStats.get(target).setBaseValue(CombatStats.LIFE, 20.0);
        PlayerCombatStats.get(target).setBaseValue(CombatStats.AILMENT_THRESHOLD, 20.0);
        return target;
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

    private static SkillDefinition icePrison(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(ICE_PRISON_SKILL_ID)
                .orElseThrow(() -> helper.assertionException("Ice prison should decode successfully"));
    }

    private static SkillDefinition deepFreeze(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(DEEP_FREEZE_SKILL_ID)
                .orElseThrow(() -> helper.assertionException("Deep freeze should decode successfully"));
    }

    private static SkillDefinition dazingTap(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(DAZING_TAP_SKILL_ID)
                .orElseThrow(() -> helper.assertionException("Dazing tap should decode successfully"));
    }

    private static SkillDefinition concussiveSlam(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(CONCUSSIVE_SLAM_SKILL_ID)
                .orElseThrow(() -> helper.assertionException("Concussive slam should decode successfully"));
    }

    private static SkillDefinition basicStrike(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(BASIC_STRIKE_SKILL_ID)
                .orElseThrow(() -> helper.assertionException("Basic strike should decode successfully"));
    }

    private static SkillDefinition targetedNegativePurge() {
        return new SkillDefinition(
                NEGATIVE_PURGE_SKILL_ID.toString(),
                SkillConfig.DEFAULT,
                new SkillAttached(
                        List.of(new SkillRule(
                                Set.of(SkillTargetSelector.target()),
                                List.of(new SkillAction(SkillActionType.REMOVE_EFFECT, Map.of("purge", "negative"))),
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

    private static SkillDefinition controlAilmentSkill(
            String identifier,
            AilmentType ailmentType,
            double baseDamage,
            int durationTicks,
            SkillAilmentRefreshPolicy refreshPolicy
    ) {
        String damageKey = ailmentType == AilmentType.FREEZE ? "base_damage_cold" : "base_damage_physical";
        String soundId = ailmentType == AilmentType.FREEZE
                ? "minecraft:block.glass.break"
                : "minecraft:entity.player.attack.knockback";
        Set<SkillTag> tags = ailmentType == AilmentType.FREEZE
                ? Set.of(SkillTag.SPELL)
                : Set.of(SkillTag.ATTACK, SkillTag.MELEE);
        return new SkillDefinition(
                identifier,
                new SkillConfig(
                        "",
                        "mana",
                        0.0,
                        10,
                        0,
                        "",
                        "",
                        false,
                        false,
                        1,
                        0,
                        0.0,
                        tags
                ),
                new SkillAttached(
                        List.of(new SkillRule(
                                Set.of(SkillTargetSelector.target()),
                                List.of(
                                        new SkillAction(SkillActionType.SOUND, Map.of("sound_id", soundId)),
                                        new SkillAction(SkillActionType.DAMAGE, Map.of(
                                                damageKey, Double.toString(baseDamage)
                                        )),
                                        new SkillAction(SkillActionType.APPLY_AILMENT, Map.of(
                                                "ailment_id", ailmentType.serializedName(),
                                                "chance", "100.0",
                                                "duration_ticks", Integer.toString(durationTicks),
                                                "potency_multiplier", "100.0",
                                                "refresh_policy", refreshPolicy.serializedName()
                                        ))
                                ),
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

    private static AilmentPayload payload(GameTestHelper helper, LivingEntity entity, AilmentType type) {
        return Ailments.get(entity)
                .flatMap(state -> state.get(type))
                .orElseThrow(() -> helper.assertionException(type.serializedName() + " payload should exist on the target"));
    }

    private static net.minecraft.world.effect.MobEffect effect(AilmentType type) {
        return BuiltInRegistries.MOB_EFFECT.getOptional(type.effectId())
                .orElseThrow(() -> new IllegalStateException("Missing ailment effect: " + type.effectId()));
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
}
