package kim.biryeong.esekai2.api.skill.definition.graph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Map;
import java.util.Objects;

/**
 * Runtime predicate placeholder evaluated by entity component loops.
 *
 * @param type predicate discriminant
 * @param parameters predicate-specific key-values
 */
public record SkillPredicate(
        SkillPredicateType type,
        Map<String, String> parameters
) {
    private static final Codec<SkillPredicate> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SkillPredicateType.CODEC.fieldOf("type").forGetter(SkillPredicate::type),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("map", Map.of())
                    .forGetter(SkillPredicate::parameters)
    ).apply(instance, SkillPredicate::new));

    /**
     * Validated codec used to decode predicate nodes.
     */
    public static final Codec<SkillPredicate> CODEC = BASE_CODEC.validate(SkillPredicate::validate);

    public SkillPredicate {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(parameters, "parameters");
        parameters = Map.copyOf(parameters);
    }

    private static DataResult<SkillPredicate> validate(SkillPredicate predicate) {
        if (predicate.type() == SkillPredicateType.RANDOM_CHANCE) {
            String rawChance = predicate.parameters().get("chance");
            if (rawChance == null || rawChance.isBlank()) {
                return DataResult.error(() -> "random_chance predicate requires chance");
            }
            try {
                double chance = Double.parseDouble(rawChance);
                if (!Double.isFinite(chance) || chance < 0.0 || chance > 1.0) {
                    return DataResult.error(() -> "chance must be within [0, 1]");
                }
            } catch (NumberFormatException exception) {
                return DataResult.error(() -> "chance must be a numeric string");
            }
        }
        return DataResult.success(predicate);
    }
}
