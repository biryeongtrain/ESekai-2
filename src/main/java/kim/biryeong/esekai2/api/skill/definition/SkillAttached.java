package kim.biryeong.esekai2.api.skill.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.skill.definition.graph.SkillRule;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Execution attachment graph entrypoint for skill actions.
 *
 * <p>Only on_cast and entity component routes are currently modeled.
 * Runtime resolves {@code entity_components} names against spawned skill entities.</p>
 *
 * @param onCast action rules executed immediately when the spell is cast
 * @param entityComponents named runtime action maps for spawned skill entities
 */
public record SkillAttached(
        List<SkillRule> onCast,
        Map<String, List<SkillRule>> entityComponents
) {
    private static final Codec<SkillAttached> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SkillRule.CODEC.listOf().optionalFieldOf("on_cast", List.of()).forGetter(SkillAttached::onCast),
            Codec.unboundedMap(Codec.STRING, SkillRule.CODEC.listOf()).optionalFieldOf("entity_components", Map.of()).forGetter(SkillAttached::entityComponents)
    ).apply(instance, SkillAttached::new));

    /**
     * Validated codec used to decode attached rule graphs from datapacks.
     */
    public static final Codec<SkillAttached> CODEC = BASE_CODEC;

    public SkillAttached {
        Objects.requireNonNull(onCast, "onCast");
        Objects.requireNonNull(entityComponents, "entityComponents");

        onCast = List.copyOf(onCast);
        entityComponents = Map.copyOf(entityComponents);
    }

    /**
     * Shared fallback attached graph used by definitions that only declare config values.
     */
    public static final SkillAttached EMPTY = new SkillAttached(List.of(), Map.of());
}
