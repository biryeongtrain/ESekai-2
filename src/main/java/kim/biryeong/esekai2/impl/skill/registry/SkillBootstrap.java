package kim.biryeong.esekai2.impl.skill.registry;

import kim.biryeong.esekai2.api.skill.calculation.SkillCalculationDefinition;
import kim.biryeong.esekai2.api.skill.definition.SkillDefinition;
import kim.biryeong.esekai2.api.skill.definition.SkillRegistries;
import kim.biryeong.esekai2.api.skill.value.SkillValueDefinition;
import kim.biryeong.esekai2.api.skill.support.SkillSupportDefinition;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;

/**
 * Registers the skill dynamic registry once during mod initialization.
 */
public final class SkillBootstrap {
    private static boolean bootstrapped;

    private SkillBootstrap() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }

        DynamicRegistries.register(SkillRegistries.SKILL, SkillDefinition.CODEC);
        DynamicRegistries.register(SkillRegistries.SKILL_CALCULATION, SkillCalculationDefinition.CODEC);
        DynamicRegistries.register(SkillRegistries.SKILL_VALUE, SkillValueDefinition.CODEC);
        DynamicRegistries.register(SkillRegistries.SKILL_SUPPORT, SkillSupportDefinition.CODEC);
        bootstrapped = true;
    }
}
