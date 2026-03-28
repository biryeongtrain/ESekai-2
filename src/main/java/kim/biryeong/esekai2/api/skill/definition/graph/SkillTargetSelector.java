package kim.biryeong.esekai2.api.skill.definition.graph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Map;
import java.util.Objects;

/**
 * Target selector node parameter that determines which entities are affected by an action.
 *
 * @param type target addressing strategy
 * @param parameters type-specific parameters (radius, range, cone angle, limits)
 */
public record SkillTargetSelector(
        SkillTargetType type,
        Map<String, String> parameters
) {
    private static final Codec<SkillTargetSelector> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SkillTargetType.CODEC.fieldOf("type").forGetter(SkillTargetSelector::type),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("map", Map.of())
                    .forGetter(SkillTargetSelector::parameters)
    ).apply(instance, SkillTargetSelector::new));

    /**
     * Validated codec used to decode target selector entries.
     */
    public static final Codec<SkillTargetSelector> CODEC = BASE_CODEC.validate(SkillTargetSelector::validate);

    public SkillTargetSelector {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(parameters, "parameters");
        parameters = Map.copyOf(parameters);
    }

    private static com.mojang.serialization.DataResult<SkillTargetSelector> validate(SkillTargetSelector selector) {
        if (!selector.parameters().containsKey("radius")) {
            return com.mojang.serialization.DataResult.success(selector);
        }

        String rawRadius = selector.parameters().get("radius");
        if (rawRadius == null || rawRadius.isBlank()) {
            return com.mojang.serialization.DataResult.error(() -> "radius must be present when provided");
        }

        try {
            double radius = Double.parseDouble(rawRadius);
            if (!Double.isFinite(radius) || radius < 0.0) {
                return com.mojang.serialization.DataResult.error(() -> "radius must be a finite number >= 0");
            }
        } catch (NumberFormatException exception) {
            return com.mojang.serialization.DataResult.error(() -> "radius must be a numeric string");
        }

        return com.mojang.serialization.DataResult.success(selector);
    }

    /**
     * Default selector used for implicit on-cast single-target behavior.
     */
    public static final SkillTargetSelector DEFAULT_SELF = new SkillTargetSelector(SkillTargetType.SELF, Map.of());
}
