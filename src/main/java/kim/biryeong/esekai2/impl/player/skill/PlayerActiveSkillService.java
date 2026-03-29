package kim.biryeong.esekai2.impl.player.skill;

import kim.biryeong.esekai2.api.player.skill.SelectedActiveSkillRef;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Shared internal service for resolving and mutating persistent selected active skill state.
 */
public final class PlayerActiveSkillService {
    private PlayerActiveSkillService() {
    }

    public static Optional<SelectedActiveSkillRef> get(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return get(requireServer(player), player.getUUID());
    }

    public static Optional<SelectedActiveSkillRef> get(MinecraftServer server, UUID playerId) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(playerId, "playerId");
        return savedData(server).get(playerId);
    }

    public static SelectedActiveSkillRef select(ServerPlayer player, SelectedActiveSkillRef selectedSkill) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(selectedSkill, "selectedSkill");
        return select(requireServer(player), player.getUUID(), selectedSkill);
    }

    public static SelectedActiveSkillRef select(MinecraftServer server, UUID playerId, SelectedActiveSkillRef selectedSkill) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(selectedSkill, "selectedSkill");
        savedData(server).put(playerId, selectedSkill);
        return selectedSkill;
    }

    public static Optional<SelectedActiveSkillRef> clear(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return clear(requireServer(player), player.getUUID());
    }

    public static Optional<SelectedActiveSkillRef> clear(MinecraftServer server, UUID playerId) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(playerId, "playerId");
        return savedData(server).remove(playerId);
    }

    private static PlayerActiveSkillSavedData savedData(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(PlayerActiveSkillSavedData.TYPE);
    }

    private static MinecraftServer requireServer(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            throw new IllegalStateException("ServerPlayer is not attached to a running server");
        }

        return server;
    }
}
