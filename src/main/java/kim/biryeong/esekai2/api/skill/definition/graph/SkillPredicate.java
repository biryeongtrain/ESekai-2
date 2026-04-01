package kim.biryeong.esekai2.api.skill.definition.graph;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.player.resource.PlayerResourceIds;
import kim.biryeong.esekai2.api.player.resource.PlayerResources;
import kim.biryeong.esekai2.api.skill.execution.SkillExecutionContext;
import kim.biryeong.esekai2.api.skill.value.SkillValueExpression;
import kim.biryeong.esekai2.impl.skill.execution.PlayerSkillRuntimeStateResolver;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
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
 * @param subject entity subject inspected by {@link SkillPredicateType#HAS_EFFECT} and {@link SkillPredicateType#HAS_RESOURCE}
 * @param resource resource id inspected by {@link SkillPredicateType#HAS_RESOURCE}
 * @param amount threshold used by {@link SkillPredicateType#HAS_RESOURCE}
 */
public record SkillPredicate(
        SkillPredicateType type,
        SkillValueExpression chance,
        String effectId,
        List<String> effectIds,
        SkillPredicateMatchMode match,
        boolean negate,
        SkillPredicateSubject subject,
        String resource,
        SkillValueExpression amount
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
            Codec.STRING.optionalFieldOf("resource", "").forGetter(SkillPredicate::resource),
            SkillValueExpression.CODEC.optionalFieldOf("amount", SkillValueExpression.constant(0.0))
                    .forGetter(SkillPredicate::amount),
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
        Objects.requireNonNull(resource, "resource");
        Objects.requireNonNull(amount, "amount");
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
        this(type, chance, effectId, List.of(), SkillPredicateMatchMode.ANY_OF, false, subject, "", SkillValueExpression.constant(0.0));
    }

    /**
     * Compatibility constructor that accepts the legacy flat parameter map shape.
     *
     * @param type predicate discriminant
     * @param parameters legacy flat parameter map
     */
    public SkillPredicate(SkillPredicateType type, Map<String, String> parameters) {
        this(
                type,
                readChance(parameters),
                readEffectId(parameters),
                readEffectIds(parameters),
                readMatch(parameters),
                readNegate(parameters),
                readSubject(parameters),
                readResource(parameters),
                readAmount(parameters)
        );
    }

    /**
     * Creates an unconditional predicate.
     *
     * @return always predicate
     */
    public static SkillPredicate always() {
        return new SkillPredicate(SkillPredicateType.ALWAYS, SkillValueExpression.constant(1.0), "", List.of(), SkillPredicateMatchMode.ANY_OF, false, SkillPredicateSubject.PRIMARY_TARGET, "", SkillValueExpression.constant(0.0));
    }

    /**
     * Creates a target-presence predicate.
     *
     * @return has-target predicate
     */
    public static SkillPredicate hasTarget() {
        return new SkillPredicate(SkillPredicateType.HAS_TARGET, SkillValueExpression.constant(1.0), "", List.of(), SkillPredicateMatchMode.ANY_OF, false, SkillPredicateSubject.PRIMARY_TARGET, "", SkillValueExpression.constant(0.0));
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
        return new SkillPredicate(SkillPredicateType.HAS_EFFECT, SkillValueExpression.constant(1.0), shorthandEffectId, serializedEffectIds, match, negate, subject, "", SkillValueExpression.constant(0.0));
    }

    /**
     * Creates a random chance predicate.
     *
     * @param chance chance payload in the inclusive range [0, 1]
     * @return random chance predicate
     */
    public static SkillPredicate randomChance(SkillValueExpression chance) {
        return new SkillPredicate(SkillPredicateType.RANDOM_CHANCE, chance, "", List.of(), SkillPredicateMatchMode.ANY_OF, false, SkillPredicateSubject.PRIMARY_TARGET, "", SkillValueExpression.constant(0.0));
    }

    /**
     * Creates a resource-threshold predicate.
     *
     * @param resource resource id that should be checked
     * @param amount minimum current amount required for the predicate to pass
     * @param subject entity subject to inspect
     * @return has-resource predicate
     */
    public static SkillPredicate hasResource(String resource, SkillValueExpression amount, SkillPredicateSubject subject) {
        Objects.requireNonNull(resource, "resource");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(subject, "subject");
        return new SkillPredicate(SkillPredicateType.HAS_RESOURCE, SkillValueExpression.constant(1.0), "", List.of(), SkillPredicateMatchMode.ANY_OF, false, subject, resource, amount);
    }

    /**
     * Creates a runtime predicate that passes when the current prepared skill is not on cooldown.
     *
     * @return cooldown-ready predicate
     */
    public static SkillPredicate cooldownReady() {
        return new SkillPredicate(SkillPredicateType.COOLDOWN_READY, SkillValueExpression.constant(1.0), "", List.of(), SkillPredicateMatchMode.ANY_OF, false, SkillPredicateSubject.PRIMARY_TARGET, "", SkillValueExpression.constant(0.0));
    }

    /**
     * Creates a runtime predicate that passes when the current prepared skill has at least one available charge.
     *
     * @return has-charges predicate
     */
    public static SkillPredicate hasCharges() {
        return new SkillPredicate(SkillPredicateType.HAS_CHARGES, SkillValueExpression.constant(1.0), "", List.of(), SkillPredicateMatchMode.ANY_OF, false, SkillPredicateSubject.PRIMARY_TARGET, "", SkillValueExpression.constant(0.0));
    }

    /**
     * Creates a runtime predicate that passes when the current prepared skill still has a burst follow-up cast available.
     *
     * @return has-burst-followup predicate
     */
    public static SkillPredicate hasBurstFollowup() {
        return new SkillPredicate(SkillPredicateType.HAS_BURST_FOLLOWUP, SkillValueExpression.constant(1.0), "", List.of(), SkillPredicateMatchMode.ANY_OF, false, SkillPredicateSubject.PRIMARY_TARGET, "", SkillValueExpression.constant(0.0));
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
        } else if (type == SkillPredicateType.HAS_RESOURCE) {
            parameters.put("resource", resource);
            parameters.put("amount", serializeExpression(amount));
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
            String resource,
            SkillValueExpression amount,
            Map<String, String> legacyParameters
    ) {
        if (type == SkillPredicateType.RANDOM_CHANCE) {
            if (chance.isConstant() && chance.constant() == 1.0) {
                SkillValueExpression legacyChance = readChance(legacyParameters);
                if (!legacyChance.isConstant() || legacyChance.constant() != 1.0) {
                    chance = legacyChance;
                }
            }
            return new SkillPredicate(type, chance, "", List.of(), SkillPredicateMatchMode.ANY_OF, false, SkillPredicateSubject.PRIMARY_TARGET, "", SkillValueExpression.constant(0.0));
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
            return new SkillPredicate(type, SkillValueExpression.constant(1.0), effectId, effectIds, match, negate, subject, "", SkillValueExpression.constant(0.0));
        }

        if (type == SkillPredicateType.HAS_RESOURCE) {
            if (resource.isBlank()) {
                resource = readResource(legacyParameters);
            }
            if (amount.isConstant() && amount.constant() == 0.0) {
                SkillValueExpression legacyAmount = readAmount(legacyParameters);
                if (!legacyAmount.isConstant() || legacyAmount.constant() != 0.0) {
                    amount = legacyAmount;
                }
            }
            if (subject == SkillPredicateSubject.PRIMARY_TARGET) {
                subject = readSubject(legacyParameters);
            }
            return new SkillPredicate(type, SkillValueExpression.constant(1.0), "", List.of(), SkillPredicateMatchMode.ANY_OF, false, subject, resource, amount);
        }

        return new SkillPredicate(type, SkillValueExpression.constant(1.0), "", List.of(), SkillPredicateMatchMode.ANY_OF, false, SkillPredicateSubject.PRIMARY_TARGET, "", SkillValueExpression.constant(0.0));
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
            if (!predicate.resource().isBlank()) {
                return DataResult.error(() -> "resource is only supported by has_resource predicates");
            }
            if (!predicate.amount().isConstant() || predicate.amount().constant() != 0.0) {
                return DataResult.error(() -> "amount is only supported by has_resource predicates");
            }
        } else if (predicate.type() == SkillPredicateType.HAS_RESOURCE) {
            if (!PlayerResourceIds.isUsable(predicate.resource())) {
                return DataResult.error(() -> "has_resource predicate requires resource");
            }
            if (predicate.amount().isConstant()) {
                double constantAmount = predicate.amount().constant();
                if (!Double.isFinite(constantAmount) || constantAmount < 0.0) {
                    return DataResult.error(() -> "has_resource predicate requires amount >= 0");
                }
            }
            if (!predicate.effectId().isBlank() || !predicate.effectIds().isEmpty()) {
                return DataResult.error(() -> "effect_id and effect_ids are only supported by has_effect predicates");
            }
            if (predicate.match() != SkillPredicateMatchMode.ANY_OF || predicate.negate()) {
                return DataResult.error(() -> "match and negate are only supported by has_effect predicates");
            }
        } else if (predicate.match() != SkillPredicateMatchMode.ANY_OF || predicate.negate()) {
            return DataResult.error(() -> "match and negate are only supported by has_effect predicates");
        } else if (!predicate.effectId().isBlank() || !predicate.effectIds().isEmpty()) {
            return DataResult.error(() -> "effect_id and effect_ids are only supported by has_effect predicates");
        } else if (!predicate.resource().isBlank()) {
            return DataResult.error(() -> "resource is only supported by has_resource predicates");
        } else if (!predicate.amount().isConstant() || predicate.amount().constant() != 0.0) {
            return DataResult.error(() -> "amount is only supported by has_resource predicates");
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
            case HAS_RESOURCE -> matchesHasResource(context);
            case RANDOM_CHANCE -> matchesRandomChance(context);
            case COOLDOWN_READY -> matchesCooldownReady(context);
            case HAS_CHARGES -> matchesHasCharges(context);
            case HAS_BURST_FOLLOWUP -> matchesHasBurstFollowup(context);
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

        Entity entity = resolveSubjectEntity(context);
        if (!(entity instanceof LivingEntity livingEntity)) {
            return false;
        }

        boolean matches = switch (match) {
            case ANY_OF -> resolvedEffectIds.stream().anyMatch(effectId -> hasActiveEffect(livingEntity, effectId));
            case ALL_OF -> resolvedEffectIds.stream().allMatch(effectId -> hasActiveEffect(livingEntity, effectId));
        };
        return negate ? !matches : matches;
    }

    private boolean matchesHasResource(SkillExecutionContext context) {
        double requiredAmount = amount.resolve(context.preparedUse().useContext());
        if (!Double.isFinite(requiredAmount) || requiredAmount < 0.0) {
            return false;
        }

        var resolved = context.preparedUse().useContext().resourceLookup().resolve(resource, subject);
        if (resolved.isPresent()) {
            return resolved.orElseThrow().currentAmount() + 1.0E-6 >= requiredAmount;
        }
        if ((subject == SkillPredicateSubject.TARGET || subject == SkillPredicateSubject.PRIMARY_TARGET)
                && context.target().isEmpty()) {
            return false;
        }

        Entity entity = resolveSubjectEntity(context);
        if (!(entity instanceof ServerPlayer player)) {
            return false;
        }
        if (!PlayerResources.supports(resource)) {
            return false;
        }

        return PlayerResources.getAmount(player, resource) + 1.0E-6 >= requiredAmount;
    }

    private boolean matchesCooldownReady(SkillExecutionContext context) {
        return PlayerSkillRuntimeStateResolver.cooldownRemainingTicks(context) <= 0L;
    }

    private boolean matchesHasCharges(SkillExecutionContext context) {
        return PlayerSkillRuntimeStateResolver.availableCharges(context) > 0;
    }

    private boolean matchesHasBurstFollowup(SkillExecutionContext context) {
        return PlayerSkillRuntimeStateResolver.burstRemaining(context) > 0;
    }

    private Entity resolveSubjectEntity(SkillExecutionContext context) {
        return switch (subject) {
            case PRIMARY_TARGET -> context.primaryTarget();
            case SELF -> context.source();
            case TARGET -> context.target().orElse(null);
        };
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

    private static String readResource(Map<String, String> parameters) {
        String raw = parameters.get("resource");
        return raw == null ? "" : raw;
    }

    private static SkillValueExpression readAmount(Map<String, String> parameters) {
        String raw = parameters.get("amount");
        if (raw == null || raw.isBlank()) {
            return SkillValueExpression.constant(0.0);
        }

        try {
            return SkillValueExpression.constant(Double.parseDouble(raw));
        } catch (NumberFormatException ignored) {
            try {
                return SkillValueExpression.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(raw))
                        .result()
                        .orElseGet(() -> {
                            Identifier parsed = Identifier.tryParse(raw);
                            return parsed == null ? SkillValueExpression.constant(0.0) : SkillValueExpression.reference(parsed);
                        });
            } catch (RuntimeException ignoredJsonFailure) {
                Identifier parsed = Identifier.tryParse(raw);
                return parsed == null ? SkillValueExpression.constant(0.0) : SkillValueExpression.reference(parsed);
            }
        }
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
        if (expression.isReference()) {
            return expression.referenceId();
        }
        return SkillValueExpression.CODEC.encodeStart(JsonOps.INSTANCE, expression)
                .result()
                .map(Object::toString)
                .orElse("0.0");
    }
}
