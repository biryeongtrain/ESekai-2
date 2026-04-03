package kim.biryeong.esekai2.impl.player.skill;

import kim.biryeong.esekai2.api.player.skill.PlayerSkillCooldownState;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.OptionalLong;
import java.util.UUID;

/**
 * Shared internal service for resolving and mutating persistent player skill cooldown state.
 */
public final class PlayerSkillCooldownService {
    private PlayerSkillCooldownService() {
    }

    public static PlayerSkillCooldownState get(ServerPlayer player, long gameTime) {
        Objects.requireNonNull(player, "player");
        return get(requireServer(player), player.getUUID(), gameTime);
    }

    public static PlayerSkillCooldownState get(MinecraftServer server, UUID playerId, long gameTime) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(playerId, "playerId");
        return savedData(server).get(playerId, gameTime);
    }

    public static boolean isOnCooldown(ServerPlayer player, Identifier skillId, long gameTime) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(skillId, "skillId");
        return get(player, gameTime).isOnCooldown(skillId, gameTime);
    }

    public static OptionalLong readyGameTime(ServerPlayer player, Identifier skillId, long gameTime) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(skillId, "skillId");
        return get(player, gameTime).readyGameTime(skillId);
    }

    public static PlayerSkillCooldownState start(ServerPlayer player, Identifier skillId, long readyGameTime) {
        Objects.requireNonNull(player, "player");
        return start(requireServer(player), player.getUUID(), skillId, readyGameTime);
    }

    public static PlayerSkillCooldownState start(MinecraftServer server, UUID playerId, Identifier skillId, long readyGameTime) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(skillId, "skillId");
        if (readyGameTime < 0L) {
            throw new IllegalArgumentException("readyGameTime must be >= 0");
        }

        PlayerSkillCooldownState state = get(server, playerId, readyGameTime);
        PlayerSkillCooldownState updated = state.withCooldown(skillId, readyGameTime);
        savedData(server).put(playerId, updated);
        return updated;
    }

    public static PlayerSkillCooldownState clear(ServerPlayer player, Identifier skillId, long gameTime) {
        Objects.requireNonNull(player, "player");
        return clear(requireServer(player), player.getUUID(), skillId, gameTime);
    }

    public static PlayerSkillCooldownState clear(MinecraftServer server, UUID playerId, Identifier skillId, long gameTime) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(skillId, "skillId");
        PlayerSkillCooldownState current = get(server, playerId, gameTime);
        PlayerSkillCooldownState updated = current.withCooldown(skillId, gameTime);
        savedData(server).put(playerId, updated);
        return updated;
    }

    private static PlayerSkillCooldownSavedData savedData(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(PlayerSkillCooldownSavedData.TYPE);
    }

    private static MinecraftServer requireServer(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            throw new IllegalStateException("ServerPlayer is not attached to a running server");
        }
        return server;
    }
}
