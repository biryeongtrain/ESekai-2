package kim.biryeong.esekai2.api.damage.transform;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.damage.breakdown.DamageType;

import java.util.Objects;

/**
 * Describes one typed damage conversion applied before scaling and mitigation.
 *
 * @param from source damage type consumed by this conversion entry
 * @param to target damage type created by this conversion entry
 * @param valuePercent percentage of the source damage to convert
 */
public record DamageConversion(
        DamageType from,
        DamageType to,
        double valuePercent
) {
    private static final Codec<DamageConversion> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DamageType.CODEC.fieldOf("from").forGetter(DamageConversion::from),
            DamageType.CODEC.fieldOf("to").forGetter(DamageConversion::to),
            Codec.DOUBLE.fieldOf("value_percent").forGetter(DamageConversion::valuePercent)
    ).apply(instance, DamageConversion::new));

    /**
     * Validated codec used to decode and encode typed damage conversion entries.
     */
    public static final Codec<DamageConversion> CODEC = BASE_CODEC.validate(DamageConversion::validate);

    public DamageConversion {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");

        if (from == to) {
            throw new IllegalArgumentException("conversion source and target must differ");
        }

        if (!Double.isFinite(valuePercent)) {
            throw new IllegalArgumentException("valuePercent must be a finite number");
        }

        if (valuePercent < 0.0 || valuePercent > 100.0) {
            throw new IllegalArgumentException("valuePercent must be between 0 and 100");
        }
    }

    private static DataResult<DamageConversion> validate(DamageConversion conversion) {
        if (conversion.from() == conversion.to()) {
            return DataResult.error(() -> "conversion source and target must differ");
        }

        if (!Double.isFinite(conversion.valuePercent())) {
            return DataResult.error(() -> "valuePercent must be a finite number");
        }

        if (conversion.valuePercent() < 0.0 || conversion.valuePercent() > 100.0) {
            return DataResult.error(() -> "valuePercent must be between 0 and 100");
        }

        return DataResult.success(conversion);
    }
}
