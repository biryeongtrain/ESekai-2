package kim.biryeong.esekai2.impl.gametest.skill;

import com.mojang.serialization.JsonOps;
import kim.biryeong.esekai2.api.skill.calculation.SkillCalculationDefinition;
import kim.biryeong.esekai2.api.skill.calculation.SkillCalculationLookup;
import kim.biryeong.esekai2.api.skill.definition.SkillDefinition;
import kim.biryeong.esekai2.api.skill.definition.SkillRegistries;
import kim.biryeong.esekai2.api.skill.tag.SkillTag;
import kim.biryeong.esekai2.api.skill.tag.SkillTagCondition;
import kim.biryeong.esekai2.api.stat.definition.StatDefinition;
import kim.biryeong.esekai2.api.stat.definition.StatRegistries;
import kim.biryeong.esekai2.api.stat.holder.StatHolder;
import kim.biryeong.esekai2.api.stat.holder.StatHolders;
import kim.biryeong.esekai2.api.stat.modifier.ConditionalStatModifier;
import kim.biryeong.esekai2.api.stat.modifier.StatModifier;
import kim.biryeong.esekai2.api.stat.modifier.StatModifierOperation;
import kim.biryeong.esekai2.api.skill.stat.SkillStats;
import kim.biryeong.esekai2.api.skill.execution.SkillUseContext;
import kim.biryeong.esekai2.api.skill.execution.Skills;
import kim.biryeong.esekai2.api.skill.execution.PreparedSkillUse;
import kim.biryeong.esekai2.impl.skill.registry.SkillCalculationRegistryAccess;
import kim.biryeong.esekai2.impl.stat.registry.StatRegistryAccess;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.List;
import java.util.Set;

/**
 * Verifies skill tag handling and runtime stat resolution.
 */
public final class SkillTagGameTests {
    private static final Identifier FIREBALL_SKILL_ID = Identifier.fromNamespaceAndPath("esekai2", "fireball");

    /**
     * Verifies that a skill fixture includes tag metadata in config for conditional systems.
     */
    @GameTest
    public void skillConfigContainsTags(GameTestHelper helper) {
        SkillDefinition definition = skill(helper);
        helper.assertTrue(definition.config().tags().contains(SkillTag.SPELL), "Spell fixture should include the spell tag");
        helper.assertTrue(definition.config().tags().contains(SkillTag.PROJECTILE), "Projectile fixture should include the projectile tag");
        helper.succeed();
    }

    /**
     * Verifies that skill definitions round-trip through the public codec.
     */
    @GameTest
    public void skillDefinitionCodecRoundTrips(GameTestHelper helper) {
        SkillDefinition definition = skill(helper);

        var encoded = SkillDefinition.CODEC.encodeStart(JsonOps.INSTANCE, definition)
                .getOrThrow(message -> new IllegalStateException("Failed to encode skill definition: " + message));
        SkillDefinition decoded = SkillDefinition.CODEC.parse(JsonOps.INSTANCE, encoded)
                .getOrThrow(message -> new IllegalStateException("Failed to decode skill definition: " + message));

        helper.assertValueEqual(decoded.identifier(), definition.identifier(), "Identifier should survive a codec roundtrip");
        helper.assertValueEqual(decoded.config(), definition.config(), "Config should survive a codec roundtrip");
        helper.assertValueEqual(decoded.attached(), definition.attached(), "Attached graph should survive a codec roundtrip");
        helper.succeed();
    }

    /**
     * Verifies that use-context validation remains strict.
     */
    @GameTest
    public void skillUseContextRejectsInvalidRolls(GameTestHelper helper) {
        StatHolder attackerStats = statHolder(helper);
        StatHolder defenderStats = statHolder(helper);

        try {
            new SkillUseContext(attackerStats, defenderStats, List.of(), -0.1, 0.0);
            throw helper.assertionException("SkillUseContext should reject out-of-range hit rolls");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }

        try {
            new SkillUseContext(attackerStats, defenderStats, List.of(), 0.0, 1.0);
            throw helper.assertionException("SkillUseContext should reject out-of-range critical strike rolls");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
        helper.succeed();
    }

    /**
     * Verifies that conditional modifiers still apply to runtime resolved skill values through tags.
     */
    @GameTest
    public void matchingConditionalModifiersAffectRuntimeSkillValues(GameTestHelper helper) {
        PreparedSkillUse prepared = Skills.prepareUse(
                skill(helper),
                skillUseContext(helper, statHolder(helper), statHolder(helper), List.of(
                        new ConditionalStatModifier(
                                new StatModifier(SkillStats.SKILL_RESOURCE_COST, StatModifierOperation.ADD, 7.0, testId("conditional_cost_bonus")),
                                new SkillTagCondition(Set.of(SkillTag.SPELL), Set.of())
                        )
                ), 0.0, 0.0)
        );

        helper.assertValueEqual(prepared.resourceCost(), 19.0, "Conditional modifiers should apply when tags match");
        helper.succeed();
    }

    /**
     * Verifies that excluded skill tags block conditional stat resolution.
     */
    @GameTest
    public void excludedSkillTagsBlockConditionalRuntimeModifiers(GameTestHelper helper) {
        PreparedSkillUse prepared = Skills.prepareUse(
                skill(helper),
                skillUseContext(helper, statHolder(helper), statHolder(helper), List.of(
                        new ConditionalStatModifier(
                                new StatModifier(SkillStats.SKILL_RESOURCE_COST, StatModifierOperation.ADD, 7.0, testId("conditional_cost_blocked")),
                                new SkillTagCondition(Set.of(SkillTag.SPELL), Set.of(SkillTag.PROJECTILE))
                        )
                ), 0.0, 0.0)
        );

        helper.assertValueEqual(prepared.resourceCost(), 12.0, "Conditional modifiers should be blocked by excluded tags");
        helper.succeed();
    }

    /**
     * Verifies that skill stat definitions are loaded from datapacks for this data-driven runtime.
     */
    @GameTest
    public void skillRuntimeStatsLoad(GameTestHelper helper) {
        Registry<StatDefinition> registry = statRegistry(helper);

        helper.assertTrue(registry.containsKey(ResourceKey.create(StatRegistries.STAT, Identifier.fromNamespaceAndPath("esekai2", "skill_resource_cost"))),
                "Skill resource cost stat should be registered");
        helper.assertValueEqual(registry.getOptional(SkillStats.SKILL_RESOURCE_COST).orElseThrow().defaultValue(), 0.0, "Skill cost stat default should remain zero");
        helper.succeed();
    }

    private static SkillDefinition skill(GameTestHelper helper) {
        return skillRegistry(helper).getOptional(FIREBALL_SKILL_ID)
                .orElseThrow(() -> helper.assertionException("fireball fixture should decode successfully"));
    }

    private static Registry<SkillDefinition> skillRegistry(GameTestHelper helper) {
        return helper.getLevel().getServer().registryAccess().lookupOrThrow(SkillRegistries.SKILL);
    }

    private static Registry<StatDefinition> statRegistry(GameTestHelper helper) {
        return helper.getLevel().getServer().registryAccess().lookupOrThrow(StatRegistries.STAT);
    }

    private static Registry<SkillCalculationDefinition> skillCalculationRegistry(GameTestHelper helper) {
        return SkillCalculationRegistryAccess.skillCalculationRegistry(helper);
    }

    private static StatHolder statHolder(GameTestHelper helper) {
        return StatHolders.create(StatRegistryAccess.statRegistry(helper));
    }

    private static SkillUseContext skillUseContext(
            GameTestHelper helper,
            StatHolder attacker,
            StatHolder defender,
            List<ConditionalStatModifier> modifiers,
            double hitRoll,
            double criticalStrikeRoll
    ) {
        return new SkillUseContext(
                attacker,
                defender,
                modifiers,
                hitRoll,
                criticalStrikeRoll,
                SkillCalculationLookup.fromRegistry(skillCalculationRegistry(helper))
        );
    }

    private static Identifier testId(String path) {
        return Identifier.fromNamespaceAndPath("esekai2", "tag_test_" + path);
    }
}
