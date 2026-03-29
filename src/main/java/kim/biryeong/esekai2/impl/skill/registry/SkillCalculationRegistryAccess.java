package kim.biryeong.esekai2.impl.skill.registry;

import kim.biryeong.esekai2.api.skill.calculation.SkillCalculationDefinition;
import kim.biryeong.esekai2.api.skill.definition.SkillRegistries;
import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;

/**
 * Shared accessors for the datapack-backed skill calculation registry.
 */
public final class SkillCalculationRegistryAccess {
    private SkillCalculationRegistryAccess() {
    }

    public static Registry<SkillCalculationDefinition> skillCalculationRegistry(MinecraftServer server) {
        return server.registryAccess().lookupOrThrow(SkillRegistries.SKILL_CALCULATION);
    }

    public static Registry<SkillCalculationDefinition> skillCalculationRegistry(GameTestHelper helper) {
        return skillCalculationRegistry(helper.getLevel().getServer());
    }
}
