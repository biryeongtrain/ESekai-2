package kim.biryeong.esekai2.impl.gametest.skill;

import kim.biryeong.esekai2.api.damage.breakdown.DamageType;
import kim.biryeong.esekai2.api.damage.calculation.DamageCalculationResult;
import kim.biryeong.esekai2.api.damage.calculation.DamageCalculations;
import kim.biryeong.esekai2.api.monster.stat.MonsterStats;
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
import kim.biryeong.esekai2.api.skill.definition.graph.SkillPredicateType;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillRule;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillTargetSelector;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillTargetType;
import kim.biryeong.esekai2.api.skill.execution.PreparedDamageAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSkillAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSkillEntityComponent;
import kim.biryeong.esekai2.api.skill.execution.PreparedSkillUse;
import kim.biryeong.esekai2.api.skill.execution.SkillExecutionHooks;
import kim.biryeong.esekai2.api.skill.execution.SkillExecutionContext;
import kim.biryeong.esekai2.api.skill.execution.SkillExecutionResult;
import kim.biryeong.esekai2.api.skill.execution.SkillUseContext;
import kim.biryeong.esekai2.api.skill.execution.Skills;
import kim.biryeong.esekai2.api.skill.stat.SkillStats;
import kim.biryeong.esekai2.api.stat.combat.CombatStats;
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
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
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

    /**
     * Verifies that the skill registry loads the sample definition set used by these runtime tests.
     */
    @GameTest
    public void skillRegistryLoadsSampleDefinitions(GameTestHelper helper) {
        Registry<SkillDefinition> registry = skillRegistry(helper);

        helper.assertTrue(registry.containsKey(BASIC_STRIKE_SKILL_ID), "Basic strike should load into the skill registry");
        helper.assertTrue(registry.containsKey(FIREBALL_SKILL_ID), "Fireball should load into the skill registry");
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

        helper.assertValueEqual(decodedCondition, condition, "Skill condition codec should preserve x-tick payloads");
        helper.assertValueEqual(decodedPredicate, predicate, "Skill predicate codec should preserve random chance payloads");
        helper.succeed();
    }

    /**
     * Verifies that a runtime use resolves base resource, cast-time, and cooldown values.
     */
    @GameTest
    public void skillRuntimeValuesUseBaseConfigWithoutModifiers(GameTestHelper helper) {
        PreparedSkillUse prepared = Skills.prepareUse(fireball(helper), skillUseContext(helper, newHolder(helper), newHolder(helper), List.of(), 0.0, 0.99));

        helper.assertValueEqual(prepared.resourceCost(), 12.0, "Base skill resource cost should remain unchanged without modifiers");
        helper.assertValueEqual(prepared.useTimeTicks(), 16, "Base use-time should remain unchanged without modifiers");
        helper.assertValueEqual(prepared.cooldownTicks(), 0, "Base cooldown should remain unchanged without modifiers");
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
        helper.assertValueEqual(actionTypes.contains("sound"), true, "Basic strike should expose a sound action in on-cast");
        helper.assertValueEqual(actionTypes.contains("damage"), true, "Basic strike should expose a damage action in on-cast");
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

        List<String> onHitTypes = prepared.executeOnHit("basic_strike_component").stream().map(PreparedSkillAction::actionType).toList();
        List<String> onExpireTypes = prepared.executeOnEntityExpire("basic_strike_component").stream().map(PreparedSkillAction::actionType).toList();

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
                .map(PreparedDamageAction.class::cast)
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
                .map(PreparedDamageAction.class::cast)
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
                .map(PreparedDamageAction.class::cast)
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
                .map(PreparedDamageAction.class::cast)
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

        helper.assertValueEqual(sandstormAction.parameters().get("anchor"), "caster_hand", "Fireball should keep the anchor metadata");
        helper.assertValueEqual(sandstormAction.parameters().get("offset_y"), "0.5", "Fireball should keep the offset metadata");
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
                .map(PreparedDamageAction.class::cast)
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
        helper.assertValueEqual(result.executedActions(), 3, "Fireball on-cast should execute sound, projectile, and particle actions");

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

    private static SkillDefinition fireball(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(FIREBALL_SKILL_ID)
                .orElseThrow(() -> helper.assertionException("Fireball should decode successfully"));
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
        return new SkillDefinition(identifier, SkillConfig.DEFAULT, new SkillAttached(onCast, components), "", Set.of(), "");
    }

    private static SkillAction soundAction(String soundId) {
        return new SkillAction(SkillActionType.SOUND, Map.of("sound", soundId));
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
