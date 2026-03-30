package kim.biryeong.esekai2.api.ailment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistent runtime ailment payload stored on live entities alongside a MobEffect identity.
 *
 * @param type ailment type represented by this payload
 * @param sourceSkillId skill id that applied the ailment
 * @param sourceEntityUuid optional source entity id that applied the ailment
 * @param potency numeric potency value interpreted by the ailment type
 * @param durationTicks original ailment duration at application time
 * @param remainingTicks remaining ailment duration
 * @param tickIntervalTicks tick interval used by periodic ailment damage
 */
public record AilmentPayload(
        AilmentType type,
        String sourceSkillId,
        Optional<UUID> sourceEntityUuid,
        double potency,
        int durationTicks,
        int remainingTicks,
        int tickIntervalTicks
) {
    /**
     * Codec used by persistent entity attachments.
     */
    public static final Codec<AilmentPayload> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AilmentType.CODEC.fieldOf("type").forGetter(AilmentPayload::type),
            Codec.STRING.fieldOf("source_skill_id").forGetter(AilmentPayload::sourceSkillId),
            UUIDUtil.STRING_CODEC.optionalFieldOf("source_entity_uuid").forGetter(AilmentPayload::sourceEntityUuid),
            Codec.DOUBLE.fieldOf("potency").forGetter(AilmentPayload::potency),
            Codec.INT.fieldOf("duration_ticks").forGetter(AilmentPayload::durationTicks),
            Codec.INT.fieldOf("remaining_ticks").forGetter(AilmentPayload::remainingTicks),
            Codec.INT.optionalFieldOf("tick_interval_ticks", 1).forGetter(AilmentPayload::tickIntervalTicks)
    ).apply(instance, AilmentPayload::new));

    public AilmentPayload {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(sourceSkillId, "sourceSkillId");
        Objects.requireNonNull(sourceEntityUuid, "sourceEntityUuid");
        if (sourceSkillId.isBlank()) {
            throw new IllegalArgumentException("sourceSkillId must not be blank");
        }
        if (!Double.isFinite(potency) || potency < 0.0) {
            throw new IllegalArgumentException("potency must be a finite number >= 0");
        }
        if (durationTicks < 0) {
            throw new IllegalArgumentException("durationTicks must be >= 0");
        }
        if (remainingTicks < 0) {
            throw new IllegalArgumentException("remainingTicks must be >= 0");
        }
        if (tickIntervalTicks <= 0) {
            throw new IllegalArgumentException("tickIntervalTicks must be > 0");
        }
    }

    /**
     * Returns whether this payload has expired.
     *
     * @return {@code true} when no remaining duration is left
     */
    public boolean isExpired() {
        return remainingTicks <= 0;
    }

    /**
     * Returns whether the payload should trigger a periodic damage tick after one runtime tick elapses.
     *
     * @return {@code true} when the next runtime tick should apply periodic ailment damage
     */
    public boolean shouldTriggerDamageTick() {
        return type.isDamageOverTime() && remainingTicks > 0 && remainingTicks % tickIntervalTicks == 0;
    }

    /**
     * Returns a copy with one tick of remaining duration consumed.
     *
     * @return copied payload with decremented remaining duration
     */
    public AilmentPayload tick() {
        if (remainingTicks <= 0) {
            return this;
        }
        return new AilmentPayload(type, sourceSkillId, sourceEntityUuid, potency, durationTicks, remainingTicks - 1, tickIntervalTicks);
    }
}
