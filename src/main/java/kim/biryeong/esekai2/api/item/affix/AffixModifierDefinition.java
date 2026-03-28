package kim.biryeong.esekai2.api.item.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.stat.definition.StatDefinition;
import kim.biryeong.esekai2.api.stat.definition.StatRegistries;
import kim.biryeong.esekai2.api.stat.modifier.StatModifierOperation;
import net.minecraft.resources.ResourceKey;

import java.util.Objects;

/**
 * Describes one ranged stat modifier that an affix can roll into a concrete stat modifier.
 *
 * @param stat stat targeted by the affix modifier
 * @param operation operation used when the rolled modifier is later applied
 * @param minValue inclusive lower bound for the rolled modifier value
 * @param maxValue inclusive upper bound for the rolled modifier value
 */
public record AffixModifierDefinition(
        ResourceKey<StatDefinition> stat,
        StatModifierOperation operation,
        double minValue,
        double maxValue
) {
    private static final Codec<AffixModifierDefinition> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceKey.codec(StatRegistries.STAT).fieldOf("stat").forGetter(AffixModifierDefinition::stat),
            StatModifierOperation.CODEC.fieldOf("operation").forGetter(AffixModifierDefinition::operation),
            Codec.DOUBLE.fieldOf("min_value").forGetter(AffixModifierDefinition::minValue),
            Codec.DOUBLE.fieldOf("max_value").forGetter(AffixModifierDefinition::maxValue)
    ).apply(instance, AffixModifierDefinition::new));

    /**
     * Validated codec used to decode affix modifier definitions from datapacks and test fixtures.
     */
    public static final Codec<AffixModifierDefinition> CODEC = BASE_CODEC.validate(AffixModifierDefinition::validate);

    public AffixModifierDefinition {
        Objects.requireNonNull(stat, "stat");
        Objects.requireNonNull(operation, "operation");
    }

    private static DataResult<AffixModifierDefinition> validate(AffixModifierDefinition definition) {
        if (!Double.isFinite(definition.minValue())) {
            return DataResult.error(() -> "min_value must be a finite number");
        }

        if (!Double.isFinite(definition.maxValue())) {
            return DataResult.error(() -> "max_value must be a finite number");
        }

        if (definition.minValue() > definition.maxValue()) {
            return DataResult.error(() -> "min_value must be less than or equal to max_value");
        }

        return DataResult.success(definition);
    }
}
