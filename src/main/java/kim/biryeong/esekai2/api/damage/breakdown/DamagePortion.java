package kim.biryeong.esekai2.api.damage.breakdown;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Objects;

/**
 * Represents one typed damage contribution inside a larger damage payload.
 *
 * @param type hardcoded damage category carried by this portion
 * @param amount non-negative finite damage amount assigned to the category
 */
public record DamagePortion(
        DamageType type,
        double amount
) {
    private static final Codec<DamagePortion> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DamageType.CODEC.fieldOf("type").forGetter(DamagePortion::type),
            Codec.DOUBLE.fieldOf("amount").forGetter(DamagePortion::amount)
    ).apply(instance, DamagePortion::new));

    /**
     * Validated codec used to decode and encode typed damage entries.
     */
    public static final Codec<DamagePortion> CODEC = BASE_CODEC.validate(DamagePortion::validate);

    public DamagePortion {
        Objects.requireNonNull(type, "type");
    }

    private static DataResult<DamagePortion> validate(DamagePortion portion) {
        if (!Double.isFinite(portion.amount())) {
            return DataResult.error(() -> "amount must be a finite number");
        }

        if (portion.amount() < 0.0) {
            return DataResult.error(() -> "amount must be greater than or equal to zero");
        }

        return DataResult.success(portion);
    }
}
