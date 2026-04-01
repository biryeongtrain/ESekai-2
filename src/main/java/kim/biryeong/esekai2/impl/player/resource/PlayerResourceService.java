package kim.biryeong.esekai2.impl.player.resource;

import kim.biryeong.esekai2.api.player.resource.PlayerResourceDefinition;
import kim.biryeong.esekai2.api.player.resource.PlayerResourceIds;
import kim.biryeong.esekai2.api.player.resource.PlayerResourceState;
import kim.biryeong.esekai2.api.player.resource.PlayerTrackedResourceState;
import kim.biryeong.esekai2.api.player.stat.PlayerCombatStats;
import kim.biryeong.esekai2.impl.runtime.ServerRuntimeAccess;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Shared internal service for resolving and mutating persistent player resources.
 */
public final class PlayerResourceService {
    private PlayerResourceService() {
    }

    public static boolean supports(String resource) {
        if (!PlayerResourceIds.isUsable(resource)) {
            return false;
        }
        return ServerRuntimeAccess.currentServer()
                .flatMap(server -> definition(server, resource))
                .isPresent();
    }

    public static PlayerTrackedResourceState get(ServerPlayer player, String resource) {
        Objects.requireNonNull(player, "player");
        return get(requireServer(player), player.getUUID(), resource);
    }

    public static PlayerTrackedResourceState get(ServerPlayer player, String resource, double maxAmount) {
        Objects.requireNonNull(player, "player");
        return get(requireServer(player), player.getUUID(), resource, maxAmount);
    }

    public static PlayerTrackedResourceState get(MinecraftServer server, UUID playerId, String resource) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(playerId, "playerId");
        PlayerResourceDefinition definition = requireDefinition(server, resource);
        double maxAmount = maxAmount(server, playerId, definition);
        double initialAmount = definition.startsFull() ? maxAmount : 0.0;
        return savedData(server).getOrCreate(playerId, resource, initialAmount, maxAmount);
    }

    public static PlayerTrackedResourceState get(MinecraftServer server, UUID playerId, String resource, double maxAmount) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(playerId, "playerId");
        return savedData(server).getOrCreate(playerId, resource, maxAmount);
    }

    public static PlayerTrackedResourceState set(ServerPlayer player, String resource, double currentAmount) {
        Objects.requireNonNull(player, "player");
        return set(requireServer(player), player.getUUID(), resource, currentAmount);
    }

    public static PlayerTrackedResourceState set(ServerPlayer player, String resource, double currentAmount, double maxAmount) {
        Objects.requireNonNull(player, "player");
        return set(requireServer(player), player.getUUID(), resource, currentAmount, maxAmount);
    }

    public static PlayerTrackedResourceState set(MinecraftServer server, UUID playerId, String resource, double currentAmount) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(playerId, "playerId");
        double maxAmount = maxAmount(server, playerId, requireDefinition(server, resource));
        PlayerTrackedResourceState state = new PlayerTrackedResourceState(sanitize(currentAmount));
        return savedData(server).put(playerId, resource, state, maxAmount);
    }

    public static PlayerTrackedResourceState set(MinecraftServer server, UUID playerId, String resource, double currentAmount, double maxAmount) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(playerId, "playerId");
        PlayerTrackedResourceState state = new PlayerTrackedResourceState(sanitize(currentAmount));
        return savedData(server).put(playerId, resource, state, maxAmount);
    }

    public static PlayerTrackedResourceState add(ServerPlayer player, String resource, double amount) {
        Objects.requireNonNull(player, "player");
        return add(requireServer(player), player.getUUID(), resource, amount);
    }

    public static PlayerTrackedResourceState add(ServerPlayer player, String resource, double amount, double maxAmount) {
        Objects.requireNonNull(player, "player");
        return add(requireServer(player), player.getUUID(), resource, amount, maxAmount);
    }

    public static PlayerTrackedResourceState add(MinecraftServer server, UUID playerId, String resource, double amount) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(playerId, "playerId");
        double maxAmount = maxAmount(server, playerId, requireDefinition(server, resource));
        PlayerTrackedResourceState current = get(server, playerId, resource);
        double nextAmount = current.currentAmount() + (Double.isFinite(amount) ? amount : 0.0);
        return savedData(server).put(playerId, resource, new PlayerTrackedResourceState(nextAmount), maxAmount);
    }

    public static PlayerTrackedResourceState add(MinecraftServer server, UUID playerId, String resource, double amount, double maxAmount) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(playerId, "playerId");
        double sanitizedMax = sanitize(maxAmount);
        PlayerTrackedResourceState current = get(server, playerId, resource, sanitizedMax);
        double nextAmount = current.currentAmount() + (Double.isFinite(amount) ? amount : 0.0);
        return savedData(server).put(playerId, resource, new PlayerTrackedResourceState(nextAmount), sanitizedMax);
    }

    public static Optional<PlayerTrackedResourceState> spend(ServerPlayer player, String resource, double amount) {
        Objects.requireNonNull(player, "player");
        return spend(requireServer(player), player.getUUID(), resource, amount);
    }

    public static Optional<PlayerTrackedResourceState> spend(ServerPlayer player, String resource, double amount, double maxAmount) {
        Objects.requireNonNull(player, "player");
        return spend(requireServer(player), player.getUUID(), resource, amount, maxAmount);
    }

    public static Optional<PlayerTrackedResourceState> spend(MinecraftServer server, UUID playerId, String resource, double amount) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(playerId, "playerId");
        double maxAmount = maxAmount(server, playerId, requireDefinition(server, resource));
        double sanitizedAmount = sanitize(amount);
        PlayerTrackedResourceState current = get(server, playerId, resource);
        if (current.currentAmount() + 1.0E-6 < sanitizedAmount) {
            return Optional.empty();
        }
        return Optional.of(savedData(server).put(
                playerId,
                resource,
                new PlayerTrackedResourceState(current.currentAmount() - sanitizedAmount),
                maxAmount
        ));
    }

    public static Optional<PlayerTrackedResourceState> spend(MinecraftServer server, UUID playerId, String resource, double amount, double maxAmount) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(playerId, "playerId");
        double sanitizedAmount = sanitize(amount);
        PlayerTrackedResourceState current = get(server, playerId, resource, maxAmount);
        if (current.currentAmount() + 1.0E-6 < sanitizedAmount) {
            return Optional.empty();
        }
        return Optional.of(savedData(server).put(
                playerId,
                resource,
                new PlayerTrackedResourceState(current.currentAmount() - sanitizedAmount),
                maxAmount
        ));
    }

    public static double maxAmount(ServerPlayer player, String resource) {
        Objects.requireNonNull(player, "player");
        return maxAmount(requireServer(player), player.getUUID(), requireDefinition(requireServer(player), resource));
    }

    public static double regenerationPerSecond(ServerPlayer player, String resource) {
        Objects.requireNonNull(player, "player");
        MinecraftServer server = requireServer(player);
        PlayerResourceDefinition definition = requireDefinition(server, resource);
        if (definition.regenerationPerSecondStat().isEmpty()) {
            return 0.0;
        }
        double value = PlayerCombatStats.get(player).resolvedValue(definition.regenerationPerSecondStat().orElseThrow());
        return sanitize(value);
    }

    public static PlayerResourceState get(ServerPlayer player, double maxMana) {
        return manaState(get(player, PlayerResourceIds.MANA, maxMana));
    }

    public static PlayerResourceState get(MinecraftServer server, UUID playerId, double maxMana) {
        return manaState(get(server, playerId, PlayerResourceIds.MANA, maxMana));
    }

    public static PlayerResourceState setMana(ServerPlayer player, double currentMana, double maxMana) {
        return manaState(set(player, PlayerResourceIds.MANA, currentMana, maxMana));
    }

    public static PlayerResourceState setMana(MinecraftServer server, UUID playerId, double currentMana, double maxMana) {
        return manaState(set(server, playerId, PlayerResourceIds.MANA, currentMana, maxMana));
    }

    public static PlayerResourceState addMana(ServerPlayer player, double amount, double maxMana) {
        return manaState(add(player, PlayerResourceIds.MANA, amount, maxMana));
    }

    public static PlayerResourceState addMana(MinecraftServer server, UUID playerId, double amount, double maxMana) {
        return manaState(add(server, playerId, PlayerResourceIds.MANA, amount, maxMana));
    }

    public static Optional<PlayerResourceState> spendMana(ServerPlayer player, double amount, double maxMana) {
        return spend(player, PlayerResourceIds.MANA, amount, maxMana).map(PlayerResourceService::manaState);
    }

    public static Optional<PlayerResourceState> spendMana(MinecraftServer server, UUID playerId, double amount, double maxMana) {
        return spend(server, playerId, PlayerResourceIds.MANA, amount, maxMana).map(PlayerResourceService::manaState);
    }

    private static PlayerResourceSavedData savedData(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(PlayerResourceSavedData.TYPE);
    }

    public static Optional<PlayerResourceDefinition> definition(MinecraftServer server, String resource) {
        Objects.requireNonNull(server, "server");
        Optional<Identifier> resourceId = parseResourceId(resource);
        if (resourceId.isEmpty()) {
            return Optional.empty();
        }
        Registry<PlayerResourceDefinition> registry = PlayerResourceRegistryAccess.resourceRegistry(server);
        return registry.getOptional(resourceId.orElseThrow());
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

    private static PlayerResourceDefinition requireDefinition(MinecraftServer server, String resource) {
        return definition(server, resource)
                .orElseThrow(() -> new IllegalArgumentException("Unknown registered player resource: " + resource));
    }

    private static Optional<Identifier> parseResourceId(String resource) {
        if (!PlayerResourceIds.isUsable(resource)) {
            return Optional.empty();
        }
        String trimmed = resource.trim();
        if (trimmed.contains(":")) {
            return Optional.ofNullable(Identifier.tryParse(trimmed));
        }
        return Optional.of(Identifier.fromNamespaceAndPath("esekai2", trimmed));
    }

    private static double maxAmount(MinecraftServer server, UUID playerId, PlayerResourceDefinition definition) {
        ServerPlayer onlinePlayer = server.getPlayerList().getPlayer(playerId);
        if (onlinePlayer != null) {
            return sanitize(PlayerCombatStats.get(onlinePlayer).resolvedValue(definition.maxStat()));
        }
        return 0.0;
    }

    private static PlayerResourceState manaState(PlayerTrackedResourceState state) {
        return new PlayerResourceState(state.currentAmount());
    }
}
