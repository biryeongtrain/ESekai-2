package kim.biryeong.esekai2.impl.player.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.player.skill.PlayerSkillCooldownState;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Persistent server-side storage of player skill cooldown snapshots keyed by player UUID.
 */
public final class PlayerSkillCooldownSavedData extends SavedData {
    private static final Codec<Map<UUID, PlayerSkillCooldownState>> STATE_MAP_CODEC =
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, PlayerSkillCooldownState.CODEC);

    private static final Codec<PlayerSkillCooldownSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            STATE_MAP_CODEC.optionalFieldOf("players", Map.of()).forGetter(PlayerSkillCooldownSavedData::states)
    ).apply(instance, PlayerSkillCooldownSavedData::new));

    public static final SavedDataType<PlayerSkillCooldownSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("esekai2", "player_skill_cooldowns"),
            PlayerSkillCooldownSavedData::new,
            CODEC,
            DataFixTypes.LEVEL
    );

    private final Map<UUID, PlayerSkillCooldownState> states;

    public PlayerSkillCooldownSavedData() {
        this(Map.of());
    }

    public PlayerSkillCooldownSavedData(Map<UUID, PlayerSkillCooldownState> states) {
        Objects.requireNonNull(states, "states");
        this.states = new LinkedHashMap<>(states);
    }

    public Map<UUID, PlayerSkillCooldownState> states() {
        return Map.copyOf(states);
    }

    public PlayerSkillCooldownState get(UUID playerId, long gameTime) {
        Objects.requireNonNull(playerId, "playerId");
        PlayerSkillCooldownState state = states.getOrDefault(playerId, new PlayerSkillCooldownState(Map.of()));
        PlayerSkillCooldownState pruned = state.pruneExpired(gameTime);
        if (!pruned.equals(state)) {
            if (pruned.readyGameTimes().isEmpty()) {
                states.remove(playerId);
            } else {
                states.put(playerId, pruned);
            }
            setDirty();
        }
        return pruned;
    }

    public void put(UUID playerId, PlayerSkillCooldownState state) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(state, "state");
        if (state.readyGameTimes().isEmpty()) {
            states.remove(playerId);
        } else {
            states.put(playerId, state);
        }
        setDirty();
    }
}
