package kim.biryeong.esekai2.api.skill.support;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.player.resource.PlayerResourceIds;

import java.util.Objects;
import java.util.Optional;

/**
 * Typed support-level override for the skill config runtime surface.
 *
 * <p>This override can rewrite the resource id and a bounded subset of prepared runtime config
 * values so support-linked preparation and selected casts observe the same merged config.</p>
 *
 * @param resource optional replacement resource id
 * @param resourceCost optional replacement resource cost
 * @param castTimeTicks optional replacement cast time in ticks
 * @param cooldownTicks optional replacement cooldown in ticks
 * @param timesToCast optional replacement total burst cast count
 * @param charges optional replacement maximum charges
 */
public record SkillConfigOverride(
        Optional<String> resource,
        Optional<Double> resourceCost,
        Optional<Integer> castTimeTicks,
        Optional<Integer> cooldownTicks,
        Optional<Integer> timesToCast,
        Optional<Integer> charges
) {
    private static final Codec<SkillConfigOverride> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("resource").forGetter(SkillConfigOverride::resource),
            Codec.DOUBLE.optionalFieldOf("resource_cost").forGetter(SkillConfigOverride::resourceCost),
            Codec.INT.optionalFieldOf("cast_time_ticks").forGetter(SkillConfigOverride::castTimeTicks),
            Codec.INT.optionalFieldOf("cooldown_ticks").forGetter(SkillConfigOverride::cooldownTicks),
            Codec.INT.optionalFieldOf("times_to_cast").forGetter(SkillConfigOverride::timesToCast),
            Codec.INT.optionalFieldOf("charges").forGetter(SkillConfigOverride::charges)
    ).apply(instance, SkillConfigOverride::new));

    /**
     * Validated codec used by support datapacks.
     */
    public static final Codec<SkillConfigOverride> CODEC = BASE_CODEC.validate(SkillConfigOverride::validate);

    public SkillConfigOverride {
        Objects.requireNonNull(resource, "resource");
        Objects.requireNonNull(resourceCost, "resourceCost");
        Objects.requireNonNull(castTimeTicks, "castTimeTicks");
        Objects.requireNonNull(cooldownTicks, "cooldownTicks");
        Objects.requireNonNull(timesToCast, "timesToCast");
        Objects.requireNonNull(charges, "charges");
        resource = resource.map(String::trim);
    }

    /**
     * Compatibility constructor retaining the resource-only override surface.
     *
     * @param resource optional replacement resource id
     * @param resourceCost optional replacement resource cost
     */
    public SkillConfigOverride(
            Optional<String> resource,
            Optional<Double> resourceCost
    ) {
        this(resource, resourceCost, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    /**
     * Creates an override that only rewrites the configured resource id.
     *
     * @param resource replacement resource id
     * @return resource-only override
     */
    public static SkillConfigOverride resource(String resource) {
        return new SkillConfigOverride(Optional.of(resource), Optional.empty());
    }

    /**
     * Creates an override that only rewrites the configured resource cost.
     *
     * @param resourceCost replacement resource cost
     * @return resource-cost-only override
     */
    public static SkillConfigOverride resourceCost(double resourceCost) {
        return new SkillConfigOverride(Optional.empty(), Optional.of(resourceCost));
    }

    /**
     * Creates an override that rewrites both resource and resource cost.
     *
     * @param resource replacement resource id
     * @param resourceCost replacement resource cost
     * @return combined override
     */
    public static SkillConfigOverride resource(String resource, double resourceCost) {
        return new SkillConfigOverride(Optional.of(resource), Optional.of(resourceCost));
    }

    private static DataResult<SkillConfigOverride> validate(SkillConfigOverride override) {
        if (override.resource().isEmpty()
                && override.resourceCost().isEmpty()
                && override.castTimeTicks().isEmpty()
                && override.cooldownTicks().isEmpty()
                && override.timesToCast().isEmpty()
                && override.charges().isEmpty()) {
            return DataResult.error(() -> "config override must define at least one supported field");
        }
        if (override.resource().isPresent() && !PlayerResourceIds.isUsable(override.resource().orElseThrow())) {
            return DataResult.error(() -> "resource override must be a non-blank resource id");
        }
        if (override.resourceCost().isPresent()) {
            double resourceCost = override.resourceCost().orElseThrow();
            if (!Double.isFinite(resourceCost) || resourceCost < 0.0) {
                return DataResult.error(() -> "resource_cost override must be finite and >= 0");
            }
        }
        if (override.castTimeTicks().isPresent() && override.castTimeTicks().orElseThrow() < 0) {
            return DataResult.error(() -> "cast_time_ticks override must be >= 0");
        }
        if (override.cooldownTicks().isPresent() && override.cooldownTicks().orElseThrow() < 0) {
            return DataResult.error(() -> "cooldown_ticks override must be >= 0");
        }
        if (override.timesToCast().isPresent() && override.timesToCast().orElseThrow() < 1) {
            return DataResult.error(() -> "times_to_cast override must be >= 1");
        }
        if (override.charges().isPresent() && override.charges().orElseThrow() < 0) {
            return DataResult.error(() -> "charges override must be >= 0");
        }
        return DataResult.success(override);
    }
}
