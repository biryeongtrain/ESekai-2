package kim.biryeong.esekai2.api.ailment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Persistent ailment payload container attached to live entities.
 *
 * @param activeAilments active ailment payloads keyed by ailment type
 */
public record AilmentState(
        Map<AilmentType, AilmentPayload> activeAilments
) {
    private static final Codec<Map<AilmentType, AilmentPayload>> ACTIVE_CODEC = Codec.unboundedMap(AilmentType.CODEC, AilmentPayload.CODEC)
            .xmap(
                    map -> {
                        Map<AilmentType, AilmentPayload> copy = new EnumMap<>(AilmentType.class);
                        copy.putAll(map);
                        return Map.copyOf(copy);
                    },
                    map -> map
            );

    /**
     * Codec used by persistent entity attachments.
     */
    public static final Codec<AilmentState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ACTIVE_CODEC.optionalFieldOf("active_ailments", Map.of()).forGetter(AilmentState::activeAilments)
    ).apply(instance, AilmentState::new));

    public static final AilmentState EMPTY = new AilmentState(Map.of());

    public AilmentState {
        Objects.requireNonNull(activeAilments, "activeAilments");
        activeAilments = Map.copyOf(activeAilments);
        for (Map.Entry<AilmentType, AilmentPayload> entry : activeAilments.entrySet()) {
            Objects.requireNonNull(entry.getKey(), "activeAilments key");
            Objects.requireNonNull(entry.getValue(), "activeAilments value");
        }
    }

    /**
     * Returns the currently active payload for one ailment type.
     *
     * @param type queried ailment type
     * @return active payload when present
     */
    public Optional<AilmentPayload> get(AilmentType type) {
        Objects.requireNonNull(type, "type");
        return Optional.ofNullable(activeAilments.get(type));
    }

    /**
     * Returns a copy with the provided payload applied or replaced.
     *
     * @param payload ailment payload to store
     * @return copied ailment state containing the payload
     */
    public AilmentState with(AilmentPayload payload) {
        Objects.requireNonNull(payload, "payload");
        Map<AilmentType, AilmentPayload> updated = new EnumMap<>(AilmentType.class);
        updated.putAll(activeAilments);
        updated.put(payload.type(), payload);
        return new AilmentState(updated);
    }

    /**
     * Returns a copy with the queried ailment type removed.
     *
     * @param type ailment type to remove
     * @return copied state without the queried payload
     */
    public AilmentState without(AilmentType type) {
        Objects.requireNonNull(type, "type");
        if (!activeAilments.containsKey(type)) {
            return this;
        }
        Map<AilmentType, AilmentPayload> updated = new EnumMap<>(AilmentType.class);
        updated.putAll(activeAilments);
        updated.remove(type);
        return updated.isEmpty() ? EMPTY : new AilmentState(updated);
    }

    /**
     * Returns whether no ailment payloads are currently active.
     *
     * @return {@code true} when the state is empty
     */
    public boolean isEmpty() {
        return activeAilments.isEmpty();
    }
}
