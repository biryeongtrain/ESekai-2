package kim.biryeong.esekai2.impl.gametest.skill;

import com.mojang.serialization.JsonOps;
import kim.biryeong.esekai2.api.ailment.AilmentPayload;
import kim.biryeong.esekai2.api.ailment.AilmentState;
import kim.biryeong.esekai2.api.ailment.AilmentType;
import kim.biryeong.esekai2.api.ailment.Ailments;
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
import kim.biryeong.esekai2.api.skill.definition.SkillDefinition;
import kim.biryeong.esekai2.api.skill.definition.SkillRegistries;
import kim.biryeong.esekai2.api.skill.execution.PreparedApplyAilmentAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSkillUse;
import kim.biryeong.esekai2.api.skill.execution.SelectedSkillUseResult;
import kim.biryeong.esekai2.api.skill.execution.SkillExecutionContext;
import kim.biryeong.esekai2.api.skill.execution.SkillExecutionResult;
import kim.biryeong.esekai2.api.skill.execution.SkillUseContext;
import kim.biryeong.esekai2.api.skill.execution.Skills;
import kim.biryeong.esekai2.api.skill.support.SkillSupportDefinition;
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
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
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
        helper.assertTrue(BuiltInRegistries.MOB_EFFECT.containsKey(AilmentType.IGNITE.effectId()), "Ignite effect should be registered");
        helper.assertTrue(BuiltInRegistries.MOB_EFFECT.containsKey(AilmentType.SHOCK.effectId()), "Shock effect should be registered");

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
        helper.succeed();
    }

    /**
     * Verifies that ignite applies both a MobEffect identity and attachment payload, then deals periodic damage until expiry.
     */
    @GameTest
    public void igniteAppliesAttachmentBackedEffectAndTicks(GameTestHelper helper) {
        Player caster = helper.makeMockPlayer(GameType.CREATIVE);
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(3.0, 2.0, 3.0));
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

        float initialHealth = zombie.getHealth();
        helper.assertValueEqual(result.executedActions(), 3, "Kindling strike should execute sound, damage, and ailment actions");
        helper.assertTrue(
                zombie.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(AilmentType.IGNITE))),
                "Ignite should be represented by a MobEffect identity"
        );
        helper.assertTrue(Ailments.get(zombie).flatMap(state -> state.get(AilmentType.IGNITE)).isPresent(),
                "Ignite should also store an attachment payload on the target");
        helper.runAfterDelay(3, () ->
                helper.assertTrue(zombie.getHealth() < initialHealth, "Ignite should deal periodic damage shortly after the hit")
        );
        float[] settledHealth = new float[1];
        helper.runAfterDelay(10, () -> settledHealth[0] = zombie.getHealth());
        helper.runAfterDelay(14, () -> {
            helper.assertValueEqual(zombie.getHealth(), settledHealth[0], "Ignite should stop dealing damage after its duration expires");
            helper.assertTrue(Ailments.get(zombie).flatMap(state -> state.get(AilmentType.IGNITE)).isEmpty(),
                    "Expired ignite should clear its attachment payload");
            helper.succeed();
        });
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
        LivingEntity shockedTarget = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(3.0, 2.0, 3.0));
        LivingEntity controlTarget = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(5.0, 2.0, 3.0));
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

        float shockedBefore = shockedTarget.getHealth();
        float controlBefore = controlTarget.getHealth();
        Skills.executeOnCast(SkillExecutionContext.forCast(hitPrepared, helper.getLevel(), caster, Optional.of(shockedTarget)));
        Skills.executeOnCast(SkillExecutionContext.forCast(hitPrepared, helper.getLevel(), caster, Optional.of(controlTarget)));

        float shockedLoss = shockedBefore - shockedTarget.getHealth();
        float controlLoss = controlBefore - controlTarget.getHealth();
        helper.assertTrue(shockedLoss > controlLoss, "A shocked target should take more damage from subsequent hits");
        helper.succeed();
    }

    /**
     * Verifies that shock also amplifies the generic apply_dot runtime path, not only direct hit damage.
     */
    @GameTest
    public void shockAmplifiesGenericDotDamage(GameTestHelper helper) {
        Player caster = helper.makeMockPlayer(GameType.CREATIVE);
        LivingEntity shockedTarget = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(3.0, 2.0, 3.0));
        LivingEntity controlTarget = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(5.0, 2.0, 3.0));
        PreparedSkillUse shockPrepared = Skills.prepareUse(
                thunderStrike(helper),
                skillUseContext(helper, newHolder(helper), MonsterStats.resolveBaseHolder(shockedTarget).orElse(newHolder(helper)), 0.0, 0.99)
        );
        PreparedSkillUse dotPrepared = Skills.prepareUse(
                searingBrand(helper),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );

        Skills.executeOnCast(SkillExecutionContext.forCast(shockPrepared, helper.getLevel(), caster, Optional.of(shockedTarget)));
        float shockedBefore = shockedTarget.getHealth();
        float controlBefore = controlTarget.getHealth();
        Skills.executeOnCast(SkillExecutionContext.forCast(dotPrepared, helper.getLevel(), caster, Optional.of(shockedTarget)));
        Skills.executeOnCast(SkillExecutionContext.forCast(dotPrepared, helper.getLevel(), caster, Optional.of(controlTarget)));

        helper.runAfterDelay(3, () -> {
            float shockedLoss = shockedBefore - shockedTarget.getHealth();
            float controlLoss = controlBefore - controlTarget.getHealth();
            helper.assertTrue(shockedLoss > controlLoss, "Shock should amplify generic skill-owned DoT damage as well");
            helper.succeed();
        });
    }

    /**
     * Verifies that selected socket-backed preparation can override apply_ailment fields through a linked support.
     */
    @GameTest
    public void prepareSelectedUseAppliesAilmentSupportOverrides(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemSlot(
                SocketedEquipmentSlot.MAIN_HAND.equipmentSlot(),
                kindlingStrikeStack(List.of(supportRef(1, SUPPORT_ENDURING_KINDLING_ID)))
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
}
