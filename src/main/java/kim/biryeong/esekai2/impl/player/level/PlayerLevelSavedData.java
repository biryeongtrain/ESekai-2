package kim.biryeong.esekai2.impl.player.level;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.player.level.PlayerLevelState;
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
 * Persistent server-side storage of player level state keyed by player UUID.
 */
public final class PlayerLevelSavedData extends SavedData {
    private static final Codec<Map<UUID, PlayerLevelState>> STATE_MAP_CODEC =
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, PlayerLevelState.CODEC);

    private static final Codec<PlayerLevelSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            STATE_MAP_CODEC.optionalFieldOf("players", Map.of()).forGetter(PlayerLevelSavedData::states)
    ).apply(instance, PlayerLevelSavedData::new));

    public static final SavedDataType<PlayerLevelSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("esekai2", "player_levels"),
            PlayerLevelSavedData::new,
            CODEC,
            DataFixTypes.LEVEL
    );

    private final Map<UUID, PlayerLevelState> states;

    public PlayerLevelSavedData() {
        this(Map.of());
    }

    public PlayerLevelSavedData(Map<UUID, PlayerLevelState> states) {
        Objects.requireNonNull(states, "states");
        this.states = new LinkedHashMap<>(states);
    }

    public Map<UUID, PlayerLevelState> states() {
        return Map.copyOf(states);
    }

    public PlayerLevelState getOrCreate(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        return states.computeIfAbsent(uuid, ignored -> PlayerLevelState.DEFAULT);
    }

    public PlayerLevelState put(UUID uuid, PlayerLevelState state) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(state, "state");
        PlayerLevelState previous = states.put(uuid, state);
        setDirty();
        return previous;
    }
}
