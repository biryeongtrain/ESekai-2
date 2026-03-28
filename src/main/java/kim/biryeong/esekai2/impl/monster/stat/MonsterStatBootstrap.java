package kim.biryeong.esekai2.impl.monster.stat;

import kim.biryeong.esekai2.api.monster.stat.MonsterRegistries;
import kim.biryeong.esekai2.api.monster.stat.MonsterStatDefinition;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;

/**
 * Registers the monster stat dynamic registry once during mod initialization.
 */
public final class MonsterStatBootstrap {
    private static boolean bootstrapped;

    private MonsterStatBootstrap() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }

        DynamicRegistries.register(MonsterRegistries.MONSTER_STAT, MonsterStatDefinition.CODEC);
        bootstrapped = true;
    }
}
