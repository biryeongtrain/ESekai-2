package kim.biryeong.esekai2.impl.gametest.skill;

import kim.biryeong.esekai2.api.damage.breakdown.DamageType;
import kim.biryeong.esekai2.api.damage.calculation.DamageCalculationResult;
import kim.biryeong.esekai2.api.damage.calculation.DamageCalculations;
import kim.biryeong.esekai2.api.monster.stat.MonsterStats;
import kim.biryeong.esekai2.api.player.resource.PlayerResourceIds;
import kim.biryeong.esekai2.api.player.resource.PlayerResources;
import kim.biryeong.esekai2.api.player.stat.PlayerCombatStats;
import kim.biryeong.esekai2.api.player.skill.PlayerSkillBurstState;
import kim.biryeong.esekai2.api.player.skill.PlayerSkillBursts;
import kim.biryeong.esekai2.api.player.skill.PlayerSkillCharges;
import kim.biryeong.esekai2.api.player.skill.PlayerSkillCooldowns;
import kim.biryeong.esekai2.api.skill.calculation.SkillCalculationDefinition;
import kim.biryeong.esekai2.api.skill.calculation.SkillCalculationLookup;
import kim.biryeong.esekai2.api.skill.definition.SkillAttached;
import kim.biryeong.esekai2.api.skill.definition.SkillConfig;
import kim.biryeong.esekai2.api.skill.definition.SkillDefinition;
import kim.biryeong.esekai2.api.skill.definition.SkillRegistries;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillAction;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillActionType;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillCondition;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillConditionType;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillPredicate;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillPredicateMatchMode;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillPredicateSubject;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillPredicateType;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillRule;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillTargetSelector;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillTargetType;
import kim.biryeong.esekai2.api.skill.effect.MobEffectRefreshPolicy;
import kim.biryeong.esekai2.api.skill.effect.SkillAilmentRefreshPolicy;
import kim.biryeong.esekai2.api.skill.execution.PreparedDamageAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedHealAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedResourceDeltaAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSandstormParticleAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSkillAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSkillEntityComponent;
import kim.biryeong.esekai2.api.skill.execution.PreparedSkillUse;
import kim.biryeong.esekai2.api.skill.execution.SkillExecutionHooks;
import kim.biryeong.esekai2.api.skill.execution.SkillExecutionContext;
import kim.biryeong.esekai2.api.skill.execution.SkillExecutionResult;
import kim.biryeong.esekai2.api.skill.execution.SkillUseContext;
import kim.biryeong.esekai2.api.skill.execution.SkillUseContexts;
import kim.biryeong.esekai2.api.skill.execution.Skills;
import kim.biryeong.esekai2.api.skill.support.SkillSupportDefinition;
import kim.biryeong.esekai2.api.skill.support.SkillSupportEffect;
import kim.biryeong.esekai2.api.skill.support.SkillSupportRuleAppend;
import kim.biryeong.esekai2.api.skill.support.SkillSupportRuleAppendTarget;
import kim.biryeong.esekai2.api.skill.support.SkillSupportRuleTargetType;
import kim.biryeong.esekai2.api.skill.tag.SkillTagCondition;
import kim.biryeong.esekai2.api.skill.stat.SkillStats;
import kim.biryeong.esekai2.api.skill.value.SkillValueExpression;
import kim.biryeong.esekai2.api.stat.combat.CombatStats;
import kim.biryeong.esekai2.api.stat.definition.StatDefinition;
import kim.biryeong.esekai2.api.stat.definition.StatRegistries;
import kim.biryeong.esekai2.api.stat.holder.StatHolder;
import kim.biryeong.esekai2.api.stat.holder.StatHolders;
import kim.biryeong.esekai2.api.stat.modifier.ConditionalStatModifier;
import kim.biryeong.esekai2.api.stat.modifier.StatModifier;
import kim.biryeong.esekai2.api.stat.modifier.StatModifierOperation;
import com.mojang.serialization.JsonOps;
import kim.biryeong.esekai2.impl.skill.entity.SkillRuntimeEntityTypes;
import kim.biryeong.esekai2.impl.skill.registry.SkillCalculationRegistryAccess;
import kim.biryeong.esekai2.impl.stat.registry.StatRegistryAccess;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Verifies the new action-graph skill execution preparation flow.
 */
public final class SkillExecutionGameTests {
    private static final Identifier BASIC_STRIKE_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "basic_strike");
    private static final Identifier FIREBALL_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "fireball");
    private static final Identifier BATTLE_FOCUS_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "battle_focus");
    private static final Identifier BURST_FOCUS_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "burst_focus");
    private static final Identifier BURST_STRIKE_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "burst_strike");
    private static final Identifier BURST_RESERVE_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "burst_reserve");
    private static final Identifier CHARGED_SURGE_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "charged_surge");
    private static final Identifier CHARGED_RESERVE_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "charged_reserve");
    private static final Identifier OVERWORLD_BARRIER_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "overworld_barrier");
    private static final Identifier SUPPORT_GUARD_FOCUS_ID = Identifier.fromNamespaceAndPath("esekai2", "support_guard_focus");
    private static final Identifier SUPPORT_BROKEN_FOCUS_ID = Identifier.fromNamespaceAndPath("esekai2", "support_broken_focus");
    private static final ResourceKey<StatDefinition> GUARD_STAT = ResourceKey.create(
            StatRegistries.STAT,
            Identifier.fromNamespaceAndPath("esekai2", "guard")
    );

    /**
     * Verifies that the skill registry loads the sample definition set used by these runtime tests.
     */
    @GameTest
    public void skillRegistryLoadsSampleDefinitions(GameTestHelper helper) {
        Registry<SkillDefinition> registry = skillRegistry(helper);

        helper.assertTrue(registry.containsKey(BASIC_STRIKE_SKILL_ID), "Basic strike should load into the skill registry");
        helper.assertTrue(registry.containsKey(FIREBALL_SKILL_ID), "Fireball should load into the skill registry");
        helper.assertTrue(registry.containsKey(BURST_FOCUS_SKILL_ID), "Burst focus should load into the skill registry");
        helper.assertTrue(registry.containsKey(BURST_STRIKE_SKILL_ID), "Burst strike should load into the skill registry");
        helper.assertTrue(registry.containsKey(BURST_RESERVE_SKILL_ID), "Burst reserve should load into the skill registry");
        helper.assertTrue(registry.containsKey(CHARGED_SURGE_SKILL_ID), "Charged surge should load into the skill registry");
        helper.assertTrue(registry.containsKey(CHARGED_RESERVE_SKILL_ID), "Charged reserve should load into the skill registry");
        helper.succeed();
    }

    /**
     * Verifies that the sample skill calculation fixture loads into the dedicated dynamic registry.
     */
    @GameTest
    public void skillCalculationRegistryLoadsSampleDefinitions(GameTestHelper helper) {
        Registry<SkillCalculationDefinition> registry = skillCalculationRegistry(helper);

        helper.assertTrue(registry.containsKey(Identifier.fromNamespaceAndPath("esekai2", "fireball_primary_hit")),
                "Fireball primary hit calculation should load into the skill calculation registry");
        helper.assertTrue(registry.containsKey(Identifier.fromNamespaceAndPath("esekai2", "basic_strike_primary_hit")),
                "Basic strike primary hit calculation should load into the skill calculation registry");
        helper.assertTrue(registry.containsKey(Identifier.fromNamespaceAndPath("esekai2", "fireball_support_bonus_hit")),
                "Support bonus damage calculation should load into the skill calculation registry");
        helper.succeed();
    }

    /**
     * Verifies that skill calculation definitions survive public codec round-tripping.
     */
    @GameTest
    public void skillCalculationDefinitionCodecRoundTrips(GameTestHelper helper) {
        SkillCalculationDefinition definition = skillCalculation(helper, Identifier.fromNamespaceAndPath("esekai2", "basic_strike_primary_hit"));

        var encoded = SkillCalculationDefinition.CODEC.encodeStart(JsonOps.INSTANCE, definition)
                .getOrThrow(message -> new IllegalStateException("Failed to encode skill calculation definition: " + message));
        SkillCalculationDefinition decoded = SkillCalculationDefinition.CODEC.parse(JsonOps.INSTANCE, encoded)
                .getOrThrow(message -> new IllegalStateException("Failed to decode skill calculation definition: " + message));

        helper.assertValueEqual(decoded, definition, "Skill calculation codec should preserve typed damage payloads");
        helper.succeed();
    }

    /**
     * Verifies that condition and predicate payloads survive public codec round-tripping.
     */
    @GameTest
    public void skillConditionAndPredicateCodecRoundTrips(GameTestHelper helper) {
        SkillCondition condition = new SkillCondition(SkillConditionType.X_TICKS_CONDITION, Map.of("tick_rate", "2"));
        SkillPredicate predicate = new SkillPredicate(SkillPredicateType.RANDOM_CHANCE, Map.of("chance", "0.5"));
        SkillPredicate hasEffectPredicate = new SkillPredicate(
                SkillPredicateType.HAS_EFFECT,
                Map.of("effect_id", "minecraft:speed", "subject", "self")
        );
        SkillPredicate hasResourcePredicate = SkillPredicate.hasResource("guard", SkillValueExpression.constant(3.0), SkillPredicateSubject.SELF);

        SkillCondition decodedCondition = SkillCondition.CODEC.parse(
                JsonOps.INSTANCE,
                SkillCondition.CODEC.encodeStart(JsonOps.INSTANCE, condition)
                        .getOrThrow(message -> new IllegalStateException("Failed to encode skill condition: " + message))
        ).getOrThrow(message -> new IllegalStateException("Failed to decode skill condition: " + message));

        SkillPredicate decodedPredicate = SkillPredicate.CODEC.parse(
                JsonOps.INSTANCE,
                SkillPredicate.CODEC.encodeStart(JsonOps.INSTANCE, predicate)
                        .getOrThrow(message -> new IllegalStateException("Failed to encode skill predicate: " + message))
        ).getOrThrow(message -> new IllegalStateException("Failed to decode skill predicate: " + message));
        SkillPredicate decodedHasEffectPredicate = SkillPredicate.CODEC.parse(
                JsonOps.INSTANCE,
                SkillPredicate.CODEC.encodeStart(JsonOps.INSTANCE, hasEffectPredicate)
                        .getOrThrow(message -> new IllegalStateException("Failed to encode has_effect predicate: " + message))
        ).getOrThrow(message -> new IllegalStateException("Failed to decode has_effect predicate: " + message));
        SkillPredicate decodedHasResourcePredicate = SkillPredicate.CODEC.parse(
                JsonOps.INSTANCE,
                SkillPredicate.CODEC.encodeStart(JsonOps.INSTANCE, hasResourcePredicate)
                        .getOrThrow(message -> new IllegalStateException("Failed to encode has_resource predicate: " + message))
        ).getOrThrow(message -> new IllegalStateException("Failed to decode has_resource predicate: " + message));

        helper.assertValueEqual(decodedCondition, condition, "Skill condition codec should preserve x-tick payloads");
        helper.assertValueEqual(decodedPredicate, predicate, "Skill predicate codec should preserve random chance payloads");
        helper.assertValueEqual(decodedHasEffectPredicate, hasEffectPredicate, "Skill predicate codec should preserve has_effect payloads");
        helper.assertValueEqual(decodedHasResourcePredicate, hasResourcePredicate, "Skill predicate codec should preserve has_resource payloads");
        helper.succeed();
    }

    /**
     * Verifies that has_resource predicates evaluate registered player resources and fail safely for missing player subjects.
     */
    @GameTest
    public void hasResourcePredicateMatchesRegisteredPlayerResources(GameTestHelper helper) {
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        PlayerCombatStats.get(player).setBaseValue(GUARD_STAT, 10.0);
        PlayerResources.set(player, "guard", 5.0);

        PreparedSkillUse prepared = Skills.prepareUse(basicStrike(helper), SkillUseContexts.forPlayer(player, 0.0, 0.0));
        SkillExecutionContext executionContext = SkillExecutionContext.forCast(prepared, (net.minecraft.server.level.ServerLevel) player.level(), player, Optional.empty());

        helper.assertTrue(SkillPredicate.hasResource("guard", SkillValueExpression.constant(4.0), SkillPredicateSubject.SELF).matches(executionContext),
                "has_resource should pass when the self player has at least the required registered resource amount");
        helper.assertTrue(!SkillPredicate.hasResource("guard", SkillValueExpression.constant(6.0), SkillPredicateSubject.SELF).matches(executionContext),
                "has_resource should fail when the self player lacks the required registered resource amount");
        helper.assertTrue(!SkillPredicate.hasResource("guard", SkillValueExpression.constant(1.0), SkillPredicateSubject.TARGET).matches(executionContext),
                "has_resource should fail safely when the selected target subject does not resolve to a player");
        helper.succeed();
    }

    /**
     * Verifies that current-resource and max-resource expressions resolve through live player resource lookups.
     */
    @GameTest
    public void resourceValueExpressionsResolveThroughPlayerContexts(GameTestHelper helper) {
        ServerPlayer attacker = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        ServerPlayer target = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        PlayerCombatStats.get(attacker).setBaseValue(GUARD_STAT, 10.0);
        PlayerCombatStats.get(target).setBaseValue(GUARD_STAT, 12.0);
        PlayerResources.set(attacker, "guard", 6.0);
        PlayerResources.set(target, "guard", 4.0);

        SkillUseContext context = SkillUseContexts.forPlayer(attacker, Optional.of(target), 0.0, 0.0);

        helper.assertValueEqual(SkillValueExpression.currentResource("guard", SkillPredicateSubject.SELF).resolve(context), 6.0,
                "Current-resource expressions should read the self resource amount from the live lookup");
        helper.assertValueEqual(SkillValueExpression.currentResource("guard", SkillPredicateSubject.TARGET).resolve(context), 4.0,
                "Current-resource expressions should read the target resource amount from the live lookup");
        helper.assertValueEqual(SkillValueExpression.maxResource("guard", SkillPredicateSubject.SELF).resolve(context), 10.0,
                "Max-resource expressions should read the self resource maximum from the live lookup");
        helper.assertValueEqual(SkillValueExpression.maxResource("guard", SkillPredicateSubject.PRIMARY_TARGET).resolve(context), 12.0,
                "Max-resource expressions should read the primary-target resource maximum from the live lookup");
        helper.succeed();
    }

    /**
     * Verifies that compound has_effect predicates preserve effect_ids, match mode, negate, and subject through codec round-trip.
     */
    @GameTest
    public void compoundHasEffectPredicateCodecRoundTrips(GameTestHelper helper) {
        SkillPredicate predicate = SkillPredicate.hasEffects(
                List.of(
                        Identifier.fromNamespaceAndPath("minecraft", "speed"),
                        Identifier.fromNamespaceAndPath("esekai2", "poison")
                ),
                SkillPredicateSubject.TARGET,
                SkillPredicateMatchMode.ALL_OF,
                true
        );

        SkillPredicate decoded = SkillPredicate.CODEC.parse(
                JsonOps.INSTANCE,
                SkillPredicate.CODEC.encodeStart(JsonOps.INSTANCE, predicate)
                        .getOrThrow(message -> new IllegalStateException("Failed to encode compound has_effect predicate: " + message))
        ).getOrThrow(message -> new IllegalStateException("Failed to decode compound has_effect predicate: " + message));

        helper.assertValueEqual(decoded, predicate, "Compound has_effect codec should preserve effect_ids, match mode, negate, and subject");
        helper.assertValueEqual(decoded.resolvedEffectIds(), predicate.resolvedEffectIds(), "Compound has_effect codec should preserve the ordered deduplicated effect id list");
        helper.succeed();
    }

    /**
     * Verifies that has_effect predicates reject invalid effect identifiers during codec validation.
     */
    @GameTest
    public void hasEffectPredicateRejectsInvalidEffectId(GameTestHelper helper) {
        try {
            SkillPredicate.CODEC.parse(JsonOps.INSTANCE, JsonOps.INSTANCE.createMap(Map.of(
                    JsonOps.INSTANCE.createString("type"), JsonOps.INSTANCE.createString("has_effect"),
                    JsonOps.INSTANCE.createString("effect_id"), JsonOps.INSTANCE.createString("not a valid id")
            ))).getOrThrow(message -> new IllegalStateException("Expected has_effect validation to fail: " + message));
            throw helper.assertionException("has_effect predicates should reject invalid effect ids");
        } catch (IllegalStateException expected) {
            helper.succeed();
        }
    }

    /**
     * Verifies that has_effect predicates reject an empty compound effect list during codec validation.
     */
    @GameTest
    public void hasEffectPredicateRejectsEmptyEffectList(GameTestHelper helper) {
        try {
            SkillPredicate.CODEC.parse(JsonOps.INSTANCE, JsonOps.INSTANCE.createMap(Map.of(
                    JsonOps.INSTANCE.createString("type"), JsonOps.INSTANCE.createString("has_effect"),
                    JsonOps.INSTANCE.createString("effect_ids"), JsonOps.INSTANCE.createList(java.util.stream.Stream.<com.google.gson.JsonElement>empty())
            ))).getOrThrow(message -> new IllegalStateException("Expected empty has_effect effect_ids validation to fail: " + message));
            throw helper.assertionException("has_effect predicates should reject an empty effect_ids list");
        } catch (IllegalStateException expected) {
            helper.succeed();
        }
    }

    /**
     * Verifies that has_effect predicates reject invalid compound match values during codec validation.
     */
    @GameTest
    public void hasEffectPredicateRejectsInvalidMatchMode(GameTestHelper helper) {
        try {
            SkillPredicate.CODEC.parse(JsonOps.INSTANCE, JsonOps.INSTANCE.createMap(Map.of(
                    JsonOps.INSTANCE.createString("type"), JsonOps.INSTANCE.createString("has_effect"),
                    JsonOps.INSTANCE.createString("effect_ids"), JsonOps.INSTANCE.createList(List.of(
                            JsonOps.INSTANCE.createString("minecraft:speed"),
                            JsonOps.INSTANCE.createString("minecraft:slowness")
                    ).stream()),
                    JsonOps.INSTANCE.createString("match"), JsonOps.INSTANCE.createString("not_a_match")
            ))).getOrThrow(message -> new IllegalStateException("Expected has_effect match validation to fail: " + message));
            throw helper.assertionException("has_effect predicates should reject invalid match modes");
        } catch (IllegalStateException expected) {
            helper.succeed();
        }
    }

    /**
     * Verifies that has_effect predicates reject invalid identifiers inside effect_ids during codec validation.
     */
    @GameTest
    public void hasEffectPredicateRejectsInvalidEffectIdInEffectIds(GameTestHelper helper) {
        try {
            SkillPredicate.CODEC.parse(JsonOps.INSTANCE, JsonOps.INSTANCE.createMap(Map.of(
                    JsonOps.INSTANCE.createString("type"), JsonOps.INSTANCE.createString("has_effect"),
                    JsonOps.INSTANCE.createString("effect_ids"), JsonOps.INSTANCE.createList(List.of(
                            JsonOps.INSTANCE.createString("minecraft:speed"),
                            JsonOps.INSTANCE.createString("not a valid id")
                    ).stream())
            ))).getOrThrow(message -> new IllegalStateException("Expected compound has_effect effect_ids validation to fail: " + message));
            throw helper.assertionException("has_effect predicates should reject invalid effect_ids entries");
        } catch (IllegalStateException expected) {
            helper.succeed();
        }
    }

    /**
     * Verifies that has_effect predicates reject invalid subject values during codec validation.
     */
    @GameTest
    public void hasEffectPredicateRejectsInvalidSubject(GameTestHelper helper) {
        try {
            SkillPredicate.CODEC.parse(JsonOps.INSTANCE, JsonOps.INSTANCE.createMap(Map.of(
                    JsonOps.INSTANCE.createString("type"), JsonOps.INSTANCE.createString("has_effect"),
                    JsonOps.INSTANCE.createString("effect_id"), JsonOps.INSTANCE.createString("minecraft:speed"),
                    JsonOps.INSTANCE.createString("subject"), JsonOps.INSTANCE.createString("not_a_subject")
            ))).getOrThrow(message -> new IllegalStateException("Expected has_effect subject validation to fail: " + message));
            throw helper.assertionException("has_effect predicates should reject invalid subjects");
        } catch (IllegalStateException expected) {
            helper.succeed();
        }
    }

    /**
     * Verifies that a runtime use resolves base resource, cast-time, and cooldown values.
     */
    @GameTest
    public void skillRuntimeValuesUseBaseConfigWithoutModifiers(GameTestHelper helper) {
        PreparedSkillUse prepared = Skills.prepareUse(fireball(helper), skillUseContext(helper, newHolder(helper), newHolder(helper), List.of(), 0.0, 0.99));

        helper.assertValueEqual(prepared.resource(), PlayerResourceIds.MANA, "Skills without an explicit resource override should default to mana");
        helper.assertValueEqual(prepared.resourceCost(), 12.0, "Base skill resource cost should remain unchanged without modifiers");
        helper.assertValueEqual(prepared.useTimeTicks(), 16, "Base use-time should remain unchanged without modifiers");
        helper.assertValueEqual(prepared.cooldownTicks(), 0, "Base cooldown should remain unchanged without modifiers");
        helper.succeed();
    }

    /**
     * Verifies that legacy skill config decode defaults the resource id to mana when the field is omitted.
     */
    @GameTest
    public void skillConfigDefaultsResourceToMana(GameTestHelper helper) {
        SkillConfig decoded = SkillConfig.CODEC.parse(
                JsonOps.INSTANCE,
                JsonOps.INSTANCE.createMap(Map.of(
                        JsonOps.INSTANCE.createString("resource_cost"), JsonOps.INSTANCE.createDouble(4.0)
                ))
        ).getOrThrow(message -> new IllegalStateException("Failed to decode skill config: " + message));

        helper.assertValueEqual(decoded.resource(), PlayerResourceIds.MANA, "Legacy skill configs should default resource to mana");
        helper.assertValueEqual(decoded.resourceCost(), 4.0, "Legacy skill configs should preserve resource cost");
        helper.succeed();
    }

    /**
     * Verifies that unsupported non-mana skill cost resources are preserved at preparation time and safely block runtime execution.
     */
    @GameTest
    public void executeOnCastBlocksUnsupportedSkillCostResource(GameTestHelper helper) {
        SkillDefinition unsupportedResourceSkill = testSkill(
                "esekai2:unsupported_cost_resource",
                new SkillConfig(
                        "",
                        "life",
                        4.0,
                        0,
                        0,
                        "",
                        "",
                        false,
                        false,
                        1,
                        0,
                        0.0,
                        Set.of()
                ),
                List.of(new SkillRule(Set.of(SkillTargetSelector.self()), List.of(soundAction("minecraft:block.note_block.harp")), List.of(), List.of())),
                Map.of()
        );
        StatHolder attacker = newHolder(helper);
        attacker.setBaseValue(CombatStats.MANA, 20.0);
        PreparedSkillUse prepared = Skills.prepareUse(
                unsupportedResourceSkill,
                skillUseContext(helper, attacker, newHolder(helper), List.of(), 0.0, 0.99)
        );
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        player.snapTo(helper.absoluteVec(new Vec3(1.0, 2.0, 1.0)));
        PlayerResources.setMana(player, 20.0, 20.0);

        RecordingHooks hooks = new RecordingHooks();
        SkillExecutionResult result = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), player, Optional.empty()),
                hooks
        );

        helper.assertValueEqual(prepared.resource(), "life", "Preparation should preserve non-mana resource ids for future generic runtime expansion");
        helper.assertValueEqual(result.executedActions(), 0, "Unsupported cost resources should block runtime execution before on-cast actions run");
        helper.assertValueEqual(hooks.soundCount(), 0, "Unsupported cost resources should not emit on-cast sound actions");
        helper.assertTrue(result.warnings().stream().anyMatch(warning -> warning.contains("unsupported resource=life")),
                "Unsupported cost resources should surface a clear runtime warning");
        helper.assertValueEqual(PlayerResources.getMana(player, 20.0), 20.0, "Unsupported non-mana cost resources should not spend mana");
        helper.succeed();
    }

    /**
     * Verifies that direct casts spend the support-overridden guard resource instead of mana.
     */
    @GameTest
    public void executeOnCastSpendsSupportOverriddenGuardResourceInsteadOfMana(GameTestHelper helper) {
        StatHolder attacker = newHolder(helper);
        attacker.setBaseValue(CombatStats.MANA, 20.0);
        PreparedSkillUse prepared = Skills.prepareUse(
                skill(helper, BATTLE_FOCUS_SKILL_ID),
                skillUseContext(helper, attacker, newHolder(helper), List.of(), 0.0, 0.0)
                        .withLinkedSupports(List.of(support(helper, SUPPORT_GUARD_FOCUS_ID)))
        );
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        player.snapTo(helper.absoluteVec(new Vec3(1.0, 2.0, 1.0)));
        PlayerCombatStats.get(player).setBaseValue(GUARD_STAT, 10.0);
        PlayerResources.set(player, "guard", 5.0);
        PlayerResources.setMana(player, 20.0, 20.0);

        SkillExecutionResult result = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), player, Optional.empty()),
                new RecordingHooks()
        );

        helper.assertValueEqual(prepared.resource(), "guard", "Support config_overrides should rewrite the direct prepared resource id");
        helper.assertValueEqual(prepared.resourceCost(), 3.0, "Support config_overrides should rewrite the direct prepared resource cost");
        helper.assertValueEqual(SkillValueExpression.resourceCost().resolve(prepared.useContext()), 3.0,
                "Prepared resource_cost expressions should observe the support-overridden direct prepared resource cost");
        helper.assertTrue(result.executedActions() > 0, "Guard-backed direct casts should execute when the player has enough guard");
        helper.assertValueEqual(PlayerResources.getAmount(player, "guard", 10.0), 2.0, "Successful guard-backed direct casts should spend guard");
        helper.assertValueEqual(PlayerResources.getMana(player, 20.0), 20.0, "Guard-backed direct casts should not spend mana");
        helper.succeed();
    }

    /**
     * Verifies that prepared graph payloads can reuse resolved prepared-state values across action, selector, condition, and predicate surfaces.
     */
    @GameTest
    public void prepareUseInjectsPreparedStateValuesIntoGraphPayloads(GameTestHelper helper) {
        SkillDefinition skill = testSkill(
                "esekai2:prepared_state_probe",
                new SkillConfig(
                        "",
                        PlayerResourceIds.MANA,
                        4.0,
                        6,
                        8,
                        "",
                        "",
                        false,
                        false,
                        3,
                        2,
                        1.0,
                        Set.of()
                ),
                List.of(new SkillRule(
                        Set.of(SkillTargetSelector.aoe(SkillValueExpression.timesToCast())),
                        List.of(
                                typedResourceDeltaAction("guard", SkillValueExpression.resourceCost()),
                                typedHealAction(SkillValueExpression.maxCharges())
                        ),
                        List.of(),
                        List.of(SkillPredicate.hasResource("guard", SkillValueExpression.resourceCost(), SkillPredicateSubject.SELF))
                )),
                Map.of(
                        "cooldown_pulse", List.of(new SkillRule(
                                Set.of(SkillTargetSelector.self()),
                                List.of(soundAction("minecraft:entity.experience_orb.pickup")),
                                List.of(SkillCondition.everyTicks(SkillValueExpression.cooldownTicks())),
                                List.of()
                        )),
                        "use_time_pulse", List.of(new SkillRule(
                                Set.of(SkillTargetSelector.self()),
                                List.of(soundAction("minecraft:entity.experience_orb.pickup")),
                                List.of(SkillCondition.everyTicks(SkillValueExpression.useTimeTicks())),
                                List.of()
                        ))
                )
        );

        PreparedSkillUse prepared = Skills.prepareUse(
                skill,
                skillUseContext(helper, newHolder(helper), newHolder(helper), List.of(), 0.0, 0.0)
        );

        PreparedResourceDeltaAction delta = (PreparedResourceDeltaAction) prepared.onCastActions().stream()
                .filter(PreparedResourceDeltaAction.class::isInstance)
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Prepared on-cast actions should include one resource_delta action"));
        PreparedHealAction heal = (PreparedHealAction) prepared.onCastActions().stream()
                .filter(PreparedHealAction.class::isInstance)
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Prepared on-cast actions should include one heal action"));

        helper.assertValueEqual(delta.amount(), 4.0,
                "resource_delta.amount should resolve from the prepared resource_cost value");
        helper.assertValueEqual(heal.amount(), 2.0,
                "heal.amount should resolve from the prepared max_charges value");
        helper.assertValueEqual(
                prepared.onCastRoutes().getFirst().targets().stream().findFirst().orElseThrow().radius().resolve(prepared.useContext()),
                3.0,
                "AOE selector radius should resolve from the prepared times_to_cast value"
        );
        helper.assertValueEqual(
                prepared.onCastRoutes().getFirst().enPreds().getFirst().amount().resolve(prepared.useContext()),
                4.0,
                "has_resource.amount should resolve from the prepared resource_cost value"
        );
        helper.assertValueEqual(prepared.component("cooldown_pulse").tickActions().getFirst().intervalTicks(), 8,
                "X-ticks conditions should resolve cooldown_ticks from the prepared context");
        helper.assertValueEqual(prepared.component("use_time_pulse").tickActions().getFirst().intervalTicks(), 6,
                "X-ticks conditions should resolve use_time_ticks from the prepared context");
        helper.succeed();
    }

    /**
     * Verifies that registered support-overridden resources still block direct casts when the player has no max pool for that resource.
     */
    @GameTest
    public void executeOnCastBlocksSupportOverriddenResourceWhenRegisteredPoolIsZero(GameTestHelper helper) {
        StatHolder attacker = newHolder(helper);
        attacker.setBaseValue(CombatStats.MANA, 20.0);
        PreparedSkillUse prepared = Skills.prepareUse(
                skill(helper, BATTLE_FOCUS_SKILL_ID),
                skillUseContext(helper, attacker, newHolder(helper), List.of(), 0.0, 0.0)
                        .withLinkedSupports(List.of(support(helper, SUPPORT_GUARD_FOCUS_ID)))
        );
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        player.snapTo(helper.absoluteVec(new Vec3(1.0, 2.0, 1.0)));
        PlayerResources.setMana(player, 20.0, 20.0);

        RecordingHooks hooks = new RecordingHooks();
        SkillExecutionResult result = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), player, Optional.empty()),
                hooks
        );

        helper.assertValueEqual(prepared.resource(), "guard", "Preparation should preserve the registered support-overridden resource id");
        helper.assertValueEqual(result.executedActions(), 0, "Zero-pool registered resources should block direct runtime execution");
        helper.assertValueEqual(hooks.soundCount(), 0, "Zero-pool registered resources should not emit direct runtime actions");
        helper.assertTrue(result.warnings().stream().anyMatch(warning -> warning.contains("guard required=3.0")),
                "Zero-pool registered resources should surface a guard resource warning");
        helper.assertValueEqual(PlayerResources.getMana(player, 20.0), 20.0, "Zero-pool registered resources should not spend mana");
        helper.succeed();
    }

    /**
     * Verifies that unsupported support-overridden resources fail safely before direct runtime actions execute.
     */
    @GameTest
    public void executeOnCastBlocksUnsupportedSupportOverriddenResource(GameTestHelper helper) {
        StatHolder attacker = newHolder(helper);
        attacker.setBaseValue(CombatStats.MANA, 20.0);
        PreparedSkillUse prepared = Skills.prepareUse(
                skill(helper, BATTLE_FOCUS_SKILL_ID),
                skillUseContext(helper, attacker, newHolder(helper), List.of(), 0.0, 0.0)
                        .withLinkedSupports(List.of(support(helper, SUPPORT_BROKEN_FOCUS_ID)))
        );
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        player.snapTo(helper.absoluteVec(new Vec3(1.0, 2.0, 1.0)));
        PlayerCombatStats.get(player).setBaseValue(GUARD_STAT, 10.0);
        PlayerResources.set(player, "guard", 5.0);
        PlayerResources.setMana(player, 20.0, 20.0);

        RecordingHooks hooks = new RecordingHooks();
        SkillExecutionResult result = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), player, Optional.empty()),
                hooks
        );

        helper.assertValueEqual(prepared.resource(), "life", "Preparation should preserve unsupported resource overrides for runtime safety checks");
        helper.assertValueEqual(result.executedActions(), 0, "Unsupported overridden resources should block direct runtime execution");
        helper.assertValueEqual(hooks.soundCount(), 0, "Unsupported overridden resources should not emit on-cast sound actions");
        helper.assertTrue(result.warnings().stream().anyMatch(warning -> warning.contains("unsupported resource=life")),
                "Unsupported overridden resources should surface a clear runtime warning");
        helper.assertValueEqual(PlayerResources.getAmount(player, "guard", 10.0), 5.0, "Unsupported overridden resources should not spend guard");
        helper.assertValueEqual(PlayerResources.getMana(player, 20.0), 20.0, "Unsupported overridden resources should not spend mana");
        helper.succeed();
    }

    /**
     * Verifies that unsupported resource_delta actions preserve their resource id during preparation and emit a warning.
     */
    @GameTest
    public void prepareUseWarnsAndPreservesUnsupportedResourceDeltaAction(GameTestHelper helper) {
        SkillDefinition unsupportedResourceDeltaSkill = testSkill(
                "esekai2:unsupported_resource_delta",
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
        );

        PreparedSkillUse prepared = Skills.prepareUse(
                unsupportedResourceDeltaSkill,
                skillUseContext(helper, newHolder(helper), newHolder(helper), List.of(), 0.0, 0.99)
        );

        helper.assertTrue(prepared.warnings().stream().anyMatch(warning -> warning.contains("resource_delta references unsupported resource: life")),
                "Unsupported resource_delta actions should surface a preparation warning");
        PreparedResourceDeltaAction resourceAction = (PreparedResourceDeltaAction) prepared.onCastActions().stream()
                .filter(PreparedResourceDeltaAction.class::isInstance)
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Unsupported resource_delta actions should still prepare a resource_delta action"));
        helper.assertValueEqual(resourceAction.resource(), "life",
                "Unsupported resource_delta actions should preserve their configured resource id during preparation");
        helper.succeed();
    }

    /**
     * Verifies that runtime stat modifiers are applied to skill config values.
     */
    @GameTest
    public void attackerStatModifiersAffectRuntimeSkillValues(GameTestHelper helper) {
        StatHolder attacker = newHolder(helper);
        attacker.addModifier(new StatModifier(SkillStats.SKILL_RESOURCE_COST, StatModifierOperation.ADD, 3.0, Identifier.fromNamespaceAndPath("esekai2", "cost_add")));
        attacker.addModifier(new StatModifier(SkillStats.SKILL_USE_TIME_TICKS, StatModifierOperation.MORE, -50.0, Identifier.fromNamespaceAndPath("esekai2", "time_half")));
        attacker.addModifier(new StatModifier(SkillStats.SKILL_COOLDOWN_TICKS, StatModifierOperation.ADD, 2.0, Identifier.fromNamespaceAndPath("esekai2", "cd_add")));

        PreparedSkillUse prepared = Skills.prepareUse(
                fireball(helper),
                skillUseContext(helper, attacker, newHolder(helper), List.of(), 0.0, 0.99)
        );

        helper.assertValueEqual(prepared.resourceCost(), 15.0, "Additive resource modifiers should increase cost");
        helper.assertValueEqual(prepared.useTimeTicks(), 8, "More modifiers on cast time should apply through stat resolution");
        helper.assertValueEqual(prepared.cooldownTicks(), 2, "Additive cooldown modifiers should increase cooldown");
        helper.succeed();
    }

    /**
     * Verifies that matching conditional modifiers only apply when skill tags satisfy the skill condition.
     */
    @GameTest
    public void conditionalModifiersAffectRuntimeSkillValues(GameTestHelper helper) {
        PreparedSkillUse prepared = Skills.prepareUse(
                fireball(helper),
                skillUseContext(
                        helper,
                        newHolder(helper),
                        newHolder(helper),
                        List.of(
                                new kim.biryeong.esekai2.api.stat.modifier.ConditionalStatModifier(
                                        new StatModifier(SkillStats.SKILL_RESOURCE_COST, StatModifierOperation.ADD, 4.0, Identifier.fromNamespaceAndPath("esekai2", "spell_bonus")),
                                        new kim.biryeong.esekai2.api.skill.tag.SkillTagCondition(Set.of(kim.biryeong.esekai2.api.skill.tag.SkillTag.SPELL), Set.of())
                                )
                        ),
                        0.0,
                        0.99
                )
        );

        helper.assertValueEqual(prepared.resourceCost(), 16.0, "Matching conditional modifiers should apply to resolved runtime values");
        helper.succeed();
    }

    /**
     * Verifies that excluded skill tags block conditional modifiers.
     */
    @GameTest
    public void excludedTagsBlockConditionalRuntimeModifiers(GameTestHelper helper) {
        PreparedSkillUse prepared = Skills.prepareUse(
                basicStrike(helper),
                new SkillUseContext(
                        newHolder(helper),
                        newHolder(helper),
                        List.of(
                                new ConditionalStatModifier(
                                        new StatModifier(SkillStats.SKILL_RESOURCE_COST, StatModifierOperation.ADD, 99.0, Identifier.fromNamespaceAndPath("esekai2", "blocked")),
                                        new kim.biryeong.esekai2.api.skill.tag.SkillTagCondition(Set.of(kim.biryeong.esekai2.api.skill.tag.SkillTag.SPELL), Set.of())
                                )
                        ),
                        0.0,
                        0.99
                )
        );

        helper.assertValueEqual(prepared.resourceCost(), 0.0, "Excluded tags should not receive conditional modifier changes");
        helper.succeed();
    }

    /**
     * Verifies that on-cast action parsing produces concrete action types.
     */
    @GameTest
    public void prepareUseBuildsOnCastActionsFromConfig(GameTestHelper helper) {
        PreparedSkillUse prepared = Skills.prepareUse(
                basicStrike(helper),
                new SkillUseContext(newHolder(helper), newHolder(helper), List.of(), 0.0, 0.5)
        );

        List<String> actionTypes = prepared.onCastActions().stream().map(PreparedSkillAction::actionType).toList();
        helper.assertValueEqual(true, actionTypes.contains("sound"), "Basic strike should expose a sound action in on-cast");
        helper.assertValueEqual(true, actionTypes.contains("sandstorm_particle"), "Basic strike should expose a particle action in on-cast");
        helper.assertValueEqual(true, actionTypes.contains("damage"), "Basic strike should expose a damage action in on-cast");
        helper.succeed();
    }

    /**
     * Verifies that component events resolve into separate action buckets.
     */
    @GameTest
    public void prepareUseResolvesEntityComponents(GameTestHelper helper) {
        PreparedSkillUse prepared = Skills.prepareUse(
                basicStrike(helper),
                new SkillUseContext(newHolder(helper), newHolder(helper), List.of(), 0.0, 0.25)
        );

        PreparedSkillEntityComponent strikeComponent = prepared.components().get("basic_strike_component");
        helper.assertTrue(strikeComponent != null, "Basic strike should expose the test entity component");
        helper.assertValueEqual(strikeComponent.onHitActions().size(), 1, "On-hit action bucket should include summon_at_sight in component");
        helper.assertValueEqual(strikeComponent.onExpireActions().size(), 1, "On-expire action bucket should include summon_block in component");
        helper.assertValueEqual(strikeComponent.tickActions().size(), 1, "Tick action bucket should include sandstorm_particle in component");
        helper.succeed();
    }

    /**
     * Verifies that component event execution returns the configured on-hit and on-expire actions.
     */
    @GameTest
    public void executeComponentEventsReturnsExpectedActions(GameTestHelper helper) {
        PreparedSkillUse prepared = Skills.prepareUse(
                basicStrike(helper),
                new SkillUseContext(newHolder(helper), newHolder(helper), List.of(), 0.0, 0.25)
        );

        List<String> onHitTypes = prepared.executeOnHit("basic_strike_component").stream()
                .map(action -> ((PreparedSkillAction) action).actionType())
                .toList();
        List<String> onExpireTypes = prepared.executeOnEntityExpire("basic_strike_component").stream()
                .map(action -> ((PreparedSkillAction) action).actionType())
                .toList();

        helper.assertTrue(onHitTypes.contains("summon_at_sight"), "On-hit execution should resolve summon_at_sight");
        helper.assertTrue(onExpireTypes.contains("summon_block"), "On-expire execution should resolve summon_block");
        helper.succeed();
    }

    /**
     * Verifies that tick scheduling honors the parsed tick interval.
     */
    @GameTest
    public void executeTickUsesTickConditionIntervals(GameTestHelper helper) {
        PreparedSkillUse prepared = Skills.prepareUse(
                basicStrike(helper),
                new SkillUseContext(newHolder(helper), newHolder(helper), List.of(), 0.0, 0.25)
        );

        List<String> tickActionsAt6 = prepared.executeTick("basic_strike_component", 6).stream().map(PreparedSkillAction::actionType).toList();
        List<String> tickActionsAt7 = prepared.executeTick("basic_strike_component", 7).stream().map(PreparedSkillAction::actionType).toList();
        helper.assertTrue(!tickActionsAt6.isEmpty(), "Tick condition should run on multiples of the configured interval");
        helper.assertValueEqual(tickActionsAt7.isEmpty(), true, "Tick condition should not run on non-multiples");
        helper.succeed();
    }

    /**
     * Verifies that the basic strike sample skill binds its custom Sandstorm particle identifiers.
     */
    @GameTest
    public void basicStrikeUsesBundledSandstormParticleIdentifiers(GameTestHelper helper) {
        PreparedSkillUse prepared = Skills.prepareUse(
                basicStrike(helper),
                new SkillUseContext(newHolder(helper), newHolder(helper), List.of(), 0.0, 0.25)
        );

        PreparedSandstormParticleAction onCastParticle = prepared.onCastActions().stream()
                .filter(PreparedSandstormParticleAction.class::isInstance)
                .map(PreparedSandstormParticleAction.class::cast)
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Basic strike should expose an on-cast Sandstorm particle action"));
        PreparedSandstormParticleAction tickParticle = prepared.executeTick("basic_strike_component", 6).stream()
                .filter(PreparedSandstormParticleAction.class::isInstance)
                .map(PreparedSandstormParticleAction.class::cast)
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Basic strike component should expose a tick Sandstorm particle action"));

        helper.assertValueEqual(Identifier.fromNamespaceAndPath("esekai2", "basic_strike_burst"), onCastParticle.particleId(),
                "Basic strike on-cast particle should use the bundled custom particle id");
        helper.assertValueEqual(Identifier.fromNamespaceAndPath("esekai2", "basic_strike_burst"), tickParticle.particleId(),
                "Basic strike tick particle should use the bundled custom particle id");
        helper.succeed();
    }

    /**
     * Verifies that attack-oriented sample fixtures bind their bundled on-cast Sandstorm particle ids.
     */
    @GameTest
    public void attackFixturesUseBundledOnCastSandstormParticleIdentifiers(GameTestHelper helper) {
        Map<Identifier, Identifier> expectedParticles = Map.ofEntries(
                Map.entry(Identifier.fromNamespaceAndPath("esekai2", "bloodletting_strike"), Identifier.fromNamespaceAndPath("esekai2", "bleed_strike_burst")),
                Map.entry(Identifier.fromNamespaceAndPath("esekai2", "burst_strike"), Identifier.fromNamespaceAndPath("esekai2", "arcane_burst_pulse")),
                Map.entry(Identifier.fromNamespaceAndPath("esekai2", "concussive_slam"), Identifier.fromNamespaceAndPath("esekai2", "stun_strike_burst")),
                Map.entry(Identifier.fromNamespaceAndPath("esekai2", "dazing_tap"), Identifier.fromNamespaceAndPath("esekai2", "stun_strike_burst")),
                Map.entry(Identifier.fromNamespaceAndPath("esekai2", "deep_freeze"), Identifier.fromNamespaceAndPath("esekai2", "freeze_strike_burst")),
                Map.entry(Identifier.fromNamespaceAndPath("esekai2", "frostbite_touch"), Identifier.fromNamespaceAndPath("esekai2", "frost_strike_burst")),
                Map.entry(Identifier.fromNamespaceAndPath("esekai2", "ice_prison"), Identifier.fromNamespaceAndPath("esekai2", "freeze_strike_burst")),
                Map.entry(Identifier.fromNamespaceAndPath("esekai2", "kindling_ember"), Identifier.fromNamespaceAndPath("esekai2", "ember_strike_burst")),
                Map.entry(Identifier.fromNamespaceAndPath("esekai2", "kindling_inferno"), Identifier.fromNamespaceAndPath("esekai2", "ember_strike_burst")),
                Map.entry(Identifier.fromNamespaceAndPath("esekai2", "kindling_strike"), Identifier.fromNamespaceAndPath("esekai2", "ember_strike_burst")),
                Map.entry(Identifier.fromNamespaceAndPath("esekai2", "rime_crush"), Identifier.fromNamespaceAndPath("esekai2", "frost_strike_burst")),
                Map.entry(Identifier.fromNamespaceAndPath("esekai2", "thunder_strike"), Identifier.fromNamespaceAndPath("esekai2", "shock_strike_burst")),
                Map.entry(Identifier.fromNamespaceAndPath("esekai2", "venom_strike"), Identifier.fromNamespaceAndPath("esekai2", "toxic_strike_burst"))
        );

        for (Map.Entry<Identifier, Identifier> entry : expectedParticles.entrySet()) {
            PreparedSkillUse prepared = Skills.prepareUse(
                    skill(helper, entry.getKey()),
                    new SkillUseContext(newHolder(helper), newHolder(helper), List.of(), 0.0, 0.25)
            );

            PreparedSandstormParticleAction particleAction = prepared.onCastActions().stream()
                    .filter(PreparedSandstormParticleAction.class::isInstance)
                    .map(PreparedSandstormParticleAction.class::cast)
                    .findFirst()
                    .orElseThrow(() -> helper.assertionException("Attack fixture should expose an on-cast Sandstorm particle action: " + entry.getKey()));

            helper.assertValueEqual(entry.getValue(), particleAction.particleId(),
                    "Attack fixture should keep its bundled on-cast Sandstorm particle id: " + entry.getKey());
        }
        helper.succeed();
    }

    /**
     * Verifies that support-oriented sample fixtures bind their bundled on-cast Sandstorm particle ids.
     */
    @GameTest
    public void supportiveFixturesUseBundledOnCastSandstormParticleIdentifiers(GameTestHelper helper) {
        Map<Identifier, Identifier> expectedParticles = Map.ofEntries(
                Map.entry(Identifier.fromNamespaceAndPath("esekai2", "battle_focus"), Identifier.fromNamespaceAndPath("esekai2", "focus_guard_aura")),
                Map.entry(Identifier.fromNamespaceAndPath("esekai2", "burst_focus"), Identifier.fromNamespaceAndPath("esekai2", "focus_burst_pulse")),
                Map.entry(Identifier.fromNamespaceAndPath("esekai2", "charged_focus"), Identifier.fromNamespaceAndPath("esekai2", "charged_focus_aura")),
                Map.entry(Identifier.fromNamespaceAndPath("esekai2", "cleanse_focus"), Identifier.fromNamespaceAndPath("esekai2", "cleanse_wave_ring")),
                Map.entry(Identifier.fromNamespaceAndPath("esekai2", "cleanse_spectrum"), Identifier.fromNamespaceAndPath("esekai2", "cleanse_wave_ring")),
                Map.entry(Identifier.fromNamespaceAndPath("esekai2", "cathartic_wave"), Identifier.fromNamespaceAndPath("esekai2", "cleanse_wave_ring")),
                Map.entry(Identifier.fromNamespaceAndPath("esekai2", "purity_wave"), Identifier.fromNamespaceAndPath("esekai2", "cleanse_wave_ring")),
                Map.entry(Identifier.fromNamespaceAndPath("esekai2", "tainted_release"), Identifier.fromNamespaceAndPath("esekai2", "cleanse_wave_ring")),
                Map.entry(Identifier.fromNamespaceAndPath("esekai2", "blank_slate"), Identifier.fromNamespaceAndPath("esekai2", "cleanse_wave_ring")),
                Map.entry(Identifier.fromNamespaceAndPath("esekai2", "purging_hex"), Identifier.fromNamespaceAndPath("esekai2", "cleanse_wave_ring")),
                Map.entry(Identifier.fromNamespaceAndPath("esekai2", "mana_surge"), Identifier.fromNamespaceAndPath("esekai2", "resource_charge_aura")),
                Map.entry(Identifier.fromNamespaceAndPath("esekai2", "restorative_pulse"), Identifier.fromNamespaceAndPath("esekai2", "restorative_pulse_ring")),
                Map.entry(Identifier.fromNamespaceAndPath("esekai2", "overworld_barrier"), Identifier.fromNamespaceAndPath("esekai2", "barrier_guard_aura")),
                Map.entry(Identifier.fromNamespaceAndPath("esekai2", "burst_reserve"), Identifier.fromNamespaceAndPath("esekai2", "resource_charge_aura")),
                Map.entry(Identifier.fromNamespaceAndPath("esekai2", "charged_reserve"), Identifier.fromNamespaceAndPath("esekai2", "resource_charge_aura")),
                Map.entry(Identifier.fromNamespaceAndPath("esekai2", "charged_surge"), Identifier.fromNamespaceAndPath("esekai2", "resource_charge_aura"))
        );

        for (Map.Entry<Identifier, Identifier> entry : expectedParticles.entrySet()) {
            PreparedSkillUse prepared = Skills.prepareUse(
                    skill(helper, entry.getKey()),
                    new SkillUseContext(newHolder(helper), newHolder(helper), List.of(), 0.0, 0.25)
            );

            PreparedSandstormParticleAction particleAction = prepared.onCastActions().stream()
                    .filter(PreparedSandstormParticleAction.class::isInstance)
                    .map(PreparedSandstormParticleAction.class::cast)
                    .findFirst()
                    .orElseThrow(() -> helper.assertionException("Supportive fixture should expose an on-cast Sandstorm particle action: " + entry.getKey()));

            helper.assertValueEqual(entry.getValue(), particleAction.particleId(),
                    "Supportive fixture should keep its bundled on-cast Sandstorm particle id: " + entry.getKey());
        }
        helper.succeed();
    }

    /**
     * Verifies that the fireball sample fixture binds bundled custom particle ids on cast and projectile tick routes.
     */
    @GameTest
    public void fireballUsesBundledSandstormParticleIdentifiers(GameTestHelper helper) {
        PreparedSkillUse prepared = Skills.prepareUse(
                fireball(helper),
                new SkillUseContext(newHolder(helper), newHolder(helper), List.of(), 0.0, 0.25)
        );

        PreparedSandstormParticleAction onCastParticle = prepared.onCastActions().stream()
                .filter(PreparedSandstormParticleAction.class::isInstance)
                .map(PreparedSandstormParticleAction.class::cast)
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Fireball should expose an on-cast Sandstorm particle action"));
        PreparedSandstormParticleAction tickParticle = prepared.executeTick("default_entity_name", 2).stream()
                .filter(PreparedSandstormParticleAction.class::isInstance)
                .map(PreparedSandstormParticleAction.class::cast)
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Fireball component should expose a projectile tick Sandstorm particle action"));

        helper.assertValueEqual(Identifier.fromNamespaceAndPath("esekai2", "fireball_cast_burst"), onCastParticle.particleId(),
                "Fireball on-cast particle should use the bundled cast particle id");
        helper.assertValueEqual(Identifier.fromNamespaceAndPath("esekai2", "fireball_trail_burst"), tickParticle.particleId(),
                "Fireball projectile tick particle should use the bundled trail particle id");
        helper.succeed();
    }

    /**
     * Verifies that damage actions can still be resolved through the shared damage calculation pipeline.
     */
    @GameTest
    public void preparedDamageActionIntegratesWithDamageCalculations(GameTestHelper helper) {
        PreparedSkillUse prepared = Skills.prepareUse(
                fireball(helper),
                skillUseContext(helper, newHolder(helper), newHolder(helper), List.of(), 0.0, 0.99)
        );

        PreparedDamageAction damageAction = prepared.executeOnHit("default_entity_name").stream()
                .filter(action -> action instanceof PreparedDamageAction)
                .map(action -> (PreparedDamageAction) action)
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Fireball should expose a damage action"));

        DamageCalculationResult result = DamageCalculations.calculateHit(damageAction.hitDamageCalculation());
        helper.assertTrue(result.hitResolution().bypassedAccuracyCheck(), "Projectile spell damage should be in hit pipeline");
        helper.assertValueEqual(result.finalDamage().amount(DamageType.FIRE), 18.0, "Damage calculation should preserve fixture base payload when parsed");
        helper.assertValueEqual("esekai2:fireball_primary_hit", damageAction.calculationId(), "Damage actions should preserve calculation_id through preparation");
        helper.succeed();
    }

    /**
     * Verifies that the basic strike sample skill resolves its primary damage through the calculation registry.
     */
    @GameTest
    public void basicStrikeDamageActionUsesCalculationRegistryPayload(GameTestHelper helper) {
        PreparedSkillUse prepared = Skills.prepareUse(
                basicStrike(helper),
                skillUseContext(helper, newHolder(helper), newHolder(helper), List.of(), 0.0, 0.99)
        );

        PreparedDamageAction damageAction = prepared.onCastActions().stream()
                .filter(action -> action instanceof PreparedDamageAction)
                .map(PreparedDamageAction.class::cast)
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Basic strike should expose a damage action"));

        helper.assertValueEqual(damageAction.calculationId(), "esekai2:basic_strike_primary_hit",
                "Basic strike damage should preserve its calculation registry reference");
        helper.assertValueEqual(
                damageAction.hitDamageCalculation().baseDamage().amount(DamageType.PHYSICAL),
                12.0,
                "Basic strike damage should resolve the physical base damage from the registry fixture"
        );
        helper.succeed();
    }

    /**
     * Verifies that damage actions without calculation references keep the default empty reference.
     */
    @GameTest
    public void damageActionWithoutCalculationIdDefaultsToEmptyReference(GameTestHelper helper) {
        SkillDefinition skill = testSkill(
                "esekai2:test_no_calculation_reference",
                List.of(),
                Map.of(
                        "missing_registry_component",
                        List.of(new SkillRule(
                                Set.of(new SkillTargetSelector(SkillTargetType.TARGET, Map.of())),
                                List.of(new SkillAction(SkillActionType.DAMAGE, Map.of())),
                                List.of(new SkillCondition(SkillConditionType.ON_HIT, Map.of())),
                                List.of()
                        ))
                )
        );

        PreparedSkillUse prepared = Skills.prepareUse(
                skill,
                new SkillUseContext(newHolder(helper), newHolder(helper), List.of(), 0.0, 0.5)
        );

        PreparedDamageAction damageAction = prepared.executeOnHit("missing_registry_component").stream()
                .filter(action -> action instanceof PreparedDamageAction)
                .map(action -> (PreparedDamageAction) action)
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Synthetic test skill should expose a damage action"));

        helper.assertValueEqual("", damageAction.calculationId(), "Missing calculation_id should default to an empty reference");
        helper.succeed();
    }

    /**
     * Verifies that calculation references resolve datapack-backed damage payloads during skill preparation.
     */
    @GameTest
    public void damageActionResolvesCalculationRegistryPayload(GameTestHelper helper) {
        SkillDefinition skill = testSkill(
                "esekai2:test_registry_damage",
                List.of(),
                Map.of(
                        "registry_component",
                        List.of(new SkillRule(
                                Set.of(new SkillTargetSelector(SkillTargetType.TARGET, Map.of())),
                                List.of(new SkillAction(SkillActionType.DAMAGE, Map.of(), "esekai2:fireball_primary_hit")),
                                List.of(new SkillCondition(SkillConditionType.ON_HIT, Map.of())),
                                List.of()
                        ))
                )
        );

        PreparedSkillUse prepared = Skills.prepareUse(
                skill,
                skillUseContext(helper, newHolder(helper), newHolder(helper), List.of(), 0.0, 0.99)
        );

        PreparedDamageAction damageAction = prepared.executeOnHit("registry_component").stream()
                .filter(action -> action instanceof PreparedDamageAction)
                .map(action -> (PreparedDamageAction) action)
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Registry-backed test skill should expose a damage action"));

        helper.assertValueEqual(
                damageAction.hitDamageCalculation().baseDamage().amount(DamageType.FIRE),
                18.0,
                "Calculation registry payload should supply fire damage when no inline payload exists"
        );
        helper.assertValueEqual(damageAction.hitDamageCalculation().hitContext().kind(), kim.biryeong.esekai2.api.damage.critical.HitKind.SPELL,
                "Calculation registry payload should supply hit kind metadata");
        helper.succeed();
    }

    /**
     * Verifies that unknown calculation references preserve compatibility by warning and falling back to inline payload data.
     */
    @GameTest
    public void unknownCalculationIdWarnsAndFallsBackToInlinePayload(GameTestHelper helper) {
        SkillDefinition skill = testSkill(
                "esekai2:test_missing_registry_damage",
                List.of(),
                Map.of(
                        "missing_registry_component",
                        List.of(new SkillRule(
                                Set.of(new SkillTargetSelector(SkillTargetType.TARGET, Map.of())),
                                List.of(new SkillAction(
                                        SkillActionType.DAMAGE,
                                        Map.of(
                                                "hit_kind", "spell",
                                                "base_damage_fire", "5.0"
                                        ),
                                        "esekai2:missing_damage"
                                )),
                                List.of(new SkillCondition(SkillConditionType.ON_HIT, Map.of())),
                                List.of()
                        ))
                )
        );

        PreparedSkillUse prepared = Skills.prepareUse(
                skill,
                skillUseContext(helper, newHolder(helper), newHolder(helper), List.of(), 0.0, 0.99)
        );

        PreparedDamageAction damageAction = prepared.executeOnHit("missing_registry_component").stream()
                .filter(action -> action instanceof PreparedDamageAction)
                .map(action -> (PreparedDamageAction) action)
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Fallback test skill should expose a damage action"));

        helper.assertTrue(prepared.warnings().contains("Unknown skill calculation: esekai2:missing_damage"),
                "Unknown calculation references should be reported as warnings");
        helper.assertValueEqual(damageAction.hitDamageCalculation().baseDamage().amount(DamageType.FIRE), 5.0,
                "Inline payload should remain a compatibility fallback when a calculation lookup misses");
        helper.succeed();
    }

    /**
     * Verifies that the action codec round-trips calculation references without altering payload data.
     */
    @GameTest
    public void damageActionCalculationIdCodecRoundTrip(GameTestHelper helper) {
        SkillAction action = new SkillAction(
                SkillActionType.DAMAGE,
                Map.of(
                        "hit_kind", "spell",
                        "base_damage_fire", "18.0",
                        "base_critical_strike_chance", "6.0",
                        "base_critical_strike_multiplier", "160.0"
                ),
                "esekai2:fireball_primary_hit"
        );

        SkillAction decoded = SkillAction.CODEC.parse(
                JsonOps.INSTANCE,
                SkillAction.CODEC.encodeStart(JsonOps.INSTANCE, action)
                        .getOrThrow(message -> new IllegalStateException("Failed to encode skill action: " + message))
        ).getOrThrow(message -> new IllegalStateException("Failed to decode skill action: " + message));

        helper.assertValueEqual(decoded, action, "Damage action codec should round-trip optional calculation_id metadata");
        helper.succeed();
    }

    /**
     * Verifies that the sample fixtures keep selector metadata and Sandstorm anchor fields in the decoded graph.
     */
    @GameTest
    public void sampleFixturesKeepSelectorAndSandstormMetadata(GameTestHelper helper) {
        SkillDefinition fireball = fireball(helper);
        SkillRule onCastRule = fireball.attached().onCast().get(0);

        helper.assertTrue(onCastRule.targets().contains(new SkillTargetSelector(SkillTargetType.TARGET, Map.of())),
                "Fireball on-cast rule should keep target selector metadata");

        SkillAction sandstormAction = onCastRule.acts().stream()
                .filter(action -> action.type() == SkillActionType.SANDSTORM_PARTICLE)
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Fireball should expose a sandstorm particle action"));

        helper.assertValueEqual("caster_hand", sandstormAction.parameters().get("anchor"), "Fireball should keep the anchor metadata");
        helper.assertValueEqual("0.5", sandstormAction.parameters().get("offset_y"), "Fireball should keep the offset metadata");
        helper.assertValueEqual("esekai2:fireball_cast_burst", sandstormAction.parameters().get("particle_id"),
                "Fireball fixture should decode with the bundled cast particle id");
        helper.succeed();
    }

    /**
     * Verifies that target selectors and AoE radii survive codec round-tripping.
     */
    @GameTest
    public void skillTargetSelectorAndRuleCodecRoundTrip(GameTestHelper helper) {
        SkillTargetSelector self = new SkillTargetSelector(SkillTargetType.SELF, Map.of());
        SkillTargetSelector target = new SkillTargetSelector(SkillTargetType.TARGET, Map.of());
        SkillTargetSelector aoe = new SkillTargetSelector(SkillTargetType.AOE, Map.of("radius", "3.0"));

        SkillRule rule = new SkillRule(
                new LinkedHashSet<>(List.of(self, target, aoe)),
                List.of(
                        new SkillAction(SkillActionType.SOUND, Map.of("sound", "minecraft:block.anvil.land")),
                        new SkillAction(SkillActionType.SANDSTORM_PARTICLE, Map.of(
                                "particle_id", "sandstorm:magic",
                                "anchor", "impact_point",
                                "offset_y", "0.25"
                        ))
                ),
                List.of(
                        new SkillCondition(SkillConditionType.ON_HIT, Map.of()),
                        new SkillCondition(SkillConditionType.X_TICKS_CONDITION, Map.of("tick_rate", "2"))
                ),
                List.of()
        );

        SkillRule decoded = SkillRule.CODEC.parse(
                JsonOps.INSTANCE,
                SkillRule.CODEC.encodeStart(JsonOps.INSTANCE, rule)
                        .getOrThrow(message -> new IllegalStateException("Failed to encode rule: " + message))
        ).getOrThrow(message -> new IllegalStateException("Failed to decode rule: " + message));

        helper.assertValueEqual(decoded, rule, "Rule codec should preserve selectors, actions, and conditions");
        helper.succeed();
    }

    /**
     * Verifies that AoE selectors keep their radius metadata through the public selector codec.
     */
    @GameTest
    public void aoeTargetSelectorCodecRoundTrip(GameTestHelper helper) {
        SkillTargetSelector selector = new SkillTargetSelector(SkillTargetType.AOE, Map.of("radius", "4.5"));

        SkillTargetSelector decoded = SkillTargetSelector.CODEC.parse(
                JsonOps.INSTANCE,
                SkillTargetSelector.CODEC.encodeStart(JsonOps.INSTANCE, selector)
                        .getOrThrow(message -> new IllegalStateException("Failed to encode target selector: " + message))
        ).getOrThrow(message -> new IllegalStateException("Failed to decode target selector: " + message));

        helper.assertValueEqual(decoded, selector, "AoE selector should round-trip with radius metadata");
        helper.succeed();
    }

    /**
     * Verifies that spell resistance modifies the same fireball payload during damage calculation.
     */
    @GameTest
    public void damageActionRespectsDefenderResistance(GameTestHelper helper) {
        StatHolder defender = newHolder(helper);
        defender.setBaseValue(CombatStats.FIRE_RESISTANCE, 50.0);

        PreparedSkillUse prepared = Skills.prepareUse(
                fireball(helper),
                skillUseContext(helper, newHolder(helper), defender, List.of(), 0.0, 0.99)
        );

        PreparedDamageAction damageAction = prepared.executeOnHit("default_entity_name").stream()
                .filter(action -> action instanceof PreparedDamageAction)
                .map(action -> (PreparedDamageAction) action)
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Fireball should expose a damage action"));

        DamageCalculationResult result = DamageCalculations.calculateHit(damageAction.hitDamageCalculation());
        helper.assertValueEqual(result.finalDamage().amount(DamageType.FIRE), 9.0, "Damage pipeline should apply defender fire resistance");
        helper.succeed();
    }

    /**
     * Verifies that the skill and stat registries use expected configuration shapes.
     */
    @GameTest
    public void skillRuntimeStatsLoadFromRegistry(GameTestHelper helper) {
        Registry<kim.biryeong.esekai2.api.stat.definition.StatDefinition> registry = StatRegistryAccess.statRegistry(helper);

        helper.assertTrue(registry.getOptional(SkillStats.SKILL_RESOURCE_COST).isPresent(), "Skill resource cost stat should be available");
        helper.assertTrue(registry.getOptional(SkillStats.SKILL_USE_TIME_TICKS).isPresent(), "Skill use time stat should be available");
        helper.assertTrue(registry.getOptional(SkillStats.SKILL_COOLDOWN_TICKS).isPresent(), "Skill cooldown stat should be available");
        helper.succeed();
    }

    /**
     * Verifies that on_spell_cast component routes execute during the cast phase alongside normal on_cast routes.
     */
    @GameTest
    public void executeOnCastRunsOnSpellCastRoutes(GameTestHelper helper) {
        SkillDefinition skill = testSkill(
                "esekai2:test_spell_cast_routes",
                List.of(new SkillRule(
                        Set.of(new SkillTargetSelector(SkillTargetType.TARGET, Map.of())),
                        List.of(soundAction("minecraft:block.anvil.land")),
                        List.of(),
                        List.of()
                )),
                Map.of(
                        "spell_cast_component",
                        List.of(new SkillRule(
                                Set.of(new SkillTargetSelector(SkillTargetType.TARGET, Map.of())),
                                List.of(soundAction("minecraft:block.note_block.bell")),
                                List.of(new SkillCondition(SkillConditionType.ON_SPELL_CAST, Map.of())),
                                List.of()
                        ))
                )
        );

        PreparedSkillUse prepared = Skills.prepareUse(skill, new SkillUseContext(newHolder(helper), newHolder(helper), List.of(), 0.0, 0.0));
        Player caster = helper.makeMockPlayer(GameType.CREATIVE);
        caster.snapTo(helper.absoluteVec(new Vec3(1.0, 2.0, 1.0)));
        SkillExecutionContext context = SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.empty());

        RecordingHooks hooks = new RecordingHooks();
        SkillExecutionResult result = Skills.executeOnCast(context, hooks);

        helper.assertValueEqual(result.executedActions(), 2, "Cast execution should include both on_cast and on_spell_cast routes");
        helper.assertValueEqual(hooks.soundCount(), 2, "Both cast-time routes should emit a sound action");
        helper.succeed();
    }

    /**
     * Verifies that HAS_TARGET en_preds allow execution only when the cast has a target.
     */
    @GameTest
    public void hasTargetEnPredGatesSpellCastRoutes(GameTestHelper helper) {
        SkillDefinition skill = testSkill(
                "esekai2:test_has_target_predicate",
                List.of(),
                Map.of(
                        "spell_cast_component",
                        List.of(new SkillRule(
                                Set.of(new SkillTargetSelector(SkillTargetType.TARGET, Map.of())),
                                List.of(soundAction("minecraft:block.anvil.land")),
                                List.of(new SkillCondition(SkillConditionType.ON_SPELL_CAST, Map.of())),
                                List.of(new SkillPredicate(SkillPredicateType.HAS_TARGET, Map.of()))
                        ))
                )
        );

        PreparedSkillUse prepared = Skills.prepareUse(skill, new SkillUseContext(newHolder(helper), newHolder(helper), List.of(), 0.0, 0.0));
        Player caster = helper.makeMockPlayer(GameType.CREATIVE);
        caster.snapTo(helper.absoluteVec(new Vec3(1.0, 2.0, 1.0)));
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(1.0, 2.0, 4.0));

        SkillExecutionResult withTarget = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.of(zombie)),
                new RecordingHooks()
        );
        SkillExecutionResult withoutTarget = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.empty()),
                new RecordingHooks()
        );

        helper.assertValueEqual(withTarget.executedActions(), 1, "HAS_TARGET should allow the route when a target exists");
        helper.assertValueEqual(withoutTarget.executedActions(), 0, "HAS_TARGET should block the route when no target exists");
        helper.assertValueEqual(withoutTarget.skippedActions(), 1, "Blocked routes should count as skipped");
        helper.succeed();
    }

    /**
     * Verifies that RANDOM_CHANCE en_preds use the prepared hit roll to decide execution.
     */
    @GameTest
    public void randomChanceEnPredUsesPreparedRoll(GameTestHelper helper) {
        SkillDefinition skill = testSkill(
                "esekai2:test_random_chance_predicate",
                List.of(),
                Map.of(
                        "spell_cast_component",
                        List.of(new SkillRule(
                                Set.of(new SkillTargetSelector(SkillTargetType.TARGET, Map.of())),
                                List.of(soundAction("minecraft:block.anvil.land")),
                                List.of(new SkillCondition(SkillConditionType.ON_SPELL_CAST, Map.of())),
                                List.of(new SkillPredicate(SkillPredicateType.RANDOM_CHANCE, Map.of("chance", "0.5")))
                        ))
                )
        );

        Player caster = helper.makeMockPlayer(GameType.CREATIVE);
        caster.snapTo(helper.absoluteVec(new Vec3(1.0, 2.0, 1.0)));
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(1.0, 2.0, 4.0));

        PreparedSkillUse passingPrepared = Skills.prepareUse(skill, new SkillUseContext(newHolder(helper), newHolder(helper), List.of(), 0.0, 0.0));
        PreparedSkillUse failingPrepared = Skills.prepareUse(skill, new SkillUseContext(newHolder(helper), newHolder(helper), List.of(), 0.99, 0.0));

        SkillExecutionResult passing = Skills.executeOnCast(
                SkillExecutionContext.forCast(passingPrepared, helper.getLevel(), caster, Optional.of(zombie)),
                new RecordingHooks()
        );
        SkillExecutionResult failing = Skills.executeOnCast(
                SkillExecutionContext.forCast(failingPrepared, helper.getLevel(), caster, Optional.of(zombie)),
                new RecordingHooks()
        );

        helper.assertValueEqual(passing.executedActions(), 1, "Low prepared hit rolls should pass RANDOM_CHANCE predicates");
        helper.assertValueEqual(failing.executedActions(), 0, "High prepared hit rolls should fail RANDOM_CHANCE predicates");
        helper.assertValueEqual(failing.skippedActions(), 1, "Failed random chance routes should be counted as skipped");
        helper.succeed();
    }

    /**
     * Verifies that has_effect predicates can gate routes using the current primary target's active effects.
     */
    @GameTest
    public void hasEffectEnPredUsesPrimaryTargetEffect(GameTestHelper helper) {
        SkillDefinition skill = testSkill(
                "esekai2:test_has_effect_primary_target_predicate",
                List.of(),
                Map.of(
                        "spell_cast_component",
                        List.of(new SkillRule(
                                Set.of(new SkillTargetSelector(SkillTargetType.TARGET, Map.of())),
                                List.of(soundAction("minecraft:block.anvil.land")),
                                List.of(new SkillCondition(SkillConditionType.ON_SPELL_CAST, Map.of())),
                                List.of(new SkillPredicate(
                                        SkillPredicateType.HAS_EFFECT,
                                        Map.of("effect_id", "minecraft:slowness")
                                ))
                        ))
                )
        );

        PreparedSkillUse prepared = Skills.prepareUse(skill, new SkillUseContext(newHolder(helper), newHolder(helper), List.of(), 0.0, 0.0));
        Player caster = helper.makeMockPlayer(GameType.CREATIVE);
        caster.snapTo(helper.absoluteVec(new Vec3(1.0, 2.0, 1.0)));
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(1.0, 2.0, 4.0));

        SkillExecutionResult withoutEffect = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.of(zombie)),
                new RecordingHooks()
        );

        zombie.addEffect(new MobEffectInstance(
                BuiltInRegistries.MOB_EFFECT.wrapAsHolder(
                        BuiltInRegistries.MOB_EFFECT.getOptional(Identifier.fromNamespaceAndPath("minecraft", "slowness"))
                                .orElseThrow(() -> helper.assertionException("Slowness should exist"))
                ),
                80,
                0
        ));

        SkillExecutionResult withEffect = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.of(zombie)),
                new RecordingHooks()
        );

        helper.assertValueEqual(withoutEffect.executedActions(), 0, "has_effect should block the route when the primary target lacks the effect");
        helper.assertValueEqual(withEffect.executedActions(), 1, "has_effect should allow the route when the primary target has the effect");
        helper.succeed();
    }

    /**
     * Verifies that has_effect predicates can inspect the source entity when subject is self.
     */
    @GameTest
    public void hasEffectEnPredUsesSelfSubject(GameTestHelper helper) {
        SkillDefinition skill = testSkill(
                "esekai2:test_has_effect_self_predicate",
                List.of(new SkillRule(
                        Set.of(new SkillTargetSelector(SkillTargetType.SELF, Map.of())),
                        List.of(soundAction("minecraft:block.note_block.bell")),
                        List.of(),
                        List.of(new SkillPredicate(
                                SkillPredicateType.HAS_EFFECT,
                                Map.of("effect_id", "minecraft:speed", "subject", "self")
                        ))
                )),
                Map.of()
        );

        PreparedSkillUse prepared = Skills.prepareUse(skill, new SkillUseContext(newHolder(helper), newHolder(helper), List.of(), 0.0, 0.0));
        Player caster = helper.makeMockPlayer(GameType.CREATIVE);
        caster.snapTo(helper.absoluteVec(new Vec3(1.0, 2.0, 1.0)));

        SkillExecutionResult withoutEffect = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.empty()),
                new RecordingHooks()
        );

        caster.addEffect(new MobEffectInstance(
                BuiltInRegistries.MOB_EFFECT.wrapAsHolder(
                        BuiltInRegistries.MOB_EFFECT.getOptional(Identifier.fromNamespaceAndPath("minecraft", "speed"))
                                .orElseThrow(() -> helper.assertionException("Speed should exist"))
                ),
                80,
                0
        ));

        SkillExecutionResult withEffect = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.empty()),
                new RecordingHooks()
        );

        helper.assertValueEqual(withoutEffect.executedActions(), 0, "Self has_effect should block when the caster lacks the effect");
        helper.assertValueEqual(withEffect.executedActions(), 1, "Self has_effect should pass when the caster has the effect");
        helper.succeed();
    }

    /**
     * Verifies that target-subject any_of has_effect predicates match when any configured target effect is present.
     */
    @GameTest
    public void hasEffectEnPredAnyOfUsesTargetSubject(GameTestHelper helper) {
        SkillDefinition skill = testSkill(
                "esekai2:test_has_effect_any_of_target_subject",
                List.of(new SkillRule(
                        Set.of(new SkillTargetSelector(SkillTargetType.TARGET, Map.of())),
                        List.of(soundAction("minecraft:block.note_block.chime")),
                        List.of(),
                        List.of(SkillPredicate.hasEffects(
                                List.of(
                                        Identifier.fromNamespaceAndPath("minecraft", "speed"),
                                        Identifier.fromNamespaceAndPath("minecraft", "slowness")
                                ),
                                SkillPredicateSubject.TARGET,
                                SkillPredicateMatchMode.ANY_OF,
                                false
                        ))
                )),
                Map.of()
        );

        PreparedSkillUse prepared = Skills.prepareUse(skill, new SkillUseContext(newHolder(helper), newHolder(helper), List.of(), 0.0, 0.0));
        Player caster = helper.makeMockPlayer(GameType.CREATIVE);
        caster.snapTo(helper.absoluteVec(new Vec3(1.0, 2.0, 1.0)));
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(1.0, 2.0, 4.0));

        SkillExecutionResult withoutTarget = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.empty()),
                new RecordingHooks()
        );
        SkillExecutionResult withoutEffects = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.of(zombie)),
                new RecordingHooks()
        );

        zombie.addEffect(effect(helper, Identifier.fromNamespaceAndPath("minecraft", "slowness")));

        SkillExecutionResult withOneMatchingEffect = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.of(zombie)),
                new RecordingHooks()
        );

        helper.assertValueEqual(withoutTarget.executedActions(), 0, "Target-subject has_effect should fail when the cast has no target");
        helper.assertValueEqual(withoutEffects.executedActions(), 0, "Target-subject any_of has_effect should fail when the target has none of the configured effects");
        helper.assertValueEqual(withOneMatchingEffect.executedActions(), 1, "Target-subject any_of has_effect should pass when the target has one configured effect");
        helper.succeed();
    }

    /**
     * Verifies that primary-target all_of has_effect predicates require every configured effect, including built-in ailment identities.
     */
    @GameTest
    public void hasEffectEnPredAllOfUsesPrimaryTargetAndAilmentIdentity(GameTestHelper helper) {
        SkillDefinition skill = testSkill(
                "esekai2:test_has_effect_all_of_primary_target",
                List.of(),
                Map.of(
                        "spell_cast_component",
                        List.of(new SkillRule(
                                Set.of(new SkillTargetSelector(SkillTargetType.TARGET, Map.of())),
                                List.of(soundAction("minecraft:block.note_block.flute")),
                                List.of(new SkillCondition(SkillConditionType.ON_SPELL_CAST, Map.of())),
                                List.of(SkillPredicate.hasEffects(
                                        List.of(
                                                Identifier.fromNamespaceAndPath("minecraft", "slowness"),
                                                Identifier.fromNamespaceAndPath("esekai2", "poison")
                                        ),
                                        SkillPredicateSubject.PRIMARY_TARGET,
                                        SkillPredicateMatchMode.ALL_OF,
                                        false
                                ))
                        ))
                )
        );

        PreparedSkillUse prepared = Skills.prepareUse(skill, new SkillUseContext(newHolder(helper), newHolder(helper), List.of(), 0.0, 0.0));
        Player caster = helper.makeMockPlayer(GameType.CREATIVE);
        caster.snapTo(helper.absoluteVec(new Vec3(1.0, 2.0, 1.0)));
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(1.0, 2.0, 4.0));

        SkillExecutionResult withoutEffects = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.of(zombie)),
                new RecordingHooks()
        );

        zombie.addEffect(effect(helper, Identifier.fromNamespaceAndPath("minecraft", "slowness")));

        SkillExecutionResult withOnlyVanillaEffect = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.of(zombie)),
                new RecordingHooks()
        );

        zombie.addEffect(effect(helper, Identifier.fromNamespaceAndPath("esekai2", "poison")));

        SkillExecutionResult withAllRequiredEffects = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.of(zombie)),
                new RecordingHooks()
        );

        helper.assertValueEqual(withoutEffects.executedActions(), 0, "Primary-target all_of has_effect should fail when the target has none of the configured effects");
        helper.assertValueEqual(withOnlyVanillaEffect.executedActions(), 0, "Primary-target all_of has_effect should fail until every configured effect is active");
        helper.assertValueEqual(withAllRequiredEffects.executedActions(), 1, "Primary-target all_of has_effect should pass when both the vanilla effect and ailment identity are active");
        helper.succeed();
    }

    /**
     * Verifies that self-subject negate has_effect predicates invert the final compound match result.
     */
    @GameTest
    public void hasEffectEnPredNegateInvertsCompoundSelfMatch(GameTestHelper helper) {
        SkillDefinition skill = testSkill(
                "esekai2:test_has_effect_negate_self",
                List.of(new SkillRule(
                        Set.of(new SkillTargetSelector(SkillTargetType.SELF, Map.of())),
                        List.of(soundAction("minecraft:block.note_block.bit")),
                        List.of(),
                        List.of(SkillPredicate.hasEffects(
                                List.of(
                                        Identifier.fromNamespaceAndPath("minecraft", "speed"),
                                        Identifier.fromNamespaceAndPath("minecraft", "slowness")
                                ),
                                SkillPredicateSubject.SELF,
                                SkillPredicateMatchMode.ANY_OF,
                                true
                        ))
                )),
                Map.of()
        );

        PreparedSkillUse prepared = Skills.prepareUse(skill, new SkillUseContext(newHolder(helper), newHolder(helper), List.of(), 0.0, 0.0));
        Player caster = helper.makeMockPlayer(GameType.CREATIVE);
        caster.snapTo(helper.absoluteVec(new Vec3(1.0, 2.0, 1.0)));

        SkillExecutionResult withoutEffects = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.empty()),
                new RecordingHooks()
        );

        caster.addEffect(effect(helper, Identifier.fromNamespaceAndPath("minecraft", "speed")));

        SkillExecutionResult withMatchingEffect = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.empty()),
                new RecordingHooks()
        );

        helper.assertValueEqual(withoutEffects.executedActions(), 1, "Negated self any_of has_effect should pass when the caster lacks every configured effect");
        helper.assertValueEqual(withMatchingEffect.executedActions(), 0, "Negated self any_of has_effect should fail when the caster gains one configured effect");
        helper.succeed();
    }

    /**
     * Verifies that target-subject has_effect predicates can gate generic apply_dot actions on the runtime execution path.
     */
    @GameTest
    public void hasEffectPredicateGatesApplyDotRuntimeExecution(GameTestHelper helper) {
        SkillDefinition skill = testSkill(
                "esekai2:test_has_effect_apply_dot_runtime",
                List.of(new SkillRule(
                        Set.of(new SkillTargetSelector(SkillTargetType.TARGET, Map.of())),
                        List.of(new SkillAction(SkillActionType.APPLY_DOT, Map.of(
                                "dot_id", "predicate_runtime_dot",
                                "duration_ticks", "8",
                                "tick_interval", "2",
                                "base_damage_fire", "4.0"
                        ))),
                        List.of(),
                        List.of(SkillPredicate.hasEffect(
                                Identifier.fromNamespaceAndPath("minecraft", "slowness"),
                                SkillPredicateSubject.TARGET
                        ))
                )),
                Map.of()
        );

        PreparedSkillUse prepared = Skills.prepareUse(skill, new SkillUseContext(newHolder(helper), newHolder(helper), List.of(), 0.0, 0.0));
        Player caster = helper.makeMockPlayer(GameType.CREATIVE);
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(3.0, 2.0, 3.0));

        SkillExecutionResult gatedOff = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.of(zombie))
        );

        zombie.addEffect(effect(helper, Identifier.fromNamespaceAndPath("minecraft", "slowness")));
        float healthBeforePassingCast = zombie.getHealth();

        SkillExecutionResult passing = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.of(zombie))
        );

        helper.assertValueEqual(gatedOff.executedActions(), 0, "Generic apply_dot should not execute when the target lacks the required predicate effect");
        helper.assertValueEqual(passing.executedActions(), 1, "Generic apply_dot should execute once the target gains the required predicate effect");
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(zombie.getHealth() < healthBeforePassingCast,
                    "A predicate-approved apply_dot should still register runtime damage ticks");
            helper.succeed();
        });
    }

    /**
     * Verifies that has_effect predicates can match built-in ailment effect identities.
     */
    @GameTest
    public void hasEffectEnPredMatchesBuiltInAilmentEffectIdentity(GameTestHelper helper) {
        SkillPredicate predicate = SkillPredicate.hasEffect(Identifier.fromNamespaceAndPath("esekai2", "poison"), SkillPredicateSubject.SELF);
        PreparedSkillUse prepared = Skills.prepareUse(
                testSkill("esekai2:test_has_effect_ailment_identity", List.of(), Map.of()),
                new SkillUseContext(newHolder(helper), newHolder(helper), List.of(), 0.0, 0.0)
        );
        Player caster = helper.makeMockPlayer(GameType.CREATIVE);
        caster.snapTo(helper.absoluteVec(new Vec3(1.0, 2.0, 1.0)));

        SkillExecutionContext contextWithoutEffect = SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.empty());
        helper.assertTrue(!predicate.matches(contextWithoutEffect), "Poison has_effect should fail when the caster lacks the ailment identity");

        caster.addEffect(new MobEffectInstance(
                BuiltInRegistries.MOB_EFFECT.wrapAsHolder(
                        BuiltInRegistries.MOB_EFFECT.getOptional(Identifier.fromNamespaceAndPath("esekai2", "poison"))
                                .orElseThrow(() -> helper.assertionException("Poison effect should exist"))
                ),
                80,
                0
        ));

        SkillExecutionContext contextWithEffect = SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.empty());
        helper.assertTrue(predicate.matches(contextWithEffect), "Poison has_effect should pass when the caster has the built-in ailment identity");
        helper.succeed();
    }

    /**
     * Verifies that support-appended on-cast rules can use compound has_effect predicates after support merging.
     */
    @GameTest
    public void supportAppendedOnCastRuleUsesCompoundHasEffectPredicate(GameTestHelper helper) {
        SkillSupportDefinition support = new SkillSupportDefinition(
                "esekai2:test_compound_has_effect_support",
                List.of(new SkillSupportEffect(
                        new SkillTagCondition(Set.of(), Set.of()),
                        Set.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(new SkillSupportRuleAppend(
                                new SkillSupportRuleAppendTarget(SkillSupportRuleTargetType.ON_CAST, ""),
                                List.of(new SkillRule(
                                        Set.of(new SkillTargetSelector(SkillTargetType.TARGET, Map.of())),
                                        List.of(soundAction("minecraft:block.note_block.harp")),
                                        List.of(new SkillCondition(SkillConditionType.ON_SPELL_CAST, Map.of())),
                                        List.of(SkillPredicate.hasEffects(
                                                List.of(
                                                        Identifier.fromNamespaceAndPath("minecraft", "slowness"),
                                                        Identifier.fromNamespaceAndPath("esekai2", "poison")
                                                ),
                                                SkillPredicateSubject.TARGET,
                                                SkillPredicateMatchMode.ALL_OF,
                                                false
                                        ))
                                ))
                        ))
                ))
        );

        PreparedSkillUse prepared = Skills.prepareUse(
                fireball(helper),
                skillUseContext(helper, newHolder(helper), newHolder(helper), List.of(), 0.0, 0.0)
                        .withLinkedSupports(List.of(support))
        );
        Player caster = helper.makeMockPlayer(GameType.CREATIVE);
        caster.snapTo(helper.absoluteVec(new Vec3(1.0, 2.0, 1.0)));
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(1.0, 2.0, 4.0));

        RecordingHooks withoutAllRequiredEffectsHooks = new RecordingHooks();
        SkillExecutionResult withoutAllRequiredEffects = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.of(zombie)),
                withoutAllRequiredEffectsHooks
        );

        zombie.addEffect(effect(helper, Identifier.fromNamespaceAndPath("minecraft", "slowness")));
        zombie.addEffect(effect(helper, Identifier.fromNamespaceAndPath("esekai2", "poison")));

        RecordingHooks withAllRequiredEffectsHooks = new RecordingHooks();
        SkillExecutionResult withAllRequiredEffects = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.of(zombie)),
                withAllRequiredEffectsHooks
        );

        helper.assertTrue(withoutAllRequiredEffects.executedActions() > 0,
                "Base fireball actions should still execute when the support-appended compound rule is gated off");
        helper.assertValueEqual(withAllRequiredEffects.executedActions() - withoutAllRequiredEffects.executedActions(), 1,
                "Compound has_effect support gating should add exactly one appended action once all configured target effects are active");
        helper.assertValueEqual(withAllRequiredEffectsHooks.soundCount() - withoutAllRequiredEffectsHooks.soundCount(), 1,
                "Compound has_effect support gating should add exactly one appended sound action once all configured target effects are active");
        helper.succeed();
    }

    /**
     * Verifies that direct executeOnCast blocks skills disabled in the current dimension before any runtime actions execute.
     */
    @GameTest
    public void executeOnCastBlocksDisabledDimensionSkillDefinitions(GameTestHelper helper) {
        PreparedSkillUse prepared = Skills.prepareUse(
                skill(helper, OVERWORLD_BARRIER_SKILL_ID),
                new SkillUseContext(newHolder(helper), newHolder(helper), List.of(), 0.0, 0.0)
        );
        Player caster = helper.makeMockPlayer(GameType.CREATIVE);
        caster.snapTo(helper.absoluteVec(new Vec3(1.0, 2.0, 1.0)));

        RecordingHooks hooks = new RecordingHooks();
        SkillExecutionResult result = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.empty()),
                hooks
        );

        helper.assertValueEqual(result.executedActions(), 0, "Disabled-dimension skills should be blocked before runtime actions execute");
        helper.assertValueEqual(hooks.soundCount(), 0, "Disabled-dimension direct casts should not emit on-cast sound actions");
        helper.assertTrue(result.warnings().stream().anyMatch(warning -> warning.contains("minecraft:overworld")),
                "Disabled-dimension direct casts should surface the resolved dimension id in the warning");
        helper.succeed();
    }

    /**
     * Verifies that direct player-sourced executeOnCast starts and enforces cooldown state for positive-cooldown skills.
     */
    @GameTest
    public void executeOnCastBlocksPlayerCooldownWhileSkillIsCoolingDown(GameTestHelper helper) {
        PreparedSkillUse prepared = Skills.prepareUse(
                skill(helper, BATTLE_FOCUS_SKILL_ID),
                new SkillUseContext(newHolder(helper), newHolder(helper), List.of(), 0.0, 0.0)
        );
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        player.snapTo(helper.absoluteVec(new Vec3(1.0, 2.0, 1.0)));

        RecordingHooks firstHooks = new RecordingHooks();
        SkillExecutionResult first = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), player, Optional.empty()),
                firstHooks
        );

        RecordingHooks secondHooks = new RecordingHooks();
        SkillExecutionResult second = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), player, Optional.empty()),
                secondHooks
        );

        helper.assertTrue(first.executedActions() > 0, "The first direct player-sourced cast should execute successfully");
        helper.assertTrue(PlayerSkillCooldowns.isOnCooldown(player, BATTLE_FOCUS_SKILL_ID, helper.getLevel().getGameTime()),
                "The first direct player-sourced cast should start cooldown state");
        helper.assertValueEqual(second.executedActions(), 0, "Cooldown-active direct casts should be blocked before runtime actions execute");
        helper.assertValueEqual(secondHooks.soundCount(), 0, "Cooldown-blocked direct casts should not emit on-cast sound actions");
        helper.assertTrue(second.warnings().stream().anyMatch(warning -> warning.contains("cooldown")),
                "Cooldown-blocked direct casts should surface a warning");
        helper.succeed();
    }

    /**
     * Verifies that cooldown_remaining and cooldown_ready reflect helper-built direct player cooldown state.
     */
    @GameTest
    public void cooldownRuntimeValueAndPredicateReflectDirectPlayerCooldownState(GameTestHelper helper) {
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        PlayerCombatStats.get(player).setBaseValue(CombatStats.MANA, 20.0);
        PlayerResources.setMana(player, 20.0, 20.0);

        PreparedSkillUse prepared = Skills.prepareUse(
                skill(helper, BATTLE_FOCUS_SKILL_ID),
                SkillUseContexts.forPlayer(player, 0.0, 0.0)
        );
        SkillExecutionContext executionContext = SkillExecutionContext.forCast(prepared, helper.getLevel(), player, Optional.empty());

        helper.assertValueEqual(SkillValueExpression.cooldownRemaining().resolve(prepared.useContext()), 0.0,
                "cooldown_remaining should start at zero before the first successful direct cast");
        helper.assertTrue(SkillPredicate.cooldownReady().matches(executionContext),
                "cooldown_ready should pass before the first successful direct cast");

        SkillExecutionResult result = Skills.executeOnCast(executionContext);

        helper.assertTrue(result.executedActions() > 0, "The cooldown test cast should execute successfully");
        helper.assertTrue(SkillValueExpression.cooldownRemaining().resolve(executionContext) > 0.0,
                "cooldown_remaining should become positive after the successful direct cast");
        helper.assertFalse(SkillPredicate.cooldownReady().matches(executionContext),
                "cooldown_ready should fail while the direct cast cooldown is active");
        helper.succeed();
    }

    /**
     * Verifies that direct player-sourced executeOnCast spends mana only when the attacker mana stat is positive.
     */
    @GameTest
    public void executeOnCastSpendsManaForPositiveManaPlayerSources(GameTestHelper helper) {
        StatHolder attacker = newHolder(helper);
        attacker.setBaseValue(CombatStats.MANA, 20.0);
        PreparedSkillUse prepared = Skills.prepareUse(
                fireball(helper),
                skillUseContext(helper, attacker, newHolder(helper), List.of(), 0.0, 0.99)
        );
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        player.snapTo(helper.absoluteVec(new Vec3(1.0, 2.0, 1.0)));
        PlayerResources.setMana(player, 20.0, 20.0);

        RecordingHooks hooks = new RecordingHooks();
        SkillExecutionResult result = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), player, Optional.empty()),
                hooks
        );

        helper.assertTrue(result.executedActions() > 0, "Positive-mana direct player casts should execute when enough mana is available");
        helper.assertValueEqual(PlayerResources.getMana(player, 20.0), 8.0, "Successful positive-mana direct casts should spend mana");
        helper.succeed();
    }

    /**
     * Verifies that direct player-sourced executeOnCast blocks for insufficient mana only when the attacker mana stat is positive.
     */
    @GameTest
    public void executeOnCastBlocksInsufficientPositiveManaPlayerSources(GameTestHelper helper) {
        StatHolder attacker = newHolder(helper);
        attacker.setBaseValue(CombatStats.MANA, 20.0);
        PreparedSkillUse prepared = Skills.prepareUse(
                fireball(helper),
                skillUseContext(helper, attacker, newHolder(helper), List.of(), 0.0, 0.99)
        );
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        player.snapTo(helper.absoluteVec(new Vec3(1.0, 2.0, 1.0)));
        PlayerResources.setMana(player, 5.0, 20.0);

        RecordingHooks hooks = new RecordingHooks();
        SkillExecutionResult result = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), player, Optional.empty()),
                hooks
        );

        helper.assertValueEqual(result.executedActions(), 0, "Positive-mana direct player casts should be blocked when current mana is insufficient");
        helper.assertValueEqual(hooks.soundCount(), 0, "Insufficient-mana direct casts should not emit on-cast sound actions");
        helper.assertTrue(result.warnings().stream().anyMatch(warning -> warning.contains("mana")),
                "Insufficient positive-mana direct casts should surface a warning");
        helper.assertValueEqual(PlayerResources.getMana(player, 20.0), 5.0, "Blocked positive-mana direct casts should not spend mana");
        helper.succeed();
    }

    /**
     * Verifies that direct player-sourced executeOnCast spends one stored charge on a successful charged cast.
     */
    @GameTest
    public void executeOnCastSpendsDirectPlayerChargeOnSuccessfulCast(GameTestHelper helper) {
        PreparedSkillUse prepared = Skills.prepareUse(
                skill(helper, CHARGED_SURGE_SKILL_ID),
                new SkillUseContext(newHolder(helper), newHolder(helper), List.of(), 0.0, 0.0)
        );
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        player.snapTo(helper.absoluteVec(new Vec3(1.0, 2.0, 1.0)));

        RecordingHooks firstHooks = new RecordingHooks();
        SkillExecutionResult first = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), player, Optional.empty()),
                firstHooks
        );

        RecordingHooks secondHooks = new RecordingHooks();
        SkillExecutionResult second = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), player, Optional.empty()),
                secondHooks
        );

        helper.assertTrue(first.executedActions() > 0, "The first charged direct cast should execute successfully");
        helper.assertValueEqual(firstHooks.soundCount(), 1, "The first charged direct cast should execute its on-cast sound action once");
        helper.assertValueEqual(second.executedActions(), 0, "A second immediate charged cast should fail after the only stored charge is spent");
        helper.assertValueEqual(secondHooks.soundCount(), 0, "Charge-blocked direct casts should not emit on-cast sound actions");
        helper.assertTrue(second.warnings().stream().anyMatch(warning -> warning.contains("charge")),
                "Charge-blocked direct casts should surface a charge warning");
        helper.succeed();
    }

    /**
     * Verifies that charges_available and has_charges reflect helper-built direct player charge state.
     */
    @GameTest
    public void chargesRuntimeValueAndPredicateReflectDirectPlayerChargeState(GameTestHelper helper) {
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        PreparedSkillUse prepared = Skills.prepareUse(
                skill(helper, CHARGED_SURGE_SKILL_ID),
                SkillUseContexts.forPlayer(player, 0.0, 0.0)
        );
        SkillExecutionContext executionContext = SkillExecutionContext.forCast(prepared, helper.getLevel(), player, Optional.empty());

        helper.assertValueEqual(SkillValueExpression.chargesAvailable().resolve(prepared.useContext()), 1.0,
                "charges_available should expose the initial stored charge count for the direct cast");
        helper.assertTrue(SkillPredicate.hasCharges().matches(executionContext),
                "has_charges should pass while the direct cast still has an available charge");

        SkillExecutionResult result = Skills.executeOnCast(executionContext);

        helper.assertTrue(result.executedActions() > 0, "The charged direct cast should execute successfully");
        helper.assertValueEqual(SkillValueExpression.chargesAvailable().resolve(executionContext), 0.0,
                "charges_available should drop to zero after the only stored charge is consumed");
        helper.assertFalse(SkillPredicate.hasCharges().matches(executionContext),
                "has_charges should fail once the direct cast has no stored charges left");
        helper.succeed();
    }

    /**
     * Verifies that direct player-sourced executeOnCast blocks when a charged skill has no stored charges remaining.
     */
    @GameTest
    public void executeOnCastBlocksDirectPlayerCastAtZeroCharges(GameTestHelper helper) {
        PreparedSkillUse prepared = Skills.prepareUse(
                skill(helper, CHARGED_SURGE_SKILL_ID),
                new SkillUseContext(newHolder(helper), newHolder(helper), List.of(), 0.0, 0.0)
        );
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        player.snapTo(helper.absoluteVec(new Vec3(1.0, 2.0, 1.0)));

        Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), player, Optional.empty()),
                new RecordingHooks()
        );

        RecordingHooks blockedHooks = new RecordingHooks();
        SkillExecutionResult blocked = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), player, Optional.empty()),
                blockedHooks
        );

        helper.assertValueEqual(blocked.executedActions(), 0, "Zero-charge direct casts should be blocked before runtime actions execute");
        helper.assertValueEqual(blockedHooks.soundCount(), 0, "Zero-charge direct casts should not emit on-cast sound actions");
        helper.assertTrue(blocked.warnings().stream().anyMatch(warning -> warning.contains("charge")),
                "Zero-charge direct casts should surface a charge warning");
        helper.succeed();
    }

    /**
     * Verifies that direct player-sourced executeOnCast can consume a regenerated charge after enough game time has elapsed.
     */
    @GameTest
    public void executeOnCastRestoresDirectPlayerChargeAfterTimeAdvance(GameTestHelper helper) {
        PreparedSkillUse prepared = Skills.prepareUse(
                skill(helper, CHARGED_SURGE_SKILL_ID),
                new SkillUseContext(newHolder(helper), newHolder(helper), List.of(), 0.0, 0.0)
        );
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        player.snapTo(helper.absoluteVec(new Vec3(1.0, 2.0, 1.0)));

        Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), player, Optional.empty()),
                new RecordingHooks()
        );

        helper.runAfterDelay(11, () -> {
            RecordingHooks regenHooks = new RecordingHooks();
            SkillExecutionResult regenerated = Skills.executeOnCast(
                    SkillExecutionContext.forCast(prepared, helper.getLevel(), player, Optional.empty()),
                    regenHooks
            );

            helper.assertTrue(regenerated.executedActions() > 0, "A regenerated charged cast should execute successfully after enough time has elapsed");
            helper.assertValueEqual(regenHooks.soundCount(), 1, "A regenerated charged cast should execute its on-cast sound action once");
            helper.succeed();
        });
    }

    /**
     * Verifies that direct player-sourced executeOnCast treats stored charges and cooldown as separate gates on the same charged skill.
     */
    @GameTest
    public void executeOnCastKeepsDirectPlayerChargesAvailableAcrossCooldownExpiry(GameTestHelper helper) {
        PreparedSkillUse prepared = Skills.prepareUse(
                skill(helper, CHARGED_RESERVE_SKILL_ID),
                new SkillUseContext(newHolder(helper), newHolder(helper), List.of(), 0.0, 0.0)
        );
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        player.snapTo(helper.absoluteVec(new Vec3(1.0, 2.0, 1.0)));

        SkillExecutionResult first = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), player, Optional.empty()),
                new RecordingHooks()
        );
        SkillExecutionResult blockedByCooldown = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), player, Optional.empty()),
                new RecordingHooks()
        );

        helper.assertTrue(first.executedActions() > 0, "The first charged cooldown cast should execute successfully");
        helper.assertTrue(blockedByCooldown.warnings().stream().anyMatch(warning -> warning.contains("cooldown")),
                "An immediate recast should be blocked by cooldown before the remaining stored charge is used");

        helper.runAfterDelay(11, () -> {
            RecordingHooks postCooldownHooks = new RecordingHooks();
            SkillExecutionResult afterCooldown = Skills.executeOnCast(
                    SkillExecutionContext.forCast(prepared, helper.getLevel(), player, Optional.empty()),
                    postCooldownHooks
            );

            helper.assertTrue(afterCooldown.executedActions() > 0,
                    "Once cooldown expires, the next cast should still succeed because another stored charge remains");
            helper.assertValueEqual(postCooldownHooks.soundCount(), 1,
                    "The post-cooldown charged cast should execute its on-cast sound action once");
            helper.succeed();
        });
    }

    /**
     * Verifies that cooldown-blocked charged casts do not spend mana until a later cast actually executes.
     */
    @GameTest
    public void executeOnCastDoesNotSpendManaWhenCooldownBlocksChargedCast(GameTestHelper helper) {
        StatHolder attacker = newHolder(helper);
        attacker.setBaseValue(CombatStats.MANA, 20.0);
        PreparedSkillUse prepared = Skills.prepareUse(
                skill(helper, CHARGED_RESERVE_SKILL_ID),
                skillUseContext(helper, attacker, newHolder(helper), List.of(), 0.0, 0.0)
        );
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        player.snapTo(helper.absoluteVec(new Vec3(1.0, 2.0, 1.0)));
        PlayerResources.setMana(player, 20.0, 20.0);

        SkillExecutionResult first = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), player, Optional.empty()),
                new RecordingHooks()
        );
        SkillExecutionResult blockedByCooldown = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), player, Optional.empty()),
                new RecordingHooks()
        );

        helper.assertTrue(first.executedActions() > 0, "The first charged mana-positive cast should execute successfully");
        helper.assertValueEqual(PlayerResources.getMana(player, 20.0), 16.0, "A successful charged cast should spend mana once");
        helper.assertTrue(blockedByCooldown.warnings().stream().anyMatch(warning -> warning.contains("cooldown")),
                "The immediate recast should be blocked by cooldown");
        helper.assertValueEqual(PlayerResources.getMana(player, 20.0), 16.0, "A cooldown-blocked charged cast should not spend mana");

        helper.runAfterDelay(11, () -> {
            SkillExecutionResult afterCooldown = Skills.executeOnCast(
                    SkillExecutionContext.forCast(prepared, helper.getLevel(), player, Optional.empty()),
                    new RecordingHooks()
            );

            helper.assertTrue(afterCooldown.executedActions() > 0, "The post-cooldown charged cast should execute successfully");
            helper.assertValueEqual(PlayerResources.getMana(player, 20.0), 12.0, "Mana should only drop again when the later charged cast actually executes");
            helper.succeed();
        });
    }

    /**
     * Verifies that zero-charge direct casts do not spend mana or start cooldown on charged skills.
     */
    @GameTest
    public void executeOnCastDoesNotSpendManaOrStartCooldownWhenChargesAreEmpty(GameTestHelper helper) {
        StatHolder attacker = newHolder(helper);
        attacker.setBaseValue(CombatStats.MANA, 20.0);
        PreparedSkillUse prepared = Skills.prepareUse(
                skill(helper, CHARGED_RESERVE_SKILL_ID),
                skillUseContext(helper, attacker, newHolder(helper), List.of(), 0.0, 0.0)
        );
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        player.snapTo(helper.absoluteVec(new Vec3(1.0, 2.0, 1.0)));
        PlayerResources.setMana(player, 20.0, 20.0);
        PlayerSkillCharges.setAvailableCharges(player, CHARGED_RESERVE_SKILL_ID, 0, 2, helper.getLevel().getGameTime());

        SkillExecutionResult blocked = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), player, Optional.empty()),
                new RecordingHooks()
        );

        helper.assertValueEqual(blocked.executedActions(), 0, "Zero-charge direct casts should not execute runtime actions");
        helper.assertTrue(blocked.warnings().stream().anyMatch(warning -> warning.contains("charge")),
                "Zero-charge direct casts should surface a charge warning");
        helper.assertValueEqual(PlayerResources.getMana(player, 20.0), 20.0,
                "Zero-charge direct casts should not spend mana");
        helper.assertTrue(!PlayerSkillCooldowns.isOnCooldown(player, CHARGED_RESERVE_SKILL_ID, helper.getLevel().getGameTime()),
                "Zero-charge direct casts should not start cooldown");
        helper.succeed();
    }

    /**
     * Verifies that direct player-sourced executeOnCast uses a fixed 10-tick burst window for times_to_cast=2 and reopens after expiry.
     */
    @GameTest
    public void executeOnCastUsesBurstWindowForTimesToCastTwo(GameTestHelper helper) {
        PreparedSkillUse prepared = Skills.prepareUse(
                skill(helper, BURST_STRIKE_SKILL_ID),
                new SkillUseContext(newHolder(helper), newHolder(helper), List.of(), 0.0, 0.0)
        );
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        player.snapTo(helper.absoluteVec(new Vec3(1.0, 2.0, 1.0)));

        RecordingHooks firstHooks = new RecordingHooks();
        RecordingHooks secondHooks = new RecordingHooks();
        RecordingHooks thirdHooks = new RecordingHooks();
        SkillExecutionResult first = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), player, Optional.empty()),
                firstHooks
        );
        SkillExecutionResult second = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), player, Optional.empty()),
                secondHooks
        );
        SkillExecutionResult third = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), player, Optional.empty()),
                thirdHooks
        );

        helper.assertValueEqual(first.executedActions(), 1, "The burst opener should execute its sound action under recording hooks");
        helper.assertValueEqual(second.executedActions(), 1, "The allowed direct follow-up should execute its sound action under recording hooks");
        helper.assertValueEqual(firstHooks.soundCount(), 1, "The burst opener should emit one sound action");
        helper.assertValueEqual(secondHooks.soundCount(), 1, "The burst follow-up should emit one sound action");
        helper.assertValueEqual(
                PlayerSkillBursts.activeBurst(player, BURST_STRIKE_SKILL_ID, helper.getLevel().getGameTime())
                        .map(PlayerSkillBurstState.SkillBurstEntry::remainingCasts)
                        .orElse(-1),
                0,
                "Two successful direct casts inside one burst should exhaust the remaining follow-up count"
        );
        helper.assertValueEqual(third.executedActions(), 0, "A third direct cast inside the same burst window should be blocked");
        helper.assertValueEqual(thirdHooks.soundCount(), 0, "A burst-blocked direct cast should not emit on-cast sound actions");
        helper.assertTrue(third.warnings().stream().anyMatch(warning -> warning.contains("burst")),
                "A burst-blocked direct cast should surface a burst warning");

        helper.runAfterDelay(11, () -> {
            RecordingHooks reopenedHooks = new RecordingHooks();
            SkillExecutionResult reopened = Skills.executeOnCast(
                    SkillExecutionContext.forCast(prepared, helper.getLevel(), player, Optional.empty()),
                    reopenedHooks
            );

            helper.assertValueEqual(reopened.executedActions(), 1,
                    "After burst expiry, the next direct cast should reopen a fresh burst under recording hooks");
            helper.assertValueEqual(reopenedHooks.soundCount(), 1, "The reopened burst opener should execute its on-cast sound once");
            helper.assertValueEqual(
                    PlayerSkillBursts.activeBurst(player, BURST_STRIKE_SKILL_ID, helper.getLevel().getGameTime())
                            .map(PlayerSkillBurstState.SkillBurstEntry::remainingCasts)
                            .orElse(-1),
                    1,
                    "A fresh direct burst opener should restore one remaining follow-up cast for times_to_cast=2"
            );
            helper.succeed();
        });
    }

    /**
     * Verifies that burst_remaining and has_burst_followup reflect helper-built direct player burst state.
     */
    @GameTest
    public void burstRuntimeValueAndPredicateReflectDirectPlayerBurstState(GameTestHelper helper) {
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        PreparedSkillUse prepared = Skills.prepareUse(
                skill(helper, BURST_FOCUS_SKILL_ID),
                SkillUseContexts.forPlayer(player, 0.0, 0.0)
        );
        SkillExecutionContext executionContext = SkillExecutionContext.forCast(prepared, helper.getLevel(), player, Optional.empty());

        helper.assertValueEqual(SkillValueExpression.burstRemaining().resolve(prepared.useContext()), 0.0,
                "burst_remaining should start at zero before the opener creates an active burst window");
        helper.assertFalse(SkillPredicate.hasBurstFollowup().matches(executionContext),
                "has_burst_followup should fail before the opener creates an active burst window");

        SkillExecutionResult opener = Skills.executeOnCast(executionContext);

        helper.assertValueEqual(opener.executedActions(), 2, "The burst opener should execute both its sound and particle actions successfully");
        helper.assertValueEqual(SkillValueExpression.burstRemaining().resolve(executionContext), 1.0,
                "burst_remaining should expose the remaining follow-up cast after the opener");
        helper.assertTrue(SkillPredicate.hasBurstFollowup().matches(executionContext),
                "has_burst_followup should pass while the active burst still has a follow-up cast");

        SkillExecutionResult followup = Skills.executeOnCast(executionContext);

        helper.assertValueEqual(followup.executedActions(), 2, "The allowed burst follow-up should execute both its sound and particle actions successfully");
        helper.assertValueEqual(SkillValueExpression.burstRemaining().resolve(executionContext), 0.0,
                "burst_remaining should return to zero after the final follow-up is consumed");
        helper.assertFalse(SkillPredicate.hasBurstFollowup().matches(executionContext),
                "has_burst_followup should fail after the final follow-up is consumed");
        helper.succeed();
    }

    /**
     * Verifies that a successful direct cast of a different skill resets the previous burst window immediately.
     */
    @GameTest
    public void executeOnCastResetsBurstAfterDifferentSkillSucceeds(GameTestHelper helper) {
        PreparedSkillUse burstPrepared = Skills.prepareUse(
                skill(helper, BURST_STRIKE_SKILL_ID),
                new SkillUseContext(newHolder(helper), newHolder(helper), List.of(), 0.0, 0.0)
        );
        PreparedSkillUse otherPrepared = Skills.prepareUse(
                skill(helper, BATTLE_FOCUS_SKILL_ID),
                new SkillUseContext(newHolder(helper), newHolder(helper), List.of(), 0.0, 0.0)
        );
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        player.snapTo(helper.absoluteVec(new Vec3(1.0, 2.0, 1.0)));

        SkillExecutionResult firstBurst = Skills.executeOnCast(
                SkillExecutionContext.forCast(burstPrepared, helper.getLevel(), player, Optional.empty()),
                new RecordingHooks()
        );
        SkillExecutionResult secondBurst = Skills.executeOnCast(
                SkillExecutionContext.forCast(burstPrepared, helper.getLevel(), player, Optional.empty()),
                new RecordingHooks()
        );
        SkillExecutionResult otherSkill = Skills.executeOnCast(
                SkillExecutionContext.forCast(otherPrepared, helper.getLevel(), player, Optional.empty()),
                new RecordingHooks()
        );
        SkillExecutionResult reopened = Skills.executeOnCast(
                SkillExecutionContext.forCast(burstPrepared, helper.getLevel(), player, Optional.empty()),
                new RecordingHooks()
        );
        SkillExecutionResult followUp = Skills.executeOnCast(
                SkillExecutionContext.forCast(burstPrepared, helper.getLevel(), player, Optional.empty()),
                new RecordingHooks()
        );

        helper.assertValueEqual(firstBurst.executedActions(), 1,
                "The initial direct burst opener should execute its sound action before the reset check");
        helper.assertValueEqual(secondBurst.executedActions(), 1,
                "The second direct burst cast should exhaust the current burst while only the sound hook completes");
        helper.assertTrue(otherSkill.executedActions() > 0,
                "A different direct skill must execute successfully to reset the current burst window");
        helper.assertValueEqual(reopened.executedActions(), 1,
                "After another skill succeeds, the original direct burst should reopen while only the sound hook completes");
        helper.assertValueEqual(followUp.executedActions(), 1,
                "The reopened direct burst should still allow one follow-up cast while only the sound hook completes");
        helper.succeed();
    }

    /**
     * Verifies that cooldown-blocked direct burst follow-ups keep the remaining burst cast available and do not spend mana, charges, or cooldown twice.
     */
    @GameTest
    public void executeOnCastKeepsBurstStateWhenCooldownBlocksFollowUp(GameTestHelper helper) {
        StatHolder attacker = newHolder(helper);
        attacker.setBaseValue(CombatStats.MANA, 20.0);
        PreparedSkillUse prepared = Skills.prepareUse(
                skill(helper, BURST_RESERVE_SKILL_ID),
                skillUseContext(helper, attacker, newHolder(helper), List.of(), 0.0, 0.0)
        );
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        player.snapTo(helper.absoluteVec(new Vec3(1.0, 2.0, 1.0)));
        PlayerResources.setMana(player, 20.0, 20.0);

        SkillExecutionResult first = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), player, Optional.empty()),
                new RecordingHooks()
        );
        long cooldownReadyTime = PlayerSkillCooldowns.readyGameTime(player, BURST_RESERVE_SKILL_ID, helper.getLevel().getGameTime())
                .stream()
                .findFirst()
                .orElse(0L);
        SkillExecutionResult blocked = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), player, Optional.empty()),
                new RecordingHooks()
        );

        helper.assertValueEqual(first.executedActions(), 1, "The first direct burst-reserve cast should execute its sound action under recording hooks");
        helper.assertTrue(blocked.warnings().stream().anyMatch(warning -> warning.contains("cooldown")),
                "An immediate direct burst follow-up should still be blocked by cooldown before the remaining burst cast is consumed");
        helper.assertValueEqual(PlayerResources.getMana(player, 20.0), 16.0,
                "A cooldown-blocked direct burst follow-up should not spend mana again");
        helper.assertValueEqual(PlayerSkillCharges.availableCharges(player, BURST_RESERVE_SKILL_ID, 2, helper.getLevel().getGameTime()), 1,
                "A cooldown-blocked direct burst follow-up should not consume the remaining stored charge");
        helper.assertValueEqual(
                PlayerSkillBursts.activeBurst(player, BURST_RESERVE_SKILL_ID, helper.getLevel().getGameTime())
                        .map(PlayerSkillBurstState.SkillBurstEntry::remainingCasts)
                        .orElse(-1),
                1,
                "A cooldown-blocked direct burst follow-up should leave the remaining burst cast available"
        );
        helper.assertValueEqual(
                PlayerSkillCooldowns.readyGameTime(player, BURST_RESERVE_SKILL_ID, helper.getLevel().getGameTime())
                        .stream()
                        .findFirst()
                        .orElse(0L),
                cooldownReadyTime,
                "A cooldown-blocked direct burst follow-up should not restart cooldown"
        );

        helper.runAfterDelay(6, () -> {
            SkillExecutionResult followUp = Skills.executeOnCast(
                    SkillExecutionContext.forCast(prepared, helper.getLevel(), player, Optional.empty()),
                    new RecordingHooks()
            );

            helper.assertValueEqual(followUp.executedActions(), 1,
                    "Once cooldown expires inside the burst window, the stored direct follow-up should still execute its sound action under recording hooks");
            helper.assertValueEqual(PlayerResources.getMana(player, 20.0), 12.0,
                    "The delayed direct follow-up should spend mana only when it actually executes");
            helper.assertValueEqual(PlayerSkillCharges.availableCharges(player, BURST_RESERVE_SKILL_ID, 2, helper.getLevel().getGameTime()), 0,
                    "The delayed direct follow-up should consume the remaining stored charge when it executes");
            helper.assertValueEqual(
                    PlayerSkillBursts.activeBurst(player, BURST_RESERVE_SKILL_ID, helper.getLevel().getGameTime())
                            .map(PlayerSkillBurstState.SkillBurstEntry::remainingCasts)
                            .orElse(-1),
                    0,
                    "The delayed direct follow-up should exhaust the burst once it successfully executes"
            );
            helper.succeed();
        });
    }

    /**
     * Verifies that mana-blocked direct burst follow-ups keep the remaining burst cast available.
     */
    @GameTest
    public void executeOnCastKeepsBurstStateWhenManaBlocksFollowUp(GameTestHelper helper) {
        StatHolder attacker = newHolder(helper);
        attacker.setBaseValue(CombatStats.MANA, 6.0);
        PreparedSkillUse prepared = Skills.prepareUse(
                skill(helper, BURST_FOCUS_SKILL_ID),
                skillUseContext(helper, attacker, newHolder(helper), List.of(), 0.0, 0.0)
        );
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        player.snapTo(helper.absoluteVec(new Vec3(1.0, 2.0, 1.0)));
        PlayerResources.setMana(player, 6.0, 6.0);

        SkillExecutionResult first = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), player, Optional.empty()),
                new RecordingHooks()
        );
        SkillExecutionResult blocked = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), player, Optional.empty()),
                new RecordingHooks()
        );

        helper.assertValueEqual(first.executedActions(), 1, "The opener mana-positive direct burst cast should execute its sound action under recording hooks");
        helper.assertValueEqual(blocked.executedActions(), 0, "A mana-blocked direct follow-up should not execute runtime actions");
        helper.assertTrue(blocked.warnings().stream().anyMatch(warning -> warning.contains("mana")),
                "A mana-blocked direct follow-up should surface a mana warning");
        helper.assertValueEqual(PlayerResources.getMana(player, 6.0), 2.0,
                "A mana-blocked direct follow-up should not spend mana again");
        helper.assertValueEqual(
                PlayerSkillBursts.activeBurst(player, BURST_FOCUS_SKILL_ID, helper.getLevel().getGameTime())
                        .map(PlayerSkillBurstState.SkillBurstEntry::remainingCasts)
                        .orElse(-1),
                1,
                "A mana-blocked direct follow-up should keep the remaining burst cast available"
        );

        PlayerResources.setMana(player, 6.0, 6.0);
        SkillExecutionResult recovered = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), player, Optional.empty()),
                new RecordingHooks()
        );

        helper.assertValueEqual(recovered.executedActions(), 1,
                "Restoring mana inside the burst window should allow the remaining direct follow-up cast with its sound action under recording hooks");
        helper.assertValueEqual(
                PlayerSkillBursts.activeBurst(player, BURST_FOCUS_SKILL_ID, helper.getLevel().getGameTime())
                        .map(PlayerSkillBurstState.SkillBurstEntry::remainingCasts)
                        .orElse(-1),
                0,
                "The recovered direct follow-up should exhaust the burst when it succeeds"
        );
        helper.succeed();
    }

    /**
     * Verifies that firing the sample fireball spawns a runtime projectile, damages the hit target, and cleans up the expire block.
     */
    @GameTest
    public void fireballRuntimeProjectileHitsTargetAndRestoresExpireBlock(GameTestHelper helper) {
        Player caster = helper.makeMockPlayer(GameType.CREATIVE);
        caster.snapTo(helper.absoluteVec(new Vec3(1.0, 2.0, 1.0)));
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(1.0, 2.0, 6.0));

        PreparedSkillUse prepared = Skills.prepareUse(
                fireball(helper),
                skillUseContext(helper, newHolder(helper), MonsterStats.resolveBaseHolder(zombie).orElse(newHolder(helper)), List.of(), 0.0, 0.99)
        );
        SkillExecutionContext context = SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.of(zombie));

        SkillExecutionResult result = Skills.executeOnCast(context);
        helper.assertValueEqual(3, result.executedActions(), "Fireball on-cast should execute sound, projectile, and particle actions");

        helper.runAfterDelay(10, () ->
                helper.assertTrue(zombie.getHealth() < zombie.getMaxHealth(), "Projectile hit should damage the zombie target")
        );

        helper.runAfterDelay(16, () -> {
            helper.assertTrue(helper.findEntities(SkillRuntimeEntityTypes.PROJECTILE, zombie.position(), 8.0).isEmpty(), "Projectile carrier should be removed after hit and expire");
            helper.succeed();
        });
    }

    /**
     * Verifies that summon_at_sight spawns a tracked runtime summon and removes it after the configured lifetime.
     */
    @GameTest
    public void summonAtSightRuntimeEntityExpiresAfterLifetime(GameTestHelper helper) {
        Player caster = helper.makeMockPlayer(GameType.CREATIVE);
        caster.snapTo(helper.absoluteVec(new Vec3(1.0, 2.0, 1.0)));
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(1.0, 2.0, 4.0));

        PreparedSkillUse prepared = Skills.prepareUse(
                basicStrike(helper),
                new SkillUseContext(newHolder(helper), newHolder(helper), List.of(), 0.0, 0.25)
        );
        SkillExecutionContext context = SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.of(zombie));

        SkillExecutionResult result = Skills.executeOnHit(context, "basic_strike_component");
        helper.assertValueEqual(result.executedActions(), 1, "Basic strike on-hit should execute the summon_at_sight route");

        helper.runAfterDelay(3, () -> {
            helper.assertTrue(helper.findEntities(EntityType.BLAZE, zombie.position(), 3.0).isEmpty(), "Summoned blaze should expire after its configured lifetime");
            helper.succeed();
        });
    }

    private static StatHolder newHolder(GameTestHelper helper) {
        return StatHolders.create(StatRegistryAccess.statRegistry(helper));
    }

    private static Registry<SkillDefinition> skillRegistry(GameTestHelper helper) {
        return helper.getLevel().getServer().registryAccess().lookupOrThrow(SkillRegistries.SKILL);
    }

    private static SkillDefinition basicStrike(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(BASIC_STRIKE_SKILL_ID)
                .orElseThrow(() -> helper.assertionException("Basic strike should decode successfully"));
    }

    private static SkillDefinition skill(GameTestHelper helper, Identifier skillId) {
        return skillRegistry(helper).getOptional(skillId)
                .orElseThrow(() -> helper.assertionException("Skill should decode successfully: " + skillId));
    }

    private static SkillDefinition fireball(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(FIREBALL_SKILL_ID)
                .orElseThrow(() -> helper.assertionException("Fireball should decode successfully"));
    }

    private static SkillSupportDefinition support(GameTestHelper helper, Identifier id) {
        return helper.getLevel().getServer().registryAccess().lookupOrThrow(SkillRegistries.SKILL_SUPPORT).getOptional(id)
                .orElseThrow(() -> helper.assertionException("Support should decode successfully: " + id));
    }

    private static Registry<SkillCalculationDefinition> skillCalculationRegistry(GameTestHelper helper) {
        return SkillCalculationRegistryAccess.skillCalculationRegistry(helper);
    }

    private static SkillCalculationDefinition skillCalculation(GameTestHelper helper, Identifier id) {
        return skillCalculationRegistry(helper).getOptional(id)
                .orElseThrow(() -> helper.assertionException("Skill calculation should decode successfully: " + id));
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

    private static SkillDefinition testSkill(String identifier, List<SkillRule> onCast, Map<String, List<SkillRule>> components) {
        return testSkill(identifier, SkillConfig.DEFAULT, onCast, components);
    }

    private static SkillDefinition testSkill(
            String identifier,
            SkillConfig config,
            List<SkillRule> onCast,
            Map<String, List<SkillRule>> components
    ) {
        return new SkillDefinition(identifier, config, new SkillAttached(onCast, components), "", Set.of(), "");
    }

    private static SkillAction soundAction(String soundId) {
        return new SkillAction(SkillActionType.SOUND, Map.of("sound", soundId));
    }

    private static SkillAction typedHealAction(SkillValueExpression amount) {
        return new SkillAction(
                SkillActionType.HEAL,
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                List.of(),
                Optional.empty(),
                "",
                "",
                kim.biryeong.esekai2.api.damage.critical.HitKind.ATTACK,
                Map.of(),
                SkillValueExpression.constant(0.0),
                SkillValueExpression.constant(100.0),
                amount,
                SkillValueExpression.constant(1.0),
                SkillValueExpression.constant(1.0),
                SkillValueExpression.constant(0.0),
                SkillValueExpression.constant(0.0),
                SkillValueExpression.constant(0.0),
                SkillValueExpression.constant(100.0),
                SkillValueExpression.constant(100.0),
                SkillValueExpression.constant(1.0),
                false,
                false,
                true,
                true,
                MobEffectRefreshPolicy.OVERWRITE,
                SkillAilmentRefreshPolicy.OVERWRITE,
                "self",
                SkillValueExpression.constant(0.0),
                SkillValueExpression.constant(0.0),
                SkillValueExpression.constant(0.0),
                List.of()
        );
    }

    private static SkillAction typedResourceDeltaAction(String resource, SkillValueExpression amount) {
        return new SkillAction(
                SkillActionType.RESOURCE_DELTA,
                "",
                "",
                "",
                "",
                "",
                "",
                resource,
                "",
                List.of(),
                Optional.empty(),
                "",
                "",
                kim.biryeong.esekai2.api.damage.critical.HitKind.ATTACK,
                Map.of(),
                SkillValueExpression.constant(0.0),
                SkillValueExpression.constant(100.0),
                amount,
                SkillValueExpression.constant(1.0),
                SkillValueExpression.constant(1.0),
                SkillValueExpression.constant(0.0),
                SkillValueExpression.constant(0.0),
                SkillValueExpression.constant(0.0),
                SkillValueExpression.constant(100.0),
                SkillValueExpression.constant(100.0),
                SkillValueExpression.constant(1.0),
                false,
                false,
                true,
                true,
                MobEffectRefreshPolicy.OVERWRITE,
                SkillAilmentRefreshPolicy.OVERWRITE,
                "self",
                SkillValueExpression.constant(0.0),
                SkillValueExpression.constant(0.0),
                SkillValueExpression.constant(0.0),
                List.of()
        );
    }

    private static MobEffectInstance effect(GameTestHelper helper, Identifier effectId) {
        MobEffect effect = BuiltInRegistries.MOB_EFFECT.getOptional(effectId)
                .orElseThrow(() -> helper.assertionException("Mob effect should exist: " + effectId));
        return new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect), 80, 0);
    }

    private static final class RecordingHooks implements SkillExecutionHooks {
        private final List<String> soundTypes = new ArrayList<>();

        private int soundCount() {
            return soundTypes.size();
        }

        @Override
        public boolean playSound(SkillExecutionContext context, List<net.minecraft.world.entity.Entity> targets, kim.biryeong.esekai2.api.skill.execution.PreparedSoundAction action) {
            soundTypes.add(action.actionType());
            return true;
        }

        @Override
        public java.util.Optional<DamageCalculationResult> applyDamage(SkillExecutionContext context, List<net.minecraft.world.entity.Entity> targets, PreparedDamageAction action) {
            return Optional.empty();
        }

        @Override
        public java.util.Optional<net.minecraft.world.entity.Entity> spawnProjectile(SkillExecutionContext context, List<net.minecraft.world.entity.Entity> targets, kim.biryeong.esekai2.api.skill.execution.PreparedProjectileAction action) {
            return Optional.empty();
        }

        @Override
        public java.util.Optional<net.minecraft.world.entity.Entity> spawnSummonAtSight(SkillExecutionContext context, List<net.minecraft.world.entity.Entity> targets, kim.biryeong.esekai2.api.skill.execution.PreparedSummonAtSightAction action) {
            return Optional.empty();
        }

        @Override
        public boolean placeBlock(SkillExecutionContext context, List<net.minecraft.world.entity.Entity> targets, kim.biryeong.esekai2.api.skill.execution.PreparedSummonBlockAction action) {
            return false;
        }

        @Override
        public boolean emitSandstormParticle(SkillExecutionContext context, List<net.minecraft.world.entity.Entity> targets, kim.biryeong.esekai2.api.skill.execution.PreparedSandstormParticleAction action) {
            return false;
        }
    }
}
