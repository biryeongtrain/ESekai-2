package kim.biryeong.esekai2.api.damage.mitigation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.damage.breakdown.DamageType;

import java.util.Objects;

/**
 * Describes one elemental resistance reduction entry applied before resistance caps.
 *
 * @param type elemental damage type affected by this exposure
 * @param value resistance value subtracted from the target before cap checks
 */
public record ElementalExposure(
        DamageType type,
        double value
) {
    private static final Codec<ElementalExposure> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DamageType.CODEC.fieldOf("type").forGetter(ElementalExposure::type),
            Codec.DOUBLE.fieldOf("value").forGetter(ElementalExposure::value)
    ).apply(instance, ElementalExposure::new));

    /**
     * Validated codec used to decode and encode elemental exposure entries.
     */
    public static final Codec<ElementalExposure> CODEC = BASE_CODEC.validate(ElementalExposure::validate);

    public ElementalExposure {
        Objects.requireNonNull(type, "type");

        if (!supportsType(type)) {
            throw new IllegalArgumentException("elemental exposure only supports fire, cold, or lightning damage");
        }

        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("value must be a finite number");
        }

        if (value < 0.0) {
            throw new IllegalArgumentException("value must be greater than or equal to 0");
        }
    }

    /**
     * Returns whether this exposure can affect the provided damage type.
     *
     * @param type damage type being inspected
     * @return {@code true} when the exposure applies to the type
     */
    public boolean appliesTo(DamageType type) {
        return this.type == Objects.requireNonNull(type, "type");
    }

    private static DataResult<ElementalExposure> validate(ElementalExposure exposure) {
        if (!supportsType(exposure.type())) {
            return DataResult.error(() -> "elemental exposure only supports fire, cold, or lightning damage");
        }

        if (!Double.isFinite(exposure.value())) {
            return DataResult.error(() -> "value must be a finite number");
        }

        if (exposure.value() < 0.0) {
            return DataResult.error(() -> "value must be greater than or equal to 0");
        }

        return DataResult.success(exposure);
    }

    private static boolean supportsType(DamageType type) {
        return type == DamageType.FIRE || type == DamageType.COLD || type == DamageType.LIGHTNING;
    }
}
