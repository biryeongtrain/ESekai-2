package kim.biryeong.esekai2.impl.player.resource;

import kim.biryeong.esekai2.api.player.resource.PlayerResourceDefinition;
import kim.biryeong.esekai2.api.player.resource.PlayerResourceRegistries;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;

/**
 * Registers shared player-resource dynamic registries during mod initialization.
 */
public final class PlayerResourceBootstrap {
    private static boolean bootstrapped;

    private PlayerResourceBootstrap() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }

        DynamicRegistries.register(PlayerResourceRegistries.PLAYER_RESOURCE, PlayerResourceDefinition.CODEC);
        bootstrapped = true;
    }
}
