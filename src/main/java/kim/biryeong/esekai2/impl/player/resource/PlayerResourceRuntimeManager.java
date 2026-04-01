package kim.biryeong.esekai2.impl.player.resource;

import kim.biryeong.esekai2.api.player.resource.PlayerResourceDefinition;
import kim.biryeong.esekai2.api.player.resource.PlayerResources;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Applies server-side registered player-resource regeneration to online players.
 */
public final class PlayerResourceRuntimeManager {
    private static final double TICKS_PER_SECOND = 20.0;
    private static boolean bootstrapped;

    private PlayerResourceRuntimeManager() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }

        ServerTickEvents.END_SERVER_TICK.register(PlayerResourceRuntimeManager::tickServer);
        bootstrapped = true;
    }

    private static void tickServer(MinecraftServer server) {
        Registry<PlayerResourceDefinition> registry = PlayerResourceRegistryAccess.resourceRegistry(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            tickPlayer(player, registry);
        }
    }

    private static void tickPlayer(ServerPlayer player, Registry<PlayerResourceDefinition> registry) {
        for (var entry : registry.entrySet()) {
            PlayerResourceDefinition definition = entry.getValue();
            net.minecraft.resources.Identifier resourceId = registry.getKey(definition);
            if (resourceId == null) {
                continue;
            }
            String resource = canonicalRuntimeResourceId(resourceId);
            if (definition.regenerationPerSecondStat().isEmpty()) {
                continue;
            }

            double maxAmount = PlayerResources.maxAmount(player, resource);
            if (!Double.isFinite(maxAmount) || maxAmount <= 0.0) {
                continue;
            }

            double regenPerSecond = PlayerResources.regenerationPerSecond(player, resource);
            if (!Double.isFinite(regenPerSecond) || regenPerSecond <= 0.0) {
                continue;
            }

            double currentAmount = PlayerResources.getAmount(player, resource);
            if (currentAmount + 1.0E-6 >= maxAmount) {
                continue;
            }

            double delta = Math.min(regenPerSecond / TICKS_PER_SECOND, maxAmount - currentAmount);
            if (!Double.isFinite(delta) || delta <= 1.0E-6) {
                continue;
            }

            PlayerResources.add(player, resource, delta);
        }
    }

    private static String canonicalRuntimeResourceId(net.minecraft.resources.Identifier resourceId) {
        if ("esekai2".equals(resourceId.getNamespace())) {
            return resourceId.getPath();
        }
        return resourceId.toString();
    }
}
