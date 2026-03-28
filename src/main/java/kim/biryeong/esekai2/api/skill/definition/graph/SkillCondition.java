package kim.biryeong.esekai2.api.skill.definition.graph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Map;
import java.util.Objects;

/**
 * Predicate-like conditional branch that decides when a rule executes.
 *
 * @param type condition discriminant
 * @param parameters condition-specific key-values
 */
public record SkillCondition(
        SkillConditionType type,
        Map<String, String> parameters
) {
    private static final Codec<SkillCondition> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SkillConditionType.CODEC.fieldOf("type").forGetter(SkillCondition::type),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("map", Map.of())
                    .forGetter(SkillCondition::parameters)
    ).apply(instance, SkillCondition::new));

    /**
     * Validated codec used to decode rule conditions.
     */
    public static final Codec<SkillCondition> CODEC = BASE_CODEC.validate(SkillCondition::validate);

    public SkillCondition {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(parameters, "parameters");
        parameters = Map.copyOf(parameters);
    }

    private static DataResult<SkillCondition> validate(SkillCondition condition) {
        if (condition.type() == SkillConditionType.X_TICKS_CONDITION) {
            String rawTicks = condition.parameters().get("x_ticks");
            if (rawTicks == null || rawTicks.isBlank()) {
                rawTicks = condition.parameters().get("tick_rate");
            }
            if (rawTicks == null || rawTicks.isBlank()) {
                rawTicks = condition.parameters().get("ticks");
            }
            if (rawTicks == null || rawTicks.isBlank()) {
                return DataResult.error(() -> "x_ticks_condition requires x_ticks, tick_rate, or ticks");
            }
            try {
                int ticks = Integer.parseInt(rawTicks);
                if (ticks < 0) {
                    return DataResult.error(() -> "tick interval must be >= 0");
                }
            } catch (NumberFormatException exception) {
                return DataResult.error(() -> "tick interval must be an integer string");
            }
        }

        return DataResult.success(condition);
    }
}
