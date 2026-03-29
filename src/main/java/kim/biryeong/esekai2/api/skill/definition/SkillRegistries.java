package kim.biryeong.esekai2.api.skill.definition;

import kim.biryeong.esekai2.api.skill.calculation.SkillCalculationDefinition;
import kim.biryeong.esekai2.api.skill.value.SkillValueDefinition;
import kim.biryeong.esekai2.api.skill.support.SkillSupportDefinition;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * Public registry keys exposed by the skill API.
 */
public final class SkillRegistries {
    /**
     * Dynamic registry containing {@link SkillDefinition} entries loaded from datapacks.
     */
    public static final ResourceKey<Registry<SkillDefinition>> SKILL = ResourceKey.createRegistryKey(
            Identifier.fromNamespaceAndPath("esekai2", "skill")
    );

    /**
     * Dynamic registry containing datapack-backed skill calculation entries.
     */
    public static final ResourceKey<Registry<SkillCalculationDefinition>> SKILL_CALCULATION = ResourceKey.createRegistryKey(
            Identifier.fromNamespaceAndPath("esekai2", "skill_calculation")
    );

    /**
     * Dynamic registry containing datapack-backed skill value entries.
     */
    public static final ResourceKey<Registry<SkillValueDefinition>> SKILL_VALUE = ResourceKey.createRegistryKey(
            Identifier.fromNamespaceAndPath("esekai2", "skill_value")
    );

    /**
     * Dynamic registry containing datapack-backed skill support entries.
     */
    public static final ResourceKey<Registry<SkillSupportDefinition>> SKILL_SUPPORT = ResourceKey.createRegistryKey(
            Identifier.fromNamespaceAndPath("esekai2", "skill_support")
    );

    private SkillRegistries() {
    }
}
