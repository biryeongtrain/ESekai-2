package kim.biryeong.esekai2.impl.player.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.player.skill.PlayerSkillBurstState;
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
 * Persistent server-side storage of player skill burst snapshots keyed by player UUID.
 */
public final class PlayerSkillBurstSavedData extends SavedData {
    private static final Codec<Map<UUID, PlayerSkillBurstState>> STATE_MAP_CODEC =
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, PlayerSkillBurstState.CODEC);

    private static final Codec<PlayerSkillBurstSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            STATE_MAP_CODEC.optionalFieldOf("players", Map.of()).forGetter(PlayerSkillBurstSavedData::states)
    ).apply(instance, PlayerSkillBurstSavedData::new));

    public static final SavedDataType<PlayerSkillBurstSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("esekai2", "player_skill_bursts"),
            PlayerSkillBurstSavedData::new,
            CODEC,
            DataFixTypes.LEVEL
    );

    private final Map<UUID, PlayerSkillBurstState> states;

    public PlayerSkillBurstSavedData() {
        this(Map.of());
    }

    public PlayerSkillBurstSavedData(Map<UUID, PlayerSkillBurstState> states) {
        Objects.requireNonNull(states, "states");
        this.states = new LinkedHashMap<>(states);
    }

    public Map<UUID, PlayerSkillBurstState> states() {
        return Map.copyOf(states);
    }

    public PlayerSkillBurstState get(UUID playerId, long gameTime) {
        Objects.requireNonNull(playerId, "playerId");
        PlayerSkillBurstState state = states.getOrDefault(playerId, new PlayerSkillBurstState(Map.of()));
        PlayerSkillBurstState pruned = state.pruneExpired(gameTime);
        if (!pruned.equals(state)) {
            if (pruned.skills().isEmpty()) {
                states.remove(playerId);
            } else {
                states.put(playerId, pruned);
            }
            setDirty();
        }
        return pruned;
    }

    public void put(UUID playerId, PlayerSkillBurstState state) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(state, "state");
        if (state.skills().isEmpty()) {
            states.remove(playerId);
        } else {
            states.put(playerId, state);
        }
        setDirty();
    }
}
