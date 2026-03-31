package kim.biryeong.esekai2.api.skill.definition.graph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.skill.execution.SkillExecutionContext;
import kim.biryeong.esekai2.api.skill.value.SkillValueExpression;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Runtime predicate evaluated by prepared routes, selectors, and actions.
 *
 * @param type predicate discriminant
 * @param chance chance payload used by {@link SkillPredicateType#RANDOM_CHANCE}
 * @param effectId mob effect id shorthand payload used by {@link SkillPredicateType#HAS_EFFECT}
 * @param effectIds mob effect id list payload used by compound {@link SkillPredicateType#HAS_EFFECT} checks
 * @param match match policy used by {@link SkillPredicateType#HAS_EFFECT}
 * @param negate whether {@link SkillPredicateType#HAS_EFFECT} should invert its final result
 * @param subject entity subject inspected by {@link SkillPredicateType#HAS_EFFECT}
 */
public record SkillPredicate(
        SkillPredicateType type,
        SkillValueExpression chance,
        String effectId,
        List<String> effectIds,
        SkillPredicateMatchMode match,
        boolean negate,
        SkillPredicateSubject subject
) {
    private static final Codec<SkillPredicate> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SkillPredicateType.CODEC.fieldOf("type").forGetter(SkillPredicate::type),
            SkillValueExpression.CODEC.optionalFieldOf("chance", SkillValueExpression.constant(1.0))
                    .forGetter(SkillPredicate::chance),
            Codec.STRING.optionalFieldOf("effect_id", "").forGetter(SkillPredicate::effectId),
            Codec.STRING.listOf().optionalFieldOf("effect_ids", List.of()).forGetter(SkillPredicate::effectIds),
            SkillPredicateMatchMode.CODEC.optionalFieldOf("match", SkillPredicateMatchMode.ANY_OF)
                    .forGetter(SkillPredicate::match),
            Codec.BOOL.optionalFieldOf("negate", false).forGetter(SkillPredicate::negate),
            SkillPredicateSubject.CODEC.optionalFieldOf("subject", SkillPredicateSubject.PRIMARY_TARGET)
                    .forGetter(SkillPredicate::subject),
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
        Objects.requireNonNull(effectId, "effectId");
        Objects.requireNonNull(effectIds, "effectIds");
        effectIds = dedupeEffectIds(effectIds);
        Objects.requireNonNull(match, "match");
        Objects.requireNonNull(subject, "subject");
    }

    /**
     * Compatibility constructor retaining the existing typed shorthand surface.
     *
     * @param type predicate discriminant
     * @param chance chance payload
     * @param effectId mob effect id shorthand
     * @param subject entity subject
     */
    public SkillPredicate(
            SkillPredicateType type,
            SkillValueExpression chance,
            String effectId,
            SkillPredicateSubject subject
    ) {
        this(type, chance, effectId, List.of(), SkillPredicateMatchMode.ANY_OF, false, subject);
    }

    /**
     * Compatibility constructor that accepts the legacy flat parameter map shape.
     *
     * @param type predicate discriminant
     * @param parameters legacy flat parameter map
     */
    public SkillPredicate(SkillPredicateType type, Map<String, String> parameters) {
        this(type, readChance(parameters), readEffectId(parameters), readEffectIds(parameters), readMatch(parameters), readNegate(parameters), readSubject(parameters));
    }

    /**
     * Creates an unconditional predicate.
     *
     * @return always predicate
     */
    public static SkillPredicate always() {
        return new SkillPredicate(SkillPredicateType.ALWAYS, SkillValueExpression.constant(1.0), "", List.of(), SkillPredicateMatchMode.ANY_OF, false, SkillPredicateSubject.PRIMARY_TARGET);
    }

    /**
     * Creates a target-presence predicate.
     *
     * @return has-target predicate
     */
    public static SkillPredicate hasTarget() {
        return new SkillPredicate(SkillPredicateType.HAS_TARGET, SkillValueExpression.constant(1.0), "", List.of(), SkillPredicateMatchMode.ANY_OF, false, SkillPredicateSubject.PRIMARY_TARGET);
    }

    /**
     * Creates a target-effect predicate.
     *
     * @param effectId required active effect id
     * @param subject entity subject to inspect
     * @return has-effect predicate
     */
    public static SkillPredicate hasEffect(Identifier effectId, SkillPredicateSubject subject) {
        return hasEffects(List.of(effectId), subject, SkillPredicateMatchMode.ANY_OF, false);
    }

    /**
     * Creates a compound target-effect predicate.
     *
     * @param effectIds required active effect ids
     * @param subject entity subject to inspect
     * @param match compound match mode
     * @param negate whether to invert the final result
     * @return has-effect predicate
     */
    public static SkillPredicate hasEffects(
            List<Identifier> effectIds,
            SkillPredicateSubject subject,
            SkillPredicateMatchMode match,
            boolean negate
    ) {
        Objects.requireNonNull(effectIds, "effectIds");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(match, "match");

        List<String> serializedEffectIds = new ArrayList<>(effectIds.size());
        for (Identifier effectId : effectIds) {
            Objects.requireNonNull(effectId, "effectId");
            serializedEffectIds.add(effectId.toString());
        }

        String shorthandEffectId = serializedEffectIds.size() == 1 ? serializedEffectIds.getFirst() : "";
        return new SkillPredicate(SkillPredicateType.HAS_EFFECT, SkillValueExpression.constant(1.0), shorthandEffectId, serializedEffectIds, match, negate, subject);
    }

    /**
     * Creates a random chance predicate.
     *
     * @param chance chance payload in the inclusive range [0, 1]
     * @return random chance predicate
     */
    public static SkillPredicate randomChance(SkillValueExpression chance) {
        return new SkillPredicate(SkillPredicateType.RANDOM_CHANCE, chance, "", List.of(), SkillPredicateMatchMode.ANY_OF, false, SkillPredicateSubject.PRIMARY_TARGET);
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
        } else if (type == SkillPredicateType.HAS_EFFECT) {
            List<String> resolvedEffectIds = resolvedEffectIds();
            if (resolvedEffectIds.size() == 1) {
                parameters.put("effect_id", resolvedEffectIds.getFirst());
            } else if (!resolvedEffectIds.isEmpty()) {
                parameters.put("effect_ids", String.join(",", resolvedEffectIds));
            }
            if (match != SkillPredicateMatchMode.ANY_OF) {
                parameters.put("match", match.serializedName());
            }
            if (negate) {
                parameters.put("negate", Boolean.toString(true));
            }
            parameters.put("subject", subject.serializedName());
        }
        return Map.copyOf(parameters);
    }

    private Map<String, String> legacyParameters() {
        return Map.of();
    }

    private static SkillPredicate fromCodec(
            SkillPredicateType type,
            SkillValueExpression chance,
            String effectId,
            List<String> effectIds,
            SkillPredicateMatchMode match,
            boolean negate,
            SkillPredicateSubject subject,
            Map<String, String> legacyParameters
    ) {
        if (type == SkillPredicateType.RANDOM_CHANCE) {
            if (chance.isConstant() && chance.constant() == 1.0) {
                SkillValueExpression legacyChance = readChance(legacyParameters);
                if (!legacyChance.isConstant() || legacyChance.constant() != 1.0) {
                    chance = legacyChance;
                }
            }
            return new SkillPredicate(type, chance, "", List.of(), SkillPredicateMatchMode.ANY_OF, false, SkillPredicateSubject.PRIMARY_TARGET);
        }

        if (type == SkillPredicateType.HAS_EFFECT) {
            if (effectId.isBlank()) {
                effectId = readEffectId(legacyParameters);
            }
            if (effectIds.isEmpty()) {
                effectIds = readEffectIds(legacyParameters);
            }
            if (match == SkillPredicateMatchMode.ANY_OF) {
                match = readMatch(legacyParameters);
            }
            if (!negate) {
                negate = readNegate(legacyParameters);
            }
            if (subject == SkillPredicateSubject.PRIMARY_TARGET) {
                subject = readSubject(legacyParameters);
            }
            return new SkillPredicate(type, SkillValueExpression.constant(1.0), effectId, effectIds, match, negate, subject);
        }

        return new SkillPredicate(type, SkillValueExpression.constant(1.0), "", List.of(), SkillPredicateMatchMode.ANY_OF, false, SkillPredicateSubject.PRIMARY_TARGET);
    }

    private static DataResult<SkillPredicate> validate(SkillPredicate predicate) {
        if (predicate.type() == SkillPredicateType.HAS_EFFECT) {
            List<String> resolvedEffectIds = predicate.resolvedEffectIds();
            if (resolvedEffectIds.isEmpty()) {
                return DataResult.error(() -> "has_effect predicate requires effect_id or effect_ids");
            }

            for (String effectId : resolvedEffectIds) {
                if (Identifier.tryParse(effectId) == null) {
                    return DataResult.error(() -> "has_effect predicate requires valid effect ids: " + effectId);
                }
            }
        } else if (predicate.match() != SkillPredicateMatchMode.ANY_OF || predicate.negate()) {
            return DataResult.error(() -> "match and negate are only supported by has_effect predicates");
        }
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
            case HAS_EFFECT -> matchesHasEffect(context);
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

    private boolean matchesHasEffect(SkillExecutionContext context) {
        List<String> resolvedEffectIds = resolvedEffectIds();
        if (resolvedEffectIds.isEmpty()) {
            return false;
        }

        Entity entity = switch (subject) {
            case PRIMARY_TARGET -> context.primaryTarget();
            case SELF -> context.source();
            case TARGET -> context.target().orElse(null);
        };
        if (!(entity instanceof LivingEntity livingEntity)) {
            return false;
        }

        boolean matches = switch (match) {
            case ANY_OF -> resolvedEffectIds.stream().anyMatch(effectId -> hasActiveEffect(livingEntity, effectId));
            case ALL_OF -> resolvedEffectIds.stream().allMatch(effectId -> hasActiveEffect(livingEntity, effectId));
        };
        return negate ? !matches : matches;
    }

    /**
     * Returns the resolved has-effect target list using {@code effect_ids} first and {@code effect_id} as a shorthand fallback.
     *
     * @return resolved ordered effect id list
     */
    public List<String> resolvedEffectIds() {
        if (!effectIds.isEmpty()) {
            return dedupeEffectIds(effectIds);
        }
        if (effectId.isBlank()) {
            return List.of();
        }
        return List.of(effectId);
    }

    private static boolean hasActiveEffect(LivingEntity livingEntity, String effectId) {
        Identifier parsedEffectId = Identifier.tryParse(effectId);
        if (parsedEffectId == null) {
            return false;
        }

        MobEffect effect = BuiltInRegistries.MOB_EFFECT.getOptional(parsedEffectId).orElse(null);
        if (effect == null) {
            return false;
        }

        return livingEntity.getEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect)) != null;
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

    private static String readEffectId(Map<String, String> parameters) {
        String raw = parameters.get("effect_id");
        return raw == null ? "" : raw;
    }

    private static List<String> readEffectIds(Map<String, String> parameters) {
        return parseEffectIds(parameters.get("effect_ids"));
    }

    private static SkillPredicateSubject readSubject(Map<String, String> parameters) {
        String raw = parameters.get("subject");
        if (raw == null || raw.isBlank()) {
            return SkillPredicateSubject.PRIMARY_TARGET;
        }

        for (SkillPredicateSubject subject : SkillPredicateSubject.values()) {
            if (subject.serializedName().equals(raw)) {
                return subject;
            }
        }
        return SkillPredicateSubject.PRIMARY_TARGET;
    }

    private static SkillPredicateMatchMode readMatch(Map<String, String> parameters) {
        String raw = parameters.get("match");
        if (raw == null || raw.isBlank()) {
            return SkillPredicateMatchMode.ANY_OF;
        }

        SkillPredicateMatchMode match = SkillPredicateMatchMode.fromSerializedName(raw);
        return match == null ? SkillPredicateMatchMode.ANY_OF : match;
    }

    private static boolean readNegate(Map<String, String> parameters) {
        String raw = parameters.get("negate");
        return raw != null && Boolean.parseBoolean(raw);
    }

    private static List<String> parseEffectIds(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }

        List<String> parsed = new ArrayList<>();
        for (String token : raw.split(",")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                parsed.add(trimmed);
            }
        }
        return dedupeEffectIds(parsed);
    }

    private static List<String> dedupeEffectIds(List<String> effectIds) {
        LinkedHashSet<String> deduped = new LinkedHashSet<>(effectIds.size());
        for (String effectId : effectIds) {
            if (effectId != null && !effectId.isBlank()) {
                deduped.add(effectId);
            }
        }
        return List.copyOf(deduped);
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
