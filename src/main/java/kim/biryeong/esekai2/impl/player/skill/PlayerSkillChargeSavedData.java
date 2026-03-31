package kim.biryeong.esekai2.impl.player.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.player.skill.PlayerSkillChargeState;
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
 * Persistent server-side storage of player skill charge snapshots keyed by player UUID.
 */
public final class PlayerSkillChargeSavedData extends SavedData {
    private static final Codec<Map<UUID, PlayerSkillChargeState>> STATE_MAP_CODEC =
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, PlayerSkillChargeState.CODEC);

    private static final Codec<PlayerSkillChargeSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            STATE_MAP_CODEC.optionalFieldOf("players", Map.of()).forGetter(PlayerSkillChargeSavedData::states)
    ).apply(instance, PlayerSkillChargeSavedData::new));

    public static final SavedDataType<PlayerSkillChargeSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("esekai2", "player_skill_charges"),
            PlayerSkillChargeSavedData::new,
            CODEC,
            DataFixTypes.LEVEL
    );

    private final Map<UUID, PlayerSkillChargeState> states;

    public PlayerSkillChargeSavedData() {
        this(Map.of());
    }

    public PlayerSkillChargeSavedData(Map<UUID, PlayerSkillChargeState> states) {
        Objects.requireNonNull(states, "states");
        this.states = new LinkedHashMap<>(states);
    }

    public Map<UUID, PlayerSkillChargeState> states() {
        return Map.copyOf(states);
    }

    public PlayerSkillChargeState get(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        return states.getOrDefault(playerId, new PlayerSkillChargeState(Map.of()));
    }

    public void put(UUID playerId, PlayerSkillChargeState state) {
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
