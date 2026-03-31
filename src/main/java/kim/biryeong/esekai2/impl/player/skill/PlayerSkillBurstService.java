package kim.biryeong.esekai2.impl.player.skill;

import kim.biryeong.esekai2.api.player.skill.PlayerSkillBurstState;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Shared internal service for resolving and mutating persistent player skill burst state.
 */
public final class PlayerSkillBurstService {
    private PlayerSkillBurstService() {
    }

    public static PlayerSkillBurstState get(ServerPlayer player, long gameTime) {
        Objects.requireNonNull(player, "player");
        return get(requireServer(player), player.getUUID(), gameTime);
    }

    public static PlayerSkillBurstState get(MinecraftServer server, UUID playerId, long gameTime) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(playerId, "playerId");
        return savedData(server).get(playerId, gameTime);
    }

    public static Optional<PlayerSkillBurstState.SkillBurstEntry> entry(ServerPlayer player, Identifier skillId, long gameTime) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(skillId, "skillId");
        return get(player, gameTime).entry(skillId);
    }

    public static boolean canCast(ServerPlayer player, Identifier skillId, int timesToCast, long gameTime) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(skillId, "skillId");
        if (Math.max(1, timesToCast) <= 1) {
            return true;
        }
        return entry(player, skillId, gameTime)
                .map(entry -> entry.remainingCasts() > 0)
                .orElse(true);
    }

    public static int remainingCasts(ServerPlayer player, Identifier skillId, long gameTime) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(skillId, "skillId");
        return entry(player, skillId, gameTime)
                .map(PlayerSkillBurstState.SkillBurstEntry::remainingCasts)
                .orElse(0);
    }

    public static PlayerSkillBurstState recordSuccessfulCast(
            ServerPlayer player,
            Identifier skillId,
            int maxCasts,
            long gameTime,
            long expiresAtGameTime
    ) {
        Objects.requireNonNull(player, "player");
        return recordSuccessfulCast(requireServer(player), player.getUUID(), skillId, maxCasts, gameTime, expiresAtGameTime);
    }

    public static PlayerSkillBurstState recordSuccessfulCast(
            MinecraftServer server,
            UUID playerId,
            Identifier skillId,
            int maxCasts,
            long gameTime,
            long expiresAtGameTime
    ) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(skillId, "skillId");
        if (gameTime < 0L) {
            throw new IllegalArgumentException("gameTime must be >= 0");
        }
        if (expiresAtGameTime < 0L) {
            throw new IllegalArgumentException("expiresAtGameTime must be >= 0");
        }

        PlayerSkillBurstState current = get(server, playerId, gameTime);
        PlayerSkillBurstState cleared = current.clearExcept(skillId);
        int sanitizedMaxCasts = Math.max(1, maxCasts);
        if (sanitizedMaxCasts <= 1) {
            savedData(server).put(playerId, new PlayerSkillBurstState(java.util.Map.of()));
            return new PlayerSkillBurstState(java.util.Map.of());
        }

        int remainingCasts = cleared.entry(skillId)
                .map(entry -> Math.max(0, entry.remainingCasts() - 1))
                .orElse(sanitizedMaxCasts - 1);
        PlayerSkillBurstState.SkillBurstEntry updatedEntry = new PlayerSkillBurstState.SkillBurstEntry(
                remainingCasts,
                Math.max(gameTime + 1L, expiresAtGameTime)
        );
        PlayerSkillBurstState updated = cleared.withEntry(skillId, updatedEntry);
        savedData(server).put(playerId, updated);
        return updated;
    }

    private static PlayerSkillBurstSavedData savedData(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(PlayerSkillBurstSavedData.TYPE);
    }

    private static MinecraftServer requireServer(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            throw new IllegalStateException("ServerPlayer is not attached to a running server");
        }
        return server;
    }
}
