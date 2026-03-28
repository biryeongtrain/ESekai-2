package kim.biryeong.esekai2.api.damage.mitigation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.damage.breakdown.DamageType;

import java.util.Objects;

/**
 * Describes one resistance penetration entry applied after resistance caps during hit mitigation.
 *
 * @param type resistable damage type affected by this penetration entry
 * @param value resistance value subtracted from the capped resistance during hit mitigation
 */
public record ResistancePenetration(
        DamageType type,
        double value
) {
    private static final Codec<ResistancePenetration> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DamageType.CODEC.fieldOf("type").forGetter(ResistancePenetration::type),
            Codec.DOUBLE.fieldOf("value").forGetter(ResistancePenetration::value)
    ).apply(instance, ResistancePenetration::new));

    /**
     * Validated codec used to decode and encode resistance penetration entries.
     */
    public static final Codec<ResistancePenetration> CODEC = BASE_CODEC.validate(ResistancePenetration::validate);

    public ResistancePenetration {
        Objects.requireNonNull(type, "type");

        if (!supportsType(type)) {
            throw new IllegalArgumentException("resistance penetration only supports fire, cold, lightning, or chaos damage");
        }

        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("value must be a finite number");
        }

        if (value < 0.0) {
            throw new IllegalArgumentException("value must be greater than or equal to 0");
        }
    }

    /**
     * Returns whether this penetration entry can affect the provided damage type.
     *
     * @param type damage type being inspected
     * @return {@code true} when the penetration applies to the type
     */
    public boolean appliesTo(DamageType type) {
        return this.type == Objects.requireNonNull(type, "type");
    }

    private static DataResult<ResistancePenetration> validate(ResistancePenetration penetration) {
        if (!supportsType(penetration.type())) {
            return DataResult.error(() -> "resistance penetration only supports fire, cold, lightning, or chaos damage");
        }

        if (!Double.isFinite(penetration.value())) {
            return DataResult.error(() -> "value must be a finite number");
        }

        if (penetration.value() < 0.0) {
            return DataResult.error(() -> "value must be greater than or equal to 0");
        }

        return DataResult.success(penetration);
    }

    private static boolean supportsType(DamageType type) {
        return type == DamageType.FIRE
                || type == DamageType.COLD
                || type == DamageType.LIGHTNING
                || type == DamageType.CHAOS;
    }
}
