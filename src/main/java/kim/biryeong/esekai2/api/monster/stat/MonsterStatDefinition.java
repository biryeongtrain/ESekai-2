package kim.biryeong.esekai2.api.monster.stat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.monster.level.MonsterScaledStatAxis;
import kim.biryeong.esekai2.api.stat.definition.StatDefinition;
import kim.biryeong.esekai2.api.stat.definition.StatRegistries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;

import java.util.Map;
import java.util.Objects;

/**
 * Describes the base stat baseline loaded for one monster entity type.
 *
 * @param entityType vanilla entity type that should receive this base stat baseline
 * @param baseStats static base stat values applied when later runtime systems build a fresh monster stat holder
 * @param scaledStats stat axes whose values should be pulled from the monster level table instead of remaining fixed
 */
public record MonsterStatDefinition(
        EntityType<?> entityType,
        Map<ResourceKey<StatDefinition>, Double> baseStats,
        Map<ResourceKey<StatDefinition>, MonsterScaledStatAxis> scaledStats
) {
    private static final Codec<Map<ResourceKey<StatDefinition>, Double>> BASE_STATS_CODEC =
            Codec.unboundedMap(ResourceKey.codec(StatRegistries.STAT), Codec.DOUBLE);
    private static final Codec<Map<ResourceKey<StatDefinition>, MonsterScaledStatAxis>> SCALED_STATS_CODEC =
            Codec.unboundedMap(ResourceKey.codec(StatRegistries.STAT), MonsterScaledStatAxis.CODEC);

    private static final Codec<MonsterStatDefinition> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("entity_type").forGetter(MonsterStatDefinition::entityType),
            BASE_STATS_CODEC.optionalFieldOf("base_stats", Map.of()).forGetter(MonsterStatDefinition::baseStats),
            SCALED_STATS_CODEC.optionalFieldOf("scaled_stats", Map.of()).forGetter(MonsterStatDefinition::scaledStats)
    ).apply(instance, MonsterStatDefinition::new));

    /**
     * Validated codec used to decode monster stat definitions from datapacks and test fixtures.
     */
    public static final Codec<MonsterStatDefinition> CODEC = BASE_CODEC.validate(MonsterStatDefinition::validate);

    public MonsterStatDefinition {
        Objects.requireNonNull(entityType, "entityType");
        Objects.requireNonNull(baseStats, "baseStats");
        Objects.requireNonNull(scaledStats, "scaledStats");

        baseStats = Map.copyOf(baseStats);
        scaledStats = Map.copyOf(scaledStats);
        if (baseStats.isEmpty() && scaledStats.isEmpty()) {
            throw new IllegalArgumentException("monster stat definitions must define at least one base or scaled stat");
        }

        for (Map.Entry<ResourceKey<StatDefinition>, Double> entry : baseStats.entrySet()) {
            Objects.requireNonNull(entry.getKey(), "baseStats stat key");
            double value = Objects.requireNonNull(entry.getValue(), "baseStats value");
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException("baseStats values must be finite numbers");
            }
            if (scaledStats.containsKey(entry.getKey())) {
                throw new IllegalArgumentException("baseStats and scaledStats must not overlap on the same stat");
            }
        }

        for (Map.Entry<ResourceKey<StatDefinition>, MonsterScaledStatAxis> entry : scaledStats.entrySet()) {
            Objects.requireNonNull(entry.getKey(), "scaledStats stat key");
            Objects.requireNonNull(entry.getValue(), "scaledStats axis");
        }
    }

    private static DataResult<MonsterStatDefinition> validate(MonsterStatDefinition definition) {
        if (definition.baseStats().isEmpty() && definition.scaledStats().isEmpty()) {
            return DataResult.error(() -> "monster stat definitions must define at least one base or scaled stat");
        }

        for (Map.Entry<ResourceKey<StatDefinition>, Double> entry : definition.baseStats().entrySet()) {
            if (!Double.isFinite(entry.getValue())) {
                return DataResult.error(() -> "base_stats values must be finite numbers");
            }
            if (definition.scaledStats().containsKey(entry.getKey())) {
                return DataResult.error(() -> "base_stats and scaled_stats must not overlap on the same stat");
            }
        }

        return DataResult.success(definition);
    }
}
