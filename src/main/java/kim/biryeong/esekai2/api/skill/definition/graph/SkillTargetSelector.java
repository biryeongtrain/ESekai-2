package kim.biryeong.esekai2.api.skill.definition.graph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.skill.value.SkillValueExpression;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Typed target selector node that determines which entities are affected by one action route.
 *
 * @param type target addressing strategy
 * @param radius area radius used by {@link SkillTargetType#AOE}
 * @param enPreds selector-local predicates evaluated before target resolution
 */
public record SkillTargetSelector(
        SkillTargetType type,
        SkillValueExpression radius,
        List<SkillPredicate> enPreds
) {
    private static final Codec<SkillTargetSelector> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SkillTargetType.CODEC.fieldOf("type").forGetter(SkillTargetSelector::type),
            SkillValueExpression.CODEC.optionalFieldOf("radius", SkillValueExpression.constant(3.0))
                    .forGetter(SkillTargetSelector::radius),
            SkillPredicate.CODEC.listOf().optionalFieldOf("en_preds", List.of()).forGetter(SkillTargetSelector::enPreds),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("map", Map.of())
                    .forGetter(SkillTargetSelector::legacyParameters)
    ).apply(instance, SkillTargetSelector::fromCodec));

    /**
     * Validated codec used to decode typed target selector entries.
     */
    public static final Codec<SkillTargetSelector> CODEC = BASE_CODEC.validate(SkillTargetSelector::validate);

    /**
     * Default selector used for implicit on-cast single-target behavior.
     */
    public static final SkillTargetSelector DEFAULT_SELF = self();

    public SkillTargetSelector {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(radius, "radius");
        Objects.requireNonNull(enPreds, "enPreds");
        enPreds = List.copyOf(enPreds);
    }

    /**
     * Compatibility constructor that accepts the legacy flat parameter map shape.
     *
     * @param type target addressing strategy
     * @param parameters legacy flat parameter map
     */
    public SkillTargetSelector(SkillTargetType type, Map<String, String> parameters) {
        this(type, readRadius(type, parameters), List.of());
    }

    /**
     * Creates a self selector.
     *
     * @return self selector
     */
    public static SkillTargetSelector self() {
        return new SkillTargetSelector(SkillTargetType.SELF, SkillValueExpression.constant(0.0), List.of());
    }

    /**
     * Creates a target selector.
     *
     * @return target selector
     */
    public static SkillTargetSelector target() {
        return new SkillTargetSelector(SkillTargetType.TARGET, SkillValueExpression.constant(0.0), List.of());
    }

    /**
     * Creates an area selector.
     *
     * @param radius non-negative area radius expression
     * @return area selector
     */
    public static SkillTargetSelector aoe(SkillValueExpression radius) {
        return new SkillTargetSelector(SkillTargetType.AOE, radius, List.of());
    }

    /**
     * Returns a legacy parameter-map view of this typed selector.
     *
     * @return compatibility parameter map for existing runtime code
     */
    public Map<String, String> parameters() {
        Map<String, String> parameters = new LinkedHashMap<>();
        if (type == SkillTargetType.AOE) {
            parameters.put("radius", serializeExpression(radius));
        }
        return Map.copyOf(parameters);
    }

    private static DataResult<SkillTargetSelector> validate(SkillTargetSelector selector) {
        if (selector.type() != SkillTargetType.AOE) {
            return DataResult.success(selector);
        }
        return DataResult.success(selector);
    }

    private Map<String, String> legacyParameters() {
        return Map.of();
    }

    private static SkillTargetSelector fromCodec(
            SkillTargetType type,
            SkillValueExpression radius,
            List<SkillPredicate> enPreds,
            Map<String, String> legacyParameters
    ) {
        SkillValueExpression resolvedRadius = type == SkillTargetType.AOE ? radius : SkillValueExpression.constant(0.0);
        if (type == SkillTargetType.AOE && resolvedRadius.isConstant() && resolvedRadius.constant() == 3.0) {
            String rawRadius = legacyParameters.get("radius");
            if (rawRadius != null && !rawRadius.isBlank()) {
                resolvedRadius = readLegacyRadius(rawRadius);
            }
        }
        return new SkillTargetSelector(type, resolvedRadius, enPreds);
    }

    private static SkillValueExpression readRadius(SkillTargetType type, Map<String, String> parameters) {
        if (type != SkillTargetType.AOE) {
            return SkillValueExpression.constant(0.0);
        }

        String rawRadius = parameters.get("radius");
        if (rawRadius == null || rawRadius.isBlank()) {
            return SkillValueExpression.constant(3.0);
        }

        return readLegacyRadius(rawRadius);
    }

    private static SkillValueExpression readLegacyRadius(String rawRadius) {
        try {
            return SkillValueExpression.constant(Double.parseDouble(rawRadius));
        } catch (NumberFormatException exception) {
            Identifier reference = Identifier.tryParse(rawRadius);
            if (reference != null) {
                return SkillValueExpression.reference(reference);
            }
            return SkillValueExpression.constant(3.0);
        }
    }

    private static String serializeExpression(SkillValueExpression expression) {
        if (expression.isConstant()) {
            return Double.toString(expression.constant());
        }
        if (expression.isStat()) {
            return expression.statId().toString();
        }
        if (expression.isReference()) {
            return expression.referenceId();
        }
        return expression.type().serializedName();
    }
}
