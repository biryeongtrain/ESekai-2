package kim.biryeong.esekai2.impl.player.resource;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.player.resource.PlayerResourceState;
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
 * Persistent server-side storage of player mana state keyed by player UUID.
 */
public final class PlayerResourceSavedData extends SavedData {
    private static final Codec<Map<UUID, PlayerResourceState>> STATE_MAP_CODEC =
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, PlayerResourceState.CODEC);

    private static final Codec<PlayerResourceSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            STATE_MAP_CODEC.optionalFieldOf("players", Map.of()).forGetter(PlayerResourceSavedData::states)
    ).apply(instance, PlayerResourceSavedData::new));

    public static final SavedDataType<PlayerResourceSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("esekai2", "player_resources"),
            PlayerResourceSavedData::new,
            CODEC,
            DataFixTypes.LEVEL
    );

    private final Map<UUID, PlayerResourceState> states;

    public PlayerResourceSavedData() {
        this(Map.of());
    }

    public PlayerResourceSavedData(Map<UUID, PlayerResourceState> states) {
        Objects.requireNonNull(states, "states");
        this.states = new LinkedHashMap<>(states);
    }

    public Map<UUID, PlayerResourceState> states() {
        return Map.copyOf(states);
    }

    public PlayerResourceState getOrCreate(UUID playerId, double maxMana) {
        Objects.requireNonNull(playerId, "playerId");
        PlayerResourceState existing = states.get(playerId);
        PlayerResourceState clamped = clamp(existing == null ? new PlayerResourceState(sanitize(maxMana)) : existing, maxMana);
        if (!clamped.equals(existing)) {
            states.put(playerId, clamped);
            setDirty();
        }
        return clamped;
    }

    public PlayerResourceState put(UUID playerId, PlayerResourceState state, double maxMana) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(state, "state");
        PlayerResourceState clamped = clamp(state, maxMana);
        states.put(playerId, clamped);
        setDirty();
        return clamped;
    }

    private static PlayerResourceState clamp(PlayerResourceState state, double maxMana) {
        double sanitizedMax = sanitize(maxMana);
        return new PlayerResourceState(Math.max(0.0, Math.min(state.currentMana(), sanitizedMax)));
    }

    private static double sanitize(double value) {
        if (!Double.isFinite(value) || value < 0.0) {
            return 0.0;
        }
        return value;
    }
}
