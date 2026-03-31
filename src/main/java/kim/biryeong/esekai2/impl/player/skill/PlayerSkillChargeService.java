package kim.biryeong.esekai2.impl.player.skill;

import kim.biryeong.esekai2.api.player.skill.PlayerSkillChargeState;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Shared internal service for resolving and mutating persistent player skill charge state.
 */
public final class PlayerSkillChargeService {
    private PlayerSkillChargeService() {
    }

    public static PlayerSkillChargeState snapshot(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return savedData(requireServer(player)).get(player.getUUID());
    }

    public static PlayerSkillChargeState.SkillChargeEntry get(
            ServerPlayer player,
            Identifier skillId,
            int maxCharges,
            long gameTime
    ) {
        Objects.requireNonNull(player, "player");
        return get(requireServer(player), player.getUUID(), skillId, maxCharges, gameTime);
    }

    public static PlayerSkillChargeState.SkillChargeEntry get(
            MinecraftServer server,
            UUID playerId,
            Identifier skillId,
            int maxCharges,
            long gameTime
    ) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(skillId, "skillId");

        PlayerSkillChargeSavedData savedData = savedData(server);
        PlayerSkillChargeState current = savedData.get(playerId);
        PlayerSkillChargeState.SkillChargeEntry tracked = current.entry(skillId)
                .orElseGet(() -> new PlayerSkillChargeState.SkillChargeEntry(Math.max(0, maxCharges), List.of()));
        PlayerSkillChargeState.SkillChargeEntry resolved = tracked.resolve(maxCharges, gameTime);
        PlayerSkillChargeState updated = current.withEntry(skillId, normalizePersistentEntry(resolved, maxCharges));
        if (!updated.equals(current)) {
            savedData.put(playerId, updated);
        }
        return resolved;
    }

    public static int availableCharges(ServerPlayer player, Identifier skillId, int maxCharges, long gameTime) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(skillId, "skillId");
        return get(player, skillId, maxCharges, gameTime).currentCharges();
    }

    public static PlayerSkillChargeState setAvailableCharges(
            ServerPlayer player,
            Identifier skillId,
            int currentCharges,
            int maxCharges,
            long gameTime
    ) {
        Objects.requireNonNull(player, "player");
        return setAvailableCharges(requireServer(player), player.getUUID(), skillId, currentCharges, maxCharges, gameTime);
    }

    public static PlayerSkillChargeState setAvailableCharges(
            MinecraftServer server,
            UUID playerId,
            Identifier skillId,
            int currentCharges,
            int maxCharges,
            long gameTime
    ) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(skillId, "skillId");

        PlayerSkillChargeState current = savedData(server).get(playerId);
        PlayerSkillChargeState.SkillChargeEntry replacement = new PlayerSkillChargeState.SkillChargeEntry(
                Math.max(0, currentCharges),
                List.of()
        ).resolve(maxCharges, gameTime);
        PlayerSkillChargeState updated = current.withEntry(skillId, normalizePersistentEntry(replacement, maxCharges));
        savedData(server).put(playerId, updated);
        return updated;
    }

    public static Optional<PlayerSkillChargeState.SkillChargeEntry> consume(
            ServerPlayer player,
            Identifier skillId,
            int maxCharges,
            long gameTime,
            long chargeReadyGameTime
    ) {
        Objects.requireNonNull(player, "player");
        return consume(requireServer(player), player.getUUID(), skillId, maxCharges, gameTime, chargeReadyGameTime);
    }

    public static Optional<PlayerSkillChargeState.SkillChargeEntry> consume(
            MinecraftServer server,
            UUID playerId,
            Identifier skillId,
            int maxCharges,
            long gameTime,
            long chargeReadyGameTime
    ) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(skillId, "skillId");

        PlayerSkillChargeSavedData savedData = savedData(server);
        PlayerSkillChargeState.SkillChargeEntry resolved = get(server, playerId, skillId, maxCharges, gameTime);
        if (resolved.currentCharges() <= 0) {
            return Optional.empty();
        }

        PlayerSkillChargeState current = savedData.get(playerId);
        PlayerSkillChargeState.SkillChargeEntry consumed = resolved.consume(maxCharges, chargeReadyGameTime, gameTime);
        PlayerSkillChargeState updated = current.withEntry(skillId, normalizePersistentEntry(consumed, maxCharges));
        savedData.put(playerId, updated);
        return Optional.of(consumed.resolve(maxCharges, gameTime));
    }

    private static PlayerSkillChargeSavedData savedData(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(PlayerSkillChargeSavedData.TYPE);
    }

    private static MinecraftServer requireServer(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            throw new IllegalStateException("ServerPlayer is not attached to a running server");
        }
        return server;
    }

    private static PlayerSkillChargeState.SkillChargeEntry normalizePersistentEntry(
            PlayerSkillChargeState.SkillChargeEntry entry,
            int maxCharges
    ) {
        if (entry == null) {
            return null;
        }
        PlayerSkillChargeState.SkillChargeEntry normalized = entry.resolve(maxCharges, Long.MIN_VALUE);
        if (normalized.currentCharges() >= Math.max(0, maxCharges) && normalized.pendingReadyGameTimes().isEmpty()) {
            return null;
        }
        return normalized;
    }
}
