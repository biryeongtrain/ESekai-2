package kim.biryeong.esekai2.api.monster.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.stat.definition.StatDefinition;
import kim.biryeong.esekai2.api.stat.definition.StatRegistries;
import kim.biryeong.esekai2.api.stat.modifier.StatModifierOperation;
import net.minecraft.resources.ResourceKey;

import java.util.Objects;

/**
 * Describes one ranged stat modifier that a monster affix can roll into a concrete stat modifier.
 *
 * @param stat stat targeted by the monster affix modifier
 * @param operation operation used when the rolled modifier is later applied
 * @param minValue inclusive lower bound for the rolled modifier value
 * @param maxValue inclusive upper bound for the rolled modifier value
 */
public record MonsterAffixModifierDefinition(
        ResourceKey<StatDefinition> stat,
        StatModifierOperation operation,
        double minValue,
        double maxValue
) {
    private static final Codec<MonsterAffixModifierDefinition> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceKey.codec(StatRegistries.STAT).fieldOf("stat").forGetter(MonsterAffixModifierDefinition::stat),
            StatModifierOperation.CODEC.fieldOf("operation").forGetter(MonsterAffixModifierDefinition::operation),
            Codec.DOUBLE.fieldOf("min_value").forGetter(MonsterAffixModifierDefinition::minValue),
            Codec.DOUBLE.fieldOf("max_value").forGetter(MonsterAffixModifierDefinition::maxValue)
    ).apply(instance, MonsterAffixModifierDefinition::new));

    /**
     * Validated codec used to decode monster affix modifier definitions.
     */
    public static final Codec<MonsterAffixModifierDefinition> CODEC = BASE_CODEC.validate(MonsterAffixModifierDefinition::validate);

    public MonsterAffixModifierDefinition {
        Objects.requireNonNull(stat, "stat");
        Objects.requireNonNull(operation, "operation");
    }

    private static DataResult<MonsterAffixModifierDefinition> validate(MonsterAffixModifierDefinition definition) {
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
