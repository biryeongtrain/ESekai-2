package kim.biryeong.esekai2.api.stat.modifier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.stat.definition.StatDefinition;
import kim.biryeong.esekai2.api.stat.definition.StatRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.Objects;

/**
 * Describes a single contribution applied to a stat by a source such as an item, skill, or monster modifier.
 *
 * @param stat stat targeted by this modifier
 * @param operation operation used when the modifier is later applied in a stat calculation pipeline
 * @param value numeric value carried by the modifier
 * @param sourceId stable identifier of the system or content entry that produced this modifier
 */
public record StatModifier(
        ResourceKey<StatDefinition> stat,
        StatModifierOperation operation,
        double value,
        Identifier sourceId
) {
    private static final Codec<StatModifier> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceKey.codec(StatRegistries.STAT).fieldOf("stat").forGetter(StatModifier::stat),
            StatModifierOperation.CODEC.fieldOf("operation").forGetter(StatModifier::operation),
            Codec.DOUBLE.fieldOf("value").forGetter(StatModifier::value),
            Identifier.CODEC.fieldOf("source_id").forGetter(StatModifier::sourceId)
    ).apply(instance, StatModifier::new));

    /**
     * Validated codec used to decode stat modifiers from datapacks and test fixtures.
     */
    public static final Codec<StatModifier> CODEC = BASE_CODEC.validate(StatModifier::validate);

    public StatModifier {
        Objects.requireNonNull(stat, "stat");
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(sourceId, "sourceId");
    }

    private static DataResult<StatModifier> validate(StatModifier modifier) {
        if (!Double.isFinite(modifier.value())) {
            return DataResult.error(() -> "value must be a finite number");
        }

        return DataResult.success(modifier);
    }
}
