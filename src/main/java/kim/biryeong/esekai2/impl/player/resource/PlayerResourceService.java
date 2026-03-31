package kim.biryeong.esekai2.impl.player.resource;

import kim.biryeong.esekai2.api.player.resource.PlayerResourceState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Shared internal service for resolving and mutating persistent player mana state.
 */
public final class PlayerResourceService {
    private PlayerResourceService() {
    }

    public static PlayerResourceState get(ServerPlayer player, double maxMana) {
        Objects.requireNonNull(player, "player");
        return get(requireServer(player), player.getUUID(), maxMana);
    }

    public static PlayerResourceState get(MinecraftServer server, UUID playerId, double maxMana) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(playerId, "playerId");
        return savedData(server).getOrCreate(playerId, maxMana);
    }

    public static PlayerResourceState setMana(ServerPlayer player, double currentMana, double maxMana) {
        Objects.requireNonNull(player, "player");
        return setMana(requireServer(player), player.getUUID(), currentMana, maxMana);
    }

    public static PlayerResourceState setMana(MinecraftServer server, UUID playerId, double currentMana, double maxMana) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(playerId, "playerId");
        PlayerResourceState state = new PlayerResourceState(sanitize(currentMana));
        return savedData(server).put(playerId, state, maxMana);
    }

    public static Optional<PlayerResourceState> spendMana(ServerPlayer player, double amount, double maxMana) {
        Objects.requireNonNull(player, "player");
        return spendMana(requireServer(player), player.getUUID(), amount, maxMana);
    }

    public static Optional<PlayerResourceState> spendMana(MinecraftServer server, UUID playerId, double amount, double maxMana) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(playerId, "playerId");
        double sanitizedAmount = sanitize(amount);
        PlayerResourceState current = get(server, playerId, maxMana);
        if (current.currentMana() + 1.0E-6 < sanitizedAmount) {
            return Optional.empty();
        }
        return Optional.of(savedData(server).put(playerId, new PlayerResourceState(current.currentMana() - sanitizedAmount), maxMana));
    }

    private static PlayerResourceSavedData savedData(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(PlayerResourceSavedData.TYPE);
    }

    private static MinecraftServer requireServer(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            throw new IllegalStateException("ServerPlayer is not attached to a running server");
        }
        return server;
    }

    private static double sanitize(double value) {
        if (!Double.isFinite(value) || value < 0.0) {
            return 0.0;
        }
        return value;
    }
}
