package kim.biryeong.esekai2.impl.player.resource;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.player.resource.PlayerResourceIds;
import kim.biryeong.esekai2.api.player.resource.PlayerResourceState;
import kim.biryeong.esekai2.api.player.resource.PlayerTrackedResourceState;
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
 * Persistent server-side storage of player resources keyed by player UUID and resource id.
 */
public final class PlayerResourceSavedData extends SavedData {
    private static final Codec<Map<UUID, PlayerResourceState>> LEGACY_STATE_MAP_CODEC =
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, PlayerResourceState.CODEC);
    private static final Codec<Map<String, PlayerTrackedResourceState>> RESOURCE_MAP_CODEC =
            Codec.unboundedMap(Codec.STRING, PlayerTrackedResourceState.CODEC);
    private static final Codec<Map<UUID, Map<String, PlayerTrackedResourceState>>> STATE_MAP_CODEC =
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, RESOURCE_MAP_CODEC);

    private static final Codec<PlayerResourceSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.either(LEGACY_STATE_MAP_CODEC, STATE_MAP_CODEC)
                    .optionalFieldOf("players", Either.right(Map.of()))
                    .forGetter(savedData -> Either.right(savedData.states()))
    ).apply(instance, PlayerResourceSavedData::decodeStates));

    public static final SavedDataType<PlayerResourceSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("esekai2", "player_resources"),
            PlayerResourceSavedData::new,
            CODEC,
            DataFixTypes.LEVEL
    );

    private final Map<UUID, Map<String, PlayerTrackedResourceState>> states;

    public PlayerResourceSavedData() {
        this(Map.of());
    }

    public PlayerResourceSavedData(Map<UUID, Map<String, PlayerTrackedResourceState>> states) {
        Objects.requireNonNull(states, "states");
        this.states = new LinkedHashMap<>();
        for (Map.Entry<UUID, Map<String, PlayerTrackedResourceState>> entry : states.entrySet()) {
            this.states.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
        }
    }

    public Map<UUID, Map<String, PlayerTrackedResourceState>> states() {
        Map<UUID, Map<String, PlayerTrackedResourceState>> snapshot = new LinkedHashMap<>();
        for (Map.Entry<UUID, Map<String, PlayerTrackedResourceState>> entry : states.entrySet()) {
            snapshot.put(entry.getKey(), Map.copyOf(entry.getValue()));
        }
        return Map.copyOf(snapshot);
    }

    public PlayerTrackedResourceState getOrCreate(UUID playerId, String resource, double initialAmount, double maxAmount) {
        Objects.requireNonNull(playerId, "playerId");
        String validatedResource = PlayerResourceIds.requireUsable(resource);
        Map<String, PlayerTrackedResourceState> playerStates = states.computeIfAbsent(playerId, ignored -> new LinkedHashMap<>());
        PlayerTrackedResourceState existing = playerStates.get(validatedResource);
        PlayerTrackedResourceState clamped = clamp(
                existing == null ? new PlayerTrackedResourceState(sanitize(initialAmount)) : existing,
                maxAmount
        );
        if (!clamped.equals(existing)) {
            playerStates.put(validatedResource, clamped);
            setDirty();
        }
        return clamped;
    }

    public PlayerTrackedResourceState getOrCreate(UUID playerId, String resource, double maxAmount) {
        return getOrCreate(playerId, resource, maxAmount, maxAmount);
    }

    public PlayerTrackedResourceState put(UUID playerId, String resource, PlayerTrackedResourceState state, double maxAmount) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(state, "state");
        String validatedResource = PlayerResourceIds.requireUsable(resource);
        PlayerTrackedResourceState clamped = clamp(state, maxAmount);
        states.computeIfAbsent(playerId, ignored -> new LinkedHashMap<>())
                .put(validatedResource, clamped);
        setDirty();
        return clamped;
    }

    private static PlayerTrackedResourceState clamp(PlayerTrackedResourceState state, double maxAmount) {
        double sanitizedMax = sanitize(maxAmount);
        return new PlayerTrackedResourceState(Math.max(0.0, Math.min(state.currentAmount(), sanitizedMax)));
    }

    private static double sanitize(double value) {
        if (!Double.isFinite(value) || value < 0.0) {
            return 0.0;
        }
        return value;
    }

    private static PlayerResourceSavedData decodeStates(
            Either<Map<UUID, PlayerResourceState>, Map<UUID, Map<String, PlayerTrackedResourceState>>> players
    ) {
        if (players.left().isPresent()) {
            Map<UUID, Map<String, PlayerTrackedResourceState>> migrated = new LinkedHashMap<>();
            for (Map.Entry<UUID, PlayerResourceState> entry : players.left().orElseThrow().entrySet()) {
                migrated.put(entry.getKey(), Map.of(PlayerResourceIds.MANA, new PlayerTrackedResourceState(entry.getValue().currentMana())));
            }
            return new PlayerResourceSavedData(migrated);
        }
        return new PlayerResourceSavedData(players.right().orElseThrow());
    }
}
