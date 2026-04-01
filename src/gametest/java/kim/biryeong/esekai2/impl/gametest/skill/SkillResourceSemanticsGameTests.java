package kim.biryeong.esekai2.impl.gametest.skill;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import kim.biryeong.esekai2.api.player.resource.PlayerResources;
import kim.biryeong.esekai2.api.player.stat.PlayerCombatStats;
import kim.biryeong.esekai2.api.skill.definition.SkillDefinition;
import kim.biryeong.esekai2.api.skill.execution.PreparedResourceDeltaAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSkillAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSkillUse;
import kim.biryeong.esekai2.api.skill.execution.SkillExecutionContext;
import kim.biryeong.esekai2.api.skill.execution.SkillExecutionResult;
import kim.biryeong.esekai2.api.skill.execution.SkillUseContext;
import kim.biryeong.esekai2.api.skill.execution.Skills;
import kim.biryeong.esekai2.api.stat.combat.CombatStats;
import kim.biryeong.esekai2.api.stat.definition.StatDefinition;
import kim.biryeong.esekai2.api.stat.definition.StatRegistries;
import kim.biryeong.esekai2.api.stat.holder.StatHolder;
import kim.biryeong.esekai2.api.stat.holder.StatHolders;
import kim.biryeong.esekai2.impl.skill.execution.SkillExecutionExecutor;
import kim.biryeong.esekai2.impl.stat.registry.StatRegistryAccess;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.Optional;

/**
 * Verifies resource semantics seams for registry-backed player resources and related runtime
 * graph reuse.
 */
public final class SkillResourceSemanticsGameTests {
    private static final String GUARD_RESOURCE = "guard";
    private static final String UNREGISTERED_RESOURCE = "rage";
    private static final ResourceKey<StatDefinition> GUARD_STAT = ResourceKey.create(
            StatRegistries.STAT,
            Identifier.fromNamespaceAndPath("esekai2", "guard")
    );
    private static final ResourceExecutionHooks RESOURCE_HOOKS = new ResourceExecutionHooks();

    /**
     * Verifies that registered non-mana skill resource ids survive preparation without unsupported-resource warnings.
     */
    @GameTest
    public void prepareUsePreservesRegisteredSkillResourceWithoutWarning(GameTestHelper helper) {
        PreparedSkillUse preparedUse = Skills.prepareUse(namedCostSkill(GUARD_RESOURCE), useContext(helper, 20.0));

        helper.assertValueEqual(preparedUse.resource(), GUARD_RESOURCE,
                "Prepared skill use should preserve the configured resource id");
        helper.assertFalse(preparedUse.warnings().stream().anyMatch(warning -> warning.contains("unsupported resource")),
                "Preparing a registered non-mana resource cost should not emit unsupported-resource warnings");
        helper.succeed();
    }

    /**
     * Verifies that unregistered named skill resource ids survive preparation and warn clearly.
     */
    @GameTest
    public void prepareUsePreservesUnregisteredSkillResourceAndWarns(GameTestHelper helper) {
        PreparedSkillUse preparedUse = Skills.prepareUse(namedCostSkill(UNREGISTERED_RESOURCE), useContext(helper, 20.0));

        helper.assertTrue(
                preparedUse.warnings().contains("resource_cost references unsupported resource: " + UNREGISTERED_RESOURCE),
                "Preparing an unregistered resource cost should emit an explicit unsupported-resource warning"
        );
        helper.assertValueEqual(preparedUse.resource(), UNREGISTERED_RESOURCE,
                "Prepared skill use should preserve the configured unregistered resource id");
        helper.succeed();
    }

    /**
     * Verifies that registered resources spend and restore correctly while insufficient and
     * unregistered cost paths still block safely.
     */
    @GameTest
    public void genericResourceRuntimeSpendsAndRestoresRegisteredGuardWhileBlockingInvalidCostPaths(GameTestHelper helper) {
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        PlayerCombatStats.get(player).setBaseValue(GUARD_STAT, 10.0);
        PlayerResources.set(player, GUARD_RESOURCE, 9.0);
        PlayerResources.setMana(player, 20.0, 20.0);

        SkillExecutionResult spendResult = Skills.executeOnCast(SkillExecutionContext.forCast(
                Skills.prepareUse(namedCostSkill(GUARD_RESOURCE), useContext(helper, 20.0)),
                (ServerLevel) player.level(),
                player,
                Optional.empty()
        ), RESOURCE_HOOKS);
        helper.assertFalse(spendResult.warnings().stream().anyMatch(warning -> warning.startsWith(SkillExecutionExecutor.BLOCKED_WARNING_PREFIX)),
                "Registered non-mana resource costs should not block the cast");
        helper.assertValueEqual(spendResult.executedActions(), 1,
                "Registered non-mana resource costs should still allow runtime actions to execute");
        helper.assertTrue(Math.abs(PlayerResources.getAmount(player, GUARD_RESOURCE) - 5.0) <= 1.0E-6,
                "Successful casts should spend the configured registered non-mana resource");
        helper.assertTrue(Math.abs(PlayerResources.getMana(player, 20.0) - 20.0) <= 1.0E-6,
                "Registered non-mana resource costs should not spend mana as a fallback");

        PlayerResources.set(player, GUARD_RESOURCE, 3.0);
        SkillExecutionResult insufficientResult = Skills.executeOnCast(SkillExecutionContext.forCast(
                Skills.prepareUse(namedCostSkill(GUARD_RESOURCE), useContext(helper, 20.0)),
                (ServerLevel) player.level(),
                player,
                Optional.empty()
        ), RESOURCE_HOOKS);
        helper.assertTrue(
                insufficientResult.warnings().contains(SkillExecutionExecutor.BLOCKED_WARNING_PREFIX + GUARD_RESOURCE + " required=4.0, current=3.0"),
                "Registered non-mana resource costs should block with an explicit insufficient-resource warning"
        );
        helper.assertTrue(Math.abs(PlayerResources.getAmount(player, GUARD_RESOURCE) - 3.0) <= 1.0E-6,
                "Blocked registered non-mana costs should not spend the configured resource");

        SkillExecutionResult unsupportedResult = Skills.executeOnCast(SkillExecutionContext.forCast(
                Skills.prepareUse(namedCostSkill(UNREGISTERED_RESOURCE), useContext(helper, 20.0)),
                (ServerLevel) player.level(),
                player,
                Optional.empty()
        ), RESOURCE_HOOKS);
        helper.assertTrue(
                unsupportedResult.warnings().contains(SkillExecutionExecutor.BLOCKED_WARNING_PREFIX + "unsupported resource=" + UNREGISTERED_RESOURCE),
                "Unregistered resource costs should block execution with an explicit warning"
        );
        helper.assertTrue(Math.abs(PlayerResources.getMana(player, 20.0) - 20.0) <= 1.0E-6,
                "Blocked unregistered resource costs should not spend mana as a fallback");

        SkillExecutionResult restoreResult = Skills.executeOnCast(SkillExecutionContext.forCast(
                Skills.prepareUse(namedResourceDeltaSkill(GUARD_RESOURCE), useContext(helper, 0.0)),
                (ServerLevel) player.level(),
                player,
                Optional.empty()
        ), RESOURCE_HOOKS);
        helper.assertValueEqual(restoreResult.executedActions(), 1, "Registered resource_delta should execute at runtime");
        helper.assertTrue(Math.abs(PlayerResources.getAmount(player, GUARD_RESOURCE) - 6.0) <= 1.0E-6,
                "Registered resource_delta should restore the configured non-mana resource amount");
        helper.succeed();
    }

    /**
     * Verifies that unregistered resource_delta actions keep their resource id during preparation and warn clearly.
     */
    @GameTest
    public void prepareUsePreservesUnregisteredResourceDeltaAndWarns(GameTestHelper helper) {
        PreparedSkillUse preparedUse = Skills.prepareUse(namedResourceDeltaSkill(UNREGISTERED_RESOURCE), useContext(helper, 0.0));

        helper.assertTrue(
                preparedUse.warnings().contains("resource_delta references unsupported resource: " + UNREGISTERED_RESOURCE),
                "Preparing an unregistered resource_delta should emit an explicit unsupported-resource warning"
        );
        PreparedSkillAction action = preparedUse.onCastActions().getFirst();
        if (!(action instanceof PreparedResourceDeltaAction resourceDeltaAction)) {
            throw helper.assertionException("Prepared on-cast action should remain a PreparedResourceDeltaAction");
        }
        helper.assertValueEqual(resourceDeltaAction.resource(), UNREGISTERED_RESOURCE,
                "Prepared resource_delta actions should preserve the configured unregistered resource id");
        helper.succeed();
    }

    private static SkillDefinition namedCostSkill(String resource) {
        return SkillDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {
                  "identifier": "esekai2:test_named_cost",
                  "config": {
                    "resource": "%s",
                    "resource_cost": 4.0,
                    "cast_time_ticks": 0,
                    "cooldown_ticks": 0
                  },
                  "attached": {
                    "on_cast": [
                      {
                        "targets": [
                          {
                            "type": "self"
                          }
                        ],
                        "acts": [
                          {
                            "type": "sound",
                            "sound_id": "minecraft:block.note_block.bell"
                          }
                        ]
                      }
                    ],
                    "entity_components": {}
                  }
                }
                """.formatted(resource))).getOrThrow(message -> new IllegalStateException("Failed to decode named-cost skill fixture: " + message));
    }

    private static SkillDefinition namedResourceDeltaSkill(String resource) {
        return SkillDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {
                  "identifier": "esekai2:test_named_resource_delta",
                  "config": {
                    "resource_cost": 0.0,
                    "cast_time_ticks": 0,
                    "cooldown_ticks": 0
                  },
                  "attached": {
                    "on_cast": [
                      {
                        "targets": [
                          {
                            "type": "self"
                          }
                        ],
                        "acts": [
                          {
                            "type": "resource_delta",
                            "resource": "%s",
                            "amount": 3.0
                          }
                        ]
                      }
                    ],
                    "entity_components": {}
                  }
                }
                """.formatted(resource))).getOrThrow(message -> new IllegalStateException("Failed to decode named-resource-delta skill fixture: " + message));
    }

    private static SkillUseContext useContext(GameTestHelper helper, double mana) {
        StatHolder attacker = StatHolders.create(StatRegistryAccess.statRegistry(helper));
        attacker.setBaseValue(CombatStats.MANA, mana);
        attacker.setBaseValue(GUARD_STAT, 10.0);
        return new SkillUseContext(
                attacker,
                StatHolders.create(StatRegistryAccess.statRegistry(helper)),
                List.of(),
                0.0,
                0.0
        );
    }

    private static final class ResourceExecutionHooks implements kim.biryeong.esekai2.api.skill.execution.SkillExecutionHooks {
        @Override
        public boolean playSound(SkillExecutionContext context, List<Entity> targets, kim.biryeong.esekai2.api.skill.execution.PreparedSoundAction action) {
            return true;
        }

        @Override
        public Optional<kim.biryeong.esekai2.api.damage.calculation.DamageCalculationResult> applyDamage(
                SkillExecutionContext context,
                List<Entity> targets,
                kim.biryeong.esekai2.api.skill.execution.PreparedDamageAction action
        ) {
            return Optional.empty();
        }

        @Override
        public boolean applyResourceDelta(
                SkillExecutionContext context,
                List<Entity> targets,
                PreparedResourceDeltaAction action
        ) {
            boolean applied = false;
            for (Entity target : targets) {
                if (!(target instanceof ServerPlayer player)) {
                    continue;
                }
                double before = PlayerResources.getAmount(player, action.resource());
                double after = PlayerResources.add(player, action.resource(), action.amount()).currentAmount();
                applied |= Math.abs(after - before) > 1.0E-6;
            }
            return applied;
        }

        @Override
        public Optional<Entity> spawnProjectile(
                SkillExecutionContext context,
                List<Entity> targets,
                kim.biryeong.esekai2.api.skill.execution.PreparedProjectileAction action
        ) {
            return Optional.empty();
        }

        @Override
        public Optional<Entity> spawnSummonAtSight(
                SkillExecutionContext context,
                List<Entity> targets,
                kim.biryeong.esekai2.api.skill.execution.PreparedSummonAtSightAction action
        ) {
            return Optional.empty();
        }

        @Override
        public boolean placeBlock(
                SkillExecutionContext context,
                List<Entity> targets,
                kim.biryeong.esekai2.api.skill.execution.PreparedSummonBlockAction action
        ) {
            return false;
        }

        @Override
        public boolean emitSandstormParticle(
                SkillExecutionContext context,
                List<Entity> targets,
                kim.biryeong.esekai2.api.skill.execution.PreparedSandstormParticleAction action
        ) {
            return false;
        }
    }
}
