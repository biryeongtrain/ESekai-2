package kim.biryeong.esekai2.api.skill.stat;

import kim.biryeong.esekai2.api.stat.definition.StatDefinition;
import kim.biryeong.esekai2.api.stat.definition.StatRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * Canonical skill runtime stat keys used by ESekai's current skill execution layer.
 */
public final class SkillStats {
    /**
     * Final-value skill resource cost axis resolved from the skill base cost and runtime modifiers.
     */
    public static final ResourceKey<StatDefinition> SKILL_RESOURCE_COST = stat("skill_resource_cost");

    /**
     * Final-value skill use time axis resolved from the skill base use time and runtime modifiers.
     */
    public static final ResourceKey<StatDefinition> SKILL_USE_TIME_TICKS = stat("skill_use_time_ticks");

    /**
     * Final-value skill cooldown axis resolved from the skill base cooldown and runtime modifiers.
     */
    public static final ResourceKey<StatDefinition> SKILL_COOLDOWN_TICKS = stat("skill_cooldown_ticks");

    private SkillStats() {
    }

    private static ResourceKey<StatDefinition> stat(String path) {
        return ResourceKey.create(StatRegistries.STAT, Identifier.fromNamespaceAndPath("esekai2", path));
    }
}
