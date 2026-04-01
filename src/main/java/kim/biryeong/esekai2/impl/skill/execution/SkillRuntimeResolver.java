package kim.biryeong.esekai2.impl.skill.execution;

import kim.biryeong.esekai2.api.skill.definition.SkillDefinition;
import kim.biryeong.esekai2.api.skill.execution.SkillUseContext;
import kim.biryeong.esekai2.api.skill.stat.SkillStats;
import kim.biryeong.esekai2.api.skill.tag.SkillTag;
import kim.biryeong.esekai2.api.skill.tag.SkillTags;
import kim.biryeong.esekai2.api.stat.definition.StatDefinition;
import kim.biryeong.esekai2.api.stat.holder.StatHolder;
import kim.biryeong.esekai2.api.stat.modifier.ConditionalStatModifier;
import kim.biryeong.esekai2.api.stat.modifier.ConditionalStatModifiers;
import kim.biryeong.esekai2.api.stat.value.StatInstance;
import net.minecraft.resources.ResourceKey;

import java.util.Objects;
import java.util.Set;

/**
 * Resolves skill runtime values from a static skill definition and use-time state.
 */
public final class SkillRuntimeResolver {
    private SkillRuntimeResolver() {
    }

    public static double resolveResourceCost(SkillDefinition skill, SkillUseContext context) {
        return resolveStatValue(skill, context, SkillStats.SKILL_RESOURCE_COST, skill.config().resourceCost());
    }

    public static String resolveResource(SkillDefinition skill) {
        return skill.config().resource();
    }

    public static int resolveUseTimeTicks(SkillDefinition skill, SkillUseContext context) {
        return toWholeTicks(resolveStatValue(skill, context, SkillStats.SKILL_USE_TIME_TICKS, skill.config().castTimeTicks()));
    }

    public static int resolveCooldownTicks(SkillDefinition skill, SkillUseContext context) {
        return toWholeTicks(resolveStatValue(skill, context, SkillStats.SKILL_COOLDOWN_TICKS, skill.config().cooldownTicks()));
    }

    public static Set<SkillTag> resolveTags(SkillDefinition skill) {
        return SkillTags.copyOf(skill.config().tags());
    }

    private static double resolveStatValue(
            SkillDefinition skill,
            SkillUseContext context,
            ResourceKey<StatDefinition> stat,
            double skillBaseValue
    ) {
        StatHolder attackerStats = context.attackerStats();
        StatInstance runtimeInstance = attackerStats.stat(stat);
        runtimeInstance = runtimeInstance.withBaseValue(runtimeInstance.baseValue() + skillBaseValue);

        for (ConditionalStatModifier modifier : context.conditionalModifiers()) {
            if (modifier.modifier().stat().equals(stat) && ConditionalStatModifiers.matches(resolveTags(skill), modifier)) {
                runtimeInstance = runtimeInstance.withModifier(modifier.modifier());
            }
        }

        return runtimeInstance.resolvedValue();
    }

    private static int toWholeTicks(double value) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException("resolved skill tick value must be finite and non-negative");
        }
        double ceiledValue = Math.ceil(value);
        if (ceiledValue > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("resolved skill tick value exceeds supported integer range: " + ceiledValue);
        }

        return (int) ceiledValue;
    }
}
