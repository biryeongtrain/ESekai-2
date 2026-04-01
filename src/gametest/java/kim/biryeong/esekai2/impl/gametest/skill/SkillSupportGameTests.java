package kim.biryeong.esekai2.impl.gametest.skill;

import com.mojang.serialization.JsonOps;
import kim.biryeong.esekai2.api.damage.calculation.DamageCalculationResult;
import kim.biryeong.esekai2.api.damage.breakdown.DamageType;
import kim.biryeong.esekai2.api.skill.calculation.SkillCalculationDefinition;
import kim.biryeong.esekai2.api.skill.calculation.SkillCalculationLookup;
import kim.biryeong.esekai2.api.skill.definition.SkillDefinition;
import kim.biryeong.esekai2.api.skill.definition.SkillRegistries;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillAction;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillActionType;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillPredicate;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillPredicateType;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillRule;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillTargetSelector;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillTargetType;
import kim.biryeong.esekai2.api.skill.execution.PreparedDamageAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedHealAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedProjectileAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedResourceDeltaAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSandstormParticleAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSkillAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSkillExecutionRoute;
import kim.biryeong.esekai2.api.skill.execution.PreparedSoundAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSummonAtSightAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSummonBlockAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSkillUse;
import kim.biryeong.esekai2.api.skill.execution.SkillExecutionContext;
import kim.biryeong.esekai2.api.skill.execution.SkillExecutionHooks;
import kim.biryeong.esekai2.api.skill.execution.SkillExecutionResult;
import kim.biryeong.esekai2.api.skill.execution.SkillUseContext;
import kim.biryeong.esekai2.api.skill.execution.Skills;
import kim.biryeong.esekai2.api.skill.support.SkillActionOverride;
import kim.biryeong.esekai2.api.skill.support.SkillSupportDefinition;
import kim.biryeong.esekai2.api.skill.support.SkillSupportEffect;
import kim.biryeong.esekai2.api.skill.support.SkillSupportRuleAppend;
import kim.biryeong.esekai2.api.skill.support.SkillSupportRuleAppendTarget;
import kim.biryeong.esekai2.api.skill.support.SkillSupportRuleTargetType;
import kim.biryeong.esekai2.api.item.socket.SocketedSkillItemState;
import kim.biryeong.esekai2.api.item.socket.SocketedSkillRef;
import kim.biryeong.esekai2.api.item.socket.SocketLinkGroup;
import kim.biryeong.esekai2.api.item.socket.SocketSlotType;
import kim.biryeong.esekai2.api.item.socket.SocketedSkills;
import kim.biryeong.esekai2.api.skill.tag.SkillTagCondition;
import kim.biryeong.esekai2.api.stat.holder.StatHolder;
import kim.biryeong.esekai2.api.stat.holder.StatHolders;
import kim.biryeong.esekai2.api.stat.modifier.ConditionalStatModifier;
import kim.biryeong.esekai2.api.skill.value.SkillValueExpression;
import kim.biryeong.esekai2.impl.skill.registry.SkillCalculationRegistryAccess;
import kim.biryeong.esekai2.impl.stat.registry.StatRegistryAccess;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;

/**
 * Verifies support registry fixtures and support effect behavior for item-linked skill pipelines.
 */
public final class SkillSupportGameTests {
    private static final Identifier FIREBALL_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "fireball");
    private static final Identifier BASIC_STRIKE_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "basic_strike");
    private static final Identifier BATTLE_FOCUS_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "battle_focus");
    private static final Identifier PREPARED_STATE_PROBE_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "prepared_state_probe");

    private static final Identifier SUPPORT_COST_BOOST_ID = Identifier.fromNamespaceAndPath("esekai2", "support_cost_boost");
    private static final Identifier SUPPORT_GUARD_FOCUS_ID = Identifier.fromNamespaceAndPath("esekai2", "support_guard_focus");
    private static final Identifier SUPPORT_BROKEN_FOCUS_ID = Identifier.fromNamespaceAndPath("esekai2", "support_broken_focus");
    private static final Identifier SUPPORT_PREPARED_STATE_TUNING_ID = Identifier.fromNamespaceAndPath("esekai2", "support_prepared_state_tuning");
    private static final Identifier SUPPORT_DAMAGE_AMP_ID = Identifier.fromNamespaceAndPath("esekai2", "support_fire_damage_amp");
    private static final Identifier SUPPORT_DAMAGE_LATE_AMP_ID = Identifier.fromNamespaceAndPath("esekai2", "support_fire_damage_late_amp");
    private static final Identifier FIREBALL_SUPPORT_BONUS_HIT_ID = Identifier.fromNamespaceAndPath("esekai2", "fireball_support_bonus_hit");
    private static final Identifier SUPPORT_CAST_ECHO_ID = Identifier.fromNamespaceAndPath("esekai2", "support_cast_echo");
    private static final Identifier SUPPORT_COMPONENT_ECHO_ID = Identifier.fromNamespaceAndPath("esekai2", "support_component_echo");
    private static final Identifier SUPPORT_MELEE_ONLY_ID = Identifier.fromNamespaceAndPath("esekai2", "support_melee_only");

    /**
     * Verifies that the new support registry loads the sample support fixtures.
     */
    @GameTest
    public void supportRegistryLoadsSampleDefinitions(GameTestHelper helper) {
        Registry<SkillSupportDefinition> registry = supportRegistry(helper);

        helper.assertTrue(registry.containsKey(SUPPORT_COST_BOOST_ID), "Support fixture for spell cost adjustment should be present");
        helper.assertTrue(registry.containsKey(SUPPORT_GUARD_FOCUS_ID), "Support fixture for resource id and cost overrides should be present");
        helper.assertTrue(registry.containsKey(SUPPORT_BROKEN_FOCUS_ID), "Support fixture for unsupported resource override safety should be present");
        helper.assertTrue(registry.containsKey(SUPPORT_PREPARED_STATE_TUNING_ID), "Support fixture for prepared-state config overrides should be present");
        helper.assertTrue(registry.containsKey(SUPPORT_DAMAGE_AMP_ID), "Support fixture for damage action modification should be present");
        helper.assertTrue(registry.containsKey(SUPPORT_DAMAGE_LATE_AMP_ID), "Late socket-order override support should be present");
        helper.assertTrue(registry.containsKey(SUPPORT_CAST_ECHO_ID), "On-cast appended rule support should be present");
        helper.assertTrue(registry.containsKey(SUPPORT_COMPONENT_ECHO_ID), "Component appended rule support should be present");
        helper.assertTrue(registry.containsKey(SUPPORT_MELEE_ONLY_ID), "Support fixture for tag-specific support gating should be present");
        helper.succeed();
    }

    /**
     * Verifies that a support definition and nested effect payloads survive codec round-trip.
     */
    @GameTest
    public void supportDefinitionCodecRoundTrip(GameTestHelper helper) {
        SkillSupportDefinition definition = support(SUPPORT_DAMAGE_AMP_ID, helper);

        var encoded = SkillSupportDefinition.CODEC.encodeStart(JsonOps.INSTANCE, definition)
                .getOrThrow(message -> new IllegalStateException("Failed to encode support definition: " + message));
        SkillSupportDefinition decoded = SkillSupportDefinition.CODEC.parse(JsonOps.INSTANCE, encoded)
                .getOrThrow(message -> new IllegalStateException("Failed to decode support definition: " + message));

        helper.assertValueEqual(decoded.identifier(), definition.identifier(), "Identifier should survive a support codec round-trip");
        helper.assertValueEqual(decoded.effects(), definition.effects(), "Effect payload should survive a support codec round-trip");
        helper.assertTrue(!decoded.effects().isEmpty(), "Round-tripped support should preserve non-empty effects");
        helper.succeed();
    }

    /**
     * Verifies that a support effect survives codec round-trip on its own.
     */
    @GameTest
    public void supportEffectCodecRoundTrip(GameTestHelper helper) {
        SkillSupportEffect effect = support(SUPPORT_CAST_ECHO_ID, helper).effects().getFirst();

        var encoded = SkillSupportEffect.CODEC.encodeStart(JsonOps.INSTANCE, effect)
                .getOrThrow(message -> new IllegalStateException("Failed to encode support effect: " + message));
        SkillSupportEffect decoded = SkillSupportEffect.CODEC.parse(JsonOps.INSTANCE, encoded)
                .getOrThrow(message -> new IllegalStateException("Failed to decode support effect: " + message));

        helper.assertValueEqual(decoded, effect, "Support effect codec should preserve typed appended rules");
        helper.succeed();
    }

    /**
     * Verifies that support rule append targets survive codec round-trip.
     */
    @GameTest
    public void supportRuleAppendTargetCodecRoundTrip(GameTestHelper helper) {
        SkillSupportRuleAppendTarget target = new SkillSupportRuleAppendTarget(SkillSupportRuleTargetType.ENTITY_COMPONENT, "default_entity_name");

        var encoded = SkillSupportRuleAppendTarget.CODEC.encodeStart(JsonOps.INSTANCE, target)
                .getOrThrow(message -> new IllegalStateException("Failed to encode support append target: " + message));
        SkillSupportRuleAppendTarget decoded = SkillSupportRuleAppendTarget.CODEC.parse(JsonOps.INSTANCE, encoded)
                .getOrThrow(message -> new IllegalStateException("Failed to decode support append target: " + message));

        helper.assertValueEqual(decoded, target, "Support append target codec should preserve entity component routing");
        helper.succeed();
    }

    /**
     * Verifies that skill tag requirements and exclusions gate support effects as expected.
     */
    @GameTest
    public void supportEffectApplicabilityGatedBySkillTags(GameTestHelper helper) {
        SkillSupportEffect spellCostEffect = support(SUPPORT_COST_BOOST_ID, helper).effects().getFirst();
        SkillSupportEffect meleeOnlyEffect = support(SUPPORT_MELEE_ONLY_ID, helper).effects().getFirst();

        helper.assertTrue(spellCostEffect.matches(fireball(helper).config().tags()), "Spell-only support should apply to fireball");
        helper.assertTrue(!spellCostEffect.matches(basicStrike(helper).config().tags()), "Spell-only support should not apply to basic strike");
        helper.assertTrue(meleeOnlyEffect.matches(basicStrike(helper).config().tags()), "Melee-only support should apply to basic strike");
        helper.assertTrue(!meleeOnlyEffect.matches(fireball(helper).config().tags()), "Melee-only support should not apply to projectile fireball");
        helper.succeed();
    }

    /**
     * Verifies that support-provided conditional modifiers can change resolved runtime skill values.
     */
    @GameTest
    public void supportConditionalStatModifierAltersPreparedResourceCost(GameTestHelper helper) {
        PreparedSkillUse base = prepare(helper, fireball(helper), newHolder(helper), newHolder(helper), List.of());
        SkillSupportDefinition support = support(SUPPORT_COST_BOOST_ID, helper);
        PreparedSkillUse withSupport = Skills.prepareUse(
                fireball(helper),
                skillUseContext(helper, newHolder(helper), newHolder(helper), List.of(), 0.0, 0.0)
                        .withLinkedSupports(List.of(support))
        );

        helper.assertValueEqual(base.resourceCost(), 12.0, "Base fireball resource cost should be decode baseline");
        helper.assertValueEqual(withSupport.resourceCost(), 16.0, "Supported fireball should include the +4 resource modifier");
        helper.succeed();
    }

    /**
     * Verifies that support config_overrides can rewrite both the prepared resource id and prepared resource cost.
     */
    @GameTest
    public void supportConfigOverridesRewritePreparedResourceAndCost(GameTestHelper helper) {
        PreparedSkillUse base = prepare(helper, battleFocus(helper), newHolder(helper), newHolder(helper), List.of());
        SkillSupportDefinition support = support(SUPPORT_GUARD_FOCUS_ID, helper);
        PreparedSkillUse withSupport = Skills.prepareUse(
                battleFocus(helper),
                skillUseContext(helper, newHolder(helper), newHolder(helper), List.of(), 0.0, 0.0)
                        .withLinkedSupports(List.of(support))
        );

        helper.assertValueEqual(base.resource(), "mana", "Base battle focus should keep the default mana resource");
        helper.assertValueEqual(base.resourceCost(), 8.0, "Base battle focus resource cost should remain the decode baseline");
        helper.assertValueEqual(withSupport.resource(), "guard", "Support config_overrides should replace the prepared resource id");
        helper.assertValueEqual(withSupport.resourceCost(), 3.0, "Support config_overrides should replace the prepared resource cost");
        helper.succeed();
    }

    /**
     * Verifies that prepared-state value expressions reuse resolved prepared values across actions, predicates, selectors, and tick conditions.
     */
    @GameTest
    public void preparedStateExpressionsReuseResolvedPreparedValues(GameTestHelper helper) {
        PreparedSkillUse prepared = prepare(helper, preparedStateProbe(helper), newHolder(helper), newHolder(helper), List.of());

        PreparedHealAction heal = prepared.onCastActions().stream()
                .filter(action -> action instanceof PreparedHealAction)
                .map(PreparedHealAction.class::cast)
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Prepared state probe should expose one prepared heal action"));
        PreparedResourceDeltaAction resourceDelta = prepared.onCastActions().stream()
                .filter(action -> action instanceof PreparedResourceDeltaAction)
                .map(PreparedResourceDeltaAction.class::cast)
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Prepared state probe should expose one prepared resource-delta action"));
        PreparedSkillExecutionRoute tickRoute = prepared.component("default_entity_name").tickRoutes().stream()
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Prepared state probe should expose one tick route"));
        SkillValueExpression radius = tickRoute.targets().stream()
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Prepared tick route should expose one selector"))
                .radius();

        helper.assertValueEqual(prepared.resourceCost(), 9.0, "Prepared state probe should keep its decoded resource cost");
        helper.assertValueEqual(prepared.useTimeTicks(), 13, "Prepared state probe should keep its decoded use time");
        helper.assertValueEqual(prepared.cooldownTicks(), 7, "Prepared state probe should keep its decoded cooldown");
        helper.assertValueEqual(heal.amount(), 9.0, "resource_cost expressions should resolve against the prepared resource cost");
        helper.assertValueEqual(resourceDelta.amount(), 13.0, "use_time_ticks expressions should resolve against the prepared use time");
        helper.assertValueEqual(heal.enPreds().getFirst().amount().resolve(prepared.useContext()), 2.0,
                "max_charges expressions should resolve inside action predicates");
        helper.assertValueEqual(resourceDelta.enPreds().getFirst().amount().resolve(prepared.useContext()), 9.0,
                "resource_cost expressions should resolve inside action predicates");
        helper.assertValueEqual(radius.resolve(prepared.useContext()), 3.0,
                "times_to_cast expressions should resolve inside selector radius payloads");
        helper.assertValueEqual(tickRoute.tickIntervalTicks(), 7,
                "cooldown_ticks expressions should resolve inside x_ticks_condition intervals");
        helper.succeed();
    }

    /**
     * Verifies that support-overridden config values are visible through prepared-state value expressions on the direct prepare path.
     */
    @GameTest
    public void supportPreparedStateExpressionsObserveMergedConfigOverrides(GameTestHelper helper) {
        PreparedSkillUse prepared = Skills.prepareUse(
                preparedStateProbe(helper),
                skillUseContext(helper, newHolder(helper), newHolder(helper), List.of(), 0.0, 0.0)
                        .withLinkedSupports(List.of(support(SUPPORT_PREPARED_STATE_TUNING_ID, helper)))
        );

        PreparedHealAction heal = prepared.onCastActions().stream()
                .filter(action -> action instanceof PreparedHealAction)
                .map(PreparedHealAction.class::cast)
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Supported prepared state probe should expose one prepared heal action"));
        PreparedResourceDeltaAction resourceDelta = prepared.onCastActions().stream()
                .filter(action -> action instanceof PreparedResourceDeltaAction)
                .map(PreparedResourceDeltaAction.class::cast)
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Supported prepared state probe should expose one prepared resource-delta action"));
        PreparedSkillExecutionRoute tickRoute = prepared.component("default_entity_name").tickRoutes().stream()
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Supported prepared state probe should expose one tick route"));
        SkillValueExpression radius = tickRoute.targets().stream()
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Supported prepared tick route should expose one selector"))
                .radius();

        helper.assertValueEqual(prepared.resourceCost(), 4.0, "Support override should replace the prepared resource cost");
        helper.assertValueEqual(prepared.useTimeTicks(), 6, "Support override should replace the prepared use time");
        helper.assertValueEqual(prepared.cooldownTicks(), 11, "Support override should replace the prepared cooldown");
        helper.assertValueEqual(heal.amount(), 4.0, "Prepared heal amount should observe the support-overridden resource cost");
        helper.assertValueEqual(resourceDelta.amount(), 6.0, "Prepared resource-delta amount should observe the support-overridden use time");
        helper.assertValueEqual(heal.enPreds().getFirst().amount().resolve(prepared.useContext()), 4.0,
                "Prepared action predicates should observe the support-overridden max charges");
        helper.assertValueEqual(resourceDelta.enPreds().getFirst().amount().resolve(prepared.useContext()), 4.0,
                "Prepared action predicates should observe the support-overridden resource cost");
        helper.assertValueEqual(radius.resolve(prepared.useContext()), 5.0,
                "Prepared selector radius should observe the support-overridden times_to_cast");
        helper.assertValueEqual(tickRoute.tickIntervalTicks(), 11,
                "Prepared tick route should observe the support-overridden cooldown");
        helper.succeed();
    }

    /**
     * Verifies that support-like action overrides and appended actions can produce modified damage payloads.
     */
    @GameTest
    public void supportActionOverrideAndAppendCanModifyDamageActionPayloads(GameTestHelper helper) {
        SkillSupportDefinition support = support(SUPPORT_DAMAGE_AMP_ID, helper);
        SkillActionOverride override = support.effects().getFirst().actionParameterOverrides().getFirst();
        SkillAction baseAction = new SkillAction(SkillActionType.DAMAGE, Map.of(), "esekai2:fireball_primary_hit");
        helper.assertTrue(override.matches(baseAction), "Damage override fixture should match fireball_primary_hit actions");

        PreparedSkillUse basePrepared = prepare(helper, fireball(helper), newHolder(helper), newHolder(helper), List.of());
        PreparedSkillUse supportedPrepared = Skills.prepareUse(
                fireball(helper),
                skillUseContext(helper, newHolder(helper), newHolder(helper), List.of(), 0.0, 0.0)
                        .withLinkedSupports(List.of(support))
        );

        PreparedDamageAction baseDamage = basePrepared.executeOnHit("default_entity_name").stream()
                .filter(action -> action instanceof PreparedDamageAction)
                .map(action -> (PreparedDamageAction) action)
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Base fireball should expose a damage action"));
        List<PreparedDamageAction> supportedDamageActions = supportedPrepared.executeOnHit("default_entity_name").stream()
                .filter(action -> action instanceof PreparedDamageAction)
                .map(action -> (PreparedDamageAction) action)
                .toList();
        PreparedDamageAction overriddenDamage = supportedDamageActions.getFirst();

        helper.assertValueEqual(baseDamage.hitDamageCalculation().baseDamage().amount(DamageType.FIRE), 18.0, "Base damage test skill should resolve fireball_primary_hit baseline damage");
        helper.assertValueEqual(overriddenDamage.hitDamageCalculation().baseDamage().amount(DamageType.FIRE), 24.0, "Overridden damage test skill should use the fixture override value");
        helper.assertValueEqual(supportedDamageActions.size(), 2, "Support payload should be capable of appending one extra damage action");
        PreparedDamageAction appendedDamage = supportedDamageActions.stream()
                .filter(action -> FIREBALL_SUPPORT_BONUS_HIT_ID.toString().equals(action.calculationId()))
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Support payload should append the calculation-backed damage action"));
        helper.assertValueEqual(appendedDamage.hitDamageCalculation().baseDamage().amount(DamageType.FIRE), 6.0,
                "Appended support damage should resolve from the bonus calculation fixture");
        helper.succeed();
    }

    /**
     * Verifies that a socketed item can resolve linked supports into the prepare-use pipeline.
     */
    @GameTest
    public void socketedItemPrepareUseResolvesLinkedSupports(GameTestHelper helper) {
        ItemStack stack = new ItemStack(Items.STICK);
        SocketedSkills.set(stack, new SocketedSkillItemState(
                Optional.of(FIREBALL_SKILL_ID),
                3,
                List.of(new SocketLinkGroup(0, List.of(0, 1, 2))),
                List.of(
                        new SocketedSkillRef(0, SocketSlotType.SKILL, FIREBALL_SKILL_ID),
                        new SocketedSkillRef(1, SocketSlotType.SUPPORT, SUPPORT_COST_BOOST_ID),
                        new SocketedSkillRef(2, SocketSlotType.SUPPORT, SUPPORT_DAMAGE_AMP_ID)
                )
        ));

        PreparedSkillUse prepared = Skills.prepareUse(
                stack,
                skillRegistry(helper),
                supportRegistry(helper),
                skillUseContext(helper, newHolder(helper), newHolder(helper), List.of(), 0.0, 0.0)
        );

        List<PreparedDamageAction> damageActions = prepared.executeOnHit("default_entity_name").stream()
                .filter(action -> action instanceof PreparedDamageAction)
                .map(PreparedDamageAction.class::cast)
                .toList();

        helper.assertValueEqual(prepared.resourceCost(), 16.0, "Socket-linked support should alter prepared resource cost");
        helper.assertValueEqual(damageActions.size(), 2, "Socket-linked appended damage action should be added to the supported route");
        helper.assertValueEqual(damageActions.getFirst().hitDamageCalculation().baseDamage().amount(DamageType.FIRE), 24.0,
                "Socket-linked override should modify the primary supported fire damage action");
        helper.succeed();
    }

    /**
     * Verifies that linked supports are merged in socket index order rather than raw socket-ref list order.
     */
    @GameTest
    public void socketedSupportOverridesUseSocketIndexOrder(GameTestHelper helper) {
        ItemStack stack = new ItemStack(Items.STICK);
        SocketedSkills.set(stack, new SocketedSkillItemState(
                Optional.of(FIREBALL_SKILL_ID),
                3,
                List.of(new SocketLinkGroup(0, List.of(0, 1, 2))),
                List.of(
                        new SocketedSkillRef(0, SocketSlotType.SKILL, FIREBALL_SKILL_ID),
                        new SocketedSkillRef(2, SocketSlotType.SUPPORT, SUPPORT_DAMAGE_AMP_ID),
                        new SocketedSkillRef(1, SocketSlotType.SUPPORT, SUPPORT_DAMAGE_LATE_AMP_ID)
                )
        ));

        PreparedSkillUse prepared = Skills.prepareUse(
                stack,
                skillRegistry(helper),
                supportRegistry(helper),
                skillUseContext(helper, newHolder(helper), newHolder(helper), List.of(), 0.0, 0.0)
        );

        PreparedDamageAction damageAction = prepared.executeOnHit("default_entity_name").stream()
                .filter(action -> action instanceof PreparedDamageAction)
                .map(PreparedDamageAction.class::cast)
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Supported fireball should expose a primary damage action"));

        helper.assertValueEqual(
                damageAction.hitDamageCalculation().baseDamage().amount(DamageType.FIRE),
                24.0,
                "Higher socket index support should win even when socket refs are stored out of order"
        );
        helper.succeed();
    }

    /**
     * Verifies that support-appended on-cast rules can use existing runtime predicates such as HAS_TARGET.
     */
    @GameTest
    public void supportAppendedOnCastRuleUsesRuntimePredicates(GameTestHelper helper) {
        PreparedSkillUse prepared = Skills.prepareUse(
                fireball(helper),
                skillUseContext(helper, newHolder(helper), newHolder(helper), List.of(), 0.0, 0.0)
                        .withLinkedSupports(List.of(support(SUPPORT_CAST_ECHO_ID, helper)))
        );
        Player caster = helper.makeMockPlayer(GameType.CREATIVE);
        caster.snapTo(helper.absoluteVec(new Vec3(1.0, 2.0, 1.0)));
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(1.0, 2.0, 4.0));

        RecordingHooks withTargetHooks = new RecordingHooks();
        SkillExecutionResult withTarget = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.of(zombie)),
                withTargetHooks
        );

        RecordingHooks withoutTargetHooks = new RecordingHooks();
        SkillExecutionResult withoutTarget = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.empty()),
                withoutTargetHooks
        );

        helper.assertValueEqual(withTarget.executedActions(), 4, "Targeted cast should include the support-appended on-cast rule");
        helper.assertValueEqual(withoutTarget.executedActions(), 3, "Support-appended HAS_TARGET rule should be skipped without a target");
        helper.assertValueEqual(withTargetHooks.soundCount(), 2, "Targeted cast should emit both base and support-appended sound actions");
        helper.assertValueEqual(withoutTargetHooks.soundCount(), 1, "Untargeted cast should only emit the base sound action");
        helper.succeed();
    }

    /**
     * Verifies that support-appended entity-component rules are added to the expected prepared component bucket.
     */
    @GameTest
    public void supportAppendedEntityComponentRuleAddsPreparedOnHitAction(GameTestHelper helper) {
        PreparedSkillUse prepared = Skills.prepareUse(
                fireball(helper),
                skillUseContext(helper, newHolder(helper), newHolder(helper), List.of(), 0.0, 0.0)
                        .withLinkedSupports(List.of(support(SUPPORT_COMPONENT_ECHO_ID, helper)))
        );

        List<PreparedSkillAction> onHitActions = prepared.executeOnHit("default_entity_name");
        helper.assertValueEqual(onHitActions.size(), 2, "Component support should append one extra on-hit action");
        helper.assertTrue(onHitActions.stream().anyMatch(action -> action.actionType().equals("sound")),
                "Component support should contribute a sound action to the on-hit bucket");
        helper.succeed();
    }

    /**
     * Verifies that support-appended rules targeting a missing component are ignored with a preparation warning.
     */
    @GameTest
    public void supportAppendedRuleWarnsForUnknownEntityComponent(GameTestHelper helper) {
        SkillSupportDefinition support = new SkillSupportDefinition(
                "esekai2:test_invalid_component_append",
                List.of(new SkillSupportEffect(
                        new SkillTagCondition(Set.of(), Set.of()),
                        Set.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(new SkillSupportRuleAppend(
                                new SkillSupportRuleAppendTarget(SkillSupportRuleTargetType.ENTITY_COMPONENT, "missing_component"),
                                List.of(new SkillRule(
                                        Set.of(new SkillTargetSelector(SkillTargetType.TARGET, Map.of())),
                                        List.of(new SkillAction(SkillActionType.SOUND, Map.of("sound", "minecraft:block.note_block.bell"))),
                                        List.of(),
                                        List.of(new SkillPredicate(SkillPredicateType.HAS_TARGET, Map.of()))
                                ))
                        ))
                ))
        );

        PreparedSkillUse prepared = Skills.prepareUse(
                fireball(helper),
                skillUseContext(helper, newHolder(helper), newHolder(helper), List.of(), 0.0, 0.0)
                        .withLinkedSupports(List.of(support))
        );

        helper.assertTrue(
                prepared.warnings().stream().anyMatch(warning -> warning.contains("missing_component")),
                "Unknown component append target should surface a preparation warning"
        );
        helper.succeed();
    }

    /**
     * Verifies that socket state can be stored on an ItemStack, round-tripped by codec, and extracted through public helper methods.
     */
    @GameTest
    public void socketStateRoundTripAndExtraction(GameTestHelper helper) {
        SocketedSkillItemState originalState = new SocketedSkillItemState(
                Optional.of(FIREBALL_SKILL_ID),
                3,
                List.of(
                        new SocketLinkGroup(0, List.of(0, 1)),
                        new SocketLinkGroup(1, List.of(2))
                ),
                List.of(
                        new SocketedSkillRef(0, SocketSlotType.SKILL, FIREBALL_SKILL_ID),
                        new SocketedSkillRef(1, SocketSlotType.SUPPORT, SUPPORT_COST_BOOST_ID),
                        new SocketedSkillRef(2, SocketSlotType.SUPPORT, SUPPORT_DAMAGE_AMP_ID)
                )
        );
        ItemStack stack = new ItemStack(Items.STICK);
        SocketedSkills.set(stack, originalState);
        SocketedSkillItemState restored = SocketedSkills.get(stack);

        var encoded = SocketedSkillItemState.CODEC.encodeStart(JsonOps.INSTANCE, originalState)
                .getOrThrow(message -> new IllegalStateException("Failed to encode socketed skill item state: " + message));
        SocketedSkillItemState decoded = SocketedSkillItemState.CODEC.parse(JsonOps.INSTANCE, encoded)
                .getOrThrow(message -> new IllegalStateException("Failed to decode socketed skill item state: " + message));

        helper.assertValueEqual(restored, originalState, "Restored item state should match what was stored");
        helper.assertValueEqual(restored, decoded, "Codec round-trip should preserve item socket state");
        helper.assertValueEqual(restored.socketCount(), 3, "Socket count should be preserved");
        helper.assertValueEqual(restored.activeSkill().orElseThrow(), FIREBALL_SKILL_ID, "Active skill id should be preserved");
        helper.assertValueEqual(restored.skillRefs().size(), 1, "Only one skill ref should be extracted from socket refs");
        helper.assertValueEqual(restored.supportRefs().size(), 2, "Two support refs should be extracted from socket refs");
        helper.assertTrue(restored.hasSocketRef(0), "First socket index should report an assigned ref");
        helper.assertTrue(restored.socketRefsForSocket(1).size() == 1, "Linked middle socket should return exactly one ref");
        helper.assertValueEqual(restored.linkedSocketIndices(), Set.of(0, 1, 2), "Link groups should flatten to all declared linked indices");
        helper.succeed();
    }

    private static PreparedSkillUse prepare(
            GameTestHelper helper,
            SkillDefinition skill,
            StatHolder attacker,
            StatHolder defender,
            List<ConditionalStatModifier> modifiers
    ) {
        return Skills.prepareUse(skill, skillUseContext(helper, attacker, defender, modifiers, 0.0, 0.0));
    }

    private static SkillUseContext skillUseContext(
            GameTestHelper helper,
            StatHolder attacker,
            StatHolder defender,
            List<ConditionalStatModifier> modifiers,
            double hitRoll,
            double criticalStrikeRoll
    ) {
        return new SkillUseContext(
                attacker,
                defender,
                modifiers,
                hitRoll,
                criticalStrikeRoll,
                SkillCalculationLookup.fromRegistry(skillCalculationRegistry(helper))
        );
    }

    private static Registry<SkillSupportDefinition> supportRegistry(GameTestHelper helper) {
        return helper.getLevel().getServer().registryAccess().lookupOrThrow(SkillRegistries.SKILL_SUPPORT);
    }

    private static SkillSupportDefinition support(Identifier id, GameTestHelper helper) {
        return supportRegistry(helper).getOptional(id)
                .orElseThrow(() -> helper.assertionException("Support fixture should decode successfully"));
    }

    private static Registry<SkillCalculationDefinition> skillCalculationRegistry(GameTestHelper helper) {
        return SkillCalculationRegistryAccess.skillCalculationRegistry(helper);
    }

    private static StatHolder newHolder(GameTestHelper helper) {
        return StatHolders.create(StatRegistryAccess.statRegistry(helper));
    }

    private static SkillDefinition fireball(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(FIREBALL_SKILL_ID)
                .orElseThrow(() -> helper.assertionException("Fireball should decode successfully"));
    }

    private static SkillDefinition basicStrike(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(BASIC_STRIKE_SKILL_ID)
                .orElseThrow(() -> helper.assertionException("Basic strike should decode successfully"));
    }

    private static SkillDefinition battleFocus(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(BATTLE_FOCUS_SKILL_ID)
                .orElseThrow(() -> helper.assertionException("Battle focus should decode successfully"));
    }

    private static SkillDefinition preparedStateProbe(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(PREPARED_STATE_PROBE_SKILL_ID)
                .orElseThrow(() -> helper.assertionException("Prepared state probe should decode successfully"));
    }

    private static Registry<SkillDefinition> skillRegistry(GameTestHelper helper) {
        return helper.getLevel().getServer().registryAccess().lookupOrThrow(SkillRegistries.SKILL);
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
