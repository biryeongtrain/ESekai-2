package kim.biryeong.esekai2.impl.gametest.skill;

import kim.biryeong.esekai2.api.damage.breakdown.DamageType;
import kim.biryeong.esekai2.api.damage.calculation.DamageCalculationResult;
import kim.biryeong.esekai2.api.damage.calculation.DamageCalculations;
import kim.biryeong.esekai2.api.monster.stat.MonsterStats;
import kim.biryeong.esekai2.api.skill.definition.SkillDefinition;
import kim.biryeong.esekai2.api.skill.definition.SkillRegistries;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillAction;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillActionType;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillCondition;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillConditionType;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillRule;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillTargetSelector;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillTargetType;
import kim.biryeong.esekai2.api.skill.execution.PreparedDamageAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSkillAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSkillEntityComponent;
import kim.biryeong.esekai2.api.skill.execution.PreparedSkillUse;
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
     * Verifies that a runtime use resolves base resource, cast-time, and cooldown values.
     */
    @GameTest
    public void skillRuntimeValuesUseBaseConfigWithoutModifiers(GameTestHelper helper) {
        PreparedSkillUse prepared = Skills.prepareUse(fireball(helper), new SkillUseContext(newHolder(helper), newHolder(helper), List.of(), 0.0, 0.99));

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
                new SkillUseContext(attacker, newHolder(helper), List.of(), 0.0, 0.99)
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
                new SkillUseContext(
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
                new SkillUseContext(newHolder(helper), newHolder(helper), List.of(), 0.0, 0.99)
        );

        PreparedDamageAction damageAction = prepared.executeOnHit("default_entity_name").stream()
                .filter(action -> action instanceof PreparedDamageAction)
                .map(PreparedDamageAction.class::cast)
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Fireball should expose a damage action"));

        DamageCalculationResult result = DamageCalculations.calculateHit(damageAction.hitDamageCalculation());
        helper.assertTrue(result.hitResolution().bypassedAccuracyCheck(), "Projectile spell damage should be in hit pipeline");
        helper.assertValueEqual(result.finalDamage().amount(DamageType.FIRE), 18.0, "Damage calculation should preserve fixture base payload when parsed");
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
                new SkillUseContext(newHolder(helper), defender, List.of(), 0.0, 0.99)
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
     * Verifies that firing the sample fireball spawns a runtime projectile, damages the hit target, and cleans up the expire block.
     */
    @GameTest
    public void fireballRuntimeProjectileHitsTargetAndRestoresExpireBlock(GameTestHelper helper) {
        Player caster = helper.makeMockPlayer(GameType.CREATIVE);
        caster.snapTo(new Vec3(1.0, 2.0, 1.0));
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(1.0, 2.0, 6.0));

        PreparedSkillUse prepared = Skills.prepareUse(
                fireball(helper),
                new SkillUseContext(newHolder(helper), MonsterStats.resolveBaseHolder(zombie).orElse(newHolder(helper)), List.of(), 0.0, 0.99)
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
        caster.snapTo(new Vec3(1.0, 2.0, 1.0));
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
}
