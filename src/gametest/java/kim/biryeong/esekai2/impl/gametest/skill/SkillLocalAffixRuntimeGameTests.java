package kim.biryeong.esekai2.impl.gametest.skill;

import kim.biryeong.esekai2.api.item.affix.AffixDefinition;
import kim.biryeong.esekai2.api.item.affix.AffixRegistries;
import kim.biryeong.esekai2.api.item.affix.AffixScope;
import kim.biryeong.esekai2.api.item.affix.Affixes;
import kim.biryeong.esekai2.api.item.affix.ItemAffixState;
import kim.biryeong.esekai2.api.item.affix.ItemAffixes;
import kim.biryeong.esekai2.api.item.affix.RolledAffix;
import kim.biryeong.esekai2.api.item.socket.SocketLinkGroup;
import kim.biryeong.esekai2.api.item.socket.SocketSlotType;
import kim.biryeong.esekai2.api.item.socket.SocketedEquipmentSlot;
import kim.biryeong.esekai2.api.item.socket.SocketedSkillItemState;
import kim.biryeong.esekai2.api.item.socket.SocketedSkillRef;
import kim.biryeong.esekai2.api.item.socket.SocketedSkills;
import kim.biryeong.esekai2.api.item.type.ItemFamily;
import kim.biryeong.esekai2.api.player.skill.PlayerActiveSkills;
import kim.biryeong.esekai2.api.player.skill.SelectedActiveSkillRef;
import kim.biryeong.esekai2.api.skill.calculation.SkillCalculationDefinition;
import kim.biryeong.esekai2.api.skill.calculation.SkillCalculationLookup;
import kim.biryeong.esekai2.api.skill.definition.SkillDefinition;
import kim.biryeong.esekai2.api.skill.definition.SkillRegistries;
import kim.biryeong.esekai2.api.skill.execution.PreparedDamageAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSkillAction;
import kim.biryeong.esekai2.api.skill.execution.PreparedSkillUse;
import kim.biryeong.esekai2.api.skill.execution.SelectedSkillUseResult;
import kim.biryeong.esekai2.api.skill.execution.SkillUseContext;
import kim.biryeong.esekai2.api.skill.execution.Skills;
import kim.biryeong.esekai2.api.skill.stat.SkillStats;
import kim.biryeong.esekai2.api.skill.support.SkillSupportDefinition;
import kim.biryeong.esekai2.api.stat.combat.CombatStats;
import kim.biryeong.esekai2.api.stat.definition.StatDefinition;
import kim.biryeong.esekai2.api.stat.holder.StatHolder;
import kim.biryeong.esekai2.api.stat.holder.StatHolders;
import kim.biryeong.esekai2.api.stat.modifier.ConditionalStatModifier;
import kim.biryeong.esekai2.impl.runtime.ServerRuntimeAccess;
import kim.biryeong.esekai2.impl.skill.registry.SkillCalculationRegistryAccess;
import kim.biryeong.esekai2.impl.stat.registry.StatRegistryAccess;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Verifies that LOCAL affixes stay owner-item scoped and only affect socket-backed preparation paths.
 */
public final class SkillLocalAffixRuntimeGameTests {
    private static final Identifier BASIC_STRIKE_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "basic_strike");
    private static final Identifier BATTLE_FOCUS_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "battle_focus");
    private static final Identifier WEAPON_ACCURACY_T1_AFFIX_ID = Identifier.fromNamespaceAndPath("esekai2", "weapon_accuracy_t1");

    /**
     * Verifies that prepareUse(ItemStack, ...) applies the owner item's LOCAL weapon affix as an attacker-only overlay.
     */
    @GameTest
    public void prepareUseItemStackAppliesLocalWeaponOwnerOverlay(GameTestHelper helper) {
        ItemStack stack = basicStrikeStack(Items.IRON_SWORD);
        ItemAffixes.set(stack, new ItemAffixState(List.of(roll(helper, WEAPON_ACCURACY_T1_AFFIX_ID, ItemFamily.WEAPON, 1.0))));

        StatHolder attacker = newHolder(helper);
        PreparedSkillUse prepared = Skills.prepareUse(
                stack,
                skillRegistry(helper),
                supportRegistry(helper),
                skillUseContext(helper, attacker, newHolder(helper), 0.0, 0.99)
        );
        PreparedDamageAction damage = preparedDamageAction(helper, prepared);

        helper.assertValueEqual(attacker.resolvedValue(CombatStats.ACCURACY), 0.0,
                "Owner-item local overlay should not mutate the caller's attacker stat holder");
        helper.assertTrue(prepared.useContext().attackerStats().resolvedValue(CombatStats.ACCURACY) > 0.0,
                "Socket-backed prepareUse(ItemStack, ...) should expose positive local weapon accuracy on the prepared attacker context");
        helper.assertTrue(damage.hitDamageCalculation().attackerStats().resolvedValue(CombatStats.ACCURACY) > 0.0,
                "Prepared attack damage should read the local weapon accuracy overlay from the owner item");
        helper.succeed();
    }

    /**
     * Verifies that the explicit affix-registry overload still applies LOCAL owner-item overlays without any live server singleton lookup.
     */
    @GameTest
    public void prepareUseItemStackExplicitAffixRegistryOverloadAppliesLocalWeaponOwnerOverlayWithoutLiveServerSingleton(
            GameTestHelper helper
    ) {
        ItemStack stack = basicStrikeStack(Items.IRON_SWORD);
        ItemAffixes.set(stack, new ItemAffixState(List.of(roll(helper, WEAPON_ACCURACY_T1_AFFIX_ID, ItemFamily.WEAPON, 1.0))));

        StatHolder attacker = newHolder(helper);
        ServerRuntimeAccess.setCurrentForTesting(null);
        try {
            PreparedSkillUse prepared = Skills.prepareUse(
                    stack,
                    skillRegistry(helper),
                    supportRegistry(helper),
                    affixRegistry(helper),
                    skillUseContext(helper, attacker, newHolder(helper), 0.0, 0.99)
            );
            PreparedDamageAction damage = preparedDamageAction(helper, prepared);

            helper.assertValueEqual(attacker.resolvedValue(CombatStats.ACCURACY), 0.0,
                    "Explicit affix-registry overload should not mutate the caller's attacker stat holder");
            helper.assertTrue(prepared.useContext().attackerStats().resolvedValue(CombatStats.ACCURACY) > 0.0,
                    "Explicit affix-registry overload should expose positive local weapon accuracy on the prepared attacker context");
            helper.assertTrue(damage.hitDamageCalculation().attackerStats().resolvedValue(CombatStats.ACCURACY) > 0.0,
                    "Prepared attack damage should still read the local weapon accuracy overlay without a live server singleton");
        } finally {
            ServerRuntimeAccess.clearCurrentForTesting();
        }
        helper.succeed();
    }

    /**
     * Verifies that the explicit affix-registry prepareUse overload applies the owner item's LOCAL overlay without using singleton fallback.
     */
    @GameTest
    public void prepareUseItemStackWithExplicitAffixRegistryAppliesLocalWeaponOwnerOverlay(GameTestHelper helper) {
        ItemStack stack = basicStrikeStack(Items.IRON_SWORD);
        ItemAffixes.set(stack, new ItemAffixState(List.of(roll(helper, WEAPON_ACCURACY_T1_AFFIX_ID, ItemFamily.WEAPON, 1.0))));

        StatHolder attacker = newHolder(helper);
        PreparedSkillUse prepared = Skills.prepareUse(
                stack,
                skillRegistry(helper),
                supportRegistry(helper),
                affixRegistry(helper),
                skillUseContext(helper, attacker, newHolder(helper), 0.0, 0.99)
        );
        PreparedDamageAction damage = preparedDamageAction(helper, prepared);

        helper.assertValueEqual(attacker.resolvedValue(CombatStats.ACCURACY), 0.0,
                "Explicit affix-registry preparation should not mutate the caller's attacker stat holder");
        helper.assertTrue(prepared.useContext().attackerStats().resolvedValue(CombatStats.ACCURACY) > 0.0,
                "Explicit affix-registry preparation should expose positive local weapon accuracy on the prepared attacker context");
        helper.assertTrue(damage.hitDamageCalculation().attackerStats().resolvedValue(CombatStats.ACCURACY) > 0.0,
                "Prepared attack damage should read the explicit owner-item local weapon overlay");
        helper.succeed();
    }

    /**
     * Verifies that the explicit affix-registry overload applies the owner item's LOCAL overlay without relying on live server singleton state.
     */
    @GameTest
    public void prepareUseItemStackExplicitAffixRegistryAppliesLocalWeaponOwnerOverlay(GameTestHelper helper) {
        ItemStack stack = basicStrikeStack(Items.IRON_SWORD);
        ItemAffixes.set(stack, new ItemAffixState(List.of(roll(helper, WEAPON_ACCURACY_T1_AFFIX_ID, ItemFamily.WEAPON, 1.0))));

        StatHolder attacker = newHolder(helper);
        PreparedSkillUse prepared = Skills.prepareUse(
                stack,
                skillRegistry(helper),
                supportRegistry(helper),
                affixRegistry(helper),
                skillUseContext(helper, attacker, newHolder(helper), 0.0, 0.99)
        );
        PreparedDamageAction damage = preparedDamageAction(helper, prepared);

        helper.assertValueEqual(attacker.resolvedValue(CombatStats.ACCURACY), 0.0,
                "Explicit owner-item local overlay should not mutate the caller's attacker stat holder");
        helper.assertTrue(prepared.useContext().attackerStats().resolvedValue(CombatStats.ACCURACY) > 0.0,
                "Explicit affix-registry prepareUse(ItemStack, ...) should expose positive local weapon accuracy on the prepared attacker context");
        helper.assertTrue(damage.hitDamageCalculation().attackerStats().resolvedValue(CombatStats.ACCURACY) > 0.0,
                "Prepared attack damage should read the explicit local weapon accuracy overlay from the owner item");
        helper.succeed();
    }

    /**
     * Verifies that raw prepareUse(skill, context) remains a no-op for LOCAL affixes because no owner item was provided.
     */
    @GameTest
    public void rawPrepareUseDoesNotApplyLocalOwnerAffixWithoutSourceItem(GameTestHelper helper) {
        PreparedSkillUse prepared = Skills.prepareUse(
                basicStrike(helper),
                skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
        );
        PreparedDamageAction damage = preparedDamageAction(helper, prepared);

        helper.assertValueEqual(prepared.useContext().attackerStats().resolvedValue(CombatStats.ACCURACY), 0.0,
                "Raw prepareUse(skill, context) should keep attacker accuracy unchanged without an owner item");
        helper.assertValueEqual(damage.hitDamageCalculation().attackerStats().resolvedValue(CombatStats.ACCURACY), 0.0,
                "Raw prepareUse(skill, context) should not inject local weapon accuracy without a socket owner");
        helper.succeed();
    }

    /**
     * Verifies that selected active skill preparation reuses the equipped owner item and applies its LOCAL overlay.
     */
    @GameTest
    public void prepareSelectedUseAppliesLocalAffixFromSelectedOwnerItem(GameTestHelper helper) {
        ServerPlayer player = kim.biryeong.esekai2.impl.gametest.support.GameTestPlayers.create(helper);
        ItemStack stack = basicStrikeStack(Items.IRON_SWORD);
        ItemAffixes.set(stack, new ItemAffixState(List.of(roll(helper, WEAPON_ACCURACY_T1_AFFIX_ID, ItemFamily.WEAPON, 1.0))));
        player.setItemSlot(SocketedEquipmentSlot.MAIN_HAND.equipmentSlot(), stack);
        PlayerActiveSkills.select(player, new SelectedActiveSkillRef(SocketedEquipmentSlot.MAIN_HAND, BASIC_STRIKE_SKILL_ID));

        StatHolder attacker = newHolder(helper);
        SelectedSkillUseResult result = Skills.prepareSelectedUse(
                player,
                skillUseContext(helper, attacker, newHolder(helper), 0.0, 0.99)
        );

        helper.assertTrue(result.success(), "Selected preparation should succeed when the equipped owner item resolves correctly");
        PreparedDamageAction damage = preparedDamageAction(helper, result.preparedUse().orElseThrow());
        helper.assertValueEqual(attacker.resolvedValue(CombatStats.ACCURACY), 0.0,
                "Selected preparation should still leave the caller's base attacker holder unchanged");
        helper.assertTrue(result.preparedUse().orElseThrow().useContext().attackerStats().resolvedValue(CombatStats.ACCURACY) > 0.0,
                "Selected preparation should expose the equipped owner item's local weapon accuracy on the prepared attacker context");
        helper.assertTrue(damage.hitDamageCalculation().attackerStats().resolvedValue(CombatStats.ACCURACY) > 0.0,
                "Selected preparation should feed the local owner overlay into prepared attack damage");
        helper.succeed();
    }

    /**
     * Verifies that non-weapon LOCAL affixes also work on prepareUse(ItemStack, ...) so owner-skill semantics are family-general.
     */
    @GameTest
    public void prepareUseItemStackAppliesNonWeaponLocalOwnerOverlay(GameTestHelper helper) {
        LocalRuntimeAffixFixture fixture = findNonWeaponLocalRuntimeAffix(helper)
                .orElseThrow(() -> helper.assertionException(
                        "Expected one LOCAL armour or trinket affix that modifies skill_resource_cost, skill_use_time_ticks, or skill_cooldown_ticks"
                ));
        ItemStack baselineStack = ownerSkillStackForFamily(fixture.family());
        ItemStack affixedStack = ownerSkillStackForFamily(fixture.family());
        ItemAffixes.set(affixedStack, new ItemAffixState(List.of(roll(helper, fixture.affixId(), fixture.family(), 1.0))));

        StatHolder attacker = newHolder(helper);
        PreparedSkillUse baseline = Skills.prepareUse(
                baselineStack,
                skillRegistry(helper),
                supportRegistry(helper),
                skillUseContext(helper, attacker, newHolder(helper), 0.0, 0.0)
        );
        PreparedSkillUse affixed = Skills.prepareUse(
                affixedStack,
                skillRegistry(helper),
                supportRegistry(helper),
                skillUseContext(helper, attacker, newHolder(helper), 0.0, 0.0)
        );

        helper.assertTrue(fixture.family() != ItemFamily.WEAPON,
                "The non-weapon owner-path fixture must not resolve back to the weapon family");
        assertPreparedSkillStatChanged(helper, fixture.stat(), baseline, affixed, fixture.affixId(), fixture.family());
        helper.succeed();
    }

    /**
     * Verifies that the compatibility overload still prepares items without affix state even when no live server singleton is available.
     */
    @GameTest
    public void prepareUseItemStackWithoutLiveServerStillSucceedsWhenOwnerItemHasNoAffixState(GameTestHelper helper) {
        ItemStack stack = basicStrikeStack(Items.IRON_SWORD);

        ServerRuntimeAccess.setCurrentForTesting(null);
        try {
            PreparedSkillUse prepared = Skills.prepareUse(
                    stack,
                    skillRegistry(helper),
                    supportRegistry(helper),
                    skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
            );
            PreparedDamageAction damage = preparedDamageAction(helper, prepared);

            helper.assertValueEqual(prepared.useContext().attackerStats().resolvedValue(CombatStats.ACCURACY), 0.0,
                    "Compatibility overload should keep attacker accuracy unchanged when no affix state is present");
            helper.assertValueEqual(damage.hitDamageCalculation().attackerStats().resolvedValue(CombatStats.ACCURACY), 0.0,
                    "Compatibility overload should still prepare the skill successfully when no affix state is present");
        } finally {
            ServerRuntimeAccess.clearCurrentForTesting();
        }
        helper.succeed();
    }

    /**
     * Verifies that the compatibility overload fails fast when an affixed owner item is prepared without a live server singleton.
     */
    @GameTest
    public void prepareUseItemStackWithoutLiveServerFailsFastWhenOwnerItemHasAffixState(GameTestHelper helper) {
        ItemStack stack = basicStrikeStack(Items.IRON_SWORD);
        ItemAffixes.set(stack, new ItemAffixState(List.of(roll(helper, WEAPON_ACCURACY_T1_AFFIX_ID, ItemFamily.WEAPON, 1.0))));

        ServerRuntimeAccess.setCurrentForTesting(null);
        try {
            try {
                Skills.prepareUse(
                        stack,
                        skillRegistry(helper),
                        supportRegistry(helper),
                        skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
                );
                throw helper.assertionException(
                        "Compatibility overload should fail fast when owner-item affix state exists without a live server singleton"
                );
            } catch (IllegalArgumentException expected) {
                helper.assertTrue(expected.getMessage().contains("explicit affix registry"),
                        "Fail-fast message should instruct callers to use the explicit affix-registry overload");
            }
        } finally {
            ServerRuntimeAccess.clearCurrentForTesting();
        }
        helper.succeed();
    }

    private static void assertPreparedSkillStatChanged(
            GameTestHelper helper,
            ResourceKey<StatDefinition> stat,
            PreparedSkillUse baseline,
            PreparedSkillUse affixed,
            Identifier affixId,
            ItemFamily family
    ) {
        if (stat.equals(SkillStats.SKILL_RESOURCE_COST)) {
            helper.assertTrue(affixed.resourceCost() != baseline.resourceCost(),
                    "Local " + family.serializedName() + " affix " + affixId + " should change prepared skill resource cost on the owner-item path");
            return;
        }

        if (stat.equals(SkillStats.SKILL_USE_TIME_TICKS)) {
            helper.assertTrue(affixed.useTimeTicks() != baseline.useTimeTicks(),
                    "Local " + family.serializedName() + " affix " + affixId + " should change prepared use time on the owner-item path");
            return;
        }

        if (stat.equals(SkillStats.SKILL_COOLDOWN_TICKS)) {
            helper.assertTrue(affixed.cooldownTicks() != baseline.cooldownTicks(),
                    "Local " + family.serializedName() + " affix " + affixId + " should change prepared cooldown on the owner-item path");
            return;
        }

        throw helper.assertionException("Unsupported local runtime stat fixture: " + stat);
    }

    private static Optional<LocalRuntimeAffixFixture> findNonWeaponLocalRuntimeAffix(GameTestHelper helper) {
        Registry<AffixDefinition> registry = affixRegistry(helper);
        List<ResourceKey<StatDefinition>> candidateStats = List.of(
                SkillStats.SKILL_USE_TIME_TICKS,
                SkillStats.SKILL_RESOURCE_COST,
                SkillStats.SKILL_COOLDOWN_TICKS
        );

        return registry.keySet().stream()
                .sorted(Comparator.comparing(Identifier::toString))
                .flatMap(affixId -> registry.getOptional(affixId).stream()
                        .flatMap(definition -> candidateFixture(affixId, definition, candidateStats).stream()))
                .findFirst();
    }

    private static Optional<LocalRuntimeAffixFixture> candidateFixture(
            Identifier affixId,
            AffixDefinition definition,
            List<ResourceKey<StatDefinition>> candidateStats
    ) {
        if (definition.scope() != AffixScope.LOCAL) {
            return Optional.empty();
        }

        for (ItemFamily family : List.of(ItemFamily.ARMOUR, ItemFamily.TRINKET)) {
            if (!definition.supports(family)) {
                continue;
            }
            for (var range : definition.modifierRanges()) {
                if (candidateStats.contains(range.stat())) {
                    return Optional.of(new LocalRuntimeAffixFixture(affixId, family, range.stat()));
                }
            }
        }

        return Optional.empty();
    }

    private static PreparedDamageAction preparedDamageAction(GameTestHelper helper, PreparedSkillUse prepared) {
        for (PreparedSkillAction action : prepared.onCastActions()) {
            if (action instanceof PreparedDamageAction damageAction) {
                return damageAction;
            }
        }
        throw helper.assertionException("Prepared skill use should contain one prepared damage action");
    }

    private static RolledAffix roll(GameTestHelper helper, Identifier affixId, ItemFamily family, double normalizedRoll) {
        AffixDefinition definition = affixRegistry(helper).getOptional(affixId)
                .orElseThrow(() -> helper.assertionException("Affix should decode successfully: " + affixId));
        return Affixes.roll(affixId, family, 20, definition, normalizedRoll);
    }

    private static ItemStack ownerSkillStackForFamily(ItemFamily family) {
        return switch (family) {
            case ARMOUR -> battleFocusStack(Items.DIAMOND_HELMET);
            case TRINKET -> battleFocusStack(Items.AMETHYST_SHARD);
            default -> throw new IllegalArgumentException("Unsupported non-weapon family fixture: " + family.serializedName());
        };
    }

    private static ItemStack basicStrikeStack(net.minecraft.world.item.Item item) {
        ItemStack stack = new ItemStack(item);
        SocketedSkills.set(stack, new SocketedSkillItemState(
                Optional.of(BASIC_STRIKE_SKILL_ID),
                1,
                List.of(new SocketLinkGroup(0, List.of(0))),
                List.of(new SocketedSkillRef(0, SocketSlotType.SKILL, BASIC_STRIKE_SKILL_ID))
        ));
        return stack;
    }

    private static ItemStack battleFocusStack(net.minecraft.world.item.Item item) {
        ItemStack stack = new ItemStack(item);
        SocketedSkills.set(stack, new SocketedSkillItemState(
                Optional.of(BATTLE_FOCUS_SKILL_ID),
                1,
                List.of(new SocketLinkGroup(0, List.of(0))),
                List.of(new SocketedSkillRef(0, SocketSlotType.SKILL, BATTLE_FOCUS_SKILL_ID))
        ));
        return stack;
    }

    private static SkillDefinition basicStrike(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(BASIC_STRIKE_SKILL_ID)
                .orElseThrow(() -> helper.assertionException("Basic strike should decode successfully"));
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

    private static Registry<AffixDefinition> affixRegistry(GameTestHelper helper) {
        return helper.getLevel().getServer().registryAccess().lookupOrThrow(AffixRegistries.AFFIX);
    }

    private static Registry<SkillDefinition> skillRegistry(GameTestHelper helper) {
        return helper.getLevel().getServer().registryAccess().lookupOrThrow(SkillRegistries.SKILL);
    }

    private static Registry<SkillSupportDefinition> supportRegistry(GameTestHelper helper) {
        return helper.getLevel().getServer().registryAccess().lookupOrThrow(SkillRegistries.SKILL_SUPPORT);
    }

    private static Registry<SkillCalculationDefinition> skillCalculationRegistry(GameTestHelper helper) {
        return SkillCalculationRegistryAccess.skillCalculationRegistry(helper);
    }

    private record LocalRuntimeAffixFixture(
            Identifier affixId,
            ItemFamily family,
            ResourceKey<StatDefinition> stat
    ) {
    }
}
