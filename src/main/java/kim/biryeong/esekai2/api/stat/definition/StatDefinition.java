package kim.biryeong.esekai2.api.stat.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Objects;
import java.util.Optional;

/**
 * Describes a single stat entry loaded from the ESekai stat dynamic registry.
 *
 * @param translationKey translation key used when the stat is presented to players
 * @param defaultValue default numeric value used when no modifier or instance data overrides it
 * @param minValue optional lower bound metadata for the stat
 * @param maxValue optional upper bound metadata for the stat
 */
public record StatDefinition(
        String translationKey,
        double defaultValue,
        Optional<Double> minValue,
        Optional<Double> maxValue
) {
    /**
     * Codec used to load stat definitions from datapacks.
     */
    private static final Codec<StatDefinition> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("translation_key").forGetter(StatDefinition::translationKey),
            Codec.DOUBLE.fieldOf("default_value").forGetter(StatDefinition::defaultValue),
            Codec.DOUBLE.optionalFieldOf("min_value").forGetter(StatDefinition::minValue),
            Codec.DOUBLE.optionalFieldOf("max_value").forGetter(StatDefinition::maxValue)
    ).apply(instance, StatDefinition::new));

    /**
     * Validated codec used to decode stat definitions from datapacks and test fixtures.
     */
    public static final Codec<StatDefinition> CODEC = BASE_CODEC.validate(StatDefinition::validate);

    public StatDefinition {
        Objects.requireNonNull(translationKey, "translationKey");
        Objects.requireNonNull(minValue, "minValue");
        Objects.requireNonNull(maxValue, "maxValue");
    }

    private static DataResult<StatDefinition> validate(StatDefinition definition) {
        if (definition.translationKey().isBlank()) {
            return DataResult.error(() -> "translation_key must not be blank");
        }

        if (definition.minValue().isPresent() && definition.maxValue().isPresent()
                && definition.minValue().get() > definition.maxValue().get()) {
            return DataResult.error(() -> "min_value must be less than or equal to max_value");
        }

        if (definition.minValue().isPresent() && definition.defaultValue() < definition.minValue().get()) {
            return DataResult.error(() -> "default_value must be greater than or equal to min_value");
        }

        if (definition.maxValue().isPresent() && definition.defaultValue() > definition.maxValue().get()) {
            return DataResult.error(() -> "default_value must be less than or equal to max_value");
        }

        return DataResult.success(definition);
    }
}
