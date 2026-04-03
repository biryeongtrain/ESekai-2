package kim.biryeong.esekai2.impl.gametest.skill;

import kim.biryeong.esekai2.api.ailment.AilmentPayload;
import kim.biryeong.esekai2.api.ailment.AilmentType;
import kim.biryeong.esekai2.api.ailment.Ailments;
import kim.biryeong.esekai2.api.damage.breakdown.DamageBreakdown;
import kim.biryeong.esekai2.api.damage.calculation.DamageCalculations;
import kim.biryeong.esekai2.api.damage.calculation.DamageOverTimeCalculation;
import kim.biryeong.esekai2.api.item.socket.SocketLinkGroup;
import kim.biryeong.esekai2.api.item.socket.SocketSlotType;
import kim.biryeong.esekai2.api.item.socket.SocketedEquipmentSlot;
import kim.biryeong.esekai2.api.item.socket.SocketedSkillItemState;
import kim.biryeong.esekai2.api.item.socket.SocketedSkillRef;
import kim.biryeong.esekai2.api.item.socket.SocketedSkills;
import kim.biryeong.esekai2.api.player.resource.PlayerResourceIds;
import kim.biryeong.esekai2.api.player.resource.PlayerResources;
import kim.biryeong.esekai2.api.player.skill.PlayerActiveSkills;
import kim.biryeong.esekai2.api.player.skill.PlayerSkillBursts;
import kim.biryeong.esekai2.api.player.skill.PlayerSkillCharges;
import kim.biryeong.esekai2.api.player.skill.PlayerSkillCooldowns;
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
import kim.biryeong.esekai2.api.skill.definition.graph.SkillPredicateSubject;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillRule;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillTargetSelector;
import kim.biryeong.esekai2.api.skill.execution.PreparedApplyDotAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSkillUse;
import kim.biryeong.esekai2.api.skill.execution.SelectedSkillCastResult;
import kim.biryeong.esekai2.api.skill.execution.SkillExecutionContext;
import kim.biryeong.esekai2.api.skill.execution.SkillPlayerStateLookup;
import kim.biryeong.esekai2.api.skill.execution.SkillResolvedResource;
import kim.biryeong.esekai2.api.skill.execution.SkillUseContext;
import kim.biryeong.esekai2.api.skill.execution.SkillUseContexts;
import kim.biryeong.esekai2.api.skill.execution.Skills;
import kim.biryeong.esekai2.api.skill.value.SkillValueExpression;
import kim.biryeong.esekai2.api.skill.value.SkillValueLookup;
import kim.biryeong.esekai2.api.stat.combat.CombatStats;
import kim.biryeong.esekai2.api.stat.holder.StatHolder;
import kim.biryeong.esekai2.api.stat.holder.StatHolders;
import kim.biryeong.esekai2.api.stat.modifier.ConditionalStatModifier;
import kim.biryeong.esekai2.impl.ailment.AilmentRuntime;
import kim.biryeong.esekai2.impl.skill.registry.SkillCalculationRegistryAccess;
import kim.biryeong.esekai2.impl.stat.registry.StatRegistryAccess;
import kim.biryeong.esekai2.impl.stat.runtime.LivingEntityCombatStatResolver;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Verifies the recommended player-facing skill use context helper and wider player stat fallback reuse.
 */
public final class SkillPlayerContextGameTests {
    private static final Identifier BASIC_STRIKE_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "basic_strike");
    private static final Identifier VENOM_STRIKE_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "venom_strike");

    /**
     * Verifies that the player-facing helper reuses live PlayerCombatStats holders for both attacker and player defender.
     */
    @GameTest
    public void playerContextHelperUsesLivePlayerCombatStatsForAttackerAndDefender(GameTestHelper helper) {
        ServerPlayer attacker = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        ServerPlayer defender = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        PlayerCombatStats.get(attacker).setBaseValue(CombatStats.ACCURACY, 77.0);
        PlayerCombatStats.get(defender).setBaseValue(CombatStats.FIRE_RESISTANCE, 42.0);

        SkillUseContext context = SkillUseContexts.forPlayer(attacker, Optional.of(defender), List.of(), 0.10, 0.20);

        helper.assertTrue(context.attackerStats() == PlayerCombatStats.get(attacker),
                "Player attacker helper should reuse the shared runtime combat stat holder");
        helper.assertTrue(context.defenderStats() == PlayerCombatStats.get(defender),
                "Player defender helper should reuse the shared runtime combat stat holder");
        helper.assertValueEqual(context.attackerStats().resolvedValue(CombatStats.ACCURACY), 77.0,
                "Player attacker helper should expose live attacker stat mutations");
        helper.assertValueEqual(context.defenderStats().resolvedValue(CombatStats.FIRE_RESISTANCE), 42.0,
                "Player defender helper should expose live defender stat mutations");
        helper.succeed();
    }

    /**
     * Verifies that raw SkillUseContext construction does not implicitly expose live player resources or runtime skill state.
     */
    @GameTest
    public void rawSkillUseContextLeavesLivePlayerLookupsAbsentUntilHelperOrBindingProvidesThem(GameTestHelper helper) {
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        PlayerCombatStats.get(player).setBaseValue(CombatStats.MANA, 10.0);
        PlayerResources.setMana(player, 6.0, 10.0);
        long gameTime = helper.getLevel().getGameTime();
        PlayerSkillCooldowns.start(player, BASIC_STRIKE_SKILL_ID, gameTime + 20L);
        PlayerSkillCharges.setAvailableCharges(player, BASIC_STRIKE_SKILL_ID, 1, 3, gameTime);
        PlayerSkillBursts.recordSuccessfulCast(player, BASIC_STRIKE_SKILL_ID, 3, gameTime, gameTime + 20L);

        SkillUseContext rawContext = new SkillUseContext(
                PlayerCombatStats.get(player),
                newHolder(helper),
                List.<ConditionalStatModifier>of(),
                0.0,
                0.25,
                SkillCalculationLookup.fromRegistry(skillCalculationRegistry(helper))
        ).withActiveSkill(BASIC_STRIKE_SKILL_ID, 3);

        SkillUseContext helperContext = SkillUseContexts.forPlayer(player, 0.0, 0.25)
                .withActiveSkill(BASIC_STRIKE_SKILL_ID, 3);

        helper.assertValueEqual(SkillValueExpression.currentResource(PlayerResourceIds.MANA, SkillPredicateSubject.SELF).resolve(rawContext), 0.0,
                "Raw SkillUseContext should not implicitly expose live player resources without a resource lookup");
        helper.assertValueEqual(SkillValueExpression.cooldownRemaining().resolve(rawContext), 0.0,
                "Raw SkillUseContext should not implicitly expose live cooldown state without a player-state lookup");
        helper.assertValueEqual(SkillValueExpression.chargesAvailable().resolve(rawContext), 0.0,
                "Raw SkillUseContext should not implicitly expose live charge state without a player-state lookup");
        helper.assertValueEqual(SkillValueExpression.burstRemaining().resolve(rawContext), 0.0,
                "Raw SkillUseContext should not implicitly expose live burst state without a player-state lookup");
        helper.assertValueEqual(SkillValueExpression.currentResource(PlayerResourceIds.MANA, SkillPredicateSubject.SELF).resolve(helperContext), 6.0,
                "Helper-built player contexts should expose live registered player resources");
        helper.assertTrue(SkillValueExpression.cooldownRemaining().resolve(helperContext) > 0.0,
                "Helper-built player contexts should expose live cooldown state once the active skill is bound");
        helper.assertValueEqual(SkillValueExpression.chargesAvailable().resolve(helperContext), 1.0,
                "Helper-built player contexts should expose live available charge state once the active skill is bound");
        helper.assertValueEqual(SkillValueExpression.burstRemaining().resolve(helperContext), 2.0,
                "Helper-built player contexts should expose live burst follow-up state once the active skill is bound");
        helper.succeed();
    }

    /**
     * Verifies that raw SkillUseContext callers can opt into live player resources and runtime state by binding lookups manually.
     */
    @GameTest
    public void rawSkillUseContextCanExposeLivePlayerLookupsWhenCallerBindsThemManually(GameTestHelper helper) {
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        PlayerCombatStats.get(player).setBaseValue(CombatStats.MANA, 10.0);
        PlayerResources.setMana(player, 6.0, 10.0);
        long gameTime = helper.getLevel().getGameTime();
        PlayerSkillCooldowns.start(player, BASIC_STRIKE_SKILL_ID, gameTime + 20L);
        PlayerSkillCharges.setAvailableCharges(player, BASIC_STRIKE_SKILL_ID, 1, 3, gameTime);
        PlayerSkillBursts.recordSuccessfulCast(player, BASIC_STRIKE_SKILL_ID, 3, gameTime, gameTime + 20L);

        SkillUseContext boundContext = new SkillUseContext(
                PlayerCombatStats.get(player),
                newHolder(helper),
                List.<ConditionalStatModifier>of(),
                0.0,
                0.25,
                SkillCalculationLookup.fromRegistry(skillCalculationRegistry(helper))
        ).withResourceLookup((resource, subject) -> {
            if (!PlayerResourceIds.MANA.equals(resource) || subject != SkillPredicateSubject.SELF) {
                return Optional.empty();
            }
            return Optional.of(new SkillResolvedResource(
                    PlayerResources.getAmount(player, PlayerResourceIds.MANA),
                    PlayerResources.maxAmount(player, PlayerResourceIds.MANA)
            ));
        }).withPlayerStateLookup(new SkillPlayerStateLookup() {
            @Override
            public long cooldownRemainingTicks(Identifier skillId) {
                return PlayerSkillCooldowns.remainingTicks(player, skillId, gameTime);
            }

            @Override
            public int availableCharges(Identifier skillId, int maxCharges) {
                return PlayerSkillCharges.availableCharges(player, skillId, maxCharges, gameTime);
            }

            @Override
            public int burstRemainingCasts(Identifier skillId) {
                return PlayerSkillBursts.remainingCasts(player, skillId, gameTime);
            }
        }).withActiveSkill(BASIC_STRIKE_SKILL_ID, 3);

        helper.assertValueEqual(SkillValueExpression.currentResource(PlayerResourceIds.MANA, SkillPredicateSubject.SELF).resolve(boundContext), 6.0,
                "Raw SkillUseContext callers should be able to opt into live player resources by binding a resource lookup manually");
        helper.assertTrue(SkillValueExpression.cooldownRemaining().resolve(boundContext) > 0.0,
                "Raw SkillUseContext callers should be able to opt into cooldown state by binding a player-state lookup manually");
        helper.assertValueEqual(SkillValueExpression.chargesAvailable().resolve(boundContext), 1.0,
                "Raw SkillUseContext callers should be able to opt into charge state by binding a player-state lookup manually");
        helper.assertValueEqual(SkillValueExpression.burstRemaining().resolve(boundContext), 2.0,
                "Raw SkillUseContext callers should be able to opt into burst state by binding a player-state lookup manually");
        helper.succeed();
    }

    /**
     * Verifies that helper-built player contexts work unchanged on the selected active skill cast path.
     */
    @GameTest
    public void selectedCastSucceedsWithPlayerContextHelper(GameTestHelper helper) {
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        player.setItemSlot(SocketedEquipmentSlot.MAIN_HAND.equipmentSlot(), basicStrikeStack());
        PlayerActiveSkills.select(player, new SelectedActiveSkillRef(SocketedEquipmentSlot.MAIN_HAND, BASIC_STRIKE_SKILL_ID));
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(3.0, 2.0, 3.0));
        player.snapTo(zombie.getX(), zombie.getY(), zombie.getZ() - 2.0, 0.0F, 0.0F);

        SelectedSkillCastResult result = Skills.castSelectedSkill(
                player,
                SkillUseContexts.forPlayer(
                        player,
                        Optional.of(zombie),
                        List.of(),
                        0.0,
                        0.25,
                        SkillCalculationLookup.fromRegistry(skillCalculationRegistry(helper)),
                        SkillValueLookup.empty()
                ),
                Optional.of(zombie)
        );

        helper.assertTrue(result.success(), "Selected active skill cast should still succeed when using the player context helper");
        helper.assertValueEqual(3, result.executionResult().orElseThrow().executedActions(),
                "Selected basic strike should execute sound, particle, and damage on its normal server-side cast path");
        helper.assertTrue(zombie.getHealth() < zombie.getMaxHealth(),
                "Selected active skill cast should still damage the monster target");
        helper.succeed();
    }

    /**
     * Verifies that generic DoT fallback resolution can reuse live player combat stats instead of the empty prepared defender holder.
     */
    @GameTest
    public void genericDotFallbackResolutionUsesPlayerCombatStatsForPlayerTargets(GameTestHelper helper) {
        ServerPlayer resistantTarget = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        ServerPlayer controlTarget = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        PlayerCombatStats.get(resistantTarget).setBaseValue(CombatStats.FIRE_RESISTANCE, 75.0);

        PreparedSkillUse prepared = Skills.prepareUse(
                fireDotSkill(),
                rawPlayerContext(helper, kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper), 0.0, 0.99)
        );
        PreparedApplyDotAction dotAction = (PreparedApplyDotAction) prepared.onCastActions().stream()
                .filter(PreparedApplyDotAction.class::isInstance)
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Inline fire DoT skill should prepare one apply_dot action"));

        helper.assertValueEqual(dotAction.damageCalculation().defenderStats().resolvedValue(CombatStats.FIRE_RESISTANCE), 0.0,
                "The prepared DoT should still carry the raw empty defender stats from preparation");

        double resistantDamage = DamageCalculations.calculateDamageOverTime(copyCalculation(
                dotAction.damageCalculation(),
                periodicDefenderStats(helper, resistantTarget)
        )).finalDamage().totalAmount();
        double controlDamage = DamageCalculations.calculateDamageOverTime(copyCalculation(
                dotAction.damageCalculation(),
                periodicDefenderStats(helper, controlTarget)
        )).finalDamage().totalAmount();

        helper.assertTrue(controlDamage > 0.0,
                "The control player should still resolve positive generic DoT damage");
        helper.assertTrue(resistantDamage < controlDamage,
                "High player fire resistance should reduce generic DoT damage when runtime fallback resolves live player combat stats");
        helper.succeed();
    }

    /**
     * Verifies that ailment periodic fallback resolution can reuse live player combat stats instead of the empty prepared defender holder.
     */
    @GameTest
    public void ailmentPeriodicFallbackResolutionUsesPlayerCombatStatsForPlayerTargets(GameTestHelper helper) {
        ServerPlayer caster = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        ServerPlayer resistantTarget = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        ServerPlayer controlTarget = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        PlayerCombatStats.get(resistantTarget).setBaseValue(CombatStats.CHAOS_RESISTANCE, 75.0);

        PreparedSkillUse prepared = Skills.prepareUse(
                venomStrike(helper),
                rawPlayerContext(helper, caster, 0.0, 0.99)
        );

        Skills.executeOnCast(SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.of(resistantTarget)));
        Skills.executeOnCast(SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.of(controlTarget)));
        AilmentPayload resistantPayload = Ailments.get(resistantTarget)
                .flatMap(state -> state.get(AilmentType.POISON))
                .orElseThrow(() -> helper.assertionException("Poison should attach a payload to the resistant player target"));
        AilmentPayload controlPayload = Ailments.get(controlTarget)
                .flatMap(state -> state.get(AilmentType.POISON))
                .orElseThrow(() -> helper.assertionException("Poison should attach a payload to the control player target"));

        helper.assertValueEqual(resistantPayload.potency(), controlPayload.potency(),
                "Chaos resistance should not change the initial physical hit used to seed poison potency");

        double resistantDamage = DamageCalculations.calculateDamageOverTime(ailmentCalculation(
                resistantPayload,
                periodicDefenderStats(helper, resistantTarget)
        )).finalDamage().totalAmount();
        double controlDamage = DamageCalculations.calculateDamageOverTime(ailmentCalculation(
                controlPayload,
                periodicDefenderStats(helper, controlTarget)
        )).finalDamage().totalAmount();

        helper.assertTrue(controlDamage > 0.0,
                "The control player should still resolve positive poison damage");
        helper.assertTrue(resistantDamage < controlDamage,
                "High player chaos resistance should reduce poison damage when runtime fallback resolves live player combat stats");
        helper.succeed();
    }

    private static SkillUseContext rawPlayerContext(
            GameTestHelper helper,
            ServerPlayer attacker,
            double hitRoll,
            double criticalStrikeRoll
    ) {
        return new SkillUseContext(
                PlayerCombatStats.get(attacker),
                newHolder(helper),
                List.<ConditionalStatModifier>of(),
                hitRoll,
                criticalStrikeRoll,
                SkillCalculationLookup.fromRegistry(skillCalculationRegistry(helper))
        );
    }

    private static SkillDefinition fireDotSkill() {
        return new SkillDefinition(
                "esekai2:player_runtime_fire_dot_inline",
                SkillConfig.DEFAULT,
                new SkillAttached(
                        List.of(new SkillRule(
                                Set.of(SkillTargetSelector.target()),
                                List.of(new SkillAction(SkillActionType.APPLY_DOT, Map.of(
                                        "dot_id", "player_runtime_fire_dot",
                                        "duration_ticks", "8",
                                        "tick_interval", "2",
                                        "base_damage_fire", "8.0"
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

    private static SkillDefinition venomStrike(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(VENOM_STRIKE_SKILL_ID)
                .orElseThrow(() -> helper.assertionException("Venom strike should decode successfully"));
    }

    private static DamageOverTimeCalculation ailmentCalculation(AilmentPayload payload, StatHolder defenderStats) {
        return new DamageOverTimeCalculation(
                DamageBreakdown.of(payload.type().damageType(), payload.potency()),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                defenderStats
        );
    }

    private static DamageOverTimeCalculation copyCalculation(DamageOverTimeCalculation source, StatHolder defenderStats) {
        return new DamageOverTimeCalculation(
                source.baseDamage(),
                source.conversions(),
                source.extraGains(),
                source.scaling(),
                source.exposures(),
                defenderStats
        );
    }

    private static StatHolder periodicDefenderStats(GameTestHelper helper, ServerPlayer target) {
        StatHolder base = LivingEntityCombatStatResolver.resolve(target)
                .orElseThrow(() -> helper.assertionException("Player targets should resolve to live combat stats"));
        return AilmentRuntime.resolveDefenderStats(helper.getLevel(), target, base);
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

    private static Registry<SkillDefinition> skillRegistry(GameTestHelper helper) {
        return helper.getLevel().getServer().registryAccess().lookupOrThrow(SkillRegistries.SKILL);
    }

    private static Registry<SkillCalculationDefinition> skillCalculationRegistry(GameTestHelper helper) {
        return SkillCalculationRegistryAccess.skillCalculationRegistry(helper);
    }

    private static StatHolder newHolder(GameTestHelper helper) {
        return StatHolders.create(StatRegistryAccess.statRegistry(helper));
    }
}
