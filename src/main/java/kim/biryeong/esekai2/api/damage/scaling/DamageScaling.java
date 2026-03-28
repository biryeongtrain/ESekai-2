package kim.biryeong.esekai2.api.damage.scaling;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Objects;

/**
 * Describes one scaling contribution applied during hit damage calculation.
 *
 * @param target damage target matched by this scaling entry
 * @param operation scaling bucket used when applying this entry
 * @param value numeric scaling value carried by this entry
 */
public record DamageScaling(
        DamageScalingTarget target,
        DamageScalingOperation operation,
        double value
) {
    private static final Codec<DamageScaling> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DamageScalingTarget.CODEC.fieldOf("target").forGetter(DamageScaling::target),
            DamageScalingOperation.CODEC.fieldOf("operation").forGetter(DamageScaling::operation),
            Codec.DOUBLE.fieldOf("value").forGetter(DamageScaling::value)
    ).apply(instance, DamageScaling::new));

    /**
     * Validated codec used to decode and encode damage scaling entries.
     */
    public static final Codec<DamageScaling> CODEC = BASE_CODEC.validate(DamageScaling::validate);

    public DamageScaling {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(operation, "operation");

        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("value must be a finite number");
        }

        if (operation == DamageScalingOperation.ADD && target.isAll()) {
            throw new IllegalArgumentException("ADD scaling must target a specific damage type");
        }

        if (operation == DamageScalingOperation.MORE && value < -100.0) {
            throw new IllegalArgumentException("MORE scaling value must be greater than or equal to -100");
        }
    }

    private static DataResult<DamageScaling> validate(DamageScaling scaling) {
        if (!Double.isFinite(scaling.value())) {
            return DataResult.error(() -> "value must be a finite number");
        }

        if (scaling.operation() == DamageScalingOperation.ADD && scaling.target().isAll()) {
            return DataResult.error(() -> "ADD scaling must target a specific damage type");
        }

        if (scaling.operation() == DamageScalingOperation.MORE && scaling.value() < -100.0) {
            return DataResult.error(() -> "MORE scaling value must be greater than or equal to -100");
        }

        return DataResult.success(scaling);
    }
}
