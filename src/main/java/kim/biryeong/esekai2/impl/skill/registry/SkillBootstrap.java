package kim.biryeong.esekai2.impl.skill.registry;

import kim.biryeong.esekai2.api.skill.definition.SkillDefinition;
import kim.biryeong.esekai2.api.skill.definition.SkillRegistries;
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
        bootstrapped = true;
    }
}
