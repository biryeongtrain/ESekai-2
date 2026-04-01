package kim.biryeong.esekai2.impl.gametest.skill;

import com.mojang.serialization.JsonOps;
import kim.biryeong.esekai2.api.player.resource.PlayerResources;
import kim.biryeong.esekai2.api.player.stat.PlayerCombatStats;
import kim.biryeong.esekai2.api.skill.calculation.SkillCalculationLookup;
import kim.biryeong.esekai2.api.skill.definition.SkillRegistries;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillPredicateSubject;
import kim.biryeong.esekai2.api.skill.execution.SkillPreparedStateLookup;
import kim.biryeong.esekai2.api.skill.execution.SkillPlayerStateLookup;
import kim.biryeong.esekai2.api.skill.execution.SkillResolvedResource;
import kim.biryeong.esekai2.api.skill.execution.SkillUseContext;
import kim.biryeong.esekai2.api.skill.value.SkillCurrentResourceValueExpression;
import kim.biryeong.esekai2.api.skill.value.SkillLiteralValueExpression;
import kim.biryeong.esekai2.api.skill.value.SkillMaxResourceValueExpression;
import kim.biryeong.esekai2.api.skill.value.SkillStatValueExpression;
import kim.biryeong.esekai2.api.skill.value.SkillValueDefinition;
import kim.biryeong.esekai2.api.skill.value.SkillValueExpression;
import kim.biryeong.esekai2.api.skill.value.SkillValueLookup;
import kim.biryeong.esekai2.api.skill.value.SkillValueReferenceExpression;
import kim.biryeong.esekai2.api.stat.definition.StatDefinition;
import kim.biryeong.esekai2.api.stat.definition.StatRegistries;
import kim.biryeong.esekai2.api.stat.holder.StatHolder;
import kim.biryeong.esekai2.api.stat.holder.StatHolders;
import kim.biryeong.esekai2.api.skill.stat.SkillStats;
import kim.biryeong.esekai2.impl.skill.registry.SkillCalculationRegistryAccess;
import kim.biryeong.esekai2.impl.stat.registry.StatRegistryAccess;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.List;
import java.util.Map;

/**
 * Verifies the reusable typed skill value registry and value expression codec surface.
 */
public final class SkillValueGameTests {
    private static final Identifier RESOURCE_COST_SNAPSHOT_ID = Identifier.fromNamespaceAndPath("esekai2", "skill_resource_cost_snapshot");
    private static final Identifier RESOURCE_COST_SNAPSHOT_ALIAS_ID = Identifier.fromNamespaceAndPath("esekai2", "skill_resource_cost_snapshot_alias");
    private static final ResourceKey<StatDefinition> GUARD_STAT = ResourceKey.create(
            StatRegistries.STAT,
            Identifier.fromNamespaceAndPath("esekai2", "guard")
    );

    /**
     * Verifies that the skill value registry loads the sample value fixtures.
     */
    @GameTest
    public void skillValueRegistryLoadsSampleDefinitions(GameTestHelper helper) {
        Registry<SkillValueDefinition> registry = valueRegistry(helper);

        helper.assertTrue(registry.containsKey(RESOURCE_COST_SNAPSHOT_ID), "Stat-backed skill value fixture should be present");
        helper.assertTrue(registry.containsKey(RESOURCE_COST_SNAPSHOT_ALIAS_ID), "Reference-backed skill value fixture should be present");
        helper.succeed();
    }

    /**
     * Verifies that a stat-backed skill value survives codec round-trip.
     */
    @GameTest
    public void skillValueDefinitionCodecRoundTrips(GameTestHelper helper) {
        SkillValueDefinition definition = value(helper, RESOURCE_COST_SNAPSHOT_ID);

        var encoded = SkillValueDefinition.CODEC.encodeStart(JsonOps.INSTANCE, definition)
                .getOrThrow(message -> new IllegalStateException("Failed to encode skill value definition: " + message));
        SkillValueDefinition decoded = SkillValueDefinition.CODEC.parse(JsonOps.INSTANCE, encoded)
                .getOrThrow(message -> new IllegalStateException("Failed to decode skill value definition: " + message));

        helper.assertValueEqual(decoded, definition, "Skill value definition codec should preserve typed expressions");
        helper.succeed();
    }

    /**
     * Verifies that literal, reference, stat, and resource value expressions survive codec round-trip.
     */
    @GameTest
    public void skillValueExpressionCodecRoundTrips(GameTestHelper helper) {
        SkillValueExpression literal = new SkillLiteralValueExpression(4.0);
        SkillValueExpression reference = new SkillValueReferenceExpression(RESOURCE_COST_SNAPSHOT_ID);
        SkillValueExpression stat = new SkillStatValueExpression(Identifier.fromNamespaceAndPath("esekai2", "skill_resource_cost"));
        SkillValueExpression currentResource = new SkillCurrentResourceValueExpression("guard", SkillPredicateSubject.SELF);
        SkillValueExpression maxResource = new SkillMaxResourceValueExpression("guard", SkillPredicateSubject.TARGET);
        SkillValueExpression resourceCost = SkillValueExpression.resourceCost();
        SkillValueExpression useTimeTicks = SkillValueExpression.useTimeTicks();
        SkillValueExpression cooldownTicks = SkillValueExpression.cooldownTicks();
        SkillValueExpression maxCharges = SkillValueExpression.maxCharges();
        SkillValueExpression timesToCast = SkillValueExpression.timesToCast();
        SkillValueExpression cooldownRemaining = SkillValueExpression.cooldownRemaining();
        SkillValueExpression chargesAvailable = SkillValueExpression.chargesAvailable();
        SkillValueExpression burstRemaining = SkillValueExpression.burstRemaining();

        helper.assertValueEqual(
                SkillValueExpression.CODEC.parse(JsonOps.INSTANCE,
                        SkillValueExpression.CODEC.encodeStart(JsonOps.INSTANCE, literal)
                                .getOrThrow(message -> new IllegalStateException("Failed to encode literal value expression: " + message)))
                        .getOrThrow(message -> new IllegalStateException("Failed to decode literal value expression: " + message)),
                literal,
                "Literal value expression should round-trip through the public codec"
        );
        helper.assertValueEqual(
                SkillValueExpression.CODEC.parse(JsonOps.INSTANCE,
                        SkillValueExpression.CODEC.encodeStart(JsonOps.INSTANCE, reference)
                                .getOrThrow(message -> new IllegalStateException("Failed to encode reference value expression: " + message)))
                        .getOrThrow(message -> new IllegalStateException("Failed to decode reference value expression: " + message)),
                reference,
                "Reference value expression should round-trip through the public codec"
        );
        helper.assertValueEqual(
                SkillValueExpression.CODEC.parse(JsonOps.INSTANCE,
                        SkillValueExpression.CODEC.encodeStart(JsonOps.INSTANCE, stat)
                                .getOrThrow(message -> new IllegalStateException("Failed to encode stat value expression: " + message)))
                        .getOrThrow(message -> new IllegalStateException("Failed to decode stat value expression: " + message)),
                stat,
                "Stat value expression should round-trip through the public codec"
        );
        helper.assertValueEqual(
                SkillValueExpression.CODEC.parse(JsonOps.INSTANCE,
                        SkillValueExpression.CODEC.encodeStart(JsonOps.INSTANCE, currentResource)
                                .getOrThrow(message -> new IllegalStateException("Failed to encode current-resource value expression: " + message)))
                        .getOrThrow(message -> new IllegalStateException("Failed to decode current-resource value expression: " + message)),
                currentResource,
                "Current-resource value expression should round-trip through the public codec"
        );
        helper.assertValueEqual(
                SkillValueExpression.CODEC.parse(JsonOps.INSTANCE,
                        SkillValueExpression.CODEC.encodeStart(JsonOps.INSTANCE, maxResource)
                                .getOrThrow(message -> new IllegalStateException("Failed to encode max-resource value expression: " + message)))
                        .getOrThrow(message -> new IllegalStateException("Failed to decode max-resource value expression: " + message)),
                maxResource,
                "Max-resource value expression should round-trip through the public codec"
        );
        helper.assertValueEqual(
                SkillValueExpression.CODEC.parse(JsonOps.INSTANCE,
                        SkillValueExpression.CODEC.encodeStart(JsonOps.INSTANCE, resourceCost)
                                .getOrThrow(message -> new IllegalStateException("Failed to encode resource-cost value expression: " + message)))
                        .getOrThrow(message -> new IllegalStateException("Failed to decode resource-cost value expression: " + message)),
                resourceCost,
                "Prepared resource-cost value expression should round-trip through the public codec"
        );
        helper.assertValueEqual(
                SkillValueExpression.CODEC.parse(JsonOps.INSTANCE,
                        SkillValueExpression.CODEC.encodeStart(JsonOps.INSTANCE, useTimeTicks)
                                .getOrThrow(message -> new IllegalStateException("Failed to encode use-time value expression: " + message)))
                        .getOrThrow(message -> new IllegalStateException("Failed to decode use-time value expression: " + message)),
                useTimeTicks,
                "Prepared use-time value expression should round-trip through the public codec"
        );
        helper.assertValueEqual(
                SkillValueExpression.CODEC.parse(JsonOps.INSTANCE,
                        SkillValueExpression.CODEC.encodeStart(JsonOps.INSTANCE, cooldownTicks)
                                .getOrThrow(message -> new IllegalStateException("Failed to encode cooldown-ticks value expression: " + message)))
                        .getOrThrow(message -> new IllegalStateException("Failed to decode cooldown-ticks value expression: " + message)),
                cooldownTicks,
                "Prepared cooldown-ticks value expression should round-trip through the public codec"
        );
        helper.assertValueEqual(
                SkillValueExpression.CODEC.parse(JsonOps.INSTANCE,
                        SkillValueExpression.CODEC.encodeStart(JsonOps.INSTANCE, maxCharges)
                                .getOrThrow(message -> new IllegalStateException("Failed to encode max-charges value expression: " + message)))
                        .getOrThrow(message -> new IllegalStateException("Failed to decode max-charges value expression: " + message)),
                maxCharges,
                "Prepared max-charges value expression should round-trip through the public codec"
        );
        helper.assertValueEqual(
                SkillValueExpression.CODEC.parse(JsonOps.INSTANCE,
                        SkillValueExpression.CODEC.encodeStart(JsonOps.INSTANCE, timesToCast)
                                .getOrThrow(message -> new IllegalStateException("Failed to encode times-to-cast value expression: " + message)))
                        .getOrThrow(message -> new IllegalStateException("Failed to decode times-to-cast value expression: " + message)),
                timesToCast,
                "Prepared times-to-cast value expression should round-trip through the public codec"
        );
        helper.assertValueEqual(
                SkillValueExpression.CODEC.parse(JsonOps.INSTANCE,
                        SkillValueExpression.CODEC.encodeStart(JsonOps.INSTANCE, cooldownRemaining)
                                .getOrThrow(message -> new IllegalStateException("Failed to encode cooldown-remaining value expression: " + message)))
                        .getOrThrow(message -> new IllegalStateException("Failed to decode cooldown-remaining value expression: " + message)),
                cooldownRemaining,
                "Cooldown-remaining value expression should round-trip through the public codec"
        );
        helper.assertValueEqual(
                SkillValueExpression.CODEC.parse(JsonOps.INSTANCE,
                        SkillValueExpression.CODEC.encodeStart(JsonOps.INSTANCE, chargesAvailable)
                                .getOrThrow(message -> new IllegalStateException("Failed to encode charges-available value expression: " + message)))
                        .getOrThrow(message -> new IllegalStateException("Failed to decode charges-available value expression: " + message)),
                chargesAvailable,
                "Charges-available value expression should round-trip through the public codec"
        );
        helper.assertValueEqual(
                SkillValueExpression.CODEC.parse(JsonOps.INSTANCE,
                        SkillValueExpression.CODEC.encodeStart(JsonOps.INSTANCE, burstRemaining)
                                .getOrThrow(message -> new IllegalStateException("Failed to encode burst-remaining value expression: " + message)))
                        .getOrThrow(message -> new IllegalStateException("Failed to decode burst-remaining value expression: " + message)),
                burstRemaining,
                "Burst-remaining value expression should round-trip through the public codec"
        );
        helper.succeed();
    }

    /**
     * Verifies that stat-backed skill values and reference-backed aliases resolve against runtime lookup state.
     */
    @GameTest
    public void skillValueResolutionUsesStatAndReferenceLookups(GameTestHelper helper) {
        StatHolder attacker = StatHolders.create(StatRegistryAccess.statRegistry(helper));
        attacker.setBaseValue(SkillStats.SKILL_RESOURCE_COST, 14.0);

        SkillValueDefinition statBackedValue = value(helper, RESOURCE_COST_SNAPSHOT_ID);
        SkillValueDefinition aliasValue = value(helper, RESOURCE_COST_SNAPSHOT_ALIAS_ID);

        SkillUseContext context = new SkillUseContext(
                attacker,
                StatHolders.create(StatRegistryAccess.statRegistry(helper)),
                List.of(),
                0.0,
                0.0,
                SkillCalculationLookup.fromRegistry(SkillCalculationRegistryAccess.skillCalculationRegistry(helper)),
                SkillValueLookup.fromRegistry(valueRegistry(helper))
        );

        helper.assertValueEqual(statBackedValue.resolve(context), 14.0, "Stat-backed skill value should read from the attacker stat holder");
        helper.assertValueEqual(aliasValue.resolve(context), 14.0, "Reference-backed skill value should resolve through the value registry");
        helper.succeed();
    }

    /**
     * Verifies that current-resource and max-resource expressions resolve through player-backed resource lookups.
     */
    @GameTest
    public void resourceValueResolutionUsesPlayerResourceLookup(GameTestHelper helper) {
        StatHolder attacker = StatHolders.create(StatRegistryAccess.statRegistry(helper));
        StatHolder defender = StatHolders.create(StatRegistryAccess.statRegistry(helper));
        attacker.setBaseValue(GUARD_STAT, 10.0);
        defender.setBaseValue(GUARD_STAT, 12.0);

        SkillUseContext context = new SkillUseContext(
                attacker,
                defender,
                List.of(),
                0.0,
                0.0,
                SkillCalculationLookup.empty(),
                SkillValueLookup.empty()
        ).withResourceLookup((resource, subject) -> {
            if (!"guard".equals(resource)) {
                return java.util.Optional.empty();
            }
            return switch (subject) {
                case SELF -> java.util.Optional.of(new SkillResolvedResource(6.0, 10.0));
                case TARGET, PRIMARY_TARGET -> java.util.Optional.of(new SkillResolvedResource(4.0, 12.0));
            };
        });

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
     * Verifies that runtime-state value expressions can resolve from a raw SkillUseContext player-state lookup.
     */
    @GameTest
    public void runtimeStateValueExpressionsResolveFromRawRuntimeStateLookup(GameTestHelper helper) {
        SkillUseContext context = new SkillUseContext(
                StatHolders.create(StatRegistryAccess.statRegistry(helper)),
                StatHolders.create(StatRegistryAccess.statRegistry(helper)),
                List.of(),
                0.0,
                0.0,
                SkillCalculationLookup.empty(),
                SkillValueLookup.empty()
        ).withPlayerStateLookup(new SkillPlayerStateLookup() {
            @Override
            public long cooldownRemainingTicks(Identifier skillId) {
                return 7L;
            }

            @Override
            public int availableCharges(Identifier skillId, int maxCharges) {
                return 2;
            }

            @Override
            public int burstRemainingCasts(Identifier skillId) {
                return 1;
            }
        }).withActiveSkill(Identifier.fromNamespaceAndPath("esekai2", "raw_runtime_state"), 3);

        helper.assertValueEqual(SkillValueExpression.cooldownRemaining().resolve(context), 7.0,
                "cooldown_remaining should read the raw player-state lookup from SkillUseContext");
        helper.assertValueEqual(SkillValueExpression.chargesAvailable().resolve(context), 2.0,
                "charges_available should read the raw player-state lookup from SkillUseContext");
        helper.assertValueEqual(SkillValueExpression.burstRemaining().resolve(context), 1.0,
                "burst_remaining should read the raw player-state lookup from SkillUseContext");
        helper.succeed();
    }

    /**
     * Verifies that prepared-state value expressions can resolve from a raw SkillUseContext prepared-state lookup.
     */
    @GameTest
    public void preparedStateValueExpressionsResolveFromRawPreparedStateLookup(GameTestHelper helper) {
        SkillUseContext context = new SkillUseContext(
                StatHolders.create(StatRegistryAccess.statRegistry(helper)),
                StatHolders.create(StatRegistryAccess.statRegistry(helper)),
                List.of(),
                0.0,
                0.0,
                SkillCalculationLookup.empty(),
                SkillValueLookup.empty()
        ).withPreparedStateLookup(SkillPreparedStateLookup.of(6.0, 9, 14, 2, 3));

        helper.assertValueEqual(SkillValueExpression.resourceCost().resolve(context), 6.0,
                "resource_cost should read the raw prepared-state lookup from SkillUseContext");
        helper.assertValueEqual(SkillValueExpression.useTimeTicks().resolve(context), 9.0,
                "use_time_ticks should read the raw prepared-state lookup from SkillUseContext");
        helper.assertValueEqual(SkillValueExpression.cooldownTicks().resolve(context), 14.0,
                "cooldown_ticks should read the raw prepared-state lookup from SkillUseContext");
        helper.assertValueEqual(SkillValueExpression.maxCharges().resolve(context), 2.0,
                "max_charges should read the raw prepared-state lookup from SkillUseContext");
        helper.assertValueEqual(SkillValueExpression.timesToCast().resolve(context), 3.0,
                "times_to_cast should read the raw prepared-state lookup from SkillUseContext");
        helper.succeed();
    }

    /**
     * Verifies that raw SkillUseContext constructors leave resource and runtime-state lookups at safe defaults.
     */
    @GameTest
    public void rawSkillUseContextDefaultsResourceAndRuntimeStateValuesSafely(GameTestHelper helper) {
        SkillUseContext context = new SkillUseContext(
                StatHolders.create(StatRegistryAccess.statRegistry(helper)),
                StatHolders.create(StatRegistryAccess.statRegistry(helper)),
                List.of(),
                0.0,
                0.0,
                SkillCalculationLookup.empty(),
                SkillValueLookup.empty()
        );

        helper.assertValueEqual(SkillValueExpression.currentResource("guard", SkillPredicateSubject.SELF).resolve(context), 0.0,
                "Raw SkillUseContext constructors should default current-resource lookup to an absent-safe zero result");
        helper.assertValueEqual(SkillValueExpression.resourceCost().resolve(context), 0.0,
                "Raw SkillUseContext constructors should default resource_cost to zero without prepared-state bindings");
        helper.assertValueEqual(SkillValueExpression.useTimeTicks().resolve(context), 0.0,
                "Raw SkillUseContext constructors should default use_time_ticks to zero without prepared-state bindings");
        helper.assertValueEqual(SkillValueExpression.cooldownTicks().resolve(context), 0.0,
                "Raw SkillUseContext constructors should default cooldown_ticks to zero without prepared-state bindings");
        helper.assertValueEqual(SkillValueExpression.maxCharges().resolve(context), 0.0,
                "Raw SkillUseContext constructors should default max_charges to zero without prepared-state bindings");
        helper.assertValueEqual(SkillValueExpression.timesToCast().resolve(context), 0.0,
                "Raw SkillUseContext constructors should default times_to_cast to zero without prepared-state bindings");
        helper.assertValueEqual(SkillValueExpression.cooldownRemaining().resolve(context), 0.0,
                "Raw SkillUseContext constructors should default cooldown_remaining to zero without player-state bindings");
        helper.assertValueEqual(SkillValueExpression.chargesAvailable().resolve(context), 0.0,
                "Raw SkillUseContext constructors should default charges_available to zero without player-state bindings");
        helper.assertValueEqual(SkillValueExpression.burstRemaining().resolve(context), 0.0,
                "Raw SkillUseContext constructors should default burst_remaining to zero without player-state bindings");
        helper.succeed();
    }

    private static Registry<SkillValueDefinition> valueRegistry(GameTestHelper helper) {
        return helper.getLevel().getServer().registryAccess().lookupOrThrow(SkillRegistries.SKILL_VALUE);
    }

    private static SkillValueDefinition value(GameTestHelper helper, Identifier id) {
        return valueRegistry(helper).getOptional(id)
                .orElseThrow(() -> helper.assertionException("Skill value should decode successfully: " + id));
    }
}
