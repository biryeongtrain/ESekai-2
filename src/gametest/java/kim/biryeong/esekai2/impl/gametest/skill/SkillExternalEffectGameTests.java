package kim.biryeong.esekai2.impl.gametest.skill;

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
import kim.biryeong.esekai2.api.skill.execution.PreparedApplyBuffAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedApplyDotAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSkillAction;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Verifies buff and generic damage-over-time skill effects on the typed skill graph.
 */
public final class SkillExternalEffectGameTests {
    private static final Identifier BATTLE_FOCUS_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "battle_focus");
    private static final Identifier SEARING_BRAND_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "searing_brand");
    private static final Identifier SUPPORT_LINGERING_BRAND_ID = Identifier.fromNamespaceAndPath("esekai2", "support_lingering_brand");
    private static final Identifier BATTLE_FOCUS_EFFECT_ID = Identifier.fromNamespaceAndPath("minecraft", "speed");

    /**
     * Verifies that the sample buff skill, dot skill, and support fixture load into their registries.
     */
    @GameTest
    public void registryLoadsBuffDotAndSupportFixtures(GameTestHelper helper) {
        helper.assertTrue(skillRegistry(helper).containsKey(BATTLE_FOCUS_SKILL_ID), "Battle focus should load into the skill registry");
        helper.assertTrue(skillRegistry(helper).containsKey(SEARING_BRAND_SKILL_ID), "Searing brand should load into the skill registry");
        helper.assertTrue(supportRegistry(helper).containsKey(SUPPORT_LINGERING_BRAND_ID), "Lingering brand support should load into the support registry");
        helper.succeed();
    }

    /**
     * Verifies that buff and dot actions decode into concrete prepared action payloads.
     */
    @GameTest
    public void prepareUseBuildsBuffAndDotActionsFromConfig(GameTestHelper helper) {
        PreparedSkillUse buffPrepared = Skills.prepareUse(
                battleFocus(helper),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );
        PreparedSkillUse dotPrepared = Skills.prepareUse(
                searingBrand(helper),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );

        PreparedApplyBuffAction buffAction = (PreparedApplyBuffAction) buffPrepared.onCastActions().stream()
                .filter(PreparedApplyBuffAction.class::isInstance)
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Battle focus should prepare one apply_buff action"));
        PreparedApplyDotAction dotAction = (PreparedApplyDotAction) dotPrepared.onCastActions().stream()
                .filter(PreparedApplyDotAction.class::isInstance)
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Searing brand should prepare one apply_dot action"));

        helper.assertValueEqual(buffAction.effectId(), BATTLE_FOCUS_EFFECT_ID, "Prepared buff action should preserve the mob effect id");
        helper.assertValueEqual(buffAction.durationTicks(), 40, "Prepared buff action should resolve duration_ticks");
        helper.assertValueEqual(dotAction.dotId(), "searing_brand", "Prepared dot action should preserve the stable dot id");
        helper.assertValueEqual(dotAction.durationTicks(), 8, "Prepared dot action should resolve duration_ticks");
        helper.assertValueEqual(dotAction.tickIntervalTicks(), 2, "Prepared dot action should resolve tick_interval");
        helper.succeed();
    }

    /**
     * Verifies that executing the sample self-buff skill applies the configured mob effect to the caster.
     */
    @GameTest
    public void executeOnCastAppliesBuffMobEffectToCaster(GameTestHelper helper) {
        Player caster = helper.makeMockPlayer(GameType.CREATIVE);
        PreparedSkillUse prepared = Skills.prepareUse(
                battleFocus(helper),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );

        SkillExecutionResult result = Skills.executeOnCast(
                SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.empty())
        );

        MobEffectInstance effectInstance = caster.getActiveEffects().stream()
                .filter(instance -> BATTLE_FOCUS_EFFECT_ID.equals(BuiltInRegistries.MOB_EFFECT.getKey(instance.getEffect().value())))
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Battle focus should apply the configured mob effect to the caster"));

        helper.assertValueEqual(result.executedActions(), 2, "Battle focus should execute both sound and buff actions");
        helper.assertValueEqual(effectInstance.getAmplifier(), 1, "Battle focus should apply the configured amplifier");
        helper.assertTrue(effectInstance.getDuration() > 0, "Battle focus should apply a positive duration");
        helper.succeed();
    }

    /**
     * Verifies that linked support overrides can change apply_dot field values on the selected cast path.
     */
    @GameTest
    public void prepareSelectedUseAppliesDotSupportOverrides(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemSlot(
                SocketedEquipmentSlot.MAIN_HAND.equipmentSlot(),
                searingBrandStack(List.of(supportRef(1, SUPPORT_LINGERING_BRAND_ID)))
        );
        PlayerActiveSkills.select(player, new SelectedActiveSkillRef(SocketedEquipmentSlot.MAIN_HAND, SEARING_BRAND_SKILL_ID));

        SelectedSkillUseResult result = Skills.prepareSelectedUse(
                player,
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );

        PreparedApplyDotAction dotAction = (PreparedApplyDotAction) result.preparedUse().orElseThrow().onCastActions().stream()
                .filter(PreparedApplyDotAction.class::isInstance)
                .findFirst()
                .orElseThrow(() -> helper.assertionException("Selected searing brand should still expose one apply_dot action"));

        helper.assertTrue(result.success(), "Selected searing brand should prepare successfully");
        helper.assertValueEqual(dotAction.durationTicks(), 16, "Linked support should override apply_dot duration_ticks");
        helper.succeed();
    }

    /**
     * Verifies that executing the sample dot skill damages the target over time and stops once the duration expires.
     */
    @GameTest
    public void executeOnCastAppliesDotDamageAndExpires(GameTestHelper helper) {
        Player caster = helper.makeMockPlayer(GameType.CREATIVE);
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(3.0, 2.0, 3.0));
        PreparedSkillUse prepared = Skills.prepareUse(
                searingBrand(helper),
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
        float[] settledHealth = new float[1];

        helper.assertValueEqual(result.executedActions(), 2, "Searing brand should execute both sound and dot actions");
        helper.runAfterDelay(3, () ->
                helper.assertTrue(zombie.getHealth() < initialHealth, "The first DoT tick should damage the target shortly after cast")
        );
        helper.runAfterDelay(12, () -> settledHealth[0] = zombie.getHealth());
        helper.runAfterDelay(18, () -> {
            helper.assertValueEqual(zombie.getHealth(), settledHealth[0], "The generic DoT should stop dealing damage once its duration expires");
            helper.succeed();
        });
    }

    /**
     * Verifies that reapplying the same dot id refreshes duration instead of expiring on the original timer.
     */
    @GameTest
    public void reapplyingSameDotIdRefreshesDuration(GameTestHelper helper) {
        Player caster = helper.makeMockPlayer(GameType.CREATIVE);
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(3.0, 2.0, 3.0));
        PreparedSkillUse prepared = Skills.prepareUse(
                searingBrand(helper),
                skillUseContext(
                        helper,
                        newHolder(helper),
                        MonsterStats.resolveBaseHolder(zombie).orElse(newHolder(helper)),
                        0.0,
                        0.99
                )
        );

        Skills.executeOnCast(SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.of(zombie)));
        helper.runAfterDelay(7, () ->
                Skills.executeOnCast(SkillExecutionContext.forCast(prepared, helper.getLevel(), caster, Optional.of(zombie)))
        );

        float[] healthAtOriginalExpiry = new float[1];
        helper.runAfterDelay(8, () -> healthAtOriginalExpiry[0] = zombie.getHealth());
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(
                    zombie.getHealth() < healthAtOriginalExpiry[0],
                    "Reapplying the same dot id should refresh duration and continue ticking past the original expiry window"
            );
            helper.succeed();
        });
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

    private static SkillDefinition battleFocus(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(BATTLE_FOCUS_SKILL_ID)
                .orElseThrow(() -> helper.assertionException("Battle focus should decode successfully"));
    }

    private static SkillDefinition searingBrand(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(SEARING_BRAND_SKILL_ID)
                .orElseThrow(() -> helper.assertionException("Searing brand should decode successfully"));
    }

    private static ItemStack searingBrandStack(List<SocketedSkillRef> extraSupports) {
        ItemStack stack = new ItemStack(Items.BLAZE_ROD);
        List<SocketedSkillRef> socketRefs = new java.util.ArrayList<>();
        socketRefs.add(new SocketedSkillRef(0, SocketSlotType.SKILL, SEARING_BRAND_SKILL_ID));
        socketRefs.addAll(extraSupports);
        SocketedSkills.set(stack, new SocketedSkillItemState(
                Optional.of(SEARING_BRAND_SKILL_ID),
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
