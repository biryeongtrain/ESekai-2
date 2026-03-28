package kim.biryeong.esekai2.api.damage.transform;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.damage.breakdown.DamageType;

import java.util.Objects;

/**
 * Describes one gain-as-extra rule applied after conversion but before scaling.
 *
 * @param source source damage type sampled when computing the extra damage amount
 * @param gainedType target damage type created by this extra gain entry
 * @param valuePercent percentage of the source damage added as extra damage
 */
public record ExtraDamageGain(
        DamageType source,
        DamageType gainedType,
        double valuePercent
) {
    private static final Codec<ExtraDamageGain> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DamageType.CODEC.fieldOf("source").forGetter(ExtraDamageGain::source),
            DamageType.CODEC.fieldOf("gained_type").forGetter(ExtraDamageGain::gainedType),
            Codec.DOUBLE.fieldOf("value_percent").forGetter(ExtraDamageGain::valuePercent)
    ).apply(instance, ExtraDamageGain::new));

    /**
     * Validated codec used to decode and encode gain-as-extra entries.
     */
    public static final Codec<ExtraDamageGain> CODEC = BASE_CODEC.validate(ExtraDamageGain::validate);

    public ExtraDamageGain {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(gainedType, "gainedType");

        if (source == gainedType) {
            throw new IllegalArgumentException("extra damage source and target must differ");
        }

        if (!Double.isFinite(valuePercent)) {
            throw new IllegalArgumentException("valuePercent must be a finite number");
        }

        if (valuePercent < 0.0) {
            throw new IllegalArgumentException("valuePercent must be greater than or equal to 0");
        }
    }

    private static DataResult<ExtraDamageGain> validate(ExtraDamageGain gain) {
        if (gain.source() == gain.gainedType()) {
            return DataResult.error(() -> "extra damage source and target must differ");
        }

        if (!Double.isFinite(gain.valuePercent())) {
            return DataResult.error(() -> "valuePercent must be a finite number");
        }

        if (gain.valuePercent() < 0.0) {
            return DataResult.error(() -> "valuePercent must be greater than or equal to 0");
        }

        return DataResult.success(gain);
    }
}
