package kim.biryeong.esekai2.impl.stat.registry;

import kim.biryeong.esekai2.api.stat.definition.StatDefinition;
import kim.biryeong.esekai2.api.stat.definition.StatRegistries;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;

public final class StatBootstrap {
    private static boolean bootstrapped;

    private StatBootstrap() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }

        DynamicRegistries.register(StatRegistries.STAT, StatDefinition.CODEC);
        bootstrapped = true;
    }
}
