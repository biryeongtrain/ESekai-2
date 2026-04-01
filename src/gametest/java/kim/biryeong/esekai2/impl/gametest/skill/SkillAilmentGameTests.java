package kim.biryeong.esekai2.impl.gametest.skill;

import com.mojang.serialization.JsonOps;
import kim.biryeong.esekai2.api.ailment.AilmentPayload;
import kim.biryeong.esekai2.api.ailment.AilmentState;
import kim.biryeong.esekai2.api.ailment.AilmentType;
import kim.biryeong.esekai2.api.ailment.Ailments;
import kim.biryeong.esekai2.api.damage.breakdown.DamageBreakdown;
import kim.biryeong.esekai2.api.damage.breakdown.DamageType;
import kim.biryeong.esekai2.api.damage.calculation.DamageCalculationResult;
import kim.biryeong.esekai2.api.damage.calculation.DamageCalculations;
import kim.biryeong.esekai2.api.damage.calculation.DamageOverTimeCalculation;
import kim.biryeong.esekai2.api.damage.calculation.DamageOverTimeResult;
import kim.biryeong.esekai2.api.damage.calculation.HitDamageCalculation;
import kim.biryeong.esekai2.api.damage.critical.HitContext;
import kim.biryeong.esekai2.api.damage.critical.HitKind;
import kim.biryeong.esekai2.api.item.socket.SocketLinkGroup;
import kim.biryeong.esekai2.api.item.socket.SocketSlotType;
import kim.biryeong.esekai2.api.item.socket.SocketedEquipmentSlot;
import kim.biryeong.esekai2.api.item.socket.SocketedSkillItemState;
import kim.biryeong.esekai2.api.item.socket.SocketedSkillRef;
import kim.biryeong.esekai2.api.item.socket.SocketedSkills;
import kim.biryeong.esekai2.api.monster.stat.MonsterStats;
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
import kim.biryeong.esekai2.api.skill.effect.SkillAilmentRefreshPolicy;
import kim.biryeong.esekai2.api.skill.execution.PreparedApplyAilmentAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedApplyDotAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedDamageAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSkillUse;
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
import kim.biryeong.esekai2.impl.ailment.AilmentRuntime;
import kim.biryeong.esekai2.impl.skill.registry.SkillCalculationRegistryAccess;
import kim.biryeong.esekai2.impl.stat.registry.StatRegistryAccess;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Verifies MobEffect-backed ailments that store their real payloads in entity attachments.
 */
public final class SkillAilmentGameTests {
    private static final Identifier KINDLING_EMBER_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "kindling_ember");
    private static final Identifier KINDLING_STRIKE_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "kindling_strike");
    private static final Identifier KINDLING_INFERNO_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "kindling_inferno");
    private static final Identifier THUNDER_STRIKE_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "thunder_strike");
    private static final Identifier VENOM_STRIKE_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "venom_strike");
    private static final Identifier BLOODLETTING_STRIKE_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "bloodletting_strike");
    private static final Identifier SEARING_BRAND_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "searing_brand");
    private static final Identifier SUPPORT_ENDURING_KINDLING_ID = Identifier.fromNamespaceAndPath("esekai2", "support_enduring_kindling");
    private static final Identifier SUPPORT_OVERWRITING_KINDLING_ID = Identifier.fromNamespaceAndPath("esekai2", "support_overwriting_kindling");
    private static final Identifier FROSTBITE_TOUCH_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "frostbite_touch");
    private static final Identifier ICE_PRISON_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "ice_prison");
    private static final Identifier CHILLING_STRIKE_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "chilling_strike_inline");
    private static final Identifier CHILLING_EMBER_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "chilling_ember_inline");
    private static final Identifier OVERWRITING_CHILLING_EMBER_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "overwriting_chilling_ember_inline");
    private static final Identifier GLACIAL_CLEAVE_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "glacial_cleave_inline");
    private static final Identifier ARCTIC_AVALANCHE_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "arctic_avalanche_inline");
    private static final Identifier GLACIAL_PRISON_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "glacial_prison_inline");
    private static final Identifier NEGATIVE_PURGE_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "negative_purge_inline");

    /**
     * Verifies that ailment fixtures, support fixtures, MobEffects, and the attachment codec load successfully.
     */
    @GameTest
    public void registryLoadsAilmentFixturesEffectsAndCodec(GameTestHelper helper) {
        helper.assertTrue(skillRegistry(helper).containsKey(KINDLING_STRIKE_SKILL_ID), "Kindling strike should load into the skill registry");
        helper.assertTrue(skillRegistry(helper).containsKey(THUNDER_STRIKE_SKILL_ID), "Thunder strike should load into the skill registry");
        helper.assertTrue(skillRegistry(helper).containsKey(VENOM_STRIKE_SKILL_ID), "Venom strike should load into the skill registry");
        helper.assertTrue(skillRegistry(helper).containsKey(BLOODLETTING_STRIKE_SKILL_ID), "Bloodletting strike should load into the skill registry");
        helper.assertTrue(supportRegistry(helper).containsKey(SUPPORT_ENDURING_KINDLING_ID), "Enduring kindling support should load into the support registry");
        helper.assertTrue(supportRegistry(helper).containsKey(SUPPORT_OVERWRITING_KINDLING_ID), "Overwriting kindling support should load into the support registry");
        helper.assertTrue(BuiltInRegistries.MOB_EFFECT.containsKey(AilmentType.IGNITE.effectId()), "Ignite effect should be registered");
        helper.assertTrue(BuiltInRegistries.MOB_EFFECT.containsKey(AilmentType.SHOCK.effectId()), "Shock effect should be registered");
        helper.assertTrue(BuiltInRegistries.MOB_EFFECT.containsKey(AilmentType.CHILL.effectId()), "Chill effect should be registered");
        helper.assertTrue(BuiltInRegistries.MOB_EFFECT.containsKey(AilmentType.FREEZE.effectId()), "Freeze effect should be registered");
        helper.assertTrue(BuiltInRegistries.MOB_EFFECT.containsKey(AilmentType.STUN.effectId()), "Stun effect should be registered");

        AilmentState original = new AilmentState(java.util.Map.of(
                AilmentType.IGNITE,
                new AilmentPayload(AilmentType.IGNITE, KINDLING_STRIKE_SKILL_ID.toString(), Optional.of(UUID.randomUUID()), 2.5, 8, 8, 2)
        ));
        var encoded = AilmentState.CODEC.encodeStart(JsonOps.INSTANCE, original)
                .result()
                .orElseThrow(() -> helper.assertionException("AilmentState should encode successfully"));
        AilmentState decoded = AilmentState.CODEC.parse(JsonOps.INSTANCE, encoded)
                .result()
                .orElseThrow(() -> helper.assertionException("AilmentState should decode successfully"));
        helper.assertValueEqual(decoded, original, "AilmentState codec should round-trip attachment payloads");
        helper.succeed();
    }

    /**
     * Verifies that apply_ailment actions decode into a prepared ailment payload with resolved numeric values.
     */
    @GameTest
    public void prepareUseBuildsApplyAilmentAction(GameTestHelper helper) {
        PreparedSkillUse prepared = Skills.prepareUse(
                kindlingStrike(helper),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );

        PreparedApplyAilmentAction ailmentAction = (PreparedApplyAilmentAction) prepared.onCastActions().stream()
                .filter(PreparedApplyAilmentAction.class::isInstance)
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Kindling strike should prepare one apply_ailment action"));

        helper.assertValueEqual(ailmentAction.ailmentType(), AilmentType.IGNITE, "Prepared ailment action should preserve the ailment type");
        helper.assertValueEqual(ailmentAction.durationTicks(), 8, "Prepared ailment action should resolve duration_ticks");
        helper.assertValueEqual(ailmentAction.chancePercent(), 100.0, "Prepared ailment action should resolve chance");
        helper.assertValueEqual(ailmentAction.potencyMultiplierPercent(), 100.0, "Prepared ailment action should resolve potency_multiplier");
        helper.assertValueEqual(ailmentAction.refreshPolicy(), SkillAilmentRefreshPolicy.STRONGER_ONLY,
                "Prepared ailment action should default to stronger_only for potency-backed ailments");
        helper.succeed();
    }

    /**
     * Verifies that control ailments default to the longer_only refresh policy during preparation.
     */
    @GameTest
    public void prepareUseBuildsLongerOnlyApplyAilmentActionForFreeze(GameTestHelper helper) {
        PreparedSkillUse prepared = Skills.prepareUse(
                icePrison(helper),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );

        PreparedApplyAilmentAction ailmentAction = (PreparedApplyAilmentAction) prepared.onCastActions().stream()
                .filter(PreparedApplyAilmentAction.class::isInstance)
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Ice prison should prepare one apply_ailment action"));

        helper.assertValueEqual(ailmentAction.ailmentType(), AilmentType.FREEZE, "Prepared freeze action should preserve the ailment type");
        helper.assertValueEqual(ailmentAction.refreshPolicy(), SkillAilmentRefreshPolicy.LONGER_ONLY,
                "Prepared freeze action should default to longer_only");
        helper.succeed();
    }

    /**
     * Verifies that ignite applies both a MobEffect identity and attachment payload, then deals periodic damage until expiry.
     */
    @GameTest
    public void igniteAppliesAttachmentBackedEffectAndTicks(GameTestHelper helper) {
        Player caster = helper.makeMockPlayer(GameType.CREATIVE);
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.COW, new Vec3(3.0, 2.0, 3.0));
        PreparedSkillUse prepared = Skills.prepareUse(
                kindlingStrike(helper),
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

        helper.assertValueEqual(result.executedActions(), 3, "Kindling strike should execute sound, damage, and ailment actions");
        helper.assertTrue(
                zombie.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(AilmentType.IGNITE))),
                "Ignite should be represented by a MobEffect identity"
        );
        helper.assertTrue(Ailments.get(zombie).flatMap(state -> state.get(AilmentType.IGNITE)).isPresent(),
                "Ignite should also store an attachment payload on the target");
        helper.assertTrue(!AilmentRuntime.tick(helper.getLevel(), zombie, AilmentType.IGNITE),
                "Ignite should not deal damage on the first runtime tick");
        zombie.invulnerableTime = 0;
        helper.assertTrue(AilmentRuntime.tick(helper.getLevel(), zombie, AilmentType.IGNITE),
                "Ignite should deal periodic damage on its next interval tick");
        for (int tick = 0; tick < 6; tick++) {
            AilmentRuntime.tick(helper.getLevel(), zombie, AilmentType.IGNITE);
        }
        float settledHealth = zombie.getHealth();
        helper.assertTrue(Ailments.get(zombie).flatMap(state -> state.get(AilmentType.IGNITE)).isEmpty(),
                "Expired ignite should clear its attachment payload");
        helper.assertTrue(!zombie.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(AilmentType.IGNITE))),
                "Expired ignite should clear its MobEffect identity");
        helper.assertTrue(!AilmentRuntime.tick(helper.getLevel(), zombie, AilmentType.IGNITE),
                "Expired ignite should stop producing periodic damage");
        helper.assertValueEqual(zombie.getHealth(), settledHealth, "Expired ignite should stop dealing damage after its duration expires");
        helper.succeed();
    }

    /**
     * Verifies that ailment reapplication keeps the stronger ignite payload and replaces it only when a stronger one arrives.
     */
    @GameTest
    public void igniteReapplicationPrefersStrongerPayload(GameTestHelper helper) {
        Player caster = helper.makeMockPlayer(GameType.CREATIVE);
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(3.0, 2.0, 3.0));
        PreparedSkillUse baseline = Skills.prepareUse(
                kindlingStrike(helper),
                skillUseContext(helper, newHolder(helper), MonsterStats.resolveBaseHolder(zombie).orElse(newHolder(helper)), 0.0, 0.99)
        );
        PreparedSkillUse weaker = Skills.prepareUse(
                kindlingEmber(helper),
                skillUseContext(helper, newHolder(helper), MonsterStats.resolveBaseHolder(zombie).orElse(newHolder(helper)), 0.0, 0.99)
        );
        PreparedSkillUse stronger = Skills.prepareUse(
                kindlingInferno(helper),
                skillUseContext(helper, newHolder(helper), MonsterStats.resolveBaseHolder(zombie).orElse(newHolder(helper)), 0.0, 0.99)
        );

        Skills.executeOnCast(SkillExecutionContext.forCast(baseline, helper.getLevel(), caster, Optional.of(zombie)));
        double baselinePotency = ailmentPayload(helper, zombie, AilmentType.IGNITE).potency();
        Skills.executeOnCast(SkillExecutionContext.forCast(weaker, helper.getLevel(), caster, Optional.of(zombie)));
        double afterWeaker = ailmentPayload(helper, zombie, AilmentType.IGNITE).potency();
        Skills.executeOnCast(SkillExecutionContext.forCast(stronger, helper.getLevel(), caster, Optional.of(zombie)));
        double afterStronger = ailmentPayload(helper, zombie, AilmentType.IGNITE).potency();

        helper.assertValueEqual(afterWeaker, baselinePotency, "A weaker ignite reapplication should not replace the current payload");
        helper.assertTrue(afterStronger > baselinePotency, "A stronger ignite reapplication should replace the current payload");
        helper.succeed();
    }

    /**
     * Verifies that poison and bleed use the same attachment-backed periodic-damage runtime as ignite.
     */
    @GameTest
    public void poisonAndBleedDealPeriodicDamage(GameTestHelper helper) {
        Player caster = helper.makeMockPlayer(GameType.CREATIVE);
        LivingEntity poisonTarget = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(3.0, 2.0, 3.0));
        LivingEntity bleedTarget = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(5.0, 2.0, 3.0));
        PreparedSkillUse poisonPrepared = Skills.prepareUse(
                venomStrike(helper),
                skillUseContext(helper, newHolder(helper), MonsterStats.resolveBaseHolder(poisonTarget).orElse(newHolder(helper)), 0.0, 0.99)
        );
        PreparedSkillUse bleedPrepared = Skills.prepareUse(
                bloodlettingStrike(helper),
                skillUseContext(helper, newHolder(helper), MonsterStats.resolveBaseHolder(bleedTarget).orElse(newHolder(helper)), 0.0, 0.99)
        );

        float poisonInitialHealth = poisonTarget.getHealth();
        float bleedInitialHealth = bleedTarget.getHealth();
        Skills.executeOnCast(SkillExecutionContext.forCast(poisonPrepared, helper.getLevel(), caster, Optional.of(poisonTarget)));
        Skills.executeOnCast(SkillExecutionContext.forCast(bleedPrepared, helper.getLevel(), caster, Optional.of(bleedTarget)));

        helper.assertTrue(Ailments.get(poisonTarget).flatMap(state -> state.get(AilmentType.POISON)).isPresent(),
                "Poison should store an attachment payload");
        helper.assertTrue(Ailments.get(bleedTarget).flatMap(state -> state.get(AilmentType.BLEED)).isPresent(),
                "Bleed should store an attachment payload");
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(poisonTarget.getHealth() < poisonInitialHealth, "Poison should deal periodic damage");
            helper.assertTrue(bleedTarget.getHealth() < bleedInitialHealth, "Bleed should deal periodic damage");
            helper.succeed();
        });
    }

    /**
     * Verifies that shock increases subsequent hit damage through the defender-side damage_taken_more surface.
     */
    @GameTest
    public void shockAmplifiesSubsequentHitDamage(GameTestHelper helper) {
        Player caster = helper.makeMockPlayer(GameType.CREATIVE);
        LivingEntity shockedTarget = helper.spawnWithNoFreeWill(EntityType.COW, new Vec3(3.0, 2.0, 3.0));
        LivingEntity controlTarget = helper.spawnWithNoFreeWill(EntityType.COW, new Vec3(5.0, 2.0, 3.0));
        PreparedSkillUse shockPrepared = Skills.prepareUse(
                thunderStrike(helper),
                skillUseContext(helper, newHolder(helper), MonsterStats.resolveBaseHolder(shockedTarget).orElse(newHolder(helper)), 0.0, 0.99)
        );
        PreparedSkillUse hitPrepared = Skills.prepareUse(
                basicStrike(helper),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.25)
        );

        Skills.executeOnCast(SkillExecutionContext.forCast(shockPrepared, helper.getLevel(), caster, Optional.of(shockedTarget)));
        helper.assertTrue(Ailments.get(shockedTarget).flatMap(state -> state.get(AilmentType.SHOCK)).isPresent(),
                "Shock should store an attachment payload");
        PreparedDamageAction hitAction = (PreparedDamageAction) hitPrepared.onCastActions().stream()
                .filter(PreparedDamageAction.class::isInstance)
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Basic strike should prepare one damage action"));

        DamageCalculationResult shockedResult = DamageCalculations.calculateHit(copyHitCalculation(
                hitAction.hitDamageCalculation(),
                AilmentRuntime.resolveDefenderStats(helper.getLevel(), shockedTarget, hitAction.hitDamageCalculation().defenderStats())
        ));
        DamageCalculationResult controlResult = DamageCalculations.calculateHit(copyHitCalculation(
                hitAction.hitDamageCalculation(),
                AilmentRuntime.resolveDefenderStats(helper.getLevel(), controlTarget, hitAction.hitDamageCalculation().defenderStats())
        ));

        helper.assertTrue(
                shockedResult.finalDamage().totalAmount() > controlResult.finalDamage().totalAmount(),
                "A shocked target should take more damage from subsequent hits"
        );
        helper.succeed();
    }

    /**
     * Verifies that shock also amplifies the generic apply_dot runtime path, not only direct hit damage.
     */
    @GameTest
    public void shockAmplifiesGenericDotDamage(GameTestHelper helper) {
        Player caster = helper.makeMockPlayer(GameType.CREATIVE);
        LivingEntity shockedTarget = helper.spawnWithNoFreeWill(EntityType.COW, new Vec3(3.0, 2.0, 3.0));
        LivingEntity controlTarget = helper.spawnWithNoFreeWill(EntityType.COW, new Vec3(5.0, 2.0, 3.0));
        PreparedSkillUse shockPrepared = Skills.prepareUse(
                thunderStrike(helper),
                skillUseContext(helper, newHolder(helper), MonsterStats.resolveBaseHolder(shockedTarget).orElse(newHolder(helper)), 0.0, 0.99)
        );
        PreparedSkillUse dotPrepared = Skills.prepareUse(
                searingBrand(helper),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );

        Skills.executeOnCast(SkillExecutionContext.forCast(shockPrepared, helper.getLevel(), caster, Optional.of(shockedTarget)));
        PreparedApplyDotAction dotAction = (PreparedApplyDotAction) dotPrepared.onCastActions().stream()
                .filter(PreparedApplyDotAction.class::isInstance)
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Searing brand should prepare one apply_dot action"));

        DamageOverTimeResult shockedResult = DamageCalculations.calculateDamageOverTime(copyDotCalculation(
                dotAction.damageCalculation(),
                AilmentRuntime.resolveDefenderStats(helper.getLevel(), shockedTarget, dotAction.damageCalculation().defenderStats())
        ));
        DamageOverTimeResult controlResult = DamageCalculations.calculateDamageOverTime(copyDotCalculation(
                dotAction.damageCalculation(),
                AilmentRuntime.resolveDefenderStats(helper.getLevel(), controlTarget, dotAction.damageCalculation().defenderStats())
        ));

        helper.assertTrue(
                shockedResult.finalDamage().totalAmount() > controlResult.finalDamage().totalAmount(),
                "Shock should amplify generic skill-owned DoT damage as well"
        );
        helper.succeed();
    }

    /**
     * Verifies that applying a generic apply_dot to a shocked target still succeeds without replacing the existing shock payload.
     */
    @GameTest
    public void genericDotApplicationPreservesExistingShockState(GameTestHelper helper) {
        Player caster = helper.makeMockPlayer(GameType.CREATIVE);
        LivingEntity shockedTarget = helper.spawnWithNoFreeWill(EntityType.COW, new Vec3(3.0, 2.0, 3.0));
        PreparedSkillUse shockPrepared = Skills.prepareUse(
                thunderStrike(helper),
                skillUseContext(helper, newHolder(helper), MonsterStats.resolveBaseHolder(shockedTarget).orElse(newHolder(helper)), 0.0, 0.99)
        );
        PreparedSkillUse dotPrepared = Skills.prepareUse(
                searingBrand(helper),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );

        Skills.executeOnCast(SkillExecutionContext.forCast(shockPrepared, helper.getLevel(), caster, Optional.of(shockedTarget)));
        AilmentPayload shockBefore = ailmentPayload(helper, shockedTarget, AilmentType.SHOCK);

        SkillExecutionResult dotResult = Skills.executeOnCast(
                SkillExecutionContext.forCast(dotPrepared, helper.getLevel(), caster, Optional.of(shockedTarget))
        );
        AilmentPayload shockAfter = ailmentPayload(helper, shockedTarget, AilmentType.SHOCK);

        helper.assertValueEqual(dotResult.executedActions(), 2,
                "Searing brand should still execute both sound and apply_dot actions against a shocked target");
        helper.assertValueEqual(shockAfter, shockBefore,
                "Applying a generic dot should not replace or clear the existing shock payload");
        helper.runAfterDelay(1, () -> {
            helper.assertTrue(Ailments.get(shockedTarget).flatMap(state -> state.get(AilmentType.SHOCK)).isPresent(),
                    "Shock should remain active after generic apply_dot registration");
            helper.succeed();
        });
    }

    /**
     * Verifies that chill applies both attachment state and a MobEffect identity, then lowers movement speed from its stored potency.
     */
    @GameTest
    public void chillAppliesAttachmentBackedEffectAndSlowsMovement(GameTestHelper helper) {
        Player caster = helper.makeMockPlayer(GameType.CREATIVE);
        LivingEntity target = helper.spawnWithNoFreeWill(EntityType.COW, new Vec3(3.0, 2.0, 3.0));
        double baselineMovementSpeed = target.getAttributeValue(Attributes.MOVEMENT_SPEED);
        PreparedSkillUse prepared = Skills.prepareUse(
                chillingStrike(),
                skillUseContext(helper, newHolder(helper), MonsterStats.resolveBaseHolder(target).orElse(newHolder(helper)), 0.0, 0.99)
        );

        SkillExecutionResult result = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.of(target))
        );

        AilmentPayload chillPayload = ailmentPayload(helper, target, AilmentType.CHILL);
        MobEffectInstance chillEffect = target.getEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(AilmentType.CHILL)));
        helper.assertValueEqual(result.executedActions(), 3, "Chilling strike should execute sound, damage, and ailment actions");
        helper.assertTrue(chillEffect != null, "Chill should be represented by a MobEffect identity");
        helper.assertTrue(chillPayload.potency() > 0.0, "Chill should store a positive potency payload");

        int expectedPercent = (int) Math.ceil(chillPayload.potency());
        double expectedMovementSpeed = baselineMovementSpeed * (1.0 - expectedPercent / 100.0);
        helper.assertTrue(
                Math.abs(target.getAttributeValue(Attributes.MOVEMENT_SPEED) - expectedMovementSpeed) < 1.0e-6,
                "Chill movement speed modifier should match the stored potency"
        );
        helper.succeed();
    }

    /**
     * Verifies that chill potency is capped at 30% and that the stored cap drives the live movement slow.
     */
    @GameTest
    public void chillPotencyCapsAtThirtyPercentAndMovementSlowUsesCap(GameTestHelper helper) {
        Player caster = helper.makeMockPlayer(GameType.CREATIVE);
        LivingEntity target = helper.spawnWithNoFreeWill(EntityType.COW, new Vec3(3.0, 2.0, 3.0));
        double baselineMovementSpeed = target.getAttributeValue(Attributes.MOVEMENT_SPEED);
        PreparedSkillUse prepared = Skills.prepareUse(
                arcticAvalanche(),
                skillUseContext(helper, newHolder(helper), MonsterStats.resolveBaseHolder(target).orElse(newHolder(helper)), 0.0, 0.99)
        );

        Skills.executeOnCast(SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.of(target)));

        AilmentPayload chillPayload = ailmentPayload(helper, target, AilmentType.CHILL);
        MobEffectInstance chillEffect = target.getEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(AilmentType.CHILL)));
        helper.assertValueEqual(chillPayload.potency(), 30.0, "High-damage chill should clamp its stored potency at the 30 percent cap");
        helper.assertTrue(chillPayload.potency() <= 30.0, "Chill potency should never exceed the control ailment cap");
        helper.assertTrue(chillEffect != null, "Capped chill should still be represented by a MobEffect identity");
        helper.assertValueEqual(chillEffect.getAmplifier(), 29, "Capped 30 percent chill should encode as amplifier 29");
        helper.assertValueEqual(
                target.getAttributeValue(Attributes.MOVEMENT_SPEED),
                baselineMovementSpeed * 0.70,
                "Capped chill should reduce movement speed by exactly 30 percent"
        );
        helper.succeed();
    }

    /**
     * Verifies that chill keeps the stronger payload and only updates when a stronger reapplication arrives.
     */
    @GameTest
    public void chillReapplicationPrefersStrongerPayload(GameTestHelper helper) {
        Player caster = helper.makeMockPlayer(GameType.CREATIVE);
        LivingEntity target = helper.spawnWithNoFreeWill(EntityType.COW, new Vec3(3.0, 2.0, 3.0));

        PreparedSkillUse baseline = Skills.prepareUse(
                chillingStrike(),
                skillUseContext(helper, newHolder(helper), MonsterStats.resolveBaseHolder(target).orElse(newHolder(helper)), 0.0, 0.99)
        );
        PreparedSkillUse weaker = Skills.prepareUse(
                chillingEmber(),
                skillUseContext(helper, newHolder(helper), MonsterStats.resolveBaseHolder(target).orElse(newHolder(helper)), 0.0, 0.99)
        );
        PreparedSkillUse stronger = Skills.prepareUse(
                glacialCleave(),
                skillUseContext(helper, newHolder(helper), MonsterStats.resolveBaseHolder(target).orElse(newHolder(helper)), 0.0, 0.99)
        );

        Skills.executeOnCast(SkillExecutionContext.forCast(baseline, helper.getLevel(), caster, Optional.of(target)));
        double baselinePotency = ailmentPayload(helper, target, AilmentType.CHILL).potency();
        Skills.executeOnCast(SkillExecutionContext.forCast(weaker, helper.getLevel(), caster, Optional.of(target)));
        double afterWeaker = ailmentPayload(helper, target, AilmentType.CHILL).potency();
        Skills.executeOnCast(SkillExecutionContext.forCast(stronger, helper.getLevel(), caster, Optional.of(target)));
        double afterStronger = ailmentPayload(helper, target, AilmentType.CHILL).potency();

        helper.assertValueEqual(afterWeaker, baselinePotency, "A weaker chill should not replace the current potency payload");
        helper.assertTrue(afterStronger > baselinePotency, "A stronger chill should replace the current potency payload");
        helper.succeed();
    }

    /**
     * Verifies that overwrite refresh_policy lets a weaker chill replace the current stronger payload.
     */
    @GameTest
    public void chillOverwriteRefreshPolicyAllowsWeakerReplacement(GameTestHelper helper) {
        Player caster = helper.makeMockPlayer(GameType.CREATIVE);
        LivingEntity target = helper.spawnWithNoFreeWill(EntityType.COW, new Vec3(3.0, 2.0, 3.0));

        PreparedSkillUse baseline = Skills.prepareUse(
                chillingStrike(),
                skillUseContext(helper, newHolder(helper), MonsterStats.resolveBaseHolder(target).orElse(newHolder(helper)), 0.0, 0.99)
        );
        PreparedSkillUse overwrite = Skills.prepareUse(
                overwritingChillingEmber(),
                skillUseContext(helper, newHolder(helper), MonsterStats.resolveBaseHolder(target).orElse(newHolder(helper)), 0.0, 0.99)
        );

        Skills.executeOnCast(SkillExecutionContext.forCast(baseline, helper.getLevel(), caster, Optional.of(target)));
        AilmentPayload strongerPayload = ailmentPayload(helper, target, AilmentType.CHILL);
        Skills.executeOnCast(SkillExecutionContext.forCast(overwrite, helper.getLevel(), caster, Optional.of(target)));
        AilmentPayload overwrittenPayload = ailmentPayload(helper, target, AilmentType.CHILL);

        helper.assertTrue(overwrittenPayload.potency() < strongerPayload.potency(),
                "Overwrite chill should replace the existing payload even when it is weaker");
        helper.assertValueEqual(overwrittenPayload.remainingTicks(), 6,
                "Overwrite chill should replace the attachment duration with the incoming shorter payload");
        helper.succeed();
    }

    /**
     * Verifies that freeze applies attachment state plus a MobEffect identity and reduces movement speed to zero while active.
     */
    @GameTest
    public void freezeAppliesAttachmentBackedEffectAndZeroesMovementSpeed(GameTestHelper helper) {
        Player caster = helper.makeMockPlayer(GameType.CREATIVE);
        LivingEntity target = helper.spawnWithNoFreeWill(EntityType.COW, new Vec3(3.0, 2.0, 3.0));
        PreparedSkillUse prepared = Skills.prepareUse(
                glacialPrison(),
                skillUseContext(helper, newHolder(helper), MonsterStats.resolveBaseHolder(target).orElse(newHolder(helper)), 0.0, 0.99)
        );

        SkillExecutionResult result = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.of(target))
        );

        helper.assertValueEqual(result.executedActions(), 3, "Glacial prison should execute sound, damage, and ailment actions");
        helper.assertTrue(target.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(AilmentType.FREEZE))),
                "Freeze should be represented by a MobEffect identity");
        helper.assertTrue(Ailments.get(target).flatMap(state -> state.get(AilmentType.FREEZE)).isPresent(),
                "Freeze should store an attachment payload");
        helper.assertTrue(target.getAttributeValue(Attributes.MOVEMENT_SPEED) <= 1.0e-9,
                "Freeze should reduce movement speed to zero while active");
        helper.succeed();
    }

    /**
     * Verifies that broad purge negative clears freeze from a target by removing both the attachment payload and MobEffect identity.
     */
    @GameTest
    public void freezeIsRemovedByTargetedNegativePurge(GameTestHelper helper) {
        Player applier = helper.makeMockPlayer(GameType.CREATIVE);
        Player cleanser = helper.makeMockPlayer(GameType.CREATIVE);
        LivingEntity target = helper.spawnWithNoFreeWill(EntityType.COW, new Vec3(3.0, 2.0, 3.0));
        PreparedSkillUse freezePrepared = Skills.prepareUse(
                glacialPrison(),
                skillUseContext(helper, newHolder(helper), MonsterStats.resolveBaseHolder(target).orElse(newHolder(helper)), 0.0, 0.99)
        );
        PreparedSkillUse purgePrepared = Skills.prepareUse(
                targetedNegativePurge(),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );

        Skills.executeOnCast(SkillExecutionContext.forCast(freezePrepared, helper.getLevel(), applier, Optional.of(target)));
        helper.assertTrue(Ailments.get(target).flatMap(state -> state.get(AilmentType.FREEZE)).isPresent(),
                "Freeze setup should attach a freeze payload before the purge");

        SkillExecutionResult purgeResult = Skills.executeOnCast(
                SkillExecutionContext.forCast(purgePrepared, helper.getLevel(), cleanser, Optional.of(target))
        );

        helper.assertValueEqual(purgeResult.executedActions(), 1, "Targeted negative purge should execute its remove_effect action");
        helper.assertTrue(Ailments.get(target).flatMap(state -> state.get(AilmentType.FREEZE)).isEmpty(),
                "Negative purge should remove the freeze attachment payload");
        helper.assertTrue(!target.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(AilmentType.FREEZE))),
                "Negative purge should remove the freeze MobEffect identity");
        helper.succeed();
    }

    /**
     * Verifies that removing freeze restores the underlying chill slow instead of clearing the chill payload.
     */
    @GameTest
    public void freezeRemovalRestoresUnderlyingChillSlow(GameTestHelper helper) {
        Player caster = helper.makeMockPlayer(GameType.CREATIVE);
        LivingEntity target = helper.spawnWithNoFreeWill(EntityType.COW, new Vec3(3.0, 2.0, 3.0));
        double baselineSpeed = target.getAttributeValue(Attributes.MOVEMENT_SPEED);
        PreparedSkillUse chillPrepared = Skills.prepareUse(
                frostbiteTouch(helper),
                skillUseContext(helper, newHolder(helper), MonsterStats.resolveBaseHolder(target).orElse(newHolder(helper)), 0.0, 0.99)
        );
        PreparedSkillUse freezePrepared = Skills.prepareUse(
                icePrison(helper),
                skillUseContext(helper, newHolder(helper), MonsterStats.resolveBaseHolder(target).orElse(newHolder(helper)), 0.0, 0.99)
        );

        Skills.executeOnCast(SkillExecutionContext.forCast(chillPrepared, helper.getLevel(), caster, Optional.of(target)));
        double chilledSpeed = target.getAttributeValue(Attributes.MOVEMENT_SPEED);
        helper.assertTrue(chilledSpeed < baselineSpeed, "Chill setup should lower movement speed before freeze is applied");
        Skills.executeOnCast(SkillExecutionContext.forCast(freezePrepared, helper.getLevel(), caster, Optional.of(target)));
        helper.assertValueEqual(target.getAttributeValue(Attributes.MOVEMENT_SPEED), 0.0,
                "Freeze should mask the underlying chill slow and clamp movement speed to zero");

        helper.assertTrue(AilmentRuntime.remove(target, AilmentType.FREEZE),
                "Explicit freeze removal should succeed while both chill and freeze are active");
        helper.assertTrue(Ailments.get(target).flatMap(state -> state.get(AilmentType.CHILL)).isPresent(),
                "Removing freeze should preserve the underlying chill attachment payload");
        helper.assertValueEqual(target.getAttributeValue(Attributes.MOVEMENT_SPEED), chilledSpeed,
                "Removing freeze should reveal the original chill slow again");
        helper.succeed();
    }

    /**
     * Verifies that selected socket-backed preparation can override apply_ailment fields through a linked support.
     */
    @GameTest
    public void prepareSelectedUseAppliesAilmentSupportOverrides(GameTestHelper helper) {
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        player.setItemSlot(
                SocketedEquipmentSlot.MAIN_HAND.equipmentSlot(),
                kindlingStrikeStack(List.of(
                        supportRef(1, SUPPORT_ENDURING_KINDLING_ID),
                        supportRef(2, SUPPORT_OVERWRITING_KINDLING_ID)
                ))
        );
        PlayerActiveSkills.select(player, new SelectedActiveSkillRef(SocketedEquipmentSlot.MAIN_HAND, KINDLING_STRIKE_SKILL_ID));

        SelectedSkillUseResult result = Skills.prepareSelectedUse(
                player,
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );

        PreparedApplyAilmentAction ailmentAction = (PreparedApplyAilmentAction) result.preparedUse().orElseThrow().onCastActions().stream()
                .filter(PreparedApplyAilmentAction.class::isInstance)
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Selected kindling strike should still expose one apply_ailment action"));

        helper.assertTrue(result.success(), "Selected kindling strike should prepare successfully");
        helper.assertValueEqual(ailmentAction.durationTicks(), 16, "Linked support should override apply_ailment duration_ticks");
        helper.assertValueEqual(ailmentAction.refreshPolicy(), SkillAilmentRefreshPolicy.OVERWRITE,
                "Linked support should override apply_ailment refresh_policy");
        helper.succeed();
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

    private static SkillDefinition kindlingEmber(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(KINDLING_EMBER_SKILL_ID)
                .orElseThrow(() -> helper.assertionException("Kindling ember should decode successfully"));
    }

    private static SkillDefinition kindlingStrike(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(KINDLING_STRIKE_SKILL_ID)
                .orElseThrow(() -> helper.assertionException("Kindling strike should decode successfully"));
    }

    private static SkillDefinition kindlingInferno(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(KINDLING_INFERNO_SKILL_ID)
                .orElseThrow(() -> helper.assertionException("Kindling inferno should decode successfully"));
    }

    private static SkillDefinition frostbiteTouch(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(FROSTBITE_TOUCH_SKILL_ID)
                .orElseThrow(() -> helper.assertionException("Frostbite touch should decode successfully"));
    }

    private static SkillDefinition icePrison(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(ICE_PRISON_SKILL_ID)
                .orElseThrow(() -> helper.assertionException("Ice prison should decode successfully"));
    }

    private static SkillDefinition thunderStrike(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(THUNDER_STRIKE_SKILL_ID)
                .orElseThrow(() -> helper.assertionException("Thunder strike should decode successfully"));
    }

    private static SkillDefinition venomStrike(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(VENOM_STRIKE_SKILL_ID)
                .orElseThrow(() -> helper.assertionException("Venom strike should decode successfully"));
    }

    private static SkillDefinition bloodlettingStrike(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(BLOODLETTING_STRIKE_SKILL_ID)
                .orElseThrow(() -> helper.assertionException("Bloodletting strike should decode successfully"));
    }

    private static SkillDefinition searingBrand(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(SEARING_BRAND_SKILL_ID)
                .orElseThrow(() -> helper.assertionException("Searing brand should decode successfully"));
    }

    private static SkillDefinition basicStrike(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(Identifier.fromNamespaceAndPath("esekai2", "basic_strike"))
                .orElseThrow(() -> helper.assertionException("Basic strike should decode successfully"));
    }

    private static AilmentPayload ailmentPayload(GameTestHelper helper, LivingEntity entity, AilmentType type) {
        return Ailments.get(entity)
                .flatMap(state -> state.get(type))
                .orElseThrow(() -> helper.assertionException(type.serializedName() + " payload should exist on the target"));
    }

    private static HitDamageCalculation copyHitCalculation(HitDamageCalculation source, StatHolder defenderStats) {
        return new HitDamageCalculation(
                source.baseDamage(),
                source.conversions(),
                source.extraGains(),
                source.scaling(),
                source.exposures(),
                source.penetrations(),
                source.attackerStats(),
                source.hitContext(),
                defenderStats
        );
    }

    private static DamageOverTimeCalculation copyDotCalculation(DamageOverTimeCalculation source, StatHolder defenderStats) {
        return new DamageOverTimeCalculation(
                source.baseDamage(),
                source.conversions(),
                source.extraGains(),
                source.scaling(),
                source.exposures(),
                defenderStats
        );
    }

    private static MobEffect effect(AilmentType type) {
        return BuiltInRegistries.MOB_EFFECT.getOptional(type.effectId())
                .orElseThrow(() -> new IllegalStateException("Missing ailment effect: " + type.effectId()));
    }

    private static ItemStack kindlingStrikeStack(List<SocketedSkillRef> extraSupports) {
        ItemStack stack = new ItemStack(Items.IRON_SWORD);
        List<SocketedSkillRef> socketRefs = new java.util.ArrayList<>();
        socketRefs.add(new SocketedSkillRef(0, SocketSlotType.SKILL, KINDLING_STRIKE_SKILL_ID));
        socketRefs.addAll(extraSupports);
        SocketedSkills.set(stack, new SocketedSkillItemState(
                Optional.of(KINDLING_STRIKE_SKILL_ID),
                Math.max(1, socketRefs.size()),
                List.of(new SocketLinkGroup(0, socketRefs.stream().map(SocketedSkillRef::socketIndex).toList())),
                List.copyOf(socketRefs)
        ));
        return stack;
    }

    private static SocketedSkillRef supportRef(int socketIndex, Identifier supportId) {
        return new SocketedSkillRef(socketIndex, SocketSlotType.SUPPORT, supportId);
    }

    private static SkillDefinition chillingStrike() {
        return ailmentSkill(CHILLING_STRIKE_SKILL_ID, AilmentType.CHILL, 10.0, 10, 100.0, "minecraft:block.glass.hit");
    }

    private static SkillDefinition chillingEmber() {
        return ailmentSkill(CHILLING_EMBER_SKILL_ID, AilmentType.CHILL, 5.0, 10, 100.0, "minecraft:block.glass.hit");
    }

    private static SkillDefinition overwritingChillingEmber() {
        return ailmentSkill(
                OVERWRITING_CHILLING_EMBER_SKILL_ID,
                AilmentType.CHILL,
                5.0,
                6,
                100.0,
                "minecraft:block.glass.hit",
                SkillAilmentRefreshPolicy.OVERWRITE
        );
    }

    private static SkillDefinition glacialCleave() {
        return ailmentSkill(GLACIAL_CLEAVE_SKILL_ID, AilmentType.CHILL, 20.0, 10, 100.0, "minecraft:block.glass.hit");
    }

    private static SkillDefinition arcticAvalanche() {
        return ailmentSkill(ARCTIC_AVALANCHE_SKILL_ID, AilmentType.CHILL, 200.0, 10, 100.0, "minecraft:block.glass.hit");
    }

    private static SkillDefinition glacialPrison() {
        return ailmentSkill(GLACIAL_PRISON_SKILL_ID, AilmentType.FREEZE, 10.0, 10, 100.0, "minecraft:block.glass.break");
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

    private static SkillDefinition ailmentSkill(
            Identifier identifier,
            AilmentType ailmentType,
            double physicalDamage,
            int durationTicks,
            double potencyMultiplier,
            String soundId
    ) {
        return ailmentSkill(identifier, ailmentType, physicalDamage, durationTicks, potencyMultiplier, soundId, ailmentType.defaultRefreshPolicy());
    }

    private static SkillDefinition ailmentSkill(
            Identifier identifier,
            AilmentType ailmentType,
            double physicalDamage,
            int durationTicks,
            double potencyMultiplier,
            String soundId,
            SkillAilmentRefreshPolicy refreshPolicy
    ) {
        return new SkillDefinition(
                identifier.toString(),
                SkillConfig.DEFAULT,
                new SkillAttached(
                        List.of(new SkillRule(
                                Set.of(SkillTargetSelector.target()),
                                List.of(
                                        new SkillAction(SkillActionType.SOUND, Map.of("sound_id", soundId)),
                                        new SkillAction(SkillActionType.DAMAGE, Map.of("base_damage_physical", Double.toString(physicalDamage))),
                                        new SkillAction(SkillActionType.APPLY_AILMENT, Map.of(
                                                "ailment_id", ailmentType.serializedName(),
                                                "chance", "100.0",
                                                "duration_ticks", Integer.toString(durationTicks),
                                                "potency_multiplier", Double.toString(potencyMultiplier),
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
}
