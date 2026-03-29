package kim.biryeong.esekai2.api.skill.definition.graph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.skill.execution.SkillExecutionContext;
import kim.biryeong.esekai2.api.skill.value.SkillValueExpression;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Runtime predicate evaluated by prepared routes, selectors, and actions.
 *
 * @param type predicate discriminant
 * @param chance chance payload used by {@link SkillPredicateType#RANDOM_CHANCE}
 */
public record SkillPredicate(
        SkillPredicateType type,
        SkillValueExpression chance
) {
    private static final Codec<SkillPredicate> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SkillPredicateType.CODEC.fieldOf("type").forGetter(SkillPredicate::type),
            SkillValueExpression.CODEC.optionalFieldOf("chance", SkillValueExpression.constant(1.0))
                    .forGetter(SkillPredicate::chance),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("map", Map.of())
                    .forGetter(SkillPredicate::legacyParameters)
    ).apply(instance, SkillPredicate::fromCodec));

    /**
     * Validated codec used to decode typed predicate nodes.
     */
    public static final Codec<SkillPredicate> CODEC = BASE_CODEC.validate(SkillPredicate::validate);

    public SkillPredicate {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(chance, "chance");
    }

    /**
     * Compatibility constructor that accepts the legacy flat parameter map shape.
     *
     * @param type predicate discriminant
     * @param parameters legacy flat parameter map
     */
    public SkillPredicate(SkillPredicateType type, Map<String, String> parameters) {
        this(type, readChance(parameters));
    }

    /**
     * Creates an unconditional predicate.
     *
     * @return always predicate
     */
    public static SkillPredicate always() {
        return new SkillPredicate(SkillPredicateType.ALWAYS, SkillValueExpression.constant(1.0));
    }

    /**
     * Creates a target-presence predicate.
     *
     * @return has-target predicate
     */
    public static SkillPredicate hasTarget() {
        return new SkillPredicate(SkillPredicateType.HAS_TARGET, SkillValueExpression.constant(1.0));
    }

    /**
     * Creates a random chance predicate.
     *
     * @param chance chance payload in the inclusive range [0, 1]
     * @return random chance predicate
     */
    public static SkillPredicate randomChance(SkillValueExpression chance) {
        return new SkillPredicate(SkillPredicateType.RANDOM_CHANCE, chance);
    }

    /**
     * Returns a legacy parameter-map view of this typed predicate.
     *
     * @return compatibility parameter map for existing runtime code
     */
    public Map<String, String> parameters() {
        Map<String, String> parameters = new LinkedHashMap<>();
        if (type == SkillPredicateType.RANDOM_CHANCE) {
            parameters.put("chance", serializeExpression(chance));
        }
        return Map.copyOf(parameters);
    }

    private Map<String, String> legacyParameters() {
        return Map.of();
    }

    private static SkillPredicate fromCodec(
            SkillPredicateType type,
            SkillValueExpression chance,
            Map<String, String> legacyParameters
    ) {
        if (type != SkillPredicateType.RANDOM_CHANCE) {
            return new SkillPredicate(type, SkillValueExpression.constant(1.0));
        }

        if (chance.isConstant() && chance.constant() == 1.0) {
            SkillValueExpression legacyChance = readChance(legacyParameters);
            if (!legacyChance.isConstant() || legacyChance.constant() != 1.0) {
                chance = legacyChance;
            }
        }
        return new SkillPredicate(type, chance);
    }

    private static DataResult<SkillPredicate> validate(SkillPredicate predicate) {
        return DataResult.success(predicate);
    }

    /**
     * Evaluates this predicate against the current runtime execution context.
     *
     * @param context current skill execution snapshot
     * @return {@code true} when the predicate allows execution
     */
    public boolean matches(SkillExecutionContext context) {
        Objects.requireNonNull(context, "context");

        return switch (type) {
            case ALWAYS -> true;
            case HAS_TARGET -> context.target().isPresent();
            case RANDOM_CHANCE -> matchesRandomChance(context);
        };
    }

    private boolean matchesRandomChance(SkillExecutionContext context) {
        double resolvedChance = chance.resolve(context.preparedUse().useContext());
        if (!Double.isFinite(resolvedChance) || resolvedChance < 0.0 || resolvedChance > 1.0) {
            return false;
        }

        return context.preparedUse().useContext().hitRoll() < resolvedChance;
    }

    private static SkillValueExpression readChance(Map<String, String> parameters) {
        String raw = parameters.get("chance");
        if (raw == null || raw.isBlank()) {
            return SkillValueExpression.constant(1.0);
        }

        try {
            return SkillValueExpression.constant(Double.parseDouble(raw));
        } catch (NumberFormatException exception) {
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
