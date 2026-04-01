package kim.biryeong.esekai2.api.player.resource;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.stat.definition.StatDefinition;
import kim.biryeong.esekai2.api.stat.definition.StatRegistries;
import net.minecraft.resources.ResourceKey;

import java.util.Objects;
import java.util.Optional;

/**
 * Datapack-backed metadata that defines one runtime player resource axis.
 *
 * @param maxStat combat stat used as the resource maximum
 * @param regenerationPerSecondStat optional combat stat used as the resource regeneration rate
 * @param startsFull whether newly created state should initialize at the current maximum
 */
public record PlayerResourceDefinition(
        ResourceKey<StatDefinition> maxStat,
        Optional<ResourceKey<StatDefinition>> regenerationPerSecondStat,
        boolean startsFull
) {
    private static final Codec<PlayerResourceDefinition> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceKey.codec(StatRegistries.STAT).fieldOf("max_stat").forGetter(PlayerResourceDefinition::maxStat),
            ResourceKey.codec(StatRegistries.STAT).optionalFieldOf("regeneration_per_second_stat")
                    .forGetter(PlayerResourceDefinition::regenerationPerSecondStat),
            Codec.BOOL.optionalFieldOf("starts_full", true).forGetter(PlayerResourceDefinition::startsFull)
    ).apply(instance, PlayerResourceDefinition::new));

    /**
     * Validated codec used to decode `esekai2:player_resource` entries.
     */
    public static final Codec<PlayerResourceDefinition> CODEC = BASE_CODEC.validate(PlayerResourceDefinition::validate);

    public PlayerResourceDefinition {
        Objects.requireNonNull(maxStat, "maxStat");
        Objects.requireNonNull(regenerationPerSecondStat, "regenerationPerSecondStat");
    }

    private static DataResult<PlayerResourceDefinition> validate(PlayerResourceDefinition definition) {
        if (definition.regenerationPerSecondStat().filter(definition.maxStat()::equals).isPresent()) {
            return DataResult.error(() -> "regeneration_per_second_stat must not equal max_stat");
        }
        return DataResult.success(definition);
    }
}
