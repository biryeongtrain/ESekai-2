package kim.biryeong.esekai2.api.skill.definition.graph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.skill.value.SkillValueExpression;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Typed condition node that decides which execution event bucket a rule belongs to.
 *
 * @param type condition discriminant
 * @param interval tick interval payload used by {@link SkillConditionType#X_TICKS_CONDITION}
 */
public record SkillCondition(
        SkillConditionType type,
        SkillValueExpression interval
) {
    private static final Codec<SkillCondition> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SkillConditionType.CODEC.fieldOf("type").forGetter(SkillCondition::type),
            SkillValueExpression.CODEC.optionalFieldOf("interval", SkillValueExpression.constant(1.0))
                    .forGetter(SkillCondition::interval),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("map", Map.of())
                    .forGetter(SkillCondition::legacyParameters)
    ).apply(instance, SkillCondition::fromCodec));

    /**
     * Validated codec used to decode typed rule conditions.
     */
    public static final Codec<SkillCondition> CODEC = BASE_CODEC.validate(SkillCondition::validate);

    public SkillCondition {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(interval, "interval");
    }

    /**
     * Compatibility constructor that accepts the legacy flat parameter map shape.
     *
     * @param type condition discriminant
     * @param parameters legacy flat parameter map
     */
    public SkillCondition(SkillConditionType type, Map<String, String> parameters) {
        this(type, readInterval(parameters));
    }

    /**
     * Creates an on-spell-cast condition.
     *
     * @return on-spell-cast condition
     */
    public static SkillCondition onSpellCast() {
        return new SkillCondition(SkillConditionType.ON_SPELL_CAST, SkillValueExpression.constant(1.0));
    }

    /**
     * Creates an on-hit condition.
     *
     * @return on-hit condition
     */
    public static SkillCondition onHit() {
        return new SkillCondition(SkillConditionType.ON_HIT, SkillValueExpression.constant(1.0));
    }

    /**
     * Creates an on-entity-expire condition.
     *
     * @return on-entity-expire condition
     */
    public static SkillCondition onEntityExpire() {
        return new SkillCondition(SkillConditionType.ON_ENTITY_EXPIRE, SkillValueExpression.constant(1.0));
    }

    /**
     * Creates a tick interval condition.
     *
     * @param interval positive tick interval expression
     * @return x-ticks condition
     */
    public static SkillCondition everyTicks(SkillValueExpression interval) {
        return new SkillCondition(SkillConditionType.X_TICKS_CONDITION, interval);
    }

    /**
     * Returns a legacy parameter-map view of this typed condition.
     *
     * @return compatibility parameter map for existing runtime code
     */
    public Map<String, String> parameters() {
        Map<String, String> parameters = new LinkedHashMap<>();
        if (type == SkillConditionType.X_TICKS_CONDITION) {
            String serialized = serializeExpression(interval);
            parameters.put("x_ticks", serialized);
            parameters.put("ticks", serialized);
            parameters.put("tick_rate", serialized);
        }
        return Map.copyOf(parameters);
    }

    private Map<String, String> legacyParameters() {
        return Map.of();
    }

    private static SkillCondition fromCodec(
            SkillConditionType type,
            SkillValueExpression interval,
            Map<String, String> legacyParameters
    ) {
        if (type != SkillConditionType.X_TICKS_CONDITION) {
            return new SkillCondition(type, SkillValueExpression.constant(1.0));
        }

        if (interval.isConstant() && interval.constant() == 1.0) {
            SkillValueExpression legacyInterval = readInterval(legacyParameters);
            if (!legacyInterval.isConstant() || legacyInterval.constant() != 1.0) {
                interval = legacyInterval;
            }
        }
        return new SkillCondition(type, interval);
    }

    private static DataResult<SkillCondition> validate(SkillCondition condition) {
        if (condition.type() != SkillConditionType.X_TICKS_CONDITION) {
            return DataResult.success(condition);
        }
        return DataResult.success(condition);
    }

    private static SkillValueExpression readInterval(Map<String, String> parameters) {
        String raw = parameters.get("x_ticks");
        if (raw == null || raw.isBlank()) {
            raw = parameters.get("tick_rate");
        }
        if (raw == null || raw.isBlank()) {
            raw = parameters.get("ticks");
        }
        if (raw == null || raw.isBlank()) {
            return SkillValueExpression.constant(1.0);
        }

        try {
            return SkillValueExpression.constant(Double.parseDouble(raw));
        } catch (NumberFormatException exception) {
            Identifier reference = Identifier.tryParse(raw);
            if (reference != null) {
                return SkillValueExpression.reference(reference);
            }
            return SkillValueExpression.constant(1.0);
        }
    }

    private static String serializeExpression(SkillValueExpression expression) {
        if (expression.isConstant()) {
            return Double.toString(expression.constant());
        }
        if (expression.isStat()) {
            return expression.statId().toString();
        }
        return expression.referenceId();
    }
}
