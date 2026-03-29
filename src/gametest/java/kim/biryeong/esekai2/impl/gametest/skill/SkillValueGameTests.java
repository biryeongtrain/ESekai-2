package kim.biryeong.esekai2.impl.gametest.skill;

import com.mojang.serialization.JsonOps;
import kim.biryeong.esekai2.api.skill.calculation.SkillCalculationLookup;
import kim.biryeong.esekai2.api.skill.definition.SkillRegistries;
import kim.biryeong.esekai2.api.skill.execution.SkillUseContext;
import kim.biryeong.esekai2.api.skill.execution.Skills;
import kim.biryeong.esekai2.api.skill.value.SkillLiteralValueExpression;
import kim.biryeong.esekai2.api.skill.value.SkillStatValueExpression;
import kim.biryeong.esekai2.api.skill.value.SkillValueDefinition;
import kim.biryeong.esekai2.api.skill.value.SkillValueExpression;
import kim.biryeong.esekai2.api.skill.value.SkillValueLookup;
import kim.biryeong.esekai2.api.skill.value.SkillValueReferenceExpression;
import kim.biryeong.esekai2.api.stat.holder.StatHolder;
import kim.biryeong.esekai2.api.stat.holder.StatHolders;
import kim.biryeong.esekai2.api.skill.stat.SkillStats;
import kim.biryeong.esekai2.impl.skill.registry.SkillCalculationRegistryAccess;
import kim.biryeong.esekai2.impl.stat.registry.StatRegistryAccess;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Map;

/**
 * Verifies the reusable typed skill value registry and value expression codec surface.
 */
public final class SkillValueGameTests {
    private static final Identifier RESOURCE_COST_SNAPSHOT_ID = Identifier.fromNamespaceAndPath("esekai2", "skill_resource_cost_snapshot");
    private static final Identifier RESOURCE_COST_SNAPSHOT_ALIAS_ID = Identifier.fromNamespaceAndPath("esekai2", "skill_resource_cost_snapshot_alias");

    /**
     * Verifies that the skill value registry loads the sample value fixtures.
     */
    @GameTest
    public void skillValueRegistryLoadsSampleDefinitions(GameTestHelper helper) {
        Registry<SkillValueDefinition> registry = valueRegistry(helper);

        helper.assertTrue(registry.containsKey(RESOURCE_COST_SNAPSHOT_ID), "Stat-backed skill value fixture should be present");
        helper.assertTrue(registry.containsKey(RESOURCE_COST_SNAPSHOT_ALIAS_ID), "Reference-backed skill value fixture should be present");
        helper.succeed();
    }

    /**
     * Verifies that a stat-backed skill value survives codec round-trip.
     */
    @GameTest
    public void skillValueDefinitionCodecRoundTrips(GameTestHelper helper) {
        SkillValueDefinition definition = value(helper, RESOURCE_COST_SNAPSHOT_ID);

        var encoded = SkillValueDefinition.CODEC.encodeStart(JsonOps.INSTANCE, definition)
                .getOrThrow(message -> new IllegalStateException("Failed to encode skill value definition: " + message));
        SkillValueDefinition decoded = SkillValueDefinition.CODEC.parse(JsonOps.INSTANCE, encoded)
                .getOrThrow(message -> new IllegalStateException("Failed to decode skill value definition: " + message));

        helper.assertValueEqual(decoded, definition, "Skill value definition codec should preserve typed expressions");
        helper.succeed();
    }

    /**
     * Verifies that literal, reference, and stat value expressions survive codec round-trip.
     */
    @GameTest
    public void skillValueExpressionCodecRoundTrips(GameTestHelper helper) {
        SkillValueExpression literal = new SkillLiteralValueExpression(4.0);
        SkillValueExpression reference = new SkillValueReferenceExpression(RESOURCE_COST_SNAPSHOT_ID);
        SkillValueExpression stat = new SkillStatValueExpression(Identifier.fromNamespaceAndPath("esekai2", "skill_resource_cost"));

        helper.assertValueEqual(
                SkillValueExpression.CODEC.parse(JsonOps.INSTANCE,
                        SkillValueExpression.CODEC.encodeStart(JsonOps.INSTANCE, literal)
                                .getOrThrow(message -> new IllegalStateException("Failed to encode literal value expression: " + message)))
                        .getOrThrow(message -> new IllegalStateException("Failed to decode literal value expression: " + message)),
                literal,
                "Literal value expression should round-trip through the public codec"
        );
        helper.assertValueEqual(
                SkillValueExpression.CODEC.parse(JsonOps.INSTANCE,
                        SkillValueExpression.CODEC.encodeStart(JsonOps.INSTANCE, reference)
                                .getOrThrow(message -> new IllegalStateException("Failed to encode reference value expression: " + message)))
                        .getOrThrow(message -> new IllegalStateException("Failed to decode reference value expression: " + message)),
                reference,
                "Reference value expression should round-trip through the public codec"
        );
        helper.assertValueEqual(
                SkillValueExpression.CODEC.parse(JsonOps.INSTANCE,
                        SkillValueExpression.CODEC.encodeStart(JsonOps.INSTANCE, stat)
                                .getOrThrow(message -> new IllegalStateException("Failed to encode stat value expression: " + message)))
                        .getOrThrow(message -> new IllegalStateException("Failed to decode stat value expression: " + message)),
                stat,
                "Stat value expression should round-trip through the public codec"
        );
        helper.succeed();
    }

    /**
     * Verifies that stat-backed skill values and reference-backed aliases resolve against runtime lookup state.
     */
    @GameTest
    public void skillValueResolutionUsesStatAndReferenceLookups(GameTestHelper helper) {
        StatHolder attacker = StatHolders.create(StatRegistryAccess.statRegistry(helper));
        attacker.setBaseValue(SkillStats.SKILL_RESOURCE_COST, 14.0);

        SkillValueDefinition statBackedValue = value(helper, RESOURCE_COST_SNAPSHOT_ID);
        SkillValueDefinition aliasValue = value(helper, RESOURCE_COST_SNAPSHOT_ALIAS_ID);

        SkillUseContext context = new SkillUseContext(
                attacker,
                StatHolders.create(StatRegistryAccess.statRegistry(helper)),
                List.of(),
                0.0,
                0.0,
                SkillCalculationLookup.fromRegistry(SkillCalculationRegistryAccess.skillCalculationRegistry(helper)),
                SkillValueLookup.fromRegistry(valueRegistry(helper))
        );

        helper.assertValueEqual(statBackedValue.resolve(context), 14.0, "Stat-backed skill value should read from the attacker stat holder");
        helper.assertValueEqual(aliasValue.resolve(context), 14.0, "Reference-backed skill value should resolve through the value registry");
        helper.succeed();
    }

    private static Registry<SkillValueDefinition> valueRegistry(GameTestHelper helper) {
        return helper.getLevel().getServer().registryAccess().lookupOrThrow(SkillRegistries.SKILL_VALUE);
    }

    private static SkillValueDefinition value(GameTestHelper helper, Identifier id) {
        return valueRegistry(helper).getOptional(id)
                .orElseThrow(() -> helper.assertionException("Skill value should decode successfully: " + id));
    }
}
