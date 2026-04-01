package kim.biryeong.esekai2.impl.player.resource;

import kim.biryeong.esekai2.api.player.resource.PlayerResourceDefinition;
import kim.biryeong.esekai2.api.player.resource.PlayerResourceRegistries;
import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;

/**
 * Internal helpers for retrieving the player resource registry in runtime and GameTest contexts.
 */
public final class PlayerResourceRegistryAccess {
    private PlayerResourceRegistryAccess() {
    }

    public static Registry<PlayerResourceDefinition> resourceRegistry(MinecraftServer server) {
        return server.registryAccess().lookupOrThrow(PlayerResourceRegistries.PLAYER_RESOURCE);
    }

    public static Registry<PlayerResourceDefinition> resourceRegistry(GameTestHelper helper) {
        return resourceRegistry(helper.getLevel().getServer());
    }
}
