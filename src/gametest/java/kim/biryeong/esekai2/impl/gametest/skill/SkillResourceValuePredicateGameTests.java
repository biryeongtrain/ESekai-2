package kim.biryeong.esekai2.impl.gametest.skill;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import kim.biryeong.esekai2.api.player.resource.PlayerResources;
import kim.biryeong.esekai2.api.player.stat.PlayerCombatStats;
import kim.biryeong.esekai2.api.skill.definition.SkillDefinition;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillPredicate;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillPredicateSubject;
import kim.biryeong.esekai2.api.skill.execution.PreparedSkillUse;
import kim.biryeong.esekai2.api.skill.execution.SkillExecutionContext;
import kim.biryeong.esekai2.api.skill.execution.SkillResolvedResource;
import kim.biryeong.esekai2.api.skill.execution.SkillUseContext;
import kim.biryeong.esekai2.api.skill.execution.SkillUseContexts;
import kim.biryeong.esekai2.api.skill.execution.Skills;
import kim.biryeong.esekai2.api.skill.value.SkillValueExpression;
import kim.biryeong.esekai2.api.stat.combat.CombatStats;
import kim.biryeong.esekai2.api.stat.definition.StatDefinition;
import kim.biryeong.esekai2.api.stat.definition.StatRegistries;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Verifies resource-backed skill value expressions and predicates.
 */
public final class SkillResourceValuePredicateGameTests {
    private static final String GUARD_RESOURCE = "guard";
    private static final String FOCUS_RESOURCE = "examplemod:focus";
    private static final ResourceKey<StatDefinition> FOCUS_STAT = ResourceKey.create(
            StatRegistries.STAT,
            Identifier.fromNamespaceAndPath("examplemod", "focus")
    );

    /**
     * Verifies that helper-built player contexts expose both current and maximum registered resource values.
     */
    @GameTest
    public void resourceValueExpressionsResolveForHelperBuiltPlayerContext(GameTestHelper helper) {
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        PlayerCombatStats.get(player).setBaseValue(CombatStats.GUARD, 10.0);
        PlayerResources.set(player, GUARD_RESOURCE, 4.0);

        SkillUseContext context = SkillUseContexts.forPlayer(player, 0.0, 0.0);

        helper.assertTrue(Math.abs(SkillValueExpression.currentResource(GUARD_RESOURCE, SkillPredicateSubject.SELF).resolve(context) - 4.0) <= 1.0E-6,
                "resource_current should resolve the player's current registered resource amount");
        helper.assertTrue(Math.abs(SkillValueExpression.maxResource(GUARD_RESOURCE, SkillPredicateSubject.SELF).resolve(context) - 10.0) <= 1.0E-6,
                "resource_max should resolve the player's registered max resource amount");
        helper.succeed();
    }

    /**
     * Verifies that has_resource passes on the threshold boundary for a player self subject.
     */
    @GameTest
    public void hasResourcePredicatePassesOnThresholdBoundary(GameTestHelper helper) {
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        PlayerCombatStats.get(player).setBaseValue(CombatStats.GUARD, 10.0);
        PlayerResources.set(player, GUARD_RESOURCE, 5.0);

        SkillPredicate predicate = SkillPredicate.hasResource(
                GUARD_RESOURCE,
                SkillValueExpression.constant(5.0),
                SkillPredicateSubject.SELF
        );
        PreparedSkillUse preparedUse = preparedUse(helper, SkillUseContexts.forPlayer(player, 0.0, 0.0));

        helper.assertTrue(predicate.matches(SkillExecutionContext.forCast(
                        preparedUse,
                        (ServerLevel) player.level(),
                        player,
                        Optional.empty()
                )),
                "has_resource should pass when current resource equals the requested amount");
        helper.succeed();
    }

    /**
     * Verifies that has_resource fails safely when the selected target subject is missing.
     */
    @GameTest
    public void hasResourcePredicateFailsWhenTargetSubjectIsMissing(GameTestHelper helper) {
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        PlayerCombatStats.get(player).setBaseValue(CombatStats.GUARD, 10.0);
        PlayerResources.set(player, GUARD_RESOURCE, 8.0);

        SkillPredicate predicate = SkillPredicate.hasResource(
                GUARD_RESOURCE,
                SkillValueExpression.constant(1.0),
                SkillPredicateSubject.TARGET
        );
        PreparedSkillUse preparedUse = preparedUse(helper, SkillUseContexts.forPlayer(player, Optional.empty(), 0.0, 0.0));

        helper.assertFalse(predicate.matches(SkillExecutionContext.forCast(
                        preparedUse,
                        (ServerLevel) player.level(),
                        player,
                        Optional.empty()
                )),
                "has_resource should fail safely when the selected target subject is absent");
        helper.succeed();
    }

    /**
     * Verifies that has_resource uses the raw SkillUseContext resource lookup before live player fallback.
     */
    @GameTest
    public void hasResourcePredicateUsesRawSkillResourceLookupBeforeLivePlayerFallback(GameTestHelper helper) {
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        SkillUseContext context = new SkillUseContext(
                PlayerCombatStats.get(player),
                PlayerCombatStats.get(player),
                java.util.List.of(),
                0.0,
                0.0
        ).withResourceLookup((resource, subject) -> {
            if (!GUARD_RESOURCE.equals(resource) || subject != SkillPredicateSubject.SELF) {
                return Optional.empty();
            }
            return Optional.of(new SkillResolvedResource(5.0, 10.0));
        });

        SkillPredicate predicate = SkillPredicate.hasResource(
                GUARD_RESOURCE,
                SkillValueExpression.constant(5.0),
                SkillPredicateSubject.SELF
        );
        PreparedSkillUse preparedUse = preparedUse(helper, context);

        helper.assertTrue(predicate.matches(SkillExecutionContext.forCast(
                        preparedUse,
                        (ServerLevel) player.level(),
                        player,
                        Optional.empty()
                )),
                "has_resource should honor a raw context resource lookup before checking live player state");
        helper.succeed();
    }

    /**
     * Verifies that helper-built no-defender contexts resolve primary-target resource values as absent.
     */
    @GameTest
    public void primaryTargetResourceExpressionsResolveAbsentWhenHelperContextHasNoDefender(GameTestHelper helper) {
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        PlayerCombatStats.get(player).setBaseValue(CombatStats.GUARD, 10.0);
        PlayerResources.set(player, GUARD_RESOURCE, 8.0);

        SkillUseContext context = SkillUseContexts.forPlayer(player, Optional.empty(), 0.0, 0.0);

        helper.assertValueEqual(SkillValueExpression.currentResource(GUARD_RESOURCE, SkillPredicateSubject.PRIMARY_TARGET).resolve(context), 0.0,
                "resource_current primary_target should resolve absent when helper context has no defender");
        helper.assertValueEqual(SkillValueExpression.maxResource(GUARD_RESOURCE, SkillPredicateSubject.PRIMARY_TARGET).resolve(context), 0.0,
                "resource_max primary_target should resolve absent when helper context has no defender");
        helper.succeed();
    }

    /**
     * Verifies that has_resource fails for primary-target subjects when helper-built contexts have no defender.
     */
    @GameTest
    public void hasResourcePredicateFailsForPrimaryTargetWhenHelperContextHasNoDefender(GameTestHelper helper) {
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        PlayerCombatStats.get(player).setBaseValue(CombatStats.GUARD, 10.0);
        PlayerResources.set(player, GUARD_RESOURCE, 8.0);

        SkillPredicate predicate = SkillPredicate.hasResource(
                GUARD_RESOURCE,
                SkillValueExpression.constant(1.0),
                SkillPredicateSubject.PRIMARY_TARGET
        );
        PreparedSkillUse preparedUse = preparedUse(helper, SkillUseContexts.forPlayer(player, Optional.empty(), 0.0, 0.0));

        helper.assertFalse(predicate.matches(SkillExecutionContext.forCast(
                        preparedUse,
                        (ServerLevel) player.level(),
                        player,
                        Optional.empty()
                )),
                "has_resource primary_target should fail when helper context has no defender");
        helper.succeed();
    }

    /**
     * Verifies that namespaced registered resources use one canonical id across values and predicates.
     */
    @GameTest
    public void namespacedResourceValueAndPredicateResolutionUseCanonicalId(GameTestHelper helper) {
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        PlayerCombatStats.get(player).setBaseValue(FOCUS_STAT, 10.0);
        PlayerResources.set(player, FOCUS_RESOURCE, 6.0);

        SkillUseContext context = SkillUseContexts.forPlayer(player, 0.0, 0.0);
        SkillPredicate predicate = SkillPredicate.hasResource(
                FOCUS_RESOURCE,
                SkillValueExpression.constant(6.0),
                SkillPredicateSubject.SELF
        );
        PreparedSkillUse preparedUse = preparedUse(helper, context);

        helper.assertValueEqual(SkillValueExpression.currentResource(FOCUS_RESOURCE, SkillPredicateSubject.SELF).resolve(context), 6.0,
                "resource_current should resolve the canonical namespaced resource id");
        helper.assertValueEqual(SkillValueExpression.maxResource(FOCUS_RESOURCE, SkillPredicateSubject.SELF).resolve(context), 10.0,
                "resource_max should resolve the canonical namespaced resource id");
        helper.assertTrue(predicate.matches(SkillExecutionContext.forCast(
                        preparedUse,
                        (ServerLevel) player.level(),
                        player,
                        Optional.empty()
                )),
                "has_resource should evaluate the canonical namespaced resource id");
        helper.succeed();
    }

    private static PreparedSkillUse preparedUse(GameTestHelper helper, SkillUseContext context) {
        return Skills.prepareUse(emptySkill(), context);
    }

    private static SkillDefinition emptySkill() {
        return SkillDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {
                  "identifier": "esekai2:test_resource_value_predicate_empty",
                  "config": {
                    "resource_cost": 0.0,
                    "cast_time_ticks": 0,
                    "cooldown_ticks": 0
                  },
                  "attached": {
                    "on_cast": [],
                    "entity_components": {}
                  }
                }
                """)).getOrThrow(message -> new IllegalStateException("Failed to decode empty test skill: " + message));
    }
}
