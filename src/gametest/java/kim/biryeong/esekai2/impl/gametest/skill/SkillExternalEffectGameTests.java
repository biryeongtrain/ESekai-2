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
import kim.biryeong.esekai2.api.player.resource.PlayerResources;
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
import kim.biryeong.esekai2.api.skill.effect.SkillEffectPurgeMode;
import kim.biryeong.esekai2.api.skill.effect.MobEffectRefreshPolicy;
import kim.biryeong.esekai2.api.skill.execution.PreparedApplyBuffAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedApplyDotAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedHealAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedRemoveEffectAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedResourceDeltaAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSkillAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSkillUse;
import kim.biryeong.esekai2.api.skill.execution.SelectedSkillCastResult;
import kim.biryeong.esekai2.api.skill.execution.SelectedSkillUseResult;
import kim.biryeong.esekai2.api.skill.execution.SkillExecutionContext;
import kim.biryeong.esekai2.api.skill.execution.SkillExecutionResult;
import kim.biryeong.esekai2.api.skill.execution.SkillUseContext;
import kim.biryeong.esekai2.api.skill.execution.Skills;
import kim.biryeong.esekai2.api.skill.support.SkillSupportDefinition;
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
    private static final Identifier CLEANSE_SPECTRUM_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "cleanse_spectrum");
    private static final Identifier PURITY_WAVE_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "purity_wave");
    private static final Identifier TAINTED_RELEASE_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "tainted_release");
    private static final Identifier BLANK_SLATE_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "blank_slate");
    private static final Identifier CATHARTIC_WAVE_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "cathartic_wave");
    private static final Identifier PURGING_HEX_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "purging_hex");
    private static final Identifier VENOM_STRIKE_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "venom_strike");
    private static final Identifier SEARING_BRAND_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "searing_brand");
    private static final Identifier RESTORATIVE_PULSE_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "restorative_pulse");
    private static final Identifier MANA_SURGE_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "mana_surge");
    private static final Identifier SUPPORT_COMPOUND_CLEANSE_ID = Identifier.fromNamespaceAndPath("esekai2", "support_compound_cleanse");
    private static final Identifier SUPPORT_MALEVOLENT_WAVE_ID = Identifier.fromNamespaceAndPath("esekai2", "support_malevolent_wave");
    private static final Identifier SUPPORT_LASTING_HEX_ID = Identifier.fromNamespaceAndPath("esekai2", "support_lasting_hex");
    private static final Identifier SUPPORT_QUICK_FOCUS_ID = Identifier.fromNamespaceAndPath("esekai2", "support_quick_focus");
    private static final Identifier SUPPORT_TOXIC_CLEANSE_ID = Identifier.fromNamespaceAndPath("esekai2", "support_toxic_cleanse");
    private static final Identifier SUPPORT_LINGERING_BRAND_ID = Identifier.fromNamespaceAndPath("esekai2", "support_lingering_brand");
    private static final Identifier SUPPORT_EMPOWERING_PULSE_ID = Identifier.fromNamespaceAndPath("esekai2", "support_empowering_pulse");
    private static final Identifier SUPPORT_ABUNDANT_SURGE_ID = Identifier.fromNamespaceAndPath("esekai2", "support_abundant_surge");
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
        helper.assertTrue(skillRegistry(helper).containsKey(CLEANSE_SPECTRUM_SKILL_ID), "Cleanse spectrum should load into the skill registry");
        helper.assertTrue(skillRegistry(helper).containsKey(PURITY_WAVE_SKILL_ID), "Purity wave should load into the skill registry");
        helper.assertTrue(skillRegistry(helper).containsKey(TAINTED_RELEASE_SKILL_ID), "Tainted release should load into the skill registry");
        helper.assertTrue(skillRegistry(helper).containsKey(BLANK_SLATE_SKILL_ID), "Blank slate should load into the skill registry");
        helper.assertTrue(skillRegistry(helper).containsKey(CATHARTIC_WAVE_SKILL_ID), "Cathartic wave should load into the skill registry");
        helper.assertTrue(skillRegistry(helper).containsKey(PURGING_HEX_SKILL_ID), "Purging hex should load into the skill registry");
        helper.assertTrue(skillRegistry(helper).containsKey(SEARING_BRAND_SKILL_ID), "Searing brand should load into the skill registry");
        helper.assertTrue(skillRegistry(helper).containsKey(RESTORATIVE_PULSE_SKILL_ID), "Restorative pulse should load into the skill registry");
        helper.assertTrue(skillRegistry(helper).containsKey(MANA_SURGE_SKILL_ID), "Mana surge should load into the skill registry");
        helper.assertTrue(supportRegistry(helper).containsKey(SUPPORT_COMPOUND_CLEANSE_ID), "Compound cleanse support should load into the support registry");
        helper.assertTrue(supportRegistry(helper).containsKey(SUPPORT_MALEVOLENT_WAVE_ID), "Malevolent wave support should load into the support registry");
        helper.assertTrue(supportRegistry(helper).containsKey(SUPPORT_LASTING_HEX_ID), "Lasting hex support should load into the support registry");
        helper.assertTrue(supportRegistry(helper).containsKey(SUPPORT_QUICK_FOCUS_ID), "Quick focus support should load into the support registry");
        helper.assertTrue(supportRegistry(helper).containsKey(SUPPORT_TOXIC_CLEANSE_ID), "Toxic cleanse support should load into the support registry");
        helper.assertTrue(supportRegistry(helper).containsKey(SUPPORT_LINGERING_BRAND_ID), "Lingering brand support should load into the support registry");
        helper.assertTrue(supportRegistry(helper).containsKey(SUPPORT_EMPOWERING_PULSE_ID), "Empowering pulse should load into the support registry");
        helper.assertTrue(supportRegistry(helper).containsKey(SUPPORT_ABUNDANT_SURGE_ID), "Abundant surge should load into the support registry");
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
     * Verifies that heal and resource_delta actions decode into concrete prepared action payloads.
     */
    @GameTest
    public void prepareUseBuildsHealAndResourceActionsFromConfig(GameTestHelper helper) {
        PreparedSkillUse healPrepared = Skills.prepareUse(
                restorativePulse(helper),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );
        PreparedSkillUse resourcePrepared = Skills.prepareUse(
                manaSurge(helper),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );

        PreparedHealAction healAction = (PreparedHealAction) healPrepared.onCastActions().stream()
                .filter(PreparedHealAction.class::isInstance)
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Restorative pulse should prepare one heal action"));
        PreparedResourceDeltaAction resourceAction = (PreparedResourceDeltaAction) resourcePrepared.onCastActions().stream()
                .filter(PreparedResourceDeltaAction.class::isInstance)
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Mana surge should prepare one resource_delta action"));

        helper.assertValueEqual(healAction.actionType(), "heal", "Prepared heal action should preserve its action type");
        helper.assertValueEqual(healAction.amount(), 4.0, "Prepared heal action should resolve amount");
        helper.assertValueEqual(resourceAction.actionType(), "resource_delta", "Prepared resource_delta action should preserve its action type");
        helper.assertValueEqual(resourceAction.resource(), PreparedResourceDeltaAction.MANA_RESOURCE, "Prepared resource_delta action should preserve resource");
        helper.assertValueEqual(resourceAction.amount(), 4.0, "Prepared resource_delta action should resolve amount");
        helper.succeed();
    }

    /**
     * Verifies that invalid resource_delta payloads surface warnings instead of preparing broken runtime actions.
     */
    @GameTest
    public void prepareUseWarnsOnInvalidResourceDelta(GameTestHelper helper) {
        SkillDefinition invalidResourceSkill = new SkillDefinition(
                "esekai2:invalid_resource_delta",
                SkillConfig.DEFAULT,
                new SkillAttached(
                        List.of(new SkillRule(
                                Set.of(SkillTargetSelector.self()),
                                List.of(new SkillAction(SkillActionType.RESOURCE_DELTA, Map.of(
                                        "resource", "life",
                                        "amount", "4.0"
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

        PreparedSkillUse prepared = Skills.prepareUse(
                invalidResourceSkill,
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );

        helper.assertTrue(prepared.onCastActions().isEmpty(), "Invalid resource_delta payloads should not produce prepared actions");
        helper.assertTrue(
                prepared.warnings().stream().anyMatch(warning -> warning.contains("resource_delta")),
                "Invalid resource_delta payloads should surface a preparation warning"
        );
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
        PreparedSkillUse multiCleansePrepared = Skills.prepareUse(
                cleanseSpectrum(helper),
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
        PreparedRemoveEffectAction multiCleanseAction = (PreparedRemoveEffectAction) multiCleansePrepared.onCastActions().stream()
                .filter(PreparedRemoveEffectAction.class::isInstance)
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Cleanse spectrum should prepare one remove_effect action"));
        PreparedRemoveEffectAction targetCleanseAction = (PreparedRemoveEffectAction) targetCleansePrepared.onCastActions().stream()
                .filter(PreparedRemoveEffectAction.class::isInstance)
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Purging hex should prepare one remove_effect action"));

        helper.assertValueEqual(selfCleanseAction.effectId(), BATTLE_FOCUS_EFFECT_ID, "Prepared self-cleanse action should preserve its effect id");
        helper.assertValueEqual(selfCleanseAction.actionType(), "remove_effect", "Prepared self-cleanse action should preserve remove_effect type");
        helper.assertValueEqual(
                multiCleanseAction.effectIds(),
                List.of(BATTLE_FOCUS_EFFECT_ID, POISON_EFFECT_ID),
                "Prepared multi-cleanse action should preserve ordered effect_ids without flattening to a scalar"
        );
        helper.assertValueEqual(multiCleanseAction.actionType(), "remove_effect", "Prepared multi-cleanse action should preserve remove_effect type");
        helper.assertValueEqual(targetCleanseAction.effectId(), CRIPPLING_HEX_EFFECT_ID, "Prepared target-cleanse action should preserve its effect id");
        helper.assertValueEqual(targetCleanseAction.actionType(), "remove_effect", "Prepared target-cleanse action should preserve remove_effect type");
        helper.succeed();
    }

    /**
     * Verifies that broad purge remove_effect actions preserve their purge mode and union with explicit ids.
     */
    @GameTest
    public void prepareUseBuildsPurgeRemoveEffectActionsFromConfig(GameTestHelper helper) {
        PreparedRemoveEffectAction positivePurgeAction = preparedRemoveEffectAction(
                Skills.prepareUse(purityWave(helper), skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)),
                helper,
                "Purity wave should prepare one remove_effect action"
        );
        PreparedRemoveEffectAction negativePurgeAction = preparedRemoveEffectAction(
                Skills.prepareUse(taintedRelease(helper), skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)),
                helper,
                "Tainted release should prepare one remove_effect action"
        );
        PreparedRemoveEffectAction allPurgeAction = preparedRemoveEffectAction(
                Skills.prepareUse(blankSlate(helper), skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)),
                helper,
                "Blank slate should prepare one remove_effect action"
        );
        PreparedRemoveEffectAction unionPurgeAction = preparedRemoveEffectAction(
                Skills.prepareUse(catharticWave(helper), skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)),
                helper,
                "Cathartic wave should prepare one remove_effect action"
        );

        helper.assertValueEqual(positivePurgeAction.purgeMode().orElseThrow(), SkillEffectPurgeMode.POSITIVE, "Purity wave should preserve purge:positive");
        helper.assertValueEqual(negativePurgeAction.purgeMode().orElseThrow(), SkillEffectPurgeMode.NEGATIVE, "Tainted release should preserve purge:negative");
        helper.assertValueEqual(allPurgeAction.purgeMode().orElseThrow(), SkillEffectPurgeMode.ALL, "Blank slate should preserve purge:all");
        helper.assertValueEqual(unionPurgeAction.purgeMode().orElseThrow(), SkillEffectPurgeMode.POSITIVE, "Cathartic wave should preserve purge:positive");
        helper.assertValueEqual(unionPurgeAction.effectIds(), List.of(POISON_EFFECT_ID), "Cathartic wave should preserve explicit union effect ids");
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
     * Verifies that heal actions restore health but do not exceed the target's maximum health.
     */
    @GameTest
    public void executeOnCastHealsCasterWithoutOverheal(GameTestHelper helper) {
        Player caster = helper.makeMockPlayer(GameType.SURVIVAL);
        caster.setHealth(caster.getMaxHealth() - 2.0F);
        PreparedSkillUse prepared = Skills.prepareUse(
                restorativePulse(helper),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );

        SkillExecutionResult result = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.empty())
        );

        helper.assertValueEqual(result.executedActions(), 2, "Restorative pulse should execute both sound and heal actions");
        helper.assertValueEqual(caster.getHealth(), caster.getMaxHealth(), "Heal actions should clamp at maximum health");
        helper.succeed();
    }

    /**
     * Verifies that resource_delta actions restore mana on direct player-sourced casts.
     */
    @GameTest
    public void executeOnCastAppliesManaDeltaToPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        StatHolder attacker = newHolder(helper);
        attacker.setBaseValue(CombatStats.MANA, 20.0);
        PlayerResources.setMana(player, 3.0, 20.0);
        PreparedSkillUse prepared = Skills.prepareUse(
                manaSurge(helper),
                skillUseContext(helper, attacker, newHolder(helper), 0.0, 0.99)
        );

        SkillExecutionResult result = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), player, Optional.empty())
        );

        helper.assertValueEqual(result.executedActions(), 2, "Mana surge should execute both sound and resource_delta actions");
        helper.assertValueEqual(PlayerResources.getMana(player, 20.0), 7.0, "resource_delta should restore the configured mana amount");
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
     * Verifies that one remove_effect action can clear both a vanilla effect and a built-in ailment in one cast.
     */
    @GameTest
    public void executeOnCastRemovesMultipleEffectsFromCaster(GameTestHelper helper) {
        Player setupCaster = helper.makeMockPlayer(GameType.CREATIVE);
        ServerPlayer caster = helper.makeMockServerPlayerInLevel();
        caster.addEffect(new MobEffectInstance(
                BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, BATTLE_FOCUS_EFFECT_ID)),
                80,
                1
        ));

        PreparedSkillUse poisonPrepared = Skills.prepareUse(
                venomStrike(helper),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );
        Skills.executeOnCast(
                SkillExecutionContext.forCast(poisonPrepared, helper.getLevel(), setupCaster, Optional.of(caster))
        );
        helper.assertTrue(caster.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, BATTLE_FOCUS_EFFECT_ID))),
                "Multi-cleanse setup should apply speed before the cleanse cast");
        helper.assertTrue(caster.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, POISON_EFFECT_ID))),
                "Multi-cleanse setup should apply poison before the cleanse cast");
        helper.assertTrue(Ailments.get(caster).flatMap(state -> state.get(AilmentType.POISON)).isPresent(),
                "Multi-cleanse setup should attach poison payload before the cleanse cast");

        PreparedSkillUse prepared = Skills.prepareUse(
                cleanseSpectrum(helper),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );

        SkillExecutionResult result = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.empty())
        );

        helper.assertValueEqual(result.executedActions(), 2, "Cleanse spectrum should execute both sound and remove_effect actions when configured effects are present");
        helper.assertTrue(!caster.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, BATTLE_FOCUS_EFFECT_ID))),
                "Cleanse spectrum should remove the configured vanilla effect");
        helper.assertTrue(!caster.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, POISON_EFFECT_ID))),
                "Cleanse spectrum should remove the configured poison MobEffect identity");
        helper.assertTrue(Ailments.get(caster).flatMap(state -> state.get(AilmentType.POISON)).isEmpty(),
                "Cleanse spectrum should clear the poison attachment payload");
        helper.succeed();
    }

    /**
     * Verifies that purge:positive removes beneficial effects without clearing harmful ones.
     */
    @GameTest
    public void executeOnCastPurgePositiveRemovesBeneficialEffects(GameTestHelper helper) {
        ServerPlayer caster = helper.makeMockServerPlayerInLevel();
        caster.addEffect(new MobEffectInstance(
                BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, BATTLE_FOCUS_EFFECT_ID)),
                80,
                1
        ));
        caster.addEffect(new MobEffectInstance(
                BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, CRIPPLING_HEX_EFFECT_ID)),
                80,
                0
        ));

        PreparedSkillUse prepared = Skills.prepareUse(
                purityWave(helper),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );

        SkillExecutionResult result = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.empty())
        );

        helper.assertValueEqual(result.executedActions(), 2, "Purity wave should execute both sound and remove_effect actions when a beneficial effect is present");
        helper.assertTrue(!caster.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, BATTLE_FOCUS_EFFECT_ID))),
                "Purity wave should remove beneficial effects");
        helper.assertTrue(caster.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, CRIPPLING_HEX_EFFECT_ID))),
                "Purity wave should leave harmful effects untouched");
        helper.succeed();
    }

    /**
     * Verifies that purge:negative removes harmful effects and built-in ailment payloads without clearing beneficial ones.
     */
    @GameTest
    public void executeOnCastPurgeNegativeRemovesHarmfulEffectsAndAilments(GameTestHelper helper) {
        Player setupCaster = helper.makeMockPlayer(GameType.CREATIVE);
        ServerPlayer caster = helper.makeMockServerPlayerInLevel();
        caster.addEffect(new MobEffectInstance(
                BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, BATTLE_FOCUS_EFFECT_ID)),
                80,
                1
        ));
        caster.addEffect(new MobEffectInstance(
                BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, CRIPPLING_HEX_EFFECT_ID)),
                80,
                0
        ));

        PreparedSkillUse poisonPrepared = Skills.prepareUse(
                venomStrike(helper),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );
        Skills.executeOnCast(
                SkillExecutionContext.forCast(poisonPrepared, helper.getLevel(), setupCaster, Optional.of(caster))
        );

        PreparedSkillUse prepared = Skills.prepareUse(
                taintedRelease(helper),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );

        SkillExecutionResult result = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.empty())
        );

        helper.assertValueEqual(result.executedActions(), 2, "Tainted release should execute both sound and remove_effect actions when harmful effects are present");
        helper.assertTrue(caster.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, BATTLE_FOCUS_EFFECT_ID))),
                "Tainted release should leave beneficial effects untouched");
        helper.assertTrue(!caster.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, CRIPPLING_HEX_EFFECT_ID))),
                "Tainted release should remove harmful vanilla effects");
        helper.assertTrue(!caster.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, POISON_EFFECT_ID))),
                "Tainted release should remove harmful ailment identities");
        helper.assertTrue(Ailments.get(caster).flatMap(state -> state.get(AilmentType.POISON)).isEmpty(),
                "Tainted release should clear harmful ailment attachment payloads");
        helper.succeed();
    }

    /**
     * Verifies that purge:all removes all active effects from the caster.
     */
    @GameTest
    public void executeOnCastPurgeAllRemovesAllActiveEffects(GameTestHelper helper) {
        Player setupCaster = helper.makeMockPlayer(GameType.CREATIVE);
        ServerPlayer caster = helper.makeMockServerPlayerInLevel();
        caster.addEffect(new MobEffectInstance(
                BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, BATTLE_FOCUS_EFFECT_ID)),
                80,
                1
        ));
        caster.addEffect(new MobEffectInstance(
                BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, CRIPPLING_HEX_EFFECT_ID)),
                80,
                0
        ));

        PreparedSkillUse poisonPrepared = Skills.prepareUse(
                venomStrike(helper),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );
        Skills.executeOnCast(
                SkillExecutionContext.forCast(poisonPrepared, helper.getLevel(), setupCaster, Optional.of(caster))
        );

        PreparedSkillUse prepared = Skills.prepareUse(
                blankSlate(helper),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );

        SkillExecutionResult result = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.empty())
        );

        helper.assertValueEqual(result.executedActions(), 2, "Blank slate should execute both sound and remove_effect actions when any active effects are present");
        helper.assertTrue(caster.getActiveEffects().isEmpty(), "Blank slate should remove all active effects");
        helper.assertTrue(Ailments.get(caster).flatMap(state -> state.get(AilmentType.POISON)).isEmpty(),
                "Blank slate should clear ailment attachment payloads while purging all");
        helper.succeed();
    }

    /**
     * Verifies that explicit effect ids are unioned with broad purge targets.
     */
    @GameTest
    public void executeOnCastPurgeUnionRemovesExplicitAndPurgedTargets(GameTestHelper helper) {
        Player setupCaster = helper.makeMockPlayer(GameType.CREATIVE);
        ServerPlayer caster = helper.makeMockServerPlayerInLevel();
        caster.addEffect(new MobEffectInstance(
                BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, BATTLE_FOCUS_EFFECT_ID)),
                80,
                1
        ));

        PreparedSkillUse poisonPrepared = Skills.prepareUse(
                venomStrike(helper),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );
        Skills.executeOnCast(
                SkillExecutionContext.forCast(poisonPrepared, helper.getLevel(), setupCaster, Optional.of(caster))
        );

        PreparedSkillUse prepared = Skills.prepareUse(
                catharticWave(helper),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );

        SkillExecutionResult result = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.empty())
        );

        helper.assertValueEqual(result.executedActions(), 2, "Cathartic wave should execute both sound and remove_effect actions when purge union targets are present");
        helper.assertTrue(!caster.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, BATTLE_FOCUS_EFFECT_ID))),
                "Cathartic wave should remove the purged beneficial effect");
        helper.assertTrue(!caster.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, POISON_EFFECT_ID))),
                "Cathartic wave should remove the explicit ailment effect target");
        helper.assertTrue(Ailments.get(caster).flatMap(state -> state.get(AilmentType.POISON)).isEmpty(),
                "Cathartic wave should clear the explicit ailment attachment payload");
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
     * Verifies that multi-remove still succeeds when only a subset of the configured effect_ids is currently present.
     */
    @GameTest
    public void executeOnCastRemovesPresentSubsetOfConfiguredEffects(GameTestHelper helper) {
        Player setupCaster = helper.makeMockPlayer(GameType.CREATIVE);
        ServerPlayer caster = helper.makeMockServerPlayerInLevel();
        PreparedSkillUse poisonPrepared = Skills.prepareUse(
                venomStrike(helper),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );
        Skills.executeOnCast(
                SkillExecutionContext.forCast(poisonPrepared, helper.getLevel(), setupCaster, Optional.of(caster))
        );

        PreparedSkillUse prepared = Skills.prepareUse(
                cleanseSpectrum(helper),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );

        SkillExecutionResult result = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.empty())
        );

        helper.assertValueEqual(result.executedActions(), 2, "Cleanse spectrum should count as executed when at least one configured effect is removed");
        helper.assertValueEqual(result.skippedActions(), 0, "Cleanse spectrum should not be skipped when one configured effect is successfully removed");
        helper.assertTrue(!caster.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, POISON_EFFECT_ID))),
                "Cleanse spectrum should remove poison even when speed was never present");
        helper.assertTrue(Ailments.get(caster).flatMap(state -> state.get(AilmentType.POISON)).isEmpty(),
                "Cleanse spectrum should still clear poison attachment payload when only a subset of the configured list existed");
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
     * Verifies that purge:positive removes beneficial effects without removing harmful ones.
     */
    @GameTest
    public void executeOnCastPurgesPositiveEffects(GameTestHelper helper) {
        ServerPlayer caster = helper.makeMockServerPlayerInLevel();
        caster.addEffect(new MobEffectInstance(
                BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, BATTLE_FOCUS_EFFECT_ID)),
                80,
                1
        ));
        caster.addEffect(new MobEffectInstance(
                BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, CRIPPLING_HEX_EFFECT_ID)),
                80,
                0
        ));

        SkillExecutionResult result = Skills.executeOnCast(
                SkillExecutionContext.forCast(
                        Skills.prepareUse(purityWave(helper), skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)),
                        helper.getLevel(),
                        caster,
                        Optional.empty()
                )
        );

        helper.assertValueEqual(result.executedActions(), 2, "Purity wave should execute both sound and remove_effect when a beneficial effect exists");
        helper.assertTrue(!caster.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, BATTLE_FOCUS_EFFECT_ID))),
                "Purity wave should remove speed through purge:positive");
        helper.assertTrue(caster.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, CRIPPLING_HEX_EFFECT_ID))),
                "Purity wave should leave harmful effects untouched");
        helper.succeed();
    }

    /**
     * Verifies that purge:negative removes harmful effects and ailment attachment payloads without clearing beneficial ones.
     */
    @GameTest
    public void executeOnCastPurgesNegativeEffects(GameTestHelper helper) {
        Player setupCaster = helper.makeMockPlayer(GameType.CREATIVE);
        ServerPlayer caster = helper.makeMockServerPlayerInLevel();
        caster.addEffect(new MobEffectInstance(
                BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, BATTLE_FOCUS_EFFECT_ID)),
                80,
                1
        ));
        caster.addEffect(new MobEffectInstance(
                BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, CRIPPLING_HEX_EFFECT_ID)),
                80,
                0
        ));
        Skills.executeOnCast(
                SkillExecutionContext.forCast(
                        Skills.prepareUse(venomStrike(helper), skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)),
                        helper.getLevel(),
                        setupCaster,
                        Optional.of(caster)
                )
        );

        SkillExecutionResult result = Skills.executeOnCast(
                SkillExecutionContext.forCast(
                        Skills.prepareUse(taintedRelease(helper), skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)),
                        helper.getLevel(),
                        caster,
                        Optional.empty()
                )
        );

        helper.assertValueEqual(result.executedActions(), 2, "Tainted release should execute both sound and remove_effect when harmful effects exist");
        helper.assertTrue(caster.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, BATTLE_FOCUS_EFFECT_ID))),
                "Tainted release should keep beneficial effects");
        helper.assertTrue(!caster.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, CRIPPLING_HEX_EFFECT_ID))),
                "Tainted release should remove harmful vanilla effects");
        helper.assertTrue(!caster.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, POISON_EFFECT_ID))),
                "Tainted release should remove harmful ailment identities");
        helper.assertTrue(Ailments.get(caster).flatMap(state -> state.get(AilmentType.POISON)).isEmpty(),
                "Tainted release should clear harmful ailment attachment payloads");
        helper.succeed();
    }

    /**
     * Verifies that purge:all removes every active effect category in one cast.
     */
    @GameTest
    public void executeOnCastPurgesAllEffects(GameTestHelper helper) {
        Player setupCaster = helper.makeMockPlayer(GameType.CREATIVE);
        ServerPlayer caster = helper.makeMockServerPlayerInLevel();
        caster.addEffect(new MobEffectInstance(
                BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, BATTLE_FOCUS_EFFECT_ID)),
                80,
                1
        ));
        caster.addEffect(new MobEffectInstance(
                BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, CRIPPLING_HEX_EFFECT_ID)),
                80,
                0
        ));
        Skills.executeOnCast(
                SkillExecutionContext.forCast(
                        Skills.prepareUse(venomStrike(helper), skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)),
                        helper.getLevel(),
                        setupCaster,
                        Optional.of(caster)
                )
        );

        SkillExecutionResult result = Skills.executeOnCast(
                SkillExecutionContext.forCast(
                        Skills.prepareUse(blankSlate(helper), skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)),
                        helper.getLevel(),
                        caster,
                        Optional.empty()
                )
        );

        helper.assertValueEqual(result.executedActions(), 2, "Blank slate should execute both sound and remove_effect when any effects exist");
        helper.assertTrue(!caster.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, BATTLE_FOCUS_EFFECT_ID))),
                "Blank slate should remove beneficial effects through purge:all");
        helper.assertTrue(!caster.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, CRIPPLING_HEX_EFFECT_ID))),
                "Blank slate should remove harmful vanilla effects through purge:all");
        helper.assertTrue(!caster.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, POISON_EFFECT_ID))),
                "Blank slate should remove ailment identities through purge:all");
        helper.assertTrue(Ailments.get(caster).flatMap(state -> state.get(AilmentType.POISON)).isEmpty(),
                "Blank slate should clear ailment attachment payloads through purge:all");
        helper.succeed();
    }

    /**
     * Verifies that explicit effect ids union with purge targets instead of replacing them.
     */
    @GameTest
    public void executeOnCastPurgesUnionOfPositiveAndExplicitEffects(GameTestHelper helper) {
        Player setupCaster = helper.makeMockPlayer(GameType.CREATIVE);
        ServerPlayer caster = helper.makeMockServerPlayerInLevel();
        caster.addEffect(new MobEffectInstance(
                BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, BATTLE_FOCUS_EFFECT_ID)),
                80,
                1
        ));
        caster.addEffect(new MobEffectInstance(
                BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, CRIPPLING_HEX_EFFECT_ID)),
                80,
                0
        ));
        Skills.executeOnCast(
                SkillExecutionContext.forCast(
                        Skills.prepareUse(venomStrike(helper), skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)),
                        helper.getLevel(),
                        setupCaster,
                        Optional.of(caster)
                )
        );

        SkillExecutionResult result = Skills.executeOnCast(
                SkillExecutionContext.forCast(
                        Skills.prepareUse(catharticWave(helper), skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)),
                        helper.getLevel(),
                        caster,
                        Optional.empty()
                )
        );

        helper.assertValueEqual(result.executedActions(), 2, "Cathartic wave should execute both sound and remove_effect when either purge or explicit targets exist");
        helper.assertTrue(!caster.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, BATTLE_FOCUS_EFFECT_ID))),
                "Cathartic wave should purge beneficial effects through purge:positive");
        helper.assertTrue(caster.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, CRIPPLING_HEX_EFFECT_ID))),
                "Cathartic wave should leave unrelated harmful effects untouched");
        helper.assertTrue(!caster.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, POISON_EFFECT_ID))),
                "Cathartic wave should remove explicit poison even though purge is positive");
        helper.assertTrue(Ailments.get(caster).flatMap(state -> state.get(AilmentType.POISON)).isEmpty(),
                "Cathartic wave should clear explicit poison attachment payload");
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
     * Verifies that linked support overrides can replace remove_effect.effect_ids with a whole ordered list.
     */
    @GameTest
    public void prepareSelectedUseAppliesMultiRemoveEffectSupportOverrides(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemSlot(
                SocketedEquipmentSlot.MAIN_HAND.equipmentSlot(),
                socketedSkillStack(Items.STICK, CLEANSE_FOCUS_SKILL_ID, List.of(supportRef(1, SUPPORT_COMPOUND_CLEANSE_ID)))
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

        helper.assertTrue(result.success(), "Selected cleanse focus should prepare successfully with multi-remove support");
        helper.assertValueEqual(
                removeEffectAction.effectIds(),
                List.of(CRIPPLING_HEX_EFFECT_ID, POISON_EFFECT_ID),
                "Linked support should replace remove_effect.effect_ids with the configured whole list"
        );
        helper.succeed();
    }

    /**
     * Verifies that selected socket-backed preparation can override remove_effect.purge through a linked support.
     */
    @GameTest
    public void prepareSelectedUseAppliesPurgeSupportOverrides(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemSlot(
                SocketedEquipmentSlot.MAIN_HAND.equipmentSlot(),
                socketedSkillStack(Items.STICK, PURITY_WAVE_SKILL_ID, List.of(supportRef(1, SUPPORT_MALEVOLENT_WAVE_ID)))
        );
        PlayerActiveSkills.select(player, new SelectedActiveSkillRef(SocketedEquipmentSlot.MAIN_HAND, PURITY_WAVE_SKILL_ID));

        SelectedSkillUseResult result = Skills.prepareSelectedUse(
                player,
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );

        PreparedRemoveEffectAction removeEffectAction = preparedRemoveEffectAction(result.preparedUse().orElseThrow(), helper,
                "Selected purity wave should still expose one remove_effect action");

        helper.assertTrue(result.success(), "Selected purity wave should prepare successfully");
        helper.assertValueEqual(removeEffectAction.purgeMode().orElseThrow(), SkillEffectPurgeMode.NEGATIVE,
                "Linked support should override remove_effect.purge on the selected cast path");
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
     * Verifies that selected remove_effect casts honor support-provided effect_ids whole-replacement at runtime.
     */
    @GameTest
    public void castSelectedSkillRemovesSupportOverrideEffectList(GameTestHelper helper) {
        Player setupCaster = helper.makeMockPlayer(GameType.CREATIVE);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.addEffect(new MobEffectInstance(
                BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, CRIPPLING_HEX_EFFECT_ID)),
                80,
                0
        ));

        PreparedSkillUse poisonPrepared = Skills.prepareUse(
                venomStrike(helper),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );
        Skills.executeOnCast(
                SkillExecutionContext.forCast(poisonPrepared, helper.getLevel(), setupCaster, Optional.of(player))
        );
        helper.assertTrue(player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, CRIPPLING_HEX_EFFECT_ID))),
                "Multi-remove selected cast setup should apply slowness before the cleanse cast");
        helper.assertTrue(player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, POISON_EFFECT_ID))),
                "Multi-remove selected cast setup should apply poison before the cleanse cast");
        helper.assertTrue(Ailments.get(player).flatMap(state -> state.get(AilmentType.POISON)).isPresent(),
                "Multi-remove selected cast setup should attach poison payload before the cleanse cast");

        player.setItemSlot(
                SocketedEquipmentSlot.MAIN_HAND.equipmentSlot(),
                socketedSkillStack(Items.STICK, CLEANSE_FOCUS_SKILL_ID, List.of(supportRef(1, SUPPORT_COMPOUND_CLEANSE_ID)))
        );
        PlayerActiveSkills.select(player, new SelectedActiveSkillRef(SocketedEquipmentSlot.MAIN_HAND, CLEANSE_FOCUS_SKILL_ID));

        SelectedSkillCastResult result = Skills.castSelectedSkill(
                player,
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99),
                Optional.empty()
        );

        helper.assertTrue(result.success(), "Selected cleanse focus cast should succeed with multi-remove support");
        helper.assertValueEqual(result.executionResult().orElseThrow().executedActions(), 2, "Selected cleanse focus should execute both sound and remove_effect actions when supported targets are present");
        helper.assertTrue(!player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, CRIPPLING_HEX_EFFECT_ID))),
                "Selected cleanse focus should remove the support-overridden slowness effect");
        helper.assertTrue(!player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, POISON_EFFECT_ID))),
                "Selected cleanse focus should remove the support-overridden poison effect");
        helper.assertTrue(Ailments.get(player).flatMap(state -> state.get(AilmentType.POISON)).isEmpty(),
                "Selected cleanse focus should clear the support-overridden poison attachment payload");
        helper.succeed();
    }

    /**
     * Verifies that selected remove_effect casts honor support-provided purge overrides at runtime.
     */
    @GameTest
    public void castSelectedSkillPurgesSupportOverriddenNegativeEffects(GameTestHelper helper) {
        Player setupCaster = helper.makeMockPlayer(GameType.CREATIVE);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.addEffect(new MobEffectInstance(
                BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, BATTLE_FOCUS_EFFECT_ID)),
                80,
                1
        ));
        player.addEffect(new MobEffectInstance(
                BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, CRIPPLING_HEX_EFFECT_ID)),
                80,
                0
        ));
        Skills.executeOnCast(
                SkillExecutionContext.forCast(
                        Skills.prepareUse(venomStrike(helper), skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)),
                        helper.getLevel(),
                        setupCaster,
                        Optional.of(player)
                )
        );

        player.setItemSlot(
                SocketedEquipmentSlot.MAIN_HAND.equipmentSlot(),
                socketedSkillStack(Items.STICK, PURITY_WAVE_SKILL_ID, List.of(supportRef(1, SUPPORT_MALEVOLENT_WAVE_ID)))
        );
        PlayerActiveSkills.select(player, new SelectedActiveSkillRef(SocketedEquipmentSlot.MAIN_HAND, PURITY_WAVE_SKILL_ID));

        SelectedSkillCastResult result = Skills.castSelectedSkill(
                player,
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99),
                Optional.empty()
        );

        helper.assertTrue(result.success(), "Selected purity wave cast should succeed with purge override");
        helper.assertValueEqual(result.executionResult().orElseThrow().executedActions(), 2,
                "Selected purity wave should execute both sound and remove_effect actions when overridden purge targets exist");
        helper.assertTrue(player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, BATTLE_FOCUS_EFFECT_ID))),
                "Selected purity wave should keep beneficial effects after purge override to negative");
        helper.assertTrue(!player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, CRIPPLING_HEX_EFFECT_ID))),
                "Selected purity wave should remove harmful vanilla effects after purge override");
        helper.assertTrue(!player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, POISON_EFFECT_ID))),
                "Selected purity wave should remove harmful ailment identities after purge override");
        helper.assertTrue(Ailments.get(player).flatMap(state -> state.get(AilmentType.POISON)).isEmpty(),
                "Selected purity wave should clear harmful ailment attachment payloads after purge override");
        helper.succeed();
    }

    /**
     * Verifies that selected remove_effect casts honor support-provided purge overrides at runtime.
     */
    @GameTest
    public void castSelectedSkillRemovesSupportOverridePurgeTargets(GameTestHelper helper) {
        Player setupCaster = helper.makeMockPlayer(GameType.CREATIVE);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.addEffect(new MobEffectInstance(
                BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, BATTLE_FOCUS_EFFECT_ID)),
                80,
                1
        ));
        player.addEffect(new MobEffectInstance(
                BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, CRIPPLING_HEX_EFFECT_ID)),
                80,
                0
        ));

        PreparedSkillUse poisonPrepared = Skills.prepareUse(
                venomStrike(helper),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );
        Skills.executeOnCast(
                SkillExecutionContext.forCast(poisonPrepared, helper.getLevel(), setupCaster, Optional.of(player))
        );

        player.setItemSlot(
                SocketedEquipmentSlot.MAIN_HAND.equipmentSlot(),
                socketedSkillStack(Items.STICK, PURITY_WAVE_SKILL_ID, List.of(supportRef(1, SUPPORT_MALEVOLENT_WAVE_ID)))
        );
        PlayerActiveSkills.select(player, new SelectedActiveSkillRef(SocketedEquipmentSlot.MAIN_HAND, PURITY_WAVE_SKILL_ID));

        SelectedSkillCastResult result = Skills.castSelectedSkill(
                player,
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99),
                Optional.empty()
        );

        helper.assertTrue(result.success(), "Selected purity wave cast should succeed with purge override support");
        helper.assertValueEqual(result.executionResult().orElseThrow().executedActions(), 2, "Selected purity wave should execute both sound and remove_effect actions when overridden purge targets are present");
        helper.assertTrue(player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, BATTLE_FOCUS_EFFECT_ID))),
                "Selected purity wave should keep beneficial effects when the support overrides purge to negative");
        helper.assertTrue(!player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, CRIPPLING_HEX_EFFECT_ID))),
                "Selected purity wave should remove the support-overridden harmful vanilla effect");
        helper.assertTrue(!player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(helper, POISON_EFFECT_ID))),
                "Selected purity wave should remove the support-overridden harmful ailment identity");
        helper.assertTrue(Ailments.get(player).flatMap(state -> state.get(AilmentType.POISON)).isEmpty(),
                "Selected purity wave should clear the support-overridden harmful ailment payload");
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
     * Verifies that linked support overrides can change heal amounts on the selected cast path.
     */
    @GameTest
    public void prepareSelectedUseAppliesHealSupportOverrides(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemSlot(
                SocketedEquipmentSlot.MAIN_HAND.equipmentSlot(),
                socketedSkillStack(Items.GHAST_TEAR, RESTORATIVE_PULSE_SKILL_ID, List.of(supportRef(1, SUPPORT_EMPOWERING_PULSE_ID)))
        );
        PlayerActiveSkills.select(player, new SelectedActiveSkillRef(SocketedEquipmentSlot.MAIN_HAND, RESTORATIVE_PULSE_SKILL_ID));

        SelectedSkillUseResult result = Skills.prepareSelectedUse(
                player,
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );

        PreparedHealAction healAction = (PreparedHealAction) result.preparedUse().orElseThrow().onCastActions().stream()
                .filter(PreparedHealAction.class::isInstance)
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Selected restorative pulse should still expose one heal action"));

        helper.assertTrue(result.success(), "Selected restorative pulse should prepare successfully");
        helper.assertValueEqual(healAction.amount(), 8.0, "Linked support should override heal amount");
        helper.succeed();
    }

    /**
     * Verifies that linked support overrides can change resource_delta amounts on the selected cast path.
     */
    @GameTest
    public void prepareSelectedUseAppliesResourceDeltaSupportOverrides(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemSlot(
                SocketedEquipmentSlot.MAIN_HAND.equipmentSlot(),
                socketedSkillStack(Items.LAPIS_LAZULI, MANA_SURGE_SKILL_ID, List.of(supportRef(1, SUPPORT_ABUNDANT_SURGE_ID)))
        );
        PlayerActiveSkills.select(player, new SelectedActiveSkillRef(SocketedEquipmentSlot.MAIN_HAND, MANA_SURGE_SKILL_ID));

        SelectedSkillUseResult result = Skills.prepareSelectedUse(
                player,
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );

        PreparedResourceDeltaAction resourceAction = (PreparedResourceDeltaAction) result.preparedUse().orElseThrow().onCastActions().stream()
                .filter(PreparedResourceDeltaAction.class::isInstance)
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Selected mana surge should still expose one resource_delta action"));

        helper.assertTrue(result.success(), "Selected mana surge should prepare successfully");
        helper.assertValueEqual(resourceAction.amount(), 10.0, "Linked support should override resource_delta amount");
        helper.succeed();
    }

    /**
     * Verifies that selected casts execute heal support overrides at runtime.
     */
    @GameTest
    public void castSelectedSkillAppliesHealSupportOverrideAtRuntime(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setHealth(player.getMaxHealth() - 10.0F);
        player.setItemSlot(
                SocketedEquipmentSlot.MAIN_HAND.equipmentSlot(),
                socketedSkillStack(Items.GHAST_TEAR, RESTORATIVE_PULSE_SKILL_ID, List.of(supportRef(1, SUPPORT_EMPOWERING_PULSE_ID)))
        );
        PlayerActiveSkills.select(player, new SelectedActiveSkillRef(SocketedEquipmentSlot.MAIN_HAND, RESTORATIVE_PULSE_SKILL_ID));

        SelectedSkillCastResult result = Skills.castSelectedSkill(
                player,
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99),
                Optional.empty()
        );

        helper.assertTrue(result.success(), "Selected restorative pulse cast should succeed");
        helper.assertValueEqual(result.executionResult().orElseThrow().executedActions(), 2, "Selected restorative pulse should execute both sound and heal actions");
        helper.assertValueEqual(player.getHealth(), player.getMaxHealth() - 2.0F, "Selected heal support override should increase the restored health");
        helper.succeed();
    }

    /**
     * Verifies that selected casts execute resource_delta support overrides at runtime.
     */
    @GameTest
    public void castSelectedSkillAppliesResourceDeltaSupportOverrideAtRuntime(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        StatHolder attacker = newHolder(helper);
        attacker.setBaseValue(CombatStats.MANA, 20.0);
        PlayerResources.setMana(player, 5.0, 20.0);
        player.setItemSlot(
                SocketedEquipmentSlot.MAIN_HAND.equipmentSlot(),
                socketedSkillStack(Items.LAPIS_LAZULI, MANA_SURGE_SKILL_ID, List.of(supportRef(1, SUPPORT_ABUNDANT_SURGE_ID)))
        );
        PlayerActiveSkills.select(player, new SelectedActiveSkillRef(SocketedEquipmentSlot.MAIN_HAND, MANA_SURGE_SKILL_ID));

        SelectedSkillCastResult result = Skills.castSelectedSkill(
                player,
                skillUseContext(helper, attacker, newHolder(helper), 0.0, 0.99),
                Optional.empty()
        );

        helper.assertTrue(result.success(), "Selected mana surge cast should succeed");
        helper.assertValueEqual(result.executionResult().orElseThrow().executedActions(), 2, "Selected mana surge should execute both sound and resource_delta actions");
        helper.assertValueEqual(PlayerResources.getMana(player, 20.0), 15.0, "Selected resource_delta support override should increase restored mana");
        helper.succeed();
    }

    /**
     * Verifies that generic apply_dot waits for its first interval, deals periodic damage, and does not tick again after expiry.
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
        float[] healthAfterExpiry = new float[1];

        helper.assertValueEqual(result.executedActions(), 2, "Searing brand should execute both sound and dot actions");
        helper.runAfterDelay(1, () ->
                helper.assertValueEqual(zombie.getHealth(), initialHealth, "The generic DoT should not tick before its first interval elapses")
        );
        helper.runAfterDelay(3, () ->
                helper.assertTrue(zombie.getHealth() < initialHealth, "The first DoT tick should damage the target shortly after cast")
        );
        helper.runAfterDelay(10, () -> healthAfterExpiry[0] = zombie.getHealth());
        helper.runAfterDelay(14, () -> {
            helper.assertValueEqual(zombie.getHealth(), healthAfterExpiry[0], "The generic DoT should stop dealing damage once its duration expires");
            helper.succeed();
        });
    }

    /**
     * Verifies that reapplying the same dot id refreshes duration past the original expiry and still stops cleanly after the refreshed window.
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

        float[] healthAfterOriginalExpiryWindow = new float[1];
        float[] healthAfterRefreshedExpiry = new float[1];
        helper.runAfterDelay(10, () -> healthAfterOriginalExpiryWindow[0] = zombie.getHealth());
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(
                    zombie.getHealth() < healthAfterOriginalExpiryWindow[0],
                    "Reapplying the same dot id should refresh duration and continue ticking past the original expiry window"
            );
        });
        helper.runAfterDelay(18, () -> healthAfterRefreshedExpiry[0] = zombie.getHealth());
        helper.runAfterDelay(19, () -> {
            helper.assertValueEqual(
                    zombie.getHealth(),
                    healthAfterRefreshedExpiry[0],
                    "The refreshed generic DoT should not deal extra ticks after its refreshed expiry window"
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

    private static SkillDefinition purityWave(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(PURITY_WAVE_SKILL_ID)
                .orElseThrow(() -> helper.assertionException("Purity wave should decode successfully"));
    }

    private static SkillDefinition taintedRelease(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(TAINTED_RELEASE_SKILL_ID)
                .orElseThrow(() -> helper.assertionException("Tainted release should decode successfully"));
    }

    private static SkillDefinition blankSlate(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(BLANK_SLATE_SKILL_ID)
                .orElseThrow(() -> helper.assertionException("Blank slate should decode successfully"));
    }

    private static SkillDefinition catharticWave(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(CATHARTIC_WAVE_SKILL_ID)
                .orElseThrow(() -> helper.assertionException("Cathartic wave should decode successfully"));
    }

    private static SkillDefinition cleanseSpectrum(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(CLEANSE_SPECTRUM_SKILL_ID)
                .orElseThrow(() -> helper.assertionException("Cleanse spectrum should decode successfully"));
    }

    private static SkillDefinition purgingHex(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(PURGING_HEX_SKILL_ID)
                .orElseThrow(() -> helper.assertionException("Purging hex should decode successfully"));
    }

    private static SkillDefinition searingBrand(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(SEARING_BRAND_SKILL_ID)
                .orElseThrow(() -> helper.assertionException("Searing brand should decode successfully"));
    }

    private static SkillDefinition restorativePulse(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(RESTORATIVE_PULSE_SKILL_ID)
                .orElseThrow(() -> helper.assertionException("Restorative pulse should decode successfully"));
    }

    private static SkillDefinition manaSurge(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(MANA_SURGE_SKILL_ID)
                .orElseThrow(() -> helper.assertionException("Mana surge should decode successfully"));
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

    private static PreparedRemoveEffectAction preparedRemoveEffectAction(
            PreparedSkillUse prepared,
            GameTestHelper helper,
            String failureMessage
    ) {
        return (PreparedRemoveEffectAction) prepared.onCastActions().stream()
                .filter(PreparedRemoveEffectAction.class::isInstance)
                .findFirst()
                .orElseThrow(() -> helper.assertionException(failureMessage));
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
