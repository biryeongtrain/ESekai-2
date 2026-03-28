package kim.biryeong.esekai2.api.skill.definition;

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

    private SkillRegistries() {
    }
}
