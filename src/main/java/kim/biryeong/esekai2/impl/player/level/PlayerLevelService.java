package kim.biryeong.esekai2.impl.player.level;

import kim.biryeong.esekai2.api.level.LevelProgressionDefinition;
import kim.biryeong.esekai2.api.level.LevelRegistries;
import kim.biryeong.esekai2.api.level.LevelRules;
import kim.biryeong.esekai2.api.player.level.PlayerLevelState;
import kim.biryeong.esekai2.impl.player.stat.PlayerCombatStatService;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.UUID;

/**
 * Shared internal service for resolving and mutating persistent player level state.
 */
public final class PlayerLevelService {
    private static final Identifier DEFAULT_PROGRESSION_ID = Identifier.fromNamespaceAndPath("esekai2", "default");

    private PlayerLevelService() {
    }

    public static PlayerLevelState get(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return get(requireServer(player), player.getUUID());
    }

    public static PlayerLevelState get(MinecraftServer server, UUID playerId) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(playerId, "playerId");
        return savedData(server).getOrCreate(playerId);
    }

    public static PlayerLevelState setLevel(ServerPlayer player, int level) {
        Objects.requireNonNull(player, "player");
        return setLevel(requireServer(player), player.getUUID(), level);
    }

    public static PlayerLevelState setLevel(MinecraftServer server, UUID playerId, int level) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(playerId, "playerId");
        int validatedLevel = LevelRules.requireValidLevel(level, "level");

        LevelProgressionDefinition progression = progression(server);
        long totalExperience = progression.totalExperienceToReachLevel(validatedLevel);
        PlayerLevelState state = new PlayerLevelState(validatedLevel, 0L, totalExperience);
        return store(server, playerId, state);
    }

    public static PlayerLevelState setExperience(ServerPlayer player, long totalExperience) {
        Objects.requireNonNull(player, "player");
        return setExperience(requireServer(player), player.getUUID(), totalExperience);
    }

    public static PlayerLevelState setExperience(MinecraftServer server, UUID playerId, long totalExperience) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(playerId, "playerId");
        if (totalExperience < 0L) {
            throw new IllegalArgumentException("totalExperience must be greater than or equal to 0");
        }

        LevelProgressionDefinition progression = progression(server);
        PlayerLevelState state = resolveFromTotalExperience(totalExperience, progression);
        return store(server, playerId, state);
    }

    public static PlayerLevelState addExperience(ServerPlayer player, long experience) {
        Objects.requireNonNull(player, "player");
        return addExperience(requireServer(player), player.getUUID(), experience);
    }

    public static PlayerLevelState addExperience(MinecraftServer server, UUID playerId, long experience) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(playerId, "playerId");
        if (experience < 0L) {
            throw new IllegalArgumentException("experience must be greater than or equal to 0");
        }

        PlayerLevelState current = get(server, playerId);
        if (current.level() == LevelRules.MAX_LEVEL) {
            return store(server, playerId, new PlayerLevelState(LevelRules.MAX_LEVEL, 0L, current.totalExperience() + experience));
        }

        return setExperience(server, playerId, current.totalExperience() + experience);
    }

    public static long experienceToNextLevel(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return experienceToNextLevel(requireServer(player), player.getUUID());
    }

    public static long experienceToNextLevel(MinecraftServer server, UUID playerId) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(playerId, "playerId");
        PlayerLevelState state = get(server, playerId);
        if (state.level() == LevelRules.MAX_LEVEL) {
            return 0L;
        }

        long required = progression(server).experienceToNextLevel(state.level());
        return Math.max(0L, required - state.experienceInLevel());
    }

    public static LevelProgressionDefinition progression(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        Registry<LevelProgressionDefinition> registry = server.registryAccess().lookupOrThrow(LevelRegistries.PLAYER_PROGRESSION);
        return registry.getOptional(DEFAULT_PROGRESSION_ID)
                .orElseThrow(() -> new IllegalStateException("Missing player progression definition: " + DEFAULT_PROGRESSION_ID));
    }

    private static PlayerLevelSavedData savedData(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(PlayerLevelSavedData.TYPE);
    }

    private static PlayerLevelState store(MinecraftServer server, UUID playerId, PlayerLevelState state) {
        savedData(server).put(playerId, state);
        PlayerCombatStatService.markDirty(server, playerId);
        return state;
    }

    private static MinecraftServer requireServer(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            throw new IllegalStateException("ServerPlayer is not attached to a running server");
        }

        return server;
    }

    private static PlayerLevelState resolveFromTotalExperience(long totalExperience, LevelProgressionDefinition progression) {
        int level = LevelRules.MIN_LEVEL;
        long remaining = totalExperience;

        while (level < LevelRules.MAX_LEVEL) {
            long required = progression.experienceToNextLevel(level);
            if (remaining < required) {
                return new PlayerLevelState(level, remaining, totalExperience);
            }

            remaining -= required;
            level++;
        }

        return new PlayerLevelState(LevelRules.MAX_LEVEL, 0L, totalExperience);
    }
}
