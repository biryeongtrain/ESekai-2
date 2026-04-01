package kim.biryeong.esekai2.api.skill.execution;

import kim.biryeong.esekai2.api.item.affix.AffixDefinition;
import kim.biryeong.esekai2.api.item.affix.AffixRegistries;
import kim.biryeong.esekai2.api.item.affix.Affixes;
import kim.biryeong.esekai2.api.item.affix.ItemAffixState;
import kim.biryeong.esekai2.api.item.affix.ItemAffixes;
import kim.biryeong.esekai2.api.item.affix.RolledAffix;
import kim.biryeong.esekai2.api.item.socket.SocketLinkGroup;
import kim.biryeong.esekai2.api.item.socket.SocketSlotType;
import kim.biryeong.esekai2.api.item.socket.SocketedSkillItemState;
import kim.biryeong.esekai2.api.item.socket.SocketedSkillRef;
import kim.biryeong.esekai2.api.item.socket.SocketedSkills;
import kim.biryeong.esekai2.api.item.type.ItemFamily;
import kim.biryeong.esekai2.api.skill.calculation.SkillCalculationDefinition;
import kim.biryeong.esekai2.api.skill.calculation.SkillCalculationLookup;
import kim.biryeong.esekai2.api.skill.definition.SkillDefinition;
import kim.biryeong.esekai2.api.skill.definition.SkillRegistries;
import kim.biryeong.esekai2.api.skill.support.SkillSupportDefinition;
import kim.biryeong.esekai2.api.stat.combat.CombatStats;
import kim.biryeong.esekai2.api.stat.holder.StatHolder;
import kim.biryeong.esekai2.api.stat.holder.StatHolders;
import kim.biryeong.esekai2.api.stat.modifier.ConditionalStatModifier;
import kim.biryeong.esekai2.impl.skill.registry.SkillCalculationRegistryAccess;
import kim.biryeong.esekai2.impl.stat.registry.StatRegistryAccess;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Optional;

/**
 * Verifies compatibility-shim behavior for item-stack preparation when no explicit affix registry is available.
 */
public final class SkillsPrepareUseGameTests {
    private static final Identifier BASIC_STRIKE_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "basic_strike");
    private static final Identifier WEAPON_ACCURACY_T1_AFFIX_ID = Identifier.fromNamespaceAndPath("esekai2", "weapon_accuracy_t1");

    /**
     * Verifies that the internal compatibility shim still succeeds without an affix registry when the owner item has no rolled affixes.
     */
    @GameTest
    public void prepareUseItemStackWithoutAffixRegistrySucceedsWhenOwnerHasNoRolledAffixes(GameTestHelper helper) {
        ItemStack stack = basicStrikeStack();
        StatHolder attacker = newHolder(helper);

        PreparedSkillUse prepared = Skills.prepareUse(
                stack,
                skillRegistry(helper),
                supportRegistry(helper),
                Optional.empty(),
                skillUseContext(helper, attacker, newHolder(helper), 0.0, 0.99)
        );

        helper.assertValueEqual(prepared.useContext().attackerStats().resolvedValue(CombatStats.ACCURACY), 0.0,
                "Compatibility shim should preserve base attacker stats when no rolled affixes are present");
        helper.succeed();
    }

    /**
     * Verifies that the internal compatibility shim fails fast without an affix registry once the owner item carries rolled affixes.
     */
    @GameTest
    public void prepareUseItemStackWithoutAffixRegistryFailsWhenOwnerHasRolledAffixes(GameTestHelper helper) {
        ItemStack stack = basicStrikeStack();
        ItemAffixes.set(stack, new ItemAffixState(List.of(roll(helper, WEAPON_ACCURACY_T1_AFFIX_ID, 1.0))));

        try {
            Skills.prepareUse(
                    stack,
                    skillRegistry(helper),
                    supportRegistry(helper),
                    Optional.empty(),
                    skillUseContext(helper, newHolder(helper), newHolder(helper), 0.0, 0.99)
            );
            throw helper.assertionException("Compatibility shim should fail fast when rolled affixes exist without a live affix registry");
        } catch (IllegalArgumentException expected) {
            helper.assertTrue(expected.getMessage().contains("explicit affix registry"),
                    "Failure should explain that an explicit affix registry or live server is required");
        }
        helper.succeed();
    }

    private static RolledAffix roll(GameTestHelper helper, Identifier affixId, double normalizedRoll) {
        AffixDefinition definition = affixRegistry(helper).getOptional(affixId)
                .orElseThrow(() -> helper.assertionException("Affix should decode successfully: " + affixId));
        return Affixes.roll(affixId, ItemFamily.WEAPON, 20, definition, normalizedRoll);
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
}
