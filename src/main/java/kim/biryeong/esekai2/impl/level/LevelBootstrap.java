package kim.biryeong.esekai2.impl.level;

import kim.biryeong.esekai2.api.level.LevelProgressionDefinition;
import kim.biryeong.esekai2.api.level.LevelRegistries;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;

/**
 * Registers shared level-related dynamic registries during mod initialization.
 */
public final class LevelBootstrap {
    private static boolean bootstrapped;

    private LevelBootstrap() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }

        DynamicRegistries.register(LevelRegistries.PLAYER_PROGRESSION, LevelProgressionDefinition.CODEC);
        bootstrapped = true;
    }
}
