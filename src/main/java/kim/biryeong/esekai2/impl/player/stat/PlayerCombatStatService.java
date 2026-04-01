package kim.biryeong.esekai2.impl.player.stat;

import kim.biryeong.esekai2.api.stat.definition.StatDefinition;
import kim.biryeong.esekai2.api.stat.holder.StatHolder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Shared runtime store for player combat stat holders.
 */
public final class PlayerCombatStatService {
    private static final Map<MinecraftServer, Map<UUID, PlayerCombatStatHolder>> HOLDERS = new IdentityHashMap<>();
    private static boolean bootstrapped;

    private PlayerCombatStatService() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }

        ServerLifecycleEvents.SERVER_STARTING.register(server -> HOLDERS.remove(server));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> HOLDERS.remove(server));
        bootstrapped = true;
    }

    public static StatHolder get(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return get(requireServer(player), player.getUUID());
    }

    public static StatHolder get(MinecraftServer server, UUID playerId) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(playerId, "playerId");

        bootstrap();
        return HOLDERS.computeIfAbsent(server, ignored -> new LinkedHashMap<>())
                .computeIfAbsent(playerId, ignored -> new PlayerCombatStatHolder(server, playerId));
    }

    public static void setBaseValue(ServerPlayer player, ResourceKey<StatDefinition> stat, double baseValue) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(stat, "stat");
        get(player).setBaseValue(stat, baseValue);
    }

    public static void markDirty(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        markDirty(requireServer(player), player.getUUID());
    }

    public static void markDirty(MinecraftServer server, UUID playerId) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(playerId, "playerId");

        bootstrap();
        PlayerCombatStatHolder holder = HOLDERS.computeIfAbsent(server, ignored -> new LinkedHashMap<>()).get(playerId);
        if (holder != null) {
            holder.markDirty();
        }
    }

    private static MinecraftServer requireServer(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            throw new IllegalStateException("ServerPlayer is not attached to a running server");
        }
        return server;
    }
}
