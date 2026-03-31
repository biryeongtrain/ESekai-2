package kim.biryeong.esekai2.impl.gametest.skill;

import kim.biryeong.esekai2.api.ailment.AilmentPayload;
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
import kim.biryeong.esekai2.api.skill.execution.PreparedSkillUse;
import kim.biryeong.esekai2.api.skill.execution.SelectedSkillCastResult;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Verifies non-DoT control ailments and their cast-blocking runtime semantics.
 */
public final class AilmentControlGameTests {
    private static final Identifier ICE_PRISON_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "ice_prison");
    private static final Identifier DEEP_FREEZE_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "deep_freeze");
    private static final Identifier DAZING_TAP_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "dazing_tap");
    private static final Identifier CONCUSSIVE_SLAM_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "concussive_slam");
    private static final Identifier BASIC_STRIKE_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "basic_strike");

    /**
     * Verifies that freeze and stun fixture skills load and register their MobEffect identities.
     */
    @GameTest
    public void registryLoadsFreezeAndStunFixturesAndEffects(GameTestHelper helper) {
        helper.assertTrue(skillRegistry(helper).containsKey(ICE_PRISON_SKILL_ID), "Ice prison should load into the skill registry");
        helper.assertTrue(skillRegistry(helper).containsKey(DEEP_FREEZE_SKILL_ID), "Deep freeze should load into the skill registry");
        helper.assertTrue(skillRegistry(helper).containsKey(DAZING_TAP_SKILL_ID), "Dazing tap should load into the skill registry");
        helper.assertTrue(skillRegistry(helper).containsKey(CONCUSSIVE_SLAM_SKILL_ID), "Concussive slam should load into the skill registry");
        helper.assertTrue(BuiltInRegistries.MOB_EFFECT.containsKey(AilmentType.FREEZE.effectId()), "Freeze effect should be registered");
        helper.assertTrue(BuiltInRegistries.MOB_EFFECT.containsKey(AilmentType.STUN.effectId()), "Stun effect should be registered");
        helper.succeed();
    }

    /**
     * Verifies that stun applies both an attachment payload and a MobEffect identity, then only refreshes when a longer payload arrives.
     */
    @GameTest
    public void stunAppliesAttachmentAndRefreshesLongerOnly(GameTestHelper helper) {
        ServerPlayer caster = helper.makeMockServerPlayerInLevel();
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(3.0, 2.0, 3.0));
        PreparedSkillUse shortStun = Skills.prepareUse(
                dazingTap(helper),
                skillUseContext(helper, newHolder(helper), MonsterStats.resolveBaseHolder(zombie).orElse(newHolder(helper)), 0.0, 0.99)
        );
        PreparedSkillUse longStun = Skills.prepareUse(
                concussiveSlam(helper),
                skillUseContext(helper, newHolder(helper), MonsterStats.resolveBaseHolder(zombie).orElse(newHolder(helper)), 0.0, 0.99)
        );

        Skills.executeOnCast(SkillExecutionContext.forCast(longStun, helper.getLevel(), caster, Optional.of(zombie)));
        AilmentPayload baseline = payload(helper, zombie, AilmentType.STUN);
        Skills.executeOnCast(SkillExecutionContext.forCast(shortStun, helper.getLevel(), caster, Optional.of(zombie)));
        AilmentPayload afterShorter = payload(helper, zombie, AilmentType.STUN);

        helper.assertTrue(zombie.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(AilmentType.STUN))),
                "Stun should be represented by a MobEffect identity");
        helper.assertValueEqual(afterShorter, baseline,
                "A shorter stun reapplication should not replace the current attachment payload");
        helper.succeed();
    }

    /**
     * Verifies that freeze reduces movement speed to zero through its MobEffect attribute modifier and stores attachment state.
     */
    @GameTest
    public void freezeAppliesAttachmentAndZeroesMovementSpeed(GameTestHelper helper) {
        ServerPlayer caster = helper.makeMockServerPlayerInLevel();
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(3.0, 2.0, 3.0));
        double baselineSpeed = zombie.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        PreparedSkillUse prepared = Skills.prepareUse(
                icePrison(helper),
                skillUseContext(helper, newHolder(helper), MonsterStats.resolveBaseHolder(zombie).orElse(newHolder(helper)), 0.0, 0.99)
        );

        Skills.executeOnCast(SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.of(zombie)));

        helper.assertTrue(Ailments.get(zombie).flatMap(state -> state.get(AilmentType.FREEZE)).isPresent(),
                "Freeze should store an attachment payload");
        helper.assertTrue(zombie.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect(AilmentType.FREEZE))),
                "Freeze should be represented by a MobEffect identity");
        helper.assertTrue(zombie.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) < baselineSpeed,
                "Freeze should lower movement speed through the MobEffect attribute modifier");
        helper.assertValueEqual(zombie.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED), 0.0,
                "Freeze should clamp movement speed to zero");
        helper.succeed();
    }

    /**
     * Verifies that executeOnCast no-ops with a warning when the source is frozen.
     */
    @GameTest
    public void freezeBlocksExecuteOnCastOnSource(GameTestHelper helper) {
        ServerPlayer controller = helper.makeMockServerPlayerInLevel();
        ServerPlayer frozenCaster = helper.makeMockServerPlayerInLevel();
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(3.0, 2.0, 3.0));
        PreparedSkillUse freezePrepared = Skills.prepareUse(
                icePrison(helper),
                skillUseContext(helper, newHolder(helper), MonsterStats.resolveBaseHolder(frozenCaster).orElse(newHolder(helper)), 0.0, 0.99)
        );
        PreparedSkillUse strikePrepared = Skills.prepareUse(
                basicStrike(helper),
                skillUseContext(helper, newHolder(helper), MonsterStats.resolveBaseHolder(zombie).orElse(newHolder(helper)), 0.0, 0.25)
        );

        Skills.executeOnCast(SkillExecutionContext.forCast(freezePrepared, helper.getLevel(), controller, Optional.of(frozenCaster)));
        float startingHealth = zombie.getHealth();
        SkillExecutionResult result = Skills.executeOnCast(
                SkillExecutionContext.forCast(strikePrepared, helper.getLevel(), frozenCaster, Optional.of(zombie))
        );

        helper.assertValueEqual(result.executedActions(), 0, "Frozen sources should not execute on-cast actions");
        helper.assertTrue(result.skippedActions() >= 2, "Frozen sources should report skipped actions");
        helper.assertTrue(result.warnings().stream().anyMatch(warning -> warning.contains("freeze")),
                "Frozen cast attempts should surface a freeze warning");
        helper.assertValueEqual(zombie.getHealth(), startingHealth, "Frozen cast attempts should not damage the target");
        helper.succeed();
    }

    /**
     * Verifies that selected active skill casts also no-op with a warning when the player is stunned.
     */
    @GameTest
    public void castSelectedSkillNoopsWhenPlayerIsStunned(GameTestHelper helper) {
        ServerPlayer controller = helper.makeMockServerPlayerInLevel();
        ServerPlayer stunnedPlayer = helper.makeMockServerPlayerInLevel();
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(3.0, 2.0, 3.0));
        stunnedPlayer.setItemSlot(SocketedEquipmentSlot.MAIN_HAND.equipmentSlot(), basicStrikeStack());
        PlayerActiveSkills.select(stunnedPlayer, new SelectedActiveSkillRef(SocketedEquipmentSlot.MAIN_HAND, BASIC_STRIKE_SKILL_ID));
        PreparedSkillUse stunPrepared = Skills.prepareUse(
                dazingTap(helper),
                skillUseContext(helper, newHolder(helper), MonsterStats.resolveBaseHolder(stunnedPlayer).orElse(newHolder(helper)), 0.0, 0.99)
        );

        Skills.executeOnCast(SkillExecutionContext.forCast(stunPrepared, helper.getLevel(), controller, Optional.of(stunnedPlayer)));
        float startingHealth = zombie.getHealth();
        SelectedSkillCastResult result = Skills.castSelectedSkill(
                stunnedPlayer,
                skillUseContext(helper, newHolder(helper), MonsterStats.resolveBaseHolder(zombie).orElse(newHolder(helper)), 0.0, 0.25),
                Optional.of(zombie)
        );

        helper.assertTrue(result.success(), "Selected casts should fail-safe into a runtime no-op instead of hard failing when stunned");
        helper.assertValueEqual(result.executionResult().orElseThrow().executedActions(), 0,
                "Stunned selected casts should execute no actions");
        helper.assertTrue(result.warnings().stream().anyMatch(warning -> warning.contains("stun")),
                "Stunned selected casts should surface a stun warning");
        helper.assertValueEqual(zombie.getHealth(), startingHealth, "Stunned selected casts should not damage the target");
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

    private static SkillDefinition icePrison(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(ICE_PRISON_SKILL_ID)
                .orElseThrow(() -> helper.assertionException("Ice prison should decode successfully"));
    }

    private static SkillDefinition deepFreeze(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(DEEP_FREEZE_SKILL_ID)
                .orElseThrow(() -> helper.assertionException("Deep freeze should decode successfully"));
    }

    private static SkillDefinition dazingTap(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(DAZING_TAP_SKILL_ID)
                .orElseThrow(() -> helper.assertionException("Dazing tap should decode successfully"));
    }

    private static SkillDefinition concussiveSlam(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(CONCUSSIVE_SLAM_SKILL_ID)
                .orElseThrow(() -> helper.assertionException("Concussive slam should decode successfully"));
    }

    private static SkillDefinition basicStrike(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(BASIC_STRIKE_SKILL_ID)
                .orElseThrow(() -> helper.assertionException("Basic strike should decode successfully"));
    }

    private static AilmentPayload payload(GameTestHelper helper, LivingEntity entity, AilmentType type) {
        return Ailments.get(entity)
                .flatMap(state -> state.get(type))
                .orElseThrow(() -> helper.assertionException(type.serializedName() + " payload should exist on the target"));
    }

    private static net.minecraft.world.effect.MobEffect effect(AilmentType type) {
        return BuiltInRegistries.MOB_EFFECT.getOptional(type.effectId())
                .orElseThrow(() -> new IllegalStateException("Missing ailment effect: " + type.effectId()));
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
}
