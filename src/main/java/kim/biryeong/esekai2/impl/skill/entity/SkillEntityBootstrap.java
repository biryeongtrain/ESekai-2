package kim.biryeong.esekai2.impl.skill.entity;

import kim.biryeong.esekai2.impl.skill.execution.SkillRuntimeManager;

/**
 * Registers skill runtime carrier entity types during mod initialization.
 */
public final class SkillEntityBootstrap {
    private static boolean bootstrapped;

    private SkillEntityBootstrap() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }

        SkillRuntimeEntityTypes.bootstrap();
        SkillRuntimeManager.bootstrap();
        bootstrapped = true;
    }
}
