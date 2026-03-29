package kim.biryeong.esekai2.impl.skill.registry;

import kim.biryeong.esekai2.api.skill.definition.SkillRegistries;
import kim.biryeong.esekai2.api.skill.value.SkillValueDefinition;
import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;

/**
 * Shared accessors for the datapack-backed skill value registry.
 */
public final class SkillValueRegistryAccess {
    private SkillValueRegistryAccess() {
    }

    public static Registry<SkillValueDefinition> skillValueRegistry(MinecraftServer server) {
        return server.registryAccess().lookupOrThrow(SkillRegistries.SKILL_VALUE);
    }

    public static Registry<SkillValueDefinition> skillValueRegistry(GameTestHelper helper) {
        return skillValueRegistry(helper.getLevel().getServer());
    }
}
