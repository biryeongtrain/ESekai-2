package kim.biryeong.esekai2.impl.monster.level;

import kim.biryeong.esekai2.api.monster.level.MonsterLevelDefinition;
import kim.biryeong.esekai2.api.monster.level.MonsterLevelRegistries;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;

/**
 * Registers the monster level dynamic registry once during mod initialization.
 */
public final class MonsterLevelBootstrap {
    private static boolean bootstrapped;

    private MonsterLevelBootstrap() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }

        DynamicRegistries.register(MonsterLevelRegistries.MONSTER_LEVEL, MonsterLevelDefinition.CODEC);
        bootstrapped = true;
    }
}
